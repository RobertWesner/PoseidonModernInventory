package io.wesner.robert.cb1060.moderninventory.adapter;

import io.wesner.robert.cb1060.moderninventory.ModernInventory;
import net.minecraft.server.Packet102WindowClick;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.packet.PacketReceivedEvent;

import lombok.val;
import org.jspecify.annotations.NullMarked;

import java.util.ArrayList;
import java.util.function.Consumer;

@NullMarked
public class PoseidonV1PacketAdapter implements ModernInventoryAdapterInterface {
    static ModernInventory plugin = ModernInventory.plugin;

    // I miss Kotlin already
    private final ArrayList<Consumer<ClickReceived>> clickHandlers = new ArrayList<>();

    {
        plugin.registerListener(new PacketListener());
    }

    @Override
    public void onReceiveClick(Consumer<ClickReceived> handler) {
        clickHandlers.add(handler);
    }

    class PacketListener implements Listener {
        @EventHandler(ignoreCancelled = true)
        public void onPacketReceived(PacketReceivedEvent event) {
            if (!(event.getPacket() instanceof Packet102WindowClick)) return;

            val player = event.getPlayer();
            val packet = (Packet102WindowClick)event.getPacket();

            // it will return -999 when you click anywhere outside the UI
            if (packet.b() < 0) return;

            val windowId = (byte)packet.a;
            val slot = (short)packet.b;
            val rightClick = packet.c > 0;
            val shift = packet.f;

            val container = ((CraftPlayer)player).getHandle().activeContainer;

            val click = new ClickReceived(player, container.windowId == windowId ? container : null, windowId, slot, rightClick, shift);
            clickHandlers.forEach((handler) -> handler.accept(click));
        }
    }
}
