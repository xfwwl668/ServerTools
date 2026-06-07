package com.nezhahq.agent.tunnelruntime;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Minimal HTTP/2 connection handler for CF Tunnel.
 *
 * Architecture contract:
 *   - ONLY serveForever() / serverHandshake() may call readFrame() / readExact().
 *   - All other threads read stream data via readData(streamId) which blocks
 *     on a per-stream BlockingQueue<Optional<byte[]>>.
 *   - Optional.empty() is used as the EOF sentinel on a stream.
 *   - GOAWAY signals all queues with EOF and throws, causing serveForever to exit.
 */
final class Http2Connection {

    // -------------------------------------------------------------------------
    // Frame type constants
    // -------------------------------------------------------------------------
    private static final int FRAME_DATA          = 0x0;
    private static final int FRAME_HEADERS       = 0x1;
    private static final int FRAME_RST_STREAM    = 0x3;
    private static final int FRAME_SETTINGS      = 0x4;
    private static final int FRAME_PING          = 0x6;
    private static final int FRAME_GOAWAY        = 0x7;
    private static final int FRAME_WINDOW_UPDATE = 0x8;

    // Flag constants
    private static final int FLAG_ACK         = 0x1;
    private static final int FLAG_END_STREAM  = 0x1;
    private static final int FLAG_END_HEADERS = 0x4;

    private static final byte[] CLIENT_PREFACE =
            "PRI * HTTP/2.0\r\n\r\nSM\r\n\r\n".getBytes(StandardCharsets.US_ASCII);

    // -------------------------------------------------------------------------
    // State
    // -------------------------------------------------------------------------
    private final InputStream  in;
    private final OutputStream out;
    private final Closeable    closeable;
    private final boolean      debug;
    private final Object       writeLock = new Object();
    private final Hpack        hpack     = new Hpack();
    private final AtomicLong   lastFrameNanos = new AtomicLong(System.nanoTime());

    /** Per-stream data queues. Optional.empty() = EOF sentinel. */
    private final Map<Integer, BlockingQueue<Optional<byte[]>>> streamQueues = new ConcurrentHashMap<>();
    private final Map<Integer, Map<String, String>>             streamHeaders = new ConcurrentHashMap<>();

    private int remoteConnWindow = 65535;
    private int initialStreamWindowSize = 65535;
    private final Map<Integer, Integer> remoteStreamWindows = new ConcurrentHashMap<>();
    private volatile boolean closed = false;

    Http2Connection(InputStream in, OutputStream out, Closeable closeable, boolean debug) {
        this.in        = in;
        this.out       = out;
        this.closeable = closeable;
        this.debug     = debug;
    }

    // =========================================================================
    // Handshake (called once, before serveForever)
    // =========================================================================

    void serverHandshake() throws IOException {
        // 1. Read client connection preface (24-byte magic)
        byte[] preface = readExact(CLIENT_PREFACE.length);
        if (!Arrays.equals(preface, CLIENT_PREFACE)) {
            throw new IOException("invalid HTTP/2 client preface");
        }
        log("received HTTP/2 client preface");

        // 2. Send our server SETTINGS
        sendSettings();

        // 3. Read client SETTINGS
        Frame clientSettings = readFrame();
        if (clientSettings.type != FRAME_SETTINGS) {
            throw new IOException("expected SETTINGS, got frame type " + clientSettings.type);
        }
        log("received client SETTINGS");

        // 4. ACK client SETTINGS
        sendSettingsAck();

        // 5. Read until we see the client's SETTINGS ACK
        while (true) {
            Frame frame = readFrame();
            if (frame.type == FRAME_SETTINGS && (frame.flags & FLAG_ACK) != 0) {
                log("received SETTINGS ACK");
                break;
            }
            handleFrame(frame, null, (byte) 0);
        }
    }

    // =========================================================================
    // Main serve loop  (single reader thread — must be called from one thread only)
    // =========================================================================

    void serveForever(CfTunnelClient tunnel, byte connIndex) throws IOException {
        while (true) {
            Frame frame = readFrame();
            handleFrame(frame, tunnel, connIndex);
        }
    }

    // =========================================================================
    // Frame dispatcher
    // =========================================================================

    private void handleFrame(Frame frame, CfTunnelClient tunnel, byte connIndex) throws IOException {
        switch (frame.type) {

            case FRAME_HEADERS -> {
                Map<String, String> headers = hpack.decode(frame.payload);
                streamHeaders.put(frame.streamId, headers);
                log("conn[" + connIndex + "] stream " + frame.streamId + " HEADERS: " + headers);

                // Create the per-stream queue BEFORE starting the handler thread
                // so data arriving immediately after HEADERS is never lost.
                streamQueues.put(frame.streamId, new LinkedBlockingQueue<>());

                if (tunnel != null) {
                    String upgrade = headers.getOrDefault(
                            "cf-cloudflared-proxy-connection-upgrade", "");
                    if ("control-stream".equalsIgnoreCase(upgrade)) {
                        int sid = frame.streamId;
                        Thread t = new Thread(
                                () -> tunnel.handleControlStream(this, sid, connIndex),
                                "cf-control-stream");
                        t.setDaemon(true);
                        t.start();
                    } else {
                        tunnel.handleProxyStream(this, frame.streamId, headers);
                    }
                }

                // If this is an end-stream HEADERS, signal EOF immediately
                if ((frame.flags & FLAG_END_STREAM) != 0) {
                    enqueueEof(frame.streamId);
                }
            }

            case FRAME_DATA -> {
                // Send WINDOW_UPDATE so the edge can keep sending
                if (frame.payload.length > 0) {
                    sendWindowUpdate(0,              frame.payload.length); // connection-level
                    sendWindowUpdate(frame.streamId, frame.payload.length); // stream-level
                }

                BlockingQueue<Optional<byte[]>> q = streamQueues.computeIfAbsent(
                        frame.streamId, k -> new LinkedBlockingQueue<>());

                if (frame.payload.length > 0) {
                    try {
                        q.put(Optional.of(frame.payload));
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
                if ((frame.flags & FLAG_END_STREAM) != 0) {
                    enqueueEof(frame.streamId);
                }
            }

            case FRAME_RST_STREAM -> {
                int errCode = frame.payload.length >= 4
                        ? (((frame.payload[0] & 0xFF) << 24) | ((frame.payload[1] & 0xFF) << 16)
                           | ((frame.payload[2] & 0xFF) << 8) | (frame.payload[3] & 0xFF))
                        : 0;
                log("RST_STREAM on stream " + frame.streamId + " errorCode=0x" + Integer.toHexString(errCode));
                enqueueEof(frame.streamId);
                streamHeaders.remove(frame.streamId);
                streamQueues.remove(frame.streamId);
            }

            case FRAME_GOAWAY -> {
                // Parse last-stream-id and error code for diagnostics
                int lastStreamId = frame.payload.length >= 4
                        ? (((frame.payload[0] & 0x7F) << 24) | ((frame.payload[1] & 0xFF) << 16)
                           | ((frame.payload[2] & 0xFF) << 8) | (frame.payload[3] & 0xFF))
                        : 0;
                int errorCode = frame.payload.length >= 8
                        ? (((frame.payload[4] & 0xFF) << 24) | ((frame.payload[5] & 0xFF) << 16)
                           | ((frame.payload[6] & 0xFF) << 8) | (frame.payload[7] & 0xFF))
                        : 0;
                log("received GOAWAY lastStream=" + lastStreamId
                        + " errorCode=0x" + Integer.toHexString(errorCode));
                // Signal all waiting stream threads to wake up and exit
                closeAllQueues();
                throw new IOException("received GOAWAY (errorCode=0x" + Integer.toHexString(errorCode) + ")");
            }

            case FRAME_PING -> {
                if ((frame.flags & FLAG_ACK) == 0) {
                    sendPingAck(frame.payload);
                }
            }

            case FRAME_SETTINGS -> {
                if ((frame.flags & FLAG_ACK) == 0) {
                    for (int i = 0; i + 6 <= frame.payload.length; i += 6) {
                        int id = ((frame.payload[i] & 0xFF) << 8) | (frame.payload[i+1] & 0xFF);
                        int val = ((frame.payload[i+2] & 0xFF) << 24) | ((frame.payload[i+3] & 0xFF) << 16) | ((frame.payload[i+4] & 0xFF) << 8) | (frame.payload[i+5] & 0xFF);
                        if (id == 4) { // SETTINGS_INITIAL_WINDOW_SIZE
                            synchronized (this) {
                                int delta = val - initialStreamWindowSize;
                                initialStreamWindowSize = val;
                                for (Map.Entry<Integer, Integer> e : remoteStreamWindows.entrySet()) {
                                    e.setValue(e.getValue() + delta);
                                }
                                this.notifyAll();
                            }
                        }
                    }
                    sendSettingsAck();
                }
            }

            case FRAME_WINDOW_UPDATE -> {
                if (frame.payload.length >= 4) {
                    int increment = ((frame.payload[0] & 0x7F) << 24) | ((frame.payload[1] & 0xFF) << 16) | ((frame.payload[2] & 0xFF) << 8) | (frame.payload[3] & 0xFF);
                    if (increment > 0) {
                        synchronized (this) {
                            if (frame.streamId == 0) {
                                remoteConnWindow += increment;
                            } else {
                                remoteStreamWindows.put(frame.streamId, remoteStreamWindows.getOrDefault(frame.streamId, initialStreamWindowSize) + increment);
                            }
                            this.notifyAll();
                        }
                    }
                }
            }

            default -> log("ignoring frame type 0x" + Integer.toHexString(frame.type));
        }
    }

    // =========================================================================
    // Stream data API  (called from handler threads, NOT the reader thread)
    // =========================================================================

    /**
     * Block until a DATA payload arrives for this stream, then return it.
     * Returns {@code null} when the stream reaches EOF (RST / END_STREAM / GOAWAY).
     * Throws IOException on timeout.
     */
    byte[] readData(int streamId) throws IOException {
        BlockingQueue<Optional<byte[]>> q = streamQueues.computeIfAbsent(
                streamId, k -> new LinkedBlockingQueue<>());
        try {
            Optional<byte[]> item = q.take();
            return item.isPresent() ? item.get() : null; // null = EOF
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("interrupted reading stream " + streamId, e);
        }
    }

    void sendData(int streamId, byte[] data, boolean endStream) throws IOException {
        if (closed) throw new IOException("Connection closed");
        int offset = 0;
        while (offset < data.length) {
            int toSend = 0;
            synchronized (this) {
                while (!closed) {
                    int streamWin = remoteStreamWindows.getOrDefault(streamId, initialStreamWindowSize);
                    int maxAllowed = Math.min(remoteConnWindow, streamWin);
                    if (maxAllowed > 0) {
                        toSend = Math.min(data.length - offset, maxAllowed);
                        toSend = Math.min(toSend, 16384); // max frame size
                        remoteConnWindow -= toSend;
                        remoteStreamWindows.put(streamId, streamWin - toSend);
                        break;
                    }
                    try {
                        this.wait(100);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new IOException("interrupted waiting for window update");
                    }
                }
                if (closed) throw new IOException("Connection closed");
            }

            int flags = (offset + toSend == data.length && endStream) ? FLAG_END_STREAM : 0;
            synchronized (writeLock) {
                writeFrame(FRAME_DATA, flags, streamId, Arrays.copyOfRange(data, offset, offset + toSend));
            }
            offset += toSend;
        }
        if (data.length == 0 && endStream) {
            synchronized (writeLock) {
                writeFrame(FRAME_DATA, FLAG_END_STREAM, streamId, new byte[0]);
            }
        }
    }

    void sendResponseHeaders(int streamId, int statusCode,
                              Map<String, String> headers) throws IOException {
        if (closed) throw new IOException("Connection closed");
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        Hpack.encodeHeader(buf, ":status", String.valueOf(statusCode));
        for (Map.Entry<String, String> e : headers.entrySet()) {
            Hpack.encodeHeader(buf, e.getKey().toLowerCase(Locale.ROOT), e.getValue());
        }
        synchronized (writeLock) {
            writeFrame(FRAME_HEADERS, FLAG_END_HEADERS, streamId, buf.toByteArray());
        }
    }

    void sendRstStream(int streamId, int errorCode) throws IOException {
        if (closed) throw new IOException("Connection closed");
        byte[] payload = new byte[4];
        payload[0] = (byte) ((errorCode >> 24) & 0xFF);
        payload[1] = (byte) ((errorCode >> 16) & 0xFF);
        payload[2] = (byte) ((errorCode >>  8) & 0xFF);
        payload[3] = (byte) ( errorCode        & 0xFF);
        synchronized (writeLock) {
            writeFrame(FRAME_RST_STREAM, 0, streamId, payload);
        }
    }

    void sendPing() throws IOException {
        if (closed) throw new IOException("Connection closed");
        byte[] payload = ByteBuffer.allocate(8)
                .order(ByteOrder.BIG_ENDIAN)
                .putLong(System.nanoTime())
                .array();
        synchronized (writeLock) {
            writeFrame(FRAME_PING, 0, 0, payload);
        }
    }

    long millisSinceLastFrame() {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - lastFrameNanos.get());
    }

    boolean isClosed() {
        return closed;
    }

    // =========================================================================
    // Internal helpers
    // =========================================================================

    private void enqueueEof(int streamId) {
        BlockingQueue<Optional<byte[]>> q = streamQueues.get(streamId);
        if (q != null) {
            try {
                q.put(Optional.empty());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    public void close() {
        if (closed) {
            return;
        }
        closeAllQueues();
        try {
            closeable.close();
        } catch (IOException ignored) {
        }
    }

    private void closeAllQueues() {
        synchronized (this) {
            if (closed) {
                return;
            }
            closed = true;
            this.notifyAll();
        }
        for (BlockingQueue<Optional<byte[]>> q : streamQueues.values()) {
            try {
                q.put(Optional.empty());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private void sendSettings() throws IOException {
        synchronized (writeLock) {
            writeFrame(FRAME_SETTINGS, 0, 0, new byte[0]);
        }
    }

    private void sendSettingsAck() throws IOException {
        synchronized (writeLock) {
            writeFrame(FRAME_SETTINGS, FLAG_ACK, 0, new byte[0]);
        }
    }

    private void sendPingAck(byte[] opaqueData) throws IOException {
        synchronized (writeLock) {
            writeFrame(FRAME_PING, FLAG_ACK, 0, opaqueData);
        }
    }

    private void sendWindowUpdate(int streamId, int increment) throws IOException {
        byte[] payload = new byte[4];
        payload[0] = (byte) ((increment >> 24) & 0x7F); // reserved bit = 0
        payload[1] = (byte) ((increment >> 16) & 0xFF);
        payload[2] = (byte) ((increment >>  8) & 0xFF);
        payload[3] = (byte) ( increment        & 0xFF);
        synchronized (writeLock) {
            writeFrame(FRAME_WINDOW_UPDATE, 0, streamId, payload);
        }
    }

    // =========================================================================
    // HTTP/2 framing I/O
    // =========================================================================

    private Frame readFrame() throws IOException {
        byte[] header = readExact(9);
        int length   = ((header[0] & 0xFF) << 16) | ((header[1] & 0xFF) << 8) | (header[2] & 0xFF);
        int type     =   header[3] & 0xFF;
        int flags    =   header[4] & 0xFF;
        int streamId = ((header[5] & 0x7F) << 24) | ((header[6] & 0xFF) << 16)
                     | ((header[7] & 0xFF) <<  8) |  (header[8] & 0xFF);
        byte[] payload = length > 0 ? readExact(length) : new byte[0];
        lastFrameNanos.set(System.nanoTime());
        return new Frame(type, flags, streamId, payload);
    }

    private void writeFrame(int type, int flags, int streamId, byte[] payload) throws IOException {
        int length = payload.length;
        byte[] header = new byte[9];
        header[0] = (byte) ((length >> 16) & 0xFF);
        header[1] = (byte) ((length >>  8) & 0xFF);
        header[2] = (byte) ( length        & 0xFF);
        header[3] = (byte) type;
        header[4] = (byte) flags;
        header[5] = (byte) ((streamId >> 24) & 0x7F);
        header[6] = (byte) ((streamId >> 16) & 0xFF);
        header[7] = (byte) ((streamId >>  8) & 0xFF);
        header[8] = (byte) ( streamId        & 0xFF);
        out.write(header);
        if (length > 0) out.write(payload);
        out.flush();
    }

    private byte[] readExact(int length) throws IOException {
        byte[] buf  = new byte[length];
        int    read = 0;
        while (read < length) {
            int n = in.read(buf, read, length - read);
            if (n < 0) throw new IOException("unexpected end of stream");
            read += n;
        }
        return buf;
    }

    private void log(String message) {
        if (debug) System.out.println("H2 - " + message);
    }

    record Frame(int type, int flags, int streamId, byte[] payload) {}
}
