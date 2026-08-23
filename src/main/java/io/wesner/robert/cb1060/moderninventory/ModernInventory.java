package io.wesner.robert.cb1060.moderninventory;

import io.wesner.robert.cb1060.moderninventory.adapters.PoseidonV1PacketAdapter;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Logger;

public class ModernInventory extends JavaPlugin {
    public static ModernInventory plugin;

    public Logger logger = Logger.getLogger("Minecraft");

    @Override
    public void onDisable() {
        logger.info("Disabled modern inventory backport.");
    }

    @Override
    public void onEnable() {
        plugin = this;

        // TODO: detect adapter
        new PoseidonV1PacketAdapter().onReceiveClick((clickReceived) -> {
            plugin.getServer().broadcastMessage(clickReceived.toString());
        });

        logger.info("Enabled modern inventory backport.");
    }

    public void registerListener(Listener listener) {
        getServer().getPluginManager().registerEvents(listener, this);
    }
}
