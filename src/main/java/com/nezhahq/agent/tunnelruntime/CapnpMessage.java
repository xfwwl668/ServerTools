package com.nezhahq.agent.tunnelruntime;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Minimal Cap'n Proto message builder. Only supports the subset needed for
 * Cloudflare Tunnel registration: single-segment messages containing structs,
 * text, data, and list-of-text fields.
 *
 * All values are little-endian. A "word" is 8 bytes.
 */
final class CapnpMessage {
    private final List<Long> words = new ArrayList<>();

    int allocate(int wordCount) {
        int offset = words.size();
        for (int i = 0; i < wordCount; i++) {
            words.add(0L);
        }
        return offset;
    }

    void setWord(int wordOffset, long value) {
        words.set(wordOffset, value);
    }

    long getWord(int wordOffset) {
        return words.get(wordOffset);
    }

    void setByte(int wordOffset, int byteIndex, byte value) {
        long word = words.get(wordOffset);
        long mask = ~(0xFFL << (byteIndex * 8));
        word = (word & mask) | ((value & 0xFFL) << (byteIndex * 8));
        words.set(wordOffset, word);
    }

    void setUint8(int wordOffset, int byteIndex, int value) {
        setByte(wordOffset, byteIndex, (byte) (value & 0xFF));
    }

    void setUint16(int wordOffset, int byteIndex, int value) {
        setByte(wordOffset, byteIndex, (byte) (value & 0xFF));
        setByte(wordOffset, byteIndex + 1, (byte) ((value >>> 8) & 0xFF));
    }

    void setUint32(int wordOffset, int byteIndex, int value) {
        long word = words.get(wordOffset);
        long mask = ~(0xFFFFFFFFL << (byteIndex * 8));
        word = (word & mask) | ((value & 0xFFFFFFFFL) << (byteIndex * 8));
        words.set(wordOffset, word);
    }

    void setUint64(int wordOffset, long value) {
        words.set(wordOffset, value);
    }

    int size() {
        return words.size();
    }

    // --- Pointer helpers ---

    /**
     * Write a struct pointer at {@code ptrWordOffset} pointing to a struct
     * whose data section starts at {@code targetWordOffset}.
     */
    void setStructPointer(int ptrWordOffset, int targetWordOffset, int dataWords, int pointerWords) {
        int offset = targetWordOffset - ptrWordOffset - 1;
        long ptr = structPointer(offset, dataWords, pointerWords);
        words.set(ptrWordOffset, ptr);
    }

    /**
     * Write a text value. Allocates space, writes UTF-8 bytes + NUL, returns
     * the word offset where the text content starts.
     */
    int writeText(int ptrWordOffset, String text) {
        byte[] utf8 = text.getBytes(StandardCharsets.UTF_8);
        int byteCount = utf8.length + 1; // +1 for NUL
        int wordCount = (byteCount + 7) / 8;
        int contentOffset = allocate(wordCount);

        // Write bytes into words
        for (int i = 0; i < utf8.length; i++) {
            setByte(contentOffset + i / 8, i % 8, utf8[i]);
        }
        // NUL terminator is already 0 from allocation

        // Write list pointer (type=2 means 1-byte elements)
        int offset = contentOffset - ptrWordOffset - 1;
        long ptr = listPointer(offset, 2, byteCount);
        words.set(ptrWordOffset, ptr);

        return contentOffset;
    }

    /**
     * Write a data (byte array) value. Allocates space, writes bytes, returns
     * the word offset where the data content starts.
     */
    int writeData(int ptrWordOffset, byte[] data) {
        int byteCount = data.length;
        int wordCount = (byteCount + 7) / 8;
        int contentOffset = allocate(wordCount);

        for (int i = 0; i < data.length; i++) {
            setByte(contentOffset + i / 8, i % 8, data[i]);
        }

        int offset = contentOffset - ptrWordOffset - 1;
        long ptr = listPointer(offset, 2, byteCount);
        words.set(ptrWordOffset, ptr);

        return contentOffset;
    }

    /**
     * Write a List(Text) value. Each element is a pointer to a text value.
     */
    int writeTextList(int ptrWordOffset, List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            words.set(ptrWordOffset, 0L);
            return -1;
        }

        // Allocate pointer list (one pointer per text)
        int listOffset = allocate(texts.size());

        // List pointer: element size = 6 (pointer), count = texts.size()
        int offset = listOffset - ptrWordOffset - 1;
        long ptr = listPointer(offset, 6, texts.size());
        words.set(ptrWordOffset, ptr);

        // Write each text
        for (int i = 0; i < texts.size(); i++) {
            writeText(listOffset + i, texts.get(i));
        }

        return listOffset;
    }

    /**
     * Serialize the message with stream framing (segment table + content).
     */
    byte[] toBytes() {
        int segmentWords = words.size();
        // Segment table: 4 bytes (count-1) + 4 bytes (segment size)
        int headerSize = 8;
        byte[] result = new byte[headerSize + segmentWords * 8];
        ByteBuffer buf = ByteBuffer.wrap(result).order(ByteOrder.LITTLE_ENDIAN);

        // Segment table
        buf.putInt(0); // 1 segment, minus 1 = 0
        buf.putInt(segmentWords);

        // Segment content
        for (long word : words) {
            buf.putLong(word);
        }

        return result;
    }

    // --- Static pointer builders ---

    static long structPointer(int offset, int dataWords, int pointerWords) {
        // A=0 (struct), B=offset (30-bit signed), C=dataWords (16-bit), D=pointerWords (16-bit)
        long low = (offset << 2) & 0xFFFFFFFCL;
        long high = ((long) dataWords & 0xFFFF) | (((long) pointerWords & 0xFFFF) << 16);
        return (low & 0xFFFFFFFFL) | (high << 32);
    }

    static long listPointer(int offset, int elementSize, int elementCount) {
        // A=1 (list), B=offset (30-bit signed), C=elementSize (3-bit), D=elementCount (29-bit)
        long low = ((offset << 2) | 1) & 0xFFFFFFFFL;
        long high = ((long) elementSize & 0x7) | (((long) elementCount & 0x1FFFFFFFL) << 3);
        return (low & 0xFFFFFFFFL) | (high << 32);
    }
}
