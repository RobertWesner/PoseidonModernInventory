package io.wesner.robert.cb1060.moderninventory;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.minecraft.server.EntityPlayer;
import net.minecraft.server.IInventory;
import net.minecraft.server.ItemStack;
import net.minecraft.server.Packet103SetSlot;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Convenient access to inventories with packet level updates!
 */
@NullMarked
@RequiredArgsConstructor
public class InventoryProxy {
    @Getter
    private final Type type;

    private final IInventory inventory;

    /**
     * Only used for sending update packets.
     */
    private final int packetOffset;

    private final EntityPlayer player;

    public @Nullable ItemStack get(int index) {
        // TODO: check index in range

        return inventory.getItem(index);
    }

    public InventoryProxy set(int index, @Nullable ItemStack item) {
        // TODO: check index in range

        inventory.setItem(index, item);
        inventory.update();
        player.netServerHandler.sendPacket(
            new Packet103SetSlot(player.activeContainer.windowId, packetOffset + index, item)
        );

        return this;
    }

    // TODO: add

    enum Type {
        PLAYER,
        CRAFTING,
        CHEST,
        WORKBENCH,
        FURNACE,
        DISPENSER,
    }
}
