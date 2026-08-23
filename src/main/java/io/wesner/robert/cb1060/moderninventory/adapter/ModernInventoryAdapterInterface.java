package io.wesner.robert.cb1060.moderninventory.adapter;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import net.minecraft.server.Container;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;

/**
 * All methods are expected to be called on the main thread for proper synchronization!
 */
@NullMarked
public interface ModernInventoryAdapterInterface {
    void onReceiveClick(Consumer<ClickReceived> handler);

    @Data
    @RequiredArgsConstructor
    final class ClickReceived {
        private final Player player;
        @Nullable private final Container container;
        private final byte windowId;
        private final short slot;
        private final boolean rightClick;
        private final boolean shift;
    }
}
