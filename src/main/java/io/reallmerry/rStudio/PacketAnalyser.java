package io.reallmerry.rStudio;

import io.reallmerry.rStudio.log.FileLogger;
import org.bukkit.plugin.java.JavaPlugin;

public final class PacketAnalyser extends JavaPlugin {

    private PacketListenerManager listenerManager;

    @Override
    public void onEnable() {
        FileLogger fileLogger = new FileLogger(this);
        this.listenerManager = new PacketListenerManager(this, fileLogger);
        listenerManager.registerListeners();

        getCommand("packets").setExecutor(new PacketCommand(this.listenerManager, fileLogger));
        getCommand("packets").setTabCompleter(new PacketTabCompleter());

        getLogger().info("PacketAnalyser v" + getDescription().getVersion() + " by rStudio enabled.");
    }

    @Override
    public void onDisable() {
        if (this.listenerManager != null) {
            this.listenerManager.stopAllLogging();
            this.listenerManager.unregisterListeners();
        }
        getLogger().info("PacketAnalyser disabled.");
    }
}