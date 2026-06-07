package com.nezhahq.agent.tunnelruntime;

import javax.naming.Context;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Connects to Cloudflare edge using the official tunnel protocol (HTTP/2 mode).
 * Registers as a named tunnel and forwards incoming requests to the local proxy.
 */
final class CfTunnelClient implements AutoCloseable {
    private static final int EDGE_PORT = 7844;
    private static final int CONNECT_TIMEOUT_MS = 10_000;
    private static final int HANDSHAKE_TIMEOUT_MS = 30_000;
    private static final int HEARTBEAT_INTERVAL_MS = 25_000;
    private static final int HEARTBEAT_TIMEOUT_MS = 90_000;
    private static final int RECONNECT_BASE_DELAY_MS = 2_000;
    private static final int RECONNECT_MAX_DELAY_MS = 30_000;
    private static final String H2_SNI = "h2.cftunnel.com";
    private static final String SRV_RECORD = "_v2-origintunneld._tcp.argotunnel.com";

    private final String accountTag;
    private final byte[] tunnelSecret;
    private final UUID tunnelId;
    private final String targetHost;
    private final int targetPort;
    private final String wsPath;
    private final boolean debug;
    private final AtomicBoolean closed = new AtomicBoolean();
    private final AtomicInteger questionCounter = new AtomicInteger(0);
    private final ExecutorService scheduler = Executors.newCachedThreadPool();
    private final Set<Socket> activeSockets = ConcurrentHashMap.newKeySet();
    private final Set<Http2Connection> activeConnections = ConcurrentHashMap.newKeySet();

    CfTunnelClient(String accountTag, byte[] tunnelSecret, UUID tunnelId,
                   String targetHost, int targetPort, String wsPath, boolean debug) {
        this.accountTag = accountTag;
        this.tunnelSecret = tunnelSecret;
        this.tunnelId = tunnelId;
        this.targetHost = targetHost;
        this.targetPort = targetPort;
        this.wsPath = wsPath;
        this.debug = debug;
    }

    void start() {
        // Cloudflare requires 4 connections for a "Healthy" HA status.
        for (byte i = 0; i < 4; i++) {
            byte idx = i;
            scheduler.execute(() -> connectLoop(idx));
        }
    }

    private void connectLoop(byte connIndex) {
        int reconnectDelay = RECONNECT_BASE_DELAY_MS;
        while (!closed.get()) {
            boolean connected = false;
            try {
                List<InetAddress> edgeAddresses = discoverEdgeAddresses();
                if (edgeAddresses.isEmpty()) {
                    log("conn[" + connIndex + "] no edge addresses found");
                }

                for (InetAddress edgeAddr : edgeAddresses) {
                    if (closed.get()) return;
                    if (!(edgeAddr instanceof Inet4Address)) {
                        continue;
                    }
                    try {
                        log("conn[" + connIndex + "] connecting to edge: " + edgeAddr.getHostAddress() + ":" + EDGE_PORT);
                        connected = true;
                        connectToEdge(edgeAddr, connIndex);
                        log("conn[" + connIndex + "] edge connection ended");
                        reconnectDelay = RECONNECT_BASE_DELAY_MS;
                        break;
                    } catch (Exception e) {
                        log("conn[" + connIndex + "] edge connection failed: " + e.getMessage());
                    }
                }
            } catch (Exception e) {
                log("conn[" + connIndex + "] connect loop error: " + e.getMessage());
            }

            if (!closed.get()) {
                int delay = connected ? RECONNECT_BASE_DELAY_MS : reconnectDelay;
                log("conn[" + connIndex + "] reconnecting in " + delay + "ms");
                sleep(delay);
                reconnectDelay = Math.min(reconnectDelay * 2, RECONNECT_MAX_DELAY_MS);
            }
        }
    }

    private void connectToEdge(InetAddress edgeAddr, byte connIndex) throws Exception {
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, new TrustManager[]{new X509TrustManager() {
            public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
            public void checkClientTrusted(X509Certificate[] certs, String authType) {}
            public void checkServerTrusted(X509Certificate[] certs, String authType) {}
        }}, new SecureRandom());

        // Use a fresh SSLSocket (not layered) so SSLParameters/ALPN are properly included
        // in the ClientHello on Java 25. Layered sockets lose ALPN extensions.
        var sslSocket = (javax.net.ssl.SSLSocket) sslContext.getSocketFactory().createSocket();

        SSLParameters params = new SSLParameters();
        params.setServerNames(List.of(new javax.net.ssl.SNIHostName(H2_SNI)));
        params.setApplicationProtocols(new String[]{"h2"});
        params.setEndpointIdentificationAlgorithm(""); // disable hostname verification
        sslSocket.setSSLParameters(params);

        // Connect and handshake. HTTP/2 heartbeat monitors liveness after TLS is established.
        sslSocket.connect(new InetSocketAddress(edgeAddr, EDGE_PORT), CONNECT_TIMEOUT_MS);
        sslSocket.setSoTimeout(HANDSHAKE_TIMEOUT_MS);
        sslSocket.startHandshake();
        sslSocket.setSoTimeout(0);

        // Diagnostic: log TLS session details regardless of ALPN result
        String negotiatedProtocol = sslSocket.getApplicationProtocol();
        String tlsVersion = sslSocket.getSession().getProtocol();
        String cipherSuite = sslSocket.getSession().getCipherSuite();
        log("TLS connected: version=" + tlsVersion + " cipher=" + cipherSuite + " alpn=[" + negotiatedProtocol + "]");

        // Do NOT abort on empty ALPN — some CF edge nodes respond with h2 framing
        // even when ALPN is not echoed back. Log a warning but continue.
        if (!"h2".equals(negotiatedProtocol)) {
            log("WARN: expected h2 ALPN, got [" + negotiatedProtocol + "] — continuing anyway");
        }


        // At this point we have an HTTP/2 connection.
        // The CF edge expects us to act as an HTTP/2 SERVER:
        // - We send the HTTP/2 connection preface
        // - The edge opens streams to us
        // - The first stream with header "Cf-Cloudflared-Proxy-Connection-Upgrade: control-stream"
        //   is the control stream where we do Cap'n Proto RPC registration

        // For now, we'll use a simplified approach:
        // Send the HTTP/2 client connection preface and SETTINGS frame,
        // then open a stream with the control-stream upgrade header.

        // Actually, cloudflared's HTTP/2 mode is INVERTED:
        // cloudflared dials out (acts as TCP client), but then acts as HTTP/2 SERVER.
        // The edge is the HTTP/2 CLIENT that opens streams.

        // This means we need to:
        // 1. Send HTTP/2 server connection preface (just SETTINGS frame, no magic)
        // 2. Read the edge's HTTP/2 client connection preface (magic + SETTINGS)
        // 3. Send SETTINGS ACK
        // 4. Wait for the edge to open a stream with control-stream header
        // 5. On that stream, do Cap'n Proto RPC

        // For a minimal implementation, let's handle the HTTP/2 framing manually.

        Http2Connection h2 = null;
        Future<?> heartbeat = null;
        Future<?> watchdog = null;
        try (sslSocket) {
            activeSockets.add(sslSocket);
            InputStream in = sslSocket.getInputStream();
            OutputStream out = sslSocket.getOutputStream();

            h2 = new Http2Connection(in, out, sslSocket, debug);
            activeConnections.add(h2);
            h2.serverHandshake();

            log("HTTP/2 handshake complete, waiting for control stream");

            Http2Connection activeH2 = h2;
            heartbeat = scheduler.submit(() -> heartbeatLoop(activeH2, sslSocket, connIndex));
            watchdog = scheduler.submit(() -> idleWatchdogLoop(activeH2, sslSocket, connIndex));

            // The edge will open streams. We need to handle them.
            h2.serveForever(this, connIndex);
        } finally {
            if (heartbeat != null) {
                heartbeat.cancel(true);
            }
            if (watchdog != null) {
                watchdog.cancel(true);
            }
            if (h2 != null) {
                h2.close();
                activeConnections.remove(h2);
            }
            activeSockets.remove(sslSocket);
        }
    }

    void handleControlStream(Http2Connection h2, int streamId, byte connIndex) {
        log("conn[" + connIndex + "] control stream opened on stream " + streamId);
        boolean registered = false;

        try {
            // MUST send HTTP 200 OK response headers before sending Cap'n Proto data payloads,
            // otherwise the HTTP/2 client will reject the DATA frames with PROTOCOL_ERROR (RST_STREAM 0x1)
            h2.sendResponseHeaders(streamId, 200, Map.of());

            // Send Bootstrap
            int bootstrapQid = questionCounter.getAndIncrement();
            byte[] bootstrapMsg = CapnpRpc.bootstrap(bootstrapQid);
            h2.sendData(streamId, bootstrapMsg, false);
            log("sent Bootstrap (questionId=" + bootstrapQid + ")");

            // Send registerConnection Call
            int registerQid = questionCounter.getAndIncrement();
            byte[] clientId = new byte[16];
            new SecureRandom().nextBytes(clientId);

            byte[] callMsg = CapnpRpc.callRegisterConnection(
                    registerQid,
                    bootstrapQid,
                    accountTag,
                    tunnelSecret,
                    tunnelId,
                    connIndex,
                    clientId,
                    "2024.10.0",
                    "java_" + System.getProperty("os.arch", "unknown"),
                    List.of("serialized_headers", "ha-connections")
            );
            h2.sendData(streamId, callMsg, false);
            log("sent registerConnection (questionId=" + registerQid + ")");

            // Read responses
            byte[] responseData = h2.readData(streamId);
            if (responseData != null) {
                Optional<CapnpRpc.ParsedMessage> parsed = CapnpRpc.parse(responseData);
                if (parsed.isPresent()) {
                    CapnpRpc.ParsedMessage pm = parsed.get();
                    if (pm.isReturn()) {
                        if (pm.isReturnResults()) {
                            registered = true;
                            log("registration successful!");
                        } else if (pm.isReturnException()) {
                            throw new IOException("registration failed with exception");
                        } else {
                            throw new IOException("registration returned unknown result type: " + pm.returnWhich());
                        }
                    } else {
                        throw new IOException("unexpected message type: " + pm.type());
                    }
                }
            }

            if (!registered) {
                throw new IOException("registration stream closed before success");
            }

            // Keep control stream alive - read any further messages.
            // If the control stream ends, the connection is no longer healthy.
            while (!closed.get()) {
                responseData = h2.readData(streamId);
                if (responseData == null) {
                    throw new IOException("control stream closed");
                }
                // Handle any additional messages (resolve, release, etc.)
            }
        } catch (IOException e) {
            log("control stream error: " + e.getMessage());
            h2.close();
        }
    }

    void handleProxyStream(Http2Connection h2, int streamId, Map<String, String> headers) {
        log("proxy stream " + streamId + " opened");

        // The edge sends us an HTTP request. We need to forward it to the local proxy.
        // For WebSocket proxying, the header Cf-Cloudflared-Proxy-Connection-Upgrade: websocket
        // will be present.

        boolean isWebSocket = headers.containsKey("sec-websocket-key") || 
                              "websocket".equalsIgnoreCase(headers.get("upgrade"));

        scheduler.execute(() -> {
            try {
                if (isWebSocket) {
                    proxyRawWebSocket(h2, streamId, headers);
                } else {
                    proxyHttp(h2, streamId, headers);
                }
            } catch (Throwable t) {
                log("proxy stream " + streamId + " FATAL ERROR: " + t.toString());
                if (debug) {
                    t.printStackTrace();
                }
                try {
                    h2.sendRstStream(streamId, 0x02); // INTERNAL_ERROR
                } catch (IOException ignored) {
                }
            }
        });
    }

    private void proxyRawWebSocket(Http2Connection h2, int streamId, Map<String, String> headers) throws Exception {
        String path = headers.getOrDefault(":path", "/" + wsPath);
        String host = headers.getOrDefault(":authority", targetHost + ":" + targetPort);
        log("stream " + streamId + " proxying raw websocket to " + targetHost + ":" + targetPort + path);

        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(targetHost, targetPort), 5000);
            socket.setSoTimeout(0); // infinite timeout for WebSocket
            log("stream " + streamId + " socket connected to " + targetHost + ":" + targetPort);

            OutputStream out = socket.getOutputStream();
            InputStream in = socket.getInputStream();

            // Construct HTTP/1.1 Upgrade request
            StringBuilder req = new StringBuilder();
            req.append("GET ").append(path).append(" HTTP/1.1\r\n");

            boolean hasHost = false;
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                String k = entry.getKey();
                if (k.startsWith(":")) continue;
                if (k.equalsIgnoreCase("host")) {
                    req.append("Host: ").append(entry.getValue()).append("\r\n");
                    hasHost = true;
                    continue;
                }
                req.append(k).append(": ").append(entry.getValue()).append("\r\n");
            }
            if (!hasHost) {
                req.append("Host: ").append(host).append("\r\n");
            }

            req.append("Connection: Upgrade\r\n");
            req.append("Upgrade: websocket\r\n");
            req.append("\r\n");

            out.write(req.toString().getBytes(StandardCharsets.UTF_8));
            out.flush();
            log("stream " + streamId + " sent HTTP/1.1 Upgrade request");

            // Read HTTP/1.1 response headers
            ByteArrayOutputStream respHeadersBuf = new ByteArrayOutputStream();
            int b;
            int state = 0; // 1: \r, 2: \r\n, 3: \r\n\r, 4: \r\n\r\n
            while ((b = in.read()) != -1) {
                respHeadersBuf.write(b);
                if (b == '\r') {
                    if (state == 0 || state == 2) state++;
                    else state = 1;
                } else if (b == '\n') {
                    if (state == 1) state = 2;
                    else if (state == 3) {
                        state = 4;
                        break;
                    } else state = 0;
                } else {
                    state = 0;
                }
            }

            if (state != 4) {
                throw new IOException("Incomplete HTTP response from local origin");
            }

            String respHeadersStr = respHeadersBuf.toString(StandardCharsets.UTF_8);
            String[] lines = respHeadersStr.split("\r\n");
            if (lines.length == 0) throw new IOException("Empty HTTP response");

            String statusLine = lines[0];
            log("stream " + streamId + " received HTTP response: " + statusLine);
            int statusCode = 500;
            String[] statusParts = statusLine.split(" ", 3);
            if (statusParts.length >= 2) {
                statusCode = Integer.parseInt(statusParts[1]);
            }

            Map<String, String> responseHeaders = new HashMap<>();
            for (int i = 1; i < lines.length; i++) {
                String line = lines[i];
                if (line.isEmpty()) continue;
                int colon = line.indexOf(':');
                if (colon > 0) {
                    String k = line.substring(0, colon).trim().toLowerCase();
                    String v = line.substring(colon + 1).trim();
                    responseHeaders.put(k, v);
                }
            }

            // Cloudflared maps 101 to 200 OK for HTTP/2 WebSocket
            int h2Status = (statusCode == 101) ? 200 : statusCode;
            log("stream " + streamId + " sending H2 response headers, status: " + h2Status + ", headers: " + responseHeaders);
            h2.sendResponseHeaders(streamId, h2Status, responseHeaders);

            if (statusCode != 101) {
                log("stream " + streamId + " websocket upgrade failed with status " + statusCode);
                h2.sendData(streamId, new byte[0], true);
                return;
            }

            log("stream " + streamId + " starting bidirectional pipe");
            Thread readerThread = new Thread(() -> {
                try {
                    while (!closed.get()) {
                        byte[] data = h2.readData(streamId);
                        if (data == null) {
                            socket.close();
                            break;
                        }
                        out.write(data);
                        out.flush();
                    }
                } catch (IOException e) {
                    try { socket.close(); } catch (Exception ignored) {}
                }
            });
            readerThread.start();

            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) != -1) {
                byte[] chunk = Arrays.copyOf(buf, n);
                h2.sendData(streamId, chunk, false);
            }
            log("stream " + streamId + " local socket EOF, sending H2 END_STREAM");
            h2.sendData(streamId, new byte[0], true);
        }
    }

    private void proxyHttp(Http2Connection h2, int streamId, Map<String, String> headers) throws Exception {
        // For regular HTTP, forward to local origin
        String method = headers.getOrDefault(":method", "GET");
        String path = headers.getOrDefault(":path", "/");
        String localUrl = "http://" + targetHost + ":" + targetPort + path;

        log("stream " + streamId + " proxying HTTP " + method + " " + path);

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                .uri(URI.create(localUrl))
                .timeout(java.time.Duration.ofSeconds(30));

        byte[] body = h2.readData(streamId);
        if ("POST".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method)) {
            reqBuilder.method(method, HttpRequest.BodyPublishers.ofByteArray(body != null ? body : new byte[0]));
        } else {
            reqBuilder.method(method, HttpRequest.BodyPublishers.noBody());
        }

        HttpResponse<byte[]> response = client.send(reqBuilder.build(), HttpResponse.BodyHandlers.ofByteArray());
        h2.sendResponseHeaders(streamId, response.statusCode(), Map.of());
        h2.sendData(streamId, response.body(), true);
    }

    // --- Edge discovery ---

    private void heartbeatLoop(Http2Connection h2, Socket socket, byte connIndex) {
        while (!closed.get() && !h2.isClosed()) {
            sleep(HEARTBEAT_INTERVAL_MS);
            if (closed.get() || h2.isClosed()) {
                return;
            }
            try {
                h2.sendPing();
            } catch (IOException e) {
                log("conn[" + connIndex + "] heartbeat failed: " + e.getMessage());
                closeConnection(h2, socket);
                return;
            }
        }
    }

    private void idleWatchdogLoop(Http2Connection h2, Socket socket, byte connIndex) {
        while (!closed.get() && !h2.isClosed()) {
            sleep(HEARTBEAT_INTERVAL_MS);
            if (closed.get() || h2.isClosed()) {
                return;
            }
            long idleMillis = h2.millisSinceLastFrame();
            if (idleMillis > HEARTBEAT_TIMEOUT_MS) {
                log("conn[" + connIndex + "] heartbeat timeout after " + idleMillis + "ms");
                closeConnection(h2, socket);
                return;
            }
        }
    }

    private void closeConnection(Http2Connection h2, Socket socket) {
        h2.close();
        try {
            socket.close();
        } catch (IOException ignored) {
        }
    }

    private List<InetAddress> discoverEdgeAddresses() {
        List<InetAddress> addresses = new ArrayList<>();
        try {
            Hashtable<String, String> env = new Hashtable<>();
            env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.dns.DnsContextFactory");
            DirContext ctx = new InitialDirContext(env);
            Attributes attrs = ctx.getAttributes(SRV_RECORD, new String[]{"SRV"});
            var srvRecords = attrs.get("SRV");
            if (srvRecords != null) {
                for (int i = 0; i < srvRecords.size(); i++) {
                    String srv = srvRecords.get(i).toString();
                    // SRV format: priority weight port target
                    String[] parts = srv.trim().split("\\s+");
                    if (parts.length >= 4) {
                        String target = parts[3].endsWith(".") ? parts[3].substring(0, parts[3].length() - 1) : parts[3];
                        try {
                            InetAddress[] addrs = InetAddress.getAllByName(target);
                            addresses.addAll(Arrays.asList(addrs));
                        } catch (Exception ignored) {
                        }
                    }
                }
            }
            ctx.close();
        } catch (Exception e) {
            log("SRV lookup failed: " + e.getMessage());
        }

        if (addresses.isEmpty()) {
            // Fallback: try direct resolution
            try {
                addresses.addAll(Arrays.asList(InetAddress.getAllByName("region1.v2.argotunnel.com")));
            } catch (Exception ignored) {
            }
            try {
                addresses.addAll(Arrays.asList(InetAddress.getAllByName("region2.v2.argotunnel.com")));
            } catch (Exception ignored) {
            }
        }

        // Only use IPv4 — container may not have IPv6 connectivity
        addresses.removeIf(addr -> !(addr instanceof Inet4Address));
        // Shuffle for load balancing
        Collections.shuffle(addresses);
        return addresses;
    }

    private void log(String message) {
        if (debug) {
            java.util.logging.Logger.getLogger("ServerTools").info("CF-TUNNEL - " + message);
        }
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) {
            return;
        }
        for (Http2Connection h2 : activeConnections) {
            h2.close();
        }
        for (Socket socket : activeSockets) {
            try {
                socket.close();
            } catch (IOException ignored) {
            }
        }
        scheduler.shutdownNow();
    }
}
