package io.wesner.robert.cb1060.moderninventory;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.bukkit.entity.Player;

import java.util.function.Consumer;

/**
 * All methods are expected to be called on the main thread for proper synchronization!
 */
public interface ModernInventoryAdapterInterface {
    void onReceiveClick(Consumer<ClickReceived> handler);

    @Data
    @AllArgsConstructor
    final class ClickReceived {
        private Player player;
        private byte windowId;
        private short slot;
        private boolean rightClick;
        private boolean shift;
    }

    class TODO_outbound_stuff {

    }
}
