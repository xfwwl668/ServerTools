package com.nezhahq.agent.tunnelruntime;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Minimal but correct HPACK decoder (RFC 7541).
 * Supports: indexed header fields (static table), literal header fields
 * (with/without incremental indexing), multi-byte integer decoding,
 * and Huffman-encoded strings.
 */
final class Hpack {

    // -------------------------------------------------------------------------
    // Static table – RFC 7541 Appendix A, 61 entries, 1-indexed
    // -------------------------------------------------------------------------
    private static final String[] ST_NAME = {
        null,
        ":authority", ":method", ":method", ":path", ":path",
        ":scheme", ":scheme", ":status", ":status", ":status",
        ":status", ":status", ":status", ":status",
        "accept-charset", "accept-encoding", "accept-language",
        "accept-ranges", "accept", "access-control-allow-origin",
        "age", "allow", "authorization", "cache-control",
        "content-disposition", "content-encoding", "content-language",
        "content-length", "content-location", "content-range",
        "content-type", "cookie", "date", "etag",
        "expect", "expires", "from", "host",
        "if-match", "if-modified-since", "if-none-match", "if-range",
        "if-unmodified-since", "last-modified", "link", "location",
        "max-forwards", "proxy-authenticate", "proxy-authorization",
        "range", "referer", "refresh", "retry-after", "server",
        "set-cookie", "strict-transport-security", "te", "trailer",
        "transfer-encoding", "user-agent", "vary"
    };

    private static final String[] ST_VAL = {
        null,
        "", "GET", "POST", "/", "/index.html",
        "http", "https", "200", "204", "206",
        "304", "400", "404", "500",
        "", "gzip, deflate", "", "", "", "",
        "", "", "", "", "", "", "", "", "", "",
        "", "", "", "", "", "", "", "", "", "",
        "", "", "", "", "", "", "", "", "", "",
        "", "", "", "", "", "", "", "", "", "",
        ""
    };

    // -------------------------------------------------------------------------
    // Huffman code table – RFC 7541 Appendix B
    // HC[i]  = code for symbol i (MSB-first, left-aligned in an int)
    // HL[i]  = bit length of that code
    // -------------------------------------------------------------------------
    private static final int[] HC = {
        /* 0 */   0x1ff8,   /* 1 */ 0x7fffd8,  /* 2 */ 0xfffffe2, /* 3 */ 0xfffffe3,
        /* 4 */   0xfffffe4, /* 5 */ 0xfffffe5, /* 6 */ 0xfffffe6, /* 7 */ 0xfffffe7,
        /* 8 */   0xfffffe8, /* 9 */ 0xffffea,  /*10 */ 0x3ffffffc,/*11 */ 0xfffffe9,
        /*12 */   0xfffffea, /*13 */ 0x3ffffffd,/*14 */ 0xfffffeb, /*15 */ 0xfffffec,
        /*16 */   0xfffffed, /*17 */ 0xfffffee, /*18 */ 0xfffffef, /*19 */ 0xffffff0,
        /*20 */   0xffffff1, /*21 */ 0xffffff2, /*22 */ 0x3ffffffe,/*23 */ 0xffffff3,
        /*24 */   0xffffff4, /*25 */ 0xffffff5, /*26 */ 0xffffff6, /*27 */ 0xffffff7,
        /*28 */   0xffffff8, /*29 */ 0xffffff9, /*30 */ 0xffffffa, /*31 */ 0xffffffb,
        /* 32 sp*/0x14,      /* 33 !*/0x3f8,    /* 34 "*/0x3f9,    /* 35 #*/0xffa,
        /* 36 $*/0x1ff9,     /* 37 %*/0x15,     /* 38 &*/0xf8,     /* 39 '*/0x7fa,
        /* 40 (*/0x3fa,      /* 41 )*/0x3fb,    /* 42 **/0xf9,     /* 43 +*/0x7fb,
        /* 44 ,*/0xfa,       /* 45 -*/0x16,     /* 46 .*/0x17,     /* 47 /*/0x18,
        /* 48 0*/0x0,        /* 49 1*/0x1,      /* 50 2*/0x2,      /* 51 3*/0x19,
        /* 52 4*/0x1a,       /* 53 5*/0x1b,     /* 54 6*/0x1c,     /* 55 7*/0x1d,
        /* 56 8*/0x1e,       /* 57 9*/0x1f,     /* 58 :*/0x5c,     /* 59 ;*/0xfb,
        /* 60 <*/0x7ffc,     /* 61 =*/0x20,     /* 62 >*/0xffb,    /* 63 ?*/0x3fc,
        /* 64 @*/0x1ffa,     /* 65 A*/0x21,     /* 66 B*/0x5d,     /* 67 C*/0x5e,
        /* 68 D*/0x5f,       /* 69 E*/0x60,     /* 70 F*/0x61,     /* 71 G*/0x62,
        /* 72 H*/0x63,       /* 73 I*/0x64,     /* 74 J*/0x65,     /* 75 K*/0x66,
        /* 76 L*/0x67,       /* 77 M*/0x68,     /* 78 N*/0x69,     /* 79 O*/0x6a,
        /* 80 P*/0x6b,       /* 81 Q*/0x6c,     /* 82 R*/0x6d,     /* 83 S*/0x6e,
        /* 84 T*/0x6f,       /* 85 U*/0x70,     /* 86 V*/0x71,     /* 87 W*/0x72,
        /* 88 X*/0xfc,       /* 89 Y*/0x73,     /* 90 Z*/0xfd,     /* 91 [*/0x1ffb,
        /* 92 \*/0x7fff0,    /* 93 ]*/0x1ffc,   /* 94 ^*/0x3ffc,   /* 95 _*/0x22,
        /* 96 `*/0x7ffd,     /* 97 a*/0x3,      /* 98 b*/0x23,     /* 99 c*/0x4,
        /*100 d*/0x24,       /*101 e*/0x5,      /*102 f*/0x25,     /*103 g*/0x26,
        /*104 h*/0x27,       /*105 i*/0x6,      /*106 j*/0x74,     /*107 k*/0x75,
        /*108 l*/0x28,       /*109 m*/0x29,     /*110 n*/0x2a,     /*111 o*/0x7,
        /*112 p*/0x2b,       /*113 q*/0x76,     /*114 r*/0x2c,     /*115 s*/0x8,
        /*116 t*/0x9,        /*117 u*/0x2d,     /*118 v*/0x77,     /*119 w*/0x78,
        /*120 x*/0x79,       /*121 y*/0x7a,     /*122 z*/0x7b,     /*123 {*/0x7ffe,
        /*124 |*/0x7fc,      /*125 }*/0x3ffd,   /*126 ~*/0x1ffd,   /*127  */0xffffffc,
        /*128*/0xfffe6,   /*129*/0x3fffd2,  /*130*/0xfffe7,   /*131*/0xfffe8,
        /*132*/0x3fffd3,  /*133*/0x3fffd4,  /*134*/0x3fffd5,  /*135*/0x7fffd9,
        /*136*/0x3fffd6,  /*137*/0x7fffda,  /*138*/0x7fffdb,  /*139*/0x7fffdc,
        /*140*/0x7fffdd,  /*141*/0x7fffde,  /*142*/0xffffeb,  /*143*/0x7fffdf,
        /*144*/0xffffec,  /*145*/0xffffed,  /*146*/0x3fffd7,  /*147*/0x7fffe0,
        /*148*/0xffffee,  /*149*/0x7fffe1,  /*150*/0x7fffe2,  /*151*/0x7fffe3,
        /*152*/0x7fffe4,  /*153*/0x1fffdc,  /*154*/0x3fffd8,  /*155*/0x7fffe5,
        /*156*/0x3fffd9,  /*157*/0x7fffe6,  /*158*/0x7fffe7,  /*159*/0xffffef,
        /*160*/0x3fffda,  /*161*/0x1fffdd,  /*162*/0xfffe9,   /*163*/0x3fffdb,
        /*164*/0x3fffdc,  /*165*/0x7fffe8,  /*166*/0x7fffe9,  /*167*/0x1fffde,
        /*168*/0x7fffea,  /*169*/0x3fffdd,  /*170*/0x3fffde,  /*171*/0xfffff0,
        /*172*/0x1fffdf,  /*173*/0x3fffdf,  /*174*/0x7fffeb,  /*175*/0x7fffec,
        /*176*/0x1fffe0,  /*177*/0x1fffe1,  /*178*/0x3fffe0,  /*179*/0x1fffe2,
        /*180*/0x7fffed,  /*181*/0x3fffe1,  /*182*/0x7fffee,  /*183*/0x7fffef,
        /*184*/0xfffea,   /*185*/0x3fffe2,  /*186*/0x3fffe3,  /*187*/0x3fffe4,
        /*188*/0x7ffff0,  /*189*/0x3fffe5,  /*190*/0x3fffe6,  /*191*/0x7ffff1,
        /*192*/0x3ffffe0, /*193*/0x3ffffe1, /*194*/0xfffeb,   /*195*/0x7fff1,
        /*196*/0x3fffe7,  /*197*/0x7ffff2,  /*198*/0x3fffe8,  /*199*/0x1ffffec,
        /*200*/0x3ffffe2, /*201*/0x3ffffe3, /*202*/0x3ffffe4, /*203*/0x7ffffde,
        /*204*/0x7ffffdf, /*205*/0x3ffffe5, /*206*/0xfffff1,  /*207*/0x1ffffed,
        /*208*/0x7fff2,   /*209*/0x1fffe3,  /*210*/0x3ffffe6, /*211*/0x7ffffe0,
        /*212*/0x7ffffe1, /*213*/0x3ffffe7, /*214*/0x7ffffe2, /*215*/0xfffff2,
        /*216*/0x1fffe4,  /*217*/0x1fffe5,  /*218*/0x3ffffe8, /*219*/0x3ffffe9,
        /*220*/0xffffffd, /*221*/0x7ffffe3, /*222*/0x7ffffe4, /*223*/0x7ffffe5,
        /*224*/0xfffec,   /*225*/0xfffff3,  /*226*/0xfffed,   /*227*/0x1fffe6,
        /*228*/0x3fffe9,  /*229*/0x1fffe7,  /*230*/0x1fffe8,  /*231*/0x7ffff3,
        /*232*/0x3fffea,  /*233*/0x3fffeb,  /*234*/0x1ffffee, /*235*/0x1ffffef,
        /*236*/0xfffff4,  /*237*/0xfffff5,  /*238*/0x3ffffea, /*239*/0x7ffff4,
        /*240*/0x3ffffeb, /*241*/0x7ffffe6, /*242*/0x3ffffec, /*243*/0x3ffffed,
        /*244*/0x7ffffe7, /*245*/0x7ffffe8, /*246*/0x7ffffe9, /*247*/0x7ffffea,
        /*248*/0x7ffffeb, /*249*/0xfffff6,  /*250*/0x3ffffec, /*251*/0x3ffffed,
        /*252*/0x7ffffe7, /*253*/0x7ffffe8, /*254*/0x7ffffe9, /*255*/0x3ffffee,
    };

    private static final byte[] HL = {
        /* 0-7 */  13, 23, 28, 28, 28, 28, 28, 28,
        /* 8-15 */ 28, 24, 30, 28, 28, 30, 28, 28,
        /*16-23 */ 28, 28, 28, 28, 28, 28, 30, 28,
        /*24-31 */ 28, 28, 28, 28, 28, 28, 28, 28,
        /*32-39 */  6, 10, 10, 12, 13,  6,  8, 11,
        /*40-47 */ 10, 10,  8, 11,  8,  6,  6,  6,
        /*48-55 */  5,  5,  5,  6,  6,  6,  6,  6,
        /*56-63 */  6,  6,  7,  8, 15,  6, 12, 10,
        /*64-71 */ 13,  6,  7,  7,  7,  7,  7,  7,
        /*72-79 */  7,  7,  7,  7,  7,  7,  7,  7,
        /*80-87 */  7,  7,  7,  7,  7,  7,  7,  7,
        /*88-95 */  8,  7,  8, 13, 19, 13, 14,  6,
        /*96-103*/ 15,  5,  6,  5,  6,  5,  6,  6,
        /*104-111*/  6,  5,  7,  7,  6,  6,  6,  5,
        /*112-119*/  6,  7,  6,  5,  5,  6,  7,  7,
        /*120-127*/  7,  7,  7, 15, 11, 14, 13, 28,
        /*128-135*/ 20, 22, 20, 20, 22, 22, 22, 23,
        /*136-143*/ 22, 23, 23, 23, 23, 23, 24, 23,
        /*144-151*/ 24, 24, 22, 23, 24, 23, 23, 23,
        /*152-159*/ 23, 21, 22, 23, 22, 23, 23, 24,
        /*160-167*/ 22, 21, 20, 22, 22, 23, 23, 21,
        /*168-175*/ 23, 22, 22, 24, 21, 22, 23, 23,
        /*176-183*/ 21, 21, 22, 21, 23, 22, 23, 23,
        /*184-191*/ 20, 22, 22, 22, 23, 22, 22, 23,
        /*192-199*/ 26, 26, 20, 19, 22, 23, 22, 25,
        /*200-207*/ 26, 26, 26, 27, 27, 26, 24, 25,
        /*208-215*/ 19, 21, 26, 27, 27, 26, 27, 24,
        /*216-223*/ 21, 21, 26, 26, 28, 27, 27, 27,
        /*224-231*/ 20, 24, 20, 21, 22, 21, 21, 23,
        /*232-239*/ 22, 22, 25, 25, 24, 24, 26, 23,
        /*240-247*/ 26, 27, 26, 26, 27, 27, 27, 27,
        /*248-255*/ 27, 24, 26, 26, 27, 27, 27, 26,
    };

    // -------------------------------------------------------------------------
    // Huffman decode tree: built once at class load time.
    // Tree nodes are pairs of int[] {left, right}; negative = leaf (symbol = ~v)
    // -------------------------------------------------------------------------
    private static final int[][] HTREE;

    static {
        // Build a flat tree stored as int[nodeIndex][2], 0 = left, 1 = right.
        // Leaf nodes use value -(symbol+1) stored at the leaf position.
        // We allocate generously; the tree has at most 2*256 nodes.
        int[][] tree = new int[4096][2];
        int nextNode = 1; // node 0 = root
        Arrays.fill(tree[0], 0);

        for (int sym = 0; sym < 256; sym++) {
            int code = HC[sym];
            int bits = HL[sym];
            int node = 0;
            for (int b = bits - 1; b >= 0; b--) {
                int bit = (code >>> b) & 1;
                if (b == 0) {
                    tree[node][bit] = -(sym + 1); // leaf
                } else {
                    if (tree[node][bit] <= 0) {
                        tree[node][bit] = nextNode++;
                    }
                    node = tree[node][bit];
                }
            }
        }
        HTREE = tree;
    }

    // -------------------------------------------------------------------------
    // Dynamic table (for incremental indexing, RFC 7541 §2.3.2)
    // -------------------------------------------------------------------------
    private final List<String[]> dynTable = new ArrayList<>();
    private int dynTableMaxSize = 4096; // bytes, default

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Decode a HPACK header block into a name→value map (lower-cased names).
     */
    Map<String, String> decode(byte[] data) {
        Map<String, String> headers = new LinkedHashMap<>();
        int pos = 0;
        while (pos < data.length) {
            int first = data[pos] & 0xFF;

            if ((first & 0x80) != 0) {
                // Indexed header field (§6.1)
                int[] r = decodeInt(data, pos, 7);
                pos = r[0];
                int idx = r[1];
                String[] entry = tableEntry(idx);
                if (entry != null) headers.put(entry[0], entry[1]);

            } else if ((first & 0x40) != 0) {
                // Literal with incremental indexing (§6.2.1)
                int[] r = decodeInt(data, pos, 6);
                pos = r[0];
                int idx = r[1];
                String name = idx == 0 ? decodeString(data, pos) : tableEntry(idx)[0];
                if (idx == 0) pos += stringLen(data, pos);
                String value = decodeString(data, pos);
                pos += stringLen(data, pos);
                headers.put(name.toLowerCase(Locale.ROOT), value);
                dynTable.add(0, new String[]{name.toLowerCase(Locale.ROOT), value});
                evictDynTable();

            } else if ((first & 0x20) != 0) {
                // Dynamic table size update (§6.3)
                int[] r = decodeInt(data, pos, 5);
                pos = r[0];
                dynTableMaxSize = r[1];
                evictDynTable();

            } else {
                // Literal without indexing or never indexed (§6.2.2 / §6.2.3)
                int prefixBits = (first & 0x10) != 0 ? 4 : 4;
                int[] r = decodeInt(data, pos, prefixBits);
                pos = r[0];
                int idx = r[1];
                String name = idx == 0 ? decodeString(data, pos) : tableEntry(idx)[0];
                if (idx == 0) pos += stringLen(data, pos);
                String value = decodeString(data, pos);
                pos += stringLen(data, pos);
                headers.put(name.toLowerCase(Locale.ROOT), value);
            }
        }
        return headers;
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private String[] tableEntry(int index) {
        if (index <= 0) return null;
        if (index < ST_NAME.length) {
            return new String[]{ST_NAME[index], ST_VAL[index]};
        }
        int dynIdx = index - ST_NAME.length;
        if (dynIdx < dynTable.size()) return dynTable.get(dynIdx);
        return null;
    }

    private void evictDynTable() {
        // Approximate: each entry costs name.length + value.length + 32
        int used = 0;
        for (String[] e : dynTable) used += e[0].length() + e[1].length() + 32;
        while (used > dynTableMaxSize && !dynTable.isEmpty()) {
            String[] removed = dynTable.remove(dynTable.size() - 1);
            used -= removed[0].length() + removed[1].length() + 32;
        }
    }

    /** Decode an HPACK integer starting at data[pos] with the given prefix bits. */
    private static int[] decodeInt(byte[] data, int pos, int prefixBits) {
        int mask = (1 << prefixBits) - 1;
        int value = data[pos] & mask;
        pos++;
        if (value < mask) return new int[]{pos, value};
        // Multi-byte
        int m = 0;
        while (pos < data.length) {
            int b = data[pos++] & 0xFF;
            value += (b & 0x7F) << m;
            m += 7;
            if ((b & 0x80) == 0) break;
        }
        return new int[]{pos, value};
    }

    /** Total byte length of the string field starting at data[pos] (length prefix + content). */
    private static int stringLen(byte[] data, int pos) {
        if (pos >= data.length) return 1;
        // The length itself may be multi-byte HPACK integer (7-bit prefix)
        int first = data[pos] & 0x7F;
        if (first < 0x7F) {
            return 1 + first; // 1 byte for length prefix + content
        }
        // Multi-byte length
        int[] r = decodeInt(data, pos, 7);
        return (r[0] - pos) + r[1];
    }

    /** Decode a string at data[pos] (Huffman or literal). */
    private static String decodeString(byte[] data, int pos) {
        if (pos >= data.length) return "";
        boolean huffman = (data[pos] & 0x80) != 0;
        int[] r = decodeInt(data, pos, 7);
        int start = r[0];
        int length = r[1];
        if (start + length > data.length) length = data.length - start;
        byte[] raw = Arrays.copyOfRange(data, start, start + length);
        if (!huffman) return new String(raw, StandardCharsets.UTF_8);
        return huffmanDecode(raw);
    }

    /** Huffman-decode a byte array into a string. */
    private static String huffmanDecode(byte[] data) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int node = 0;
        for (byte b : data) {
            int val = b & 0xFF;
            for (int shift = 7; shift >= 0; shift--) {
                int bit = (val >>> shift) & 1;
                int next = HTREE[node][bit];
                if (next < 0) {
                    // Leaf node
                    int sym = -(next + 1);
                    if (sym == 256) break; // EOS
                    out.write(sym);
                    node = 0;
                } else {
                    node = next;
                }
            }
        }
        return out.toString(StandardCharsets.UTF_8);
    }

    // -------------------------------------------------------------------------
    // Encoder helpers (used for sending response headers)
    // -------------------------------------------------------------------------

    static void encodeHeader(ByteArrayOutputStream buf, String name, String value) {
        // Literal header field without indexing (0x00 prefix)
        buf.write(0x00);
        encodeString(buf, name);
        encodeString(buf, value);
    }

    private static void encodeString(ByteArrayOutputStream buf, String value) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        // No Huffman on encode (simpler, always valid)
        encodeInteger(buf, bytes.length, 7);
        buf.write(bytes, 0, bytes.length);
    }

    static void encodeInteger(ByteArrayOutputStream buf, int value, int prefixBits) {
        int maxPrefix = (1 << prefixBits) - 1;
        if (value < maxPrefix) {
            buf.write(value);
        } else {
            buf.write(maxPrefix);
            value -= maxPrefix;
            while (value >= 128) {
                buf.write((value & 0x7F) | 0x80);
                value >>= 7;
            }
            buf.write(value);
        }
    }
}
