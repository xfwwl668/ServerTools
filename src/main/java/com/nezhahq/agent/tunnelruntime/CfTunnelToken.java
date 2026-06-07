package com.nezhahq.agent.tunnelruntime;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

/**
 * Parses a Cloudflare Tunnel token (base64-encoded JSON).
 * Token format: {"a":"accountTag","t":"tunnelId","s":"base64Secret"}
 */
final class CfTunnelToken {
    final String accountTag;
    final byte[] tunnelSecret;
    final UUID tunnelId;

    private CfTunnelToken(String accountTag, byte[] tunnelSecret, UUID tunnelId) {
        this.accountTag = accountTag;
        this.tunnelSecret = tunnelSecret;
        this.tunnelId = tunnelId;
    }

    static CfTunnelToken parse(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        try {
            String json = new String(Base64.getDecoder().decode(token.trim()), StandardCharsets.UTF_8);
            String accountTag = extractJsonField(json, "a");
            String tunnelIdStr = extractJsonField(json, "t");
            String secretB64 = extractJsonField(json, "s");

            if (accountTag == null || tunnelIdStr == null || secretB64 == null) {
                return null;
            }

            UUID tunnelId = UUID.fromString(tunnelIdStr);
            byte[] tunnelSecret = Base64.getDecoder().decode(secretB64);

            return new CfTunnelToken(accountTag, tunnelSecret, tunnelId);
        } catch (Exception e) {
            return null;
        }
    }

    private static String extractJsonField(String json, String key) {
        String pattern = "\"" + key + "\"\\s*:\\s*\"([^\"]*)\"";
        var matcher = java.util.regex.Pattern.compile(pattern).matcher(json);
        return matcher.find() ? matcher.group(1) : null;
    }
}
