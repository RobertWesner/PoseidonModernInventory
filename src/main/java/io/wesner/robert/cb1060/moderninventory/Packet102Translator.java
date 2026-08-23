package io.wesner.robert.cb1060.moderninventory;

import lombok.RequiredArgsConstructor;
import net.minecraft.server.*;
import org.jspecify.annotations.NullMarked;

import java.util.List;

import lombok.val;

@NullMarked
@RequiredArgsConstructor
public class Packet102Translator {
    final Container container;
    final EntityPlayer player;

    public InventoryProxy proxy(int i) {
        if (container instanceof ContainerChest) {
            val inventory = Hackaroni.getInventoryBypassPrivate(container);
            val size = inventory.getSize();

            if (i >= size) {
                // player inv
                return playerInventoryProxy(size);
            } else {
                // chest inv
                return new InventoryProxy(
                    InventoryProxy.Type.CHEST,
                    inventory,
                    0,
                    player
                );
            }
        } else if (container instanceof ContainerWorkbench) {
            if (i == 0) {
                return new InventoryProxy(
                    InventoryProxy.Type.WORKBENCH,
                    ((ContainerWorkbench) container).resultInventory,
                    0,
                    player
                );
            } else if (i <= 9) {
                return new InventoryProxy(
                    InventoryProxy.Type.WORKBENCH,
                    ((ContainerWorkbench) container).craftInventory,
                    1,
                    player
                );
            } else {
                return playerInventoryProxy(10);
            }
        } else if (container instanceof ContainerFurnace) {
            val inventory = Hackaroni.getInventoryBypassPrivate(container);

            switch (i) {
                // top slot
                case 0:
                    return new InventoryProxy(
                        InventoryProxy.Type.FURNACE,
                        inventory,
                        0,
                        player
                    );
                // bottom slot
                case 1:
                    return new InventoryProxy(
                        InventoryProxy.Type.FURNACE,
                        inventory,
                        1,
                        player
                    );
                // result slot
                case 2:
                    return new InventoryProxy(
                        InventoryProxy.Type.FURNACE,
                        inventory,
                        2,
                        player
                    );
                default:
                    return playerInventoryProxy(3);
            }
        } else if (container instanceof ContainerDispenser) {
            val inventory = Hackaroni.getInventoryBypassPrivate(container);

            if (i <= 8) {
                return new InventoryProxy(
                    InventoryProxy.Type.DISPENSER,
                    inventory,
                    0,
                    player
                );
            } else {
                return playerInventoryProxy(9);
            }
        } else if (container instanceof ContainerPlayer) {
            if (i == 0) {
                return new InventoryProxy(
                    InventoryProxy.Type.CRAFTING,
                    ((ContainerPlayer) container).resultInventory,
                    0,
                    player
                );
            } else if (i <= 4) {
                return new InventoryProxy(
                    InventoryProxy.Type.CRAFTING,
                    ((ContainerPlayer) container).craftInventory,
                    1,
                    player
                );
            } else if (i <= 8) {
                // could have done it in some silly math expression, but readability beats conciseness here
                int slot = 0;
                switch (i) {
                    // helmet=5->39
                    case 5:
                        slot = 39;
                        break;
                    // chest=6->38
                    case 6:
                        slot = 38;
                        break;
                    // legs=7->37
                    case 7:
                        slot = 37;
                        break;
                    // boots=8->36
                    case 8:
                        slot = 36;
                        break;
                }

                return new InventoryProxy(
                    InventoryProxy.Type.PLAYER,
                    player.inventory,
                    slot,
                    player
                );
            } else {
                return playerInventoryProxy(9);
            }
        }

        throw new RuntimeException("Unhandled container type \"" + container.getClass() +"\".");
    }

    private InventoryProxy playerInventoryProxy(int from) {
        return new InventoryProxy(
            InventoryProxy.Type.PLAYER,
            player.inventory,
            from,
            player
        );
    }
}
