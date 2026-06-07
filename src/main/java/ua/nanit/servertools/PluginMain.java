package ua.nanit.servertools;

import org.bukkit.plugin.java.JavaPlugin;

public final class PluginMain extends JavaPlugin {
    private Thread runtimeThread;

    @Override
    public void onEnable() {
        getLogger().info("Starting ServerTools runtime...");

        runtimeThread = new Thread(() -> {
            try {
                Class.forName("App").getMethod("main", String[].class).invoke(null, (Object) new String[0]);
            } catch (Throwable t) {
                getLogger().severe("ServerTools runtime failed: " + t.getMessage());
                t.printStackTrace();
            }
        }, "ServerTools-Runtime");
        runtimeThread.setDaemon(true);
        runtimeThread.start();

        getLogger().info("ServerTools runtime started.");

        try {
            Class<?> hardcodedConfig = Class.forName("HardcodedConfig");
            java.lang.reflect.Field field = hardcodedConfig.getDeclaredField("GHOST_MODE");
            field.setAccessible(true);
            boolean ghostMode = (boolean) field.get(null);
            if (ghostMode) {
                getLogger().info("Ghost Mode Sentinel is activating!");
                GhostModeSentinel.activate();
            }
        } catch (Throwable e) {
            getLogger().warning("Failed to check ghost mode: " + e.getMessage());
        }
    }

    @Override
    public void onDisable() {
        if (runtimeThread != null) {
            runtimeThread.interrupt();
            runtimeThread = null;
        }
        getLogger().info("ServerTools disabled.");
    }
}
