package com.nezhahq.agent.tunnelruntime;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Builds the exact Cap'n Proto RPC messages needed for CF Tunnel registration.
 * Field offsets are derived from the capnp C++ generated code (rpc.capnp.h).
 */
final class CapnpRpc {

    // rpc.capnp Message union discriminants (ordinals in the union)
    static final int MSG_BOOTSTRAP = 8;
    static final int MSG_CALL = 2;
    static final int MSG_RETURN = 3;
    static final int MSG_FINISH = 4;

    // RegistrationServer interface ID
    static final long REGISTRATION_SERVER_ID = 0xf71695ec7fe85497L;

    private CapnpRpc() {
    }

    // ============================================================
    // Bootstrap message
    // ============================================================

    static byte[] bootstrap(int questionId) {
        CapnpMessage msg = new CapnpMessage();

        // Word 0: root struct pointer
        int rootPtr = msg.allocate(1);
        // Message struct: data=1 word (union tag), pointers=1 (union content)
        int msgData = msg.allocate(1);
        int msgPtr = msg.allocate(1);
        msg.setStructPointer(rootPtr, msgData, 1, 1);
        msg.setUint16(msgData, 0, MSG_BOOTSTRAP);

        // Bootstrap: data=1 word (questionId @0 :UInt32), pointers=1 (deprecatedObjectId @1)
        int bsData = msg.allocate(1);
        msg.allocate(1); // bsPtr (null)
        msg.setStructPointer(msgPtr, bsData, 1, 1);
        msg.setUint32(bsData, 0, questionId);

        return msg.toBytes();
    }

    // ============================================================
    // Call message for registerConnection
    // ============================================================

    static byte[] callRegisterConnection(
            int questionId,
            int bootstrapQuestionId,
            String accountTag,
            byte[] tunnelSecret,
            UUID tunnelId,
            byte connIndex,
            byte[] clientId,
            String version,
            String arch,
            List<String> features) {

        CapnpMessage msg = new CapnpMessage();

        // --- Root: Message struct ---
        int rootPtr = msg.allocate(1);
        int msgData = msg.allocate(1);
        int msgPtr = msg.allocate(1);
        msg.setStructPointer(rootPtr, msgData, 1, 1);
        msg.setUint16(msgData, 0, MSG_CALL);

        // --- Call struct: data=3 words, pointers=3 ---
        int callData0 = msg.allocate(1); // word 0: questionId + methodId + sendResultsTo.which
        int callData1 = msg.allocate(1); // word 1: interfaceId
        int callData2 = msg.allocate(1); // word 2: flags (allowThirdPartyTailCall etc)
        int callPtr0 = msg.allocate(1);  // pointer[0]: target
        int callPtr1 = msg.allocate(1);  // pointer[1]: params
        int callPtr2 = msg.allocate(1);  // pointer[2]: sendResultsTo.thirdParty (null)
        msg.setStructPointer(msgPtr, callData0, 3, 3);

        // questionId: uint32 at data element 0 → bytes 0-3
        msg.setUint32(callData0, 0, questionId);
        // methodId: uint16 at data element 2 → bytes 4-5 (registerConnection = method 0)
        msg.setUint16(callData0, 4, 0);
        // sendResultsTo.which: uint16 at data element 3 → bytes 6-7 (caller = 0)
        msg.setUint16(callData0, 6, 0);
        // interfaceId: uint64 at data element 1
        msg.setUint64(callData1, REGISTRATION_SERVER_ID);

        // --- target: MessageTarget struct (data=1 word, pointers=1) ---
        // Using PromisedAnswer to reference bootstrap result
        int mtData = msg.allocate(1);
        int mtPtr = msg.allocate(1);
        msg.setStructPointer(callPtr0, mtData, 1, 1);
        // which = promisedAnswer (1) at uint16 element 2 → bytes 4-5
        msg.setUint16(mtData, 4, 1);
        // promisedAnswer: PromisedAnswer struct at pointer[0]
        // PromisedAnswer: data=1 word (questionId @0), pointers=1 (transform @1)
        int paData = msg.allocate(1);
        msg.allocate(1); // paPtr (transform = null/empty list)
        msg.setStructPointer(mtPtr, paData, 1, 1);
        msg.setUint32(paData, 0, bootstrapQuestionId);

        // --- params: Payload struct (data=0, pointers=2) ---
        int payloadPtr0 = msg.allocate(1); // content (AnyPointer)
        int payloadPtr1 = msg.allocate(1); // capTable (null)
        msg.setStructPointer(callPtr1, payloadPtr0, 0, 2);

        // --- params content: registerConnection_Params ---
        // ParamsSize = {DataSize: 8, PointerCount: 3}
        // data word 0: connIndex @2 :UInt8 → byte 0
        // pointer[0]: auth @0 :TunnelAuth
        // pointer[1]: tunnelId @1 :Data
        // pointer[2]: options @3 :ConnectionOptions
        int paramsData = msg.allocate(1);
        int paramsPtr0 = msg.allocate(1); // auth
        int paramsPtr1 = msg.allocate(1); // tunnelId
        int paramsPtr2 = msg.allocate(1); // options
        msg.setStructPointer(payloadPtr0, paramsData, 1, 3);
        msg.setUint8(paramsData, 0, connIndex);

        // --- TunnelAuth struct (data=0, pointers=2) ---
        // pointer[0]: accountTag @0 :Text
        // pointer[1]: tunnelSecret @1 :Data
        int authPtr0 = msg.allocate(1);
        int authPtr1 = msg.allocate(1);
        msg.setStructPointer(paramsPtr0, authPtr0, 0, 2);
        msg.writeText(authPtr0, accountTag);
        msg.writeData(authPtr1, tunnelSecret);

        // --- tunnelId: Data (16 bytes UUID) ---
        byte[] tunnelIdBytes = uuidToBytes(tunnelId);
        msg.writeData(paramsPtr1, tunnelIdBytes);

        // --- ConnectionOptions struct (data=8 bytes=1 word, pointers=2) ---
        // data byte 0: replaceExisting @2 :Bool → bit 0
        // data byte 1: compressionQuality @3 :UInt8
        // data byte 2: numPreviousAttempts @4 :UInt8
        // pointer[0]: client @0 :ClientInfo
        // pointer[1]: originLocalIp @1 :Data
        int optData = msg.allocate(1);
        int optPtr0 = msg.allocate(1); // client
        int optPtr1 = msg.allocate(1); // originLocalIp (null)
        msg.setStructPointer(paramsPtr2, optData, 1, 2);
        // replaceExisting = false (default), compressionQuality = 0, numPreviousAttempts = 0

        // --- ClientInfo struct (data=0, pointers=4) ---
        // pointer[0]: clientId @0 :Data
        // pointer[1]: features @1 :List(Text)
        // pointer[2]: version @2 :Text
        // pointer[3]: arch @3 :Text
        int ciPtr0 = msg.allocate(1);
        int ciPtr1 = msg.allocate(1);
        int ciPtr2 = msg.allocate(1);
        int ciPtr3 = msg.allocate(1);
        msg.setStructPointer(optPtr0, ciPtr0, 0, 4);
        msg.writeData(ciPtr0, clientId);
        if (features != null && !features.isEmpty()) {
            msg.writeTextList(ciPtr1, features);
        }
        msg.writeText(ciPtr2, version);
        msg.writeText(ciPtr3, arch);

        return msg.toBytes();
    }

    // ============================================================
    // Finish message
    // ============================================================

    static byte[] finish(int questionId, boolean releaseResultCaps) {
        CapnpMessage msg = new CapnpMessage();

        int rootPtr = msg.allocate(1);
        int msgData = msg.allocate(1);
        int msgPtr = msg.allocate(1);
        msg.setStructPointer(rootPtr, msgData, 1, 1);
        msg.setUint16(msgData, 0, MSG_FINISH);

        // Finish: data=1 word, pointers=0
        // questionId @0 :UInt32 → bytes 0-3
        // releaseResultCaps @1 :Bool → bit 32 (byte 4, bit 0) — default true
        // requireEarlyCancellationWorkaround @2 :Bool → bit 33 — default true
        int finData = msg.allocate(1);
        msg.setStructPointer(msgPtr, finData, 1, 0);
        msg.setUint32(finData, 0, questionId);
        // releaseResultCaps default is true, stored XOR with default
        // if we want true: XOR with true = 0 (no action needed)
        // if we want false: XOR with true = 1, set bit
        if (!releaseResultCaps) {
            msg.setUint8(finData, 4, 1); // XOR: false XOR true = 1
        }

        return msg.toBytes();
    }

    // ============================================================
    // Unregister (Call with method 1, no params)
    // ============================================================

    static byte[] callUnregisterConnection(int questionId, int bootstrapQuestionId) {
        CapnpMessage msg = new CapnpMessage();

        int rootPtr = msg.allocate(1);
        int msgData = msg.allocate(1);
        int msgPtr = msg.allocate(1);
        msg.setStructPointer(rootPtr, msgData, 1, 1);
        msg.setUint16(msgData, 0, MSG_CALL);

        int callData0 = msg.allocate(1);
        int callData1 = msg.allocate(1);
        int callData2 = msg.allocate(1);
        int callPtr0 = msg.allocate(1);
        int callPtr1 = msg.allocate(1);
        int callPtr2 = msg.allocate(1);
        msg.setStructPointer(msgPtr, callData0, 3, 3);

        msg.setUint32(callData0, 0, questionId);
        msg.setUint16(callData0, 4, 1); // methodId = 1 (unregisterConnection)
        msg.setUint16(callData0, 6, 0); // sendResultsTo = caller
        msg.setUint64(callData1, REGISTRATION_SERVER_ID);

        // target: PromisedAnswer referencing bootstrap
        int mtData = msg.allocate(1);
        int mtPtr = msg.allocate(1);
        msg.setStructPointer(callPtr0, mtData, 1, 1);
        msg.setUint16(mtData, 4, 1); // which = promisedAnswer
        int paData = msg.allocate(1);
        msg.allocate(1);
        msg.setStructPointer(mtPtr, paData, 1, 1);
        msg.setUint32(paData, 0, bootstrapQuestionId);

        // params: empty Payload
        int payloadPtr0 = msg.allocate(1);
        int payloadPtr1 = msg.allocate(1);
        msg.setStructPointer(callPtr1, payloadPtr0, 0, 2);
        // params content: empty struct (DataSize=0, PointerCount=0)
        int emptyStruct = msg.allocate(0); // zero-size, use null pointer
        // Actually for empty params, content pointer can be null

        return msg.toBytes();
    }

    // ============================================================
    // Parse received messages
    // ============================================================

    static Optional<ParsedMessage> parse(byte[] data) {
        if (data == null || data.length < 8) {
            return Optional.empty();
        }
        ByteBuffer buf = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);

        int segCountMinusOne = buf.getInt();
        if (segCountMinusOne < 0) {
            return Optional.empty();
        }
        int segCount = segCountMinusOne + 1;
        int[] segSizes = new int[segCount];
        for (int i = 0; i < segCount; i++) {
            if (buf.remaining() < 4) return Optional.empty();
            segSizes[i] = buf.getInt();
        }
        if ((4 + segCount * 4) % 8 != 0) {
            if (buf.remaining() < 4) return Optional.empty();
            buf.getInt();
        }

        int contentStart = buf.position();
        if (buf.remaining() < segSizes[0] * 8) {
            return Optional.empty();
        }

        long rootPtr = buf.getLong();
        int ptrType = (int) (rootPtr & 3);
        if (ptrType != 0) {
            return Optional.empty();
        }

        int ptrOffset = (int) (rootPtr << 32 >> 34);
        int dataWords = (int) ((rootPtr >> 32) & 0xFFFF);
        int pointerWords = (int) ((rootPtr >> 48) & 0xFFFF);

        int rootDataStart = contentStart + (1 + ptrOffset) * 8;
        if (rootDataStart + dataWords * 8 > data.length) {
            return Optional.empty();
        }

        int unionTag = readUint16LE(data, rootDataStart);

        return Optional.of(new ParsedMessage(unionTag, data, contentStart, rootDataStart, dataWords, pointerWords));
    }

    static int readUint16LE(byte[] data, int offset) {
        return (data[offset] & 0xFF) | ((data[offset + 1] & 0xFF) << 8);
    }

    static int readUint32LE(byte[] data, int offset) {
        return (data[offset] & 0xFF) | ((data[offset + 1] & 0xFF) << 8)
                | ((data[offset + 2] & 0xFF) << 16) | ((data[offset + 3] & 0xFF) << 24);
    }

    private static byte[] uuidToBytes(UUID uuid) {
        ByteBuffer buf = ByteBuffer.allocate(16);
        buf.putLong(uuid.getMostSignificantBits());
        buf.putLong(uuid.getLeastSignificantBits());
        return buf.array();
    }

    record ParsedMessage(int type, byte[] raw, int contentStart, int rootDataStart, int dataWords, int pointerWords) {
        boolean isReturn() {
            return type == MSG_RETURN;
        }

        /**
         * For a Return message, read answerId (uint32 at data element 0).
         */
        int answerId() {
            return readUint32LE(raw, rootDataStart);
        }

        /**
         * For a Return message, read the result union discriminant.
         * Return data layout:
         *   answerId @0 :UInt32 → bytes 0-3
         *   releaseParamCaps @1 :Bool → bit 32 (default true)
         *   union which → uint16 at element 3 → bytes 6-7
         */
        int returnWhich() {
            if (dataWords < 1) return -1;
            return readUint16LE(raw, rootDataStart + 6);
        }

        /**
         * Check if the Return indicates success (results, which=0).
         */
        boolean isReturnResults() {
            return isReturn() && returnWhich() == 0;
        }

        /**
         * Check if the Return indicates exception (which=1).
         */
        boolean isReturnException() {
            return isReturn() && returnWhich() == 1;
        }
    }
}
