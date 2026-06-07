import java.lang.reflect.Method;

final class TunnelSupport {
    private static final String RUNTIME_CLASS = "com.nezhahq.agent.tunnelruntime.RuntimeTunnelFeature";
    private static final String METHOD_NAME = "buildSubscriptionLine";

    private TunnelSupport() {
    }

    static String buildSubscriptionLine(boolean enabled, String uuid, String host, int port, String path, String label) {
        if (!enabled) {
            return null;
        }
        try {
            Class<?> type = Class.forName(RUNTIME_CLASS);
            Method method = type.getDeclaredMethod(METHOD_NAME, String.class, String.class, int.class, String.class, String.class);
            Object result = method.invoke(null, uuid, host, port, path, label);
            return result instanceof String line ? line : null;
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return null;
        }
    }

    static AutoCloseable start(String relayUrl, String token, String targetHost, int targetPort, String wsPath, boolean debug) {
        try {
            Class<?> type = Class.forName(RUNTIME_CLASS);
            Method method = type.getDeclaredMethod("start", String.class, String.class, String.class, int.class, String.class, boolean.class);
            Object result = method.invoke(null, relayUrl, token, targetHost, targetPort, wsPath, debug);
            return result instanceof AutoCloseable closeable ? closeable : null;
        } catch (ReflectiveOperationException | RuntimeException e) {
            if (debug) {
                e.printStackTrace();
            }
            return null;
        }
    }
}
