package com.nezhahq.agent.tunnelruntime;

public final class RuntimeTunnelFeature {
    private RuntimeTunnelFeature() {
    }

    /**
     * Start CF native tunnel client.
     * Token must be a valid Cloudflare Tunnel token (base64 JSON with a/t/s fields).
     * relayUrl is ignored (kept for API compatibility with TunnelSupport reflection).
     */
    public static AutoCloseable start(String relayUrl, String token, String targetHost, int targetPort, String wsPath, boolean debug) {
        if (targetHost == null || targetHost.isBlank() || targetPort <= 0) {
            return null;
        }
        String path = wsPath == null || wsPath.isBlank() ? "ws" : wsPath.trim();

        CfTunnelToken cfToken = CfTunnelToken.parse(token);
        if (cfToken == null) {
            return null;
        }

        CfTunnelClient client = new CfTunnelClient(
                cfToken.accountTag,
                cfToken.tunnelSecret,
                cfToken.tunnelId,
                targetHost.trim(),
                targetPort,
                path,
                debug);
        client.start();
        return client;
    }

    public static String buildSubscriptionLine(String uuid, String host, int port, String path, String label) {
        String normalizedHost = safeTrim(host);
        if (normalizedHost.isBlank()) {
            return null;
        }
        String normalizedPath = normalizePath(path);
        String normalizedLabel = safeTrim(label);
        if (normalizedLabel.isBlank()) {
            normalizedLabel = "tunnel";
        }
        return String.format(
                "vless://%s@%s:%d?encryption=none&security=tls&sni=%s&fp=chrome&type=ws&host=%s&path=%%2F%s#%s",
                safeTrim(uuid),
                normalizedHost,
                port,
                normalizedHost,
                normalizedHost,
                normalizedPath,
                normalizedLabel);
    }

    private static String normalizePath(String path) {
        String text = safeTrim(path);
        while (text.startsWith("/")) {
            text = text.substring(1);
        }
        return text.isBlank() ? "tunnel" : text;
    }

    private static String safeTrim(String value) {
        return value == null ? "" : value.trim();
    }
}
