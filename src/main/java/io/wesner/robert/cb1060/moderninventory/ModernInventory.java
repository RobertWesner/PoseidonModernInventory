package io.wesner.robert.cb1060.moderninventory;

import io.wesner.robert.cb1060.moderninventory.adapter.ModernInventoryAdapterInterface;
import io.wesner.robert.cb1060.moderninventory.adapter.PoseidonV1PacketAdapter;
import lombok.val;
import net.minecraft.server.*;
import org.bukkit.ChatColor;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.jspecify.annotations.NullMarked;

import java.util.Arrays;
import java.util.Objects;
import java.util.logging.Logger;

// TODO: transactions? would be important! (and transaction event)

@NullMarked
public class ModernInventory extends JavaPlugin {
    @SuppressWarnings("NotNullFieldNotInitialized")
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
        new PoseidonV1PacketAdapter().onReceiveClick(this::handleReceiveClick);

        logger.info("Enabled modern inventory backport.");
    }

    public void handleReceiveClick(ModernInventoryAdapterInterface.ClickReceived clickReceived) {
        if (clickReceived.getContainer() == null) {
            plugin.getServer().broadcastMessage(ChatColor.RED + "WTF!"); // TODO: lol

            return;
        }

        val player = (CraftPlayer)clickReceived.getPlayer();
        val nmsPlayer = player.getHandle();
        val world = clickReceived.getPlayer().getWorld();
        val clickedInventory = new Packet102Translator(clickReceived.getContainer(), nmsPlayer).proxy(clickReceived.getSlot());

        // I loathe Java. Thank you for giving us BiConsumer instead of arbitrary length...
        EndMySuffering<ItemStack, InventoryProxy, Integer> craftAll = ( result, grid, gridSize) -> {
            // this only works because there is no vintage story style amount>1 crafting!
            val amount = Arrays.stream(grid.getInventory().getContents())
                .filter(Objects::nonNull)
                .mapToInt(ItemStack::getAmount)
                .min()
                .orElse(0);

            val playerInventory = clickReceived.getPlayer().getInventory();

            if (
                playerInventory.firstEmpty() == -1
                    && playerInventory
                    .all(result.getType()).entrySet().stream()
                    .noneMatch((entry) -> entry.getValue().getAmount() > result.getType().getMaxStackSize() - result.getAmount())
            ) {
                return;
            }

            // loop seems stupid here, but it is much safer in almost full inventories!
            int crafted;
            for (crafted = 0; crafted < amount; crafted++) {
                val remainder = playerInventory.addItem(result);

                if (!remainder.isEmpty()) {
                    remainder.forEach((ignored, it) ->
                        // TODO: make this vanish-safe by using player events that can get cancelled
                        world.dropItemNaturally(clickReceived.getPlayer().getLocation(), it)
                    );

                    break;
                }
            }

            for (int i = 0; i < gridSize; i++) {
                val g = grid.get(i);
                if (g == null) continue;

                // cannot be lower than that!
                if (g.getAmount() == crafted) {
                    grid.set(i, null);

                    continue;
                }

                val clone = g.clone();
                clone.setAmount(g.getAmount() - crafted);
                grid.set(i, clone);
            }

            //noinspection deprecation
            clickReceived.getPlayer().updateInventory();
        };

        if (clickReceived.isShift()) {
            if (nmsPlayer.activeContainer instanceof ContainerChest) {
                // Chests already work natively so uhhhh
            } else if (nmsPlayer.activeContainer instanceof ContainerWorkbench) {
                // workbenches only need player->grid and result->player (ALL stacks!)

                if (clickedInventory.getType() == InventoryProxy.Type.PLAYER) {
                    // player -> grid
                    transferFromSlot(
                        translatePlayerInventory(clickReceived.getSlot() - 10),
                        clickedInventory,
                        new InventoryProxy(
                            InventoryProxy.Type.CRAFTING,
                            ((ContainerWorkbench) nmsPlayer.activeContainer).craftInventory /*btw .craftInventory is NOT a CraftInventory :)*/,
                            nmsPlayer,
                            1
                        )
                    );
                } else if (clickReceived.getSlot() == 0 && clickedInventory.get(0) != null) {
                    // result -> player
                    craftAll.accept(
                        // no `!!`? :wilted_rose:
                        Objects.requireNonNull(clickedInventory.get(0)),
                        new InventoryProxy(InventoryProxy.Type.CRAFTING, ((ContainerWorkbench)nmsPlayer.activeContainer).craftInventory, nmsPlayer, 0),
                        9
                    );
                }
            } else if (nmsPlayer.activeContainer instanceof ContainerFurnace) {
                if (clickedInventory.getType() == InventoryProxy.Type.PLAYER) {
                    // player -> furnace
                    // TODO: differentiate between fuel and smeltable!!! also wood in both depending on which one is full? how does modern do it?
                    val tef = ((TileEntityFurnace)Hackaroni.getInventoryBypassPrivate(nmsPlayer.activeContainer));
                    val block = world.getBlockAt(tef.x, tef.y, tef.z);

                    // TODO
                } else if (clickReceived.getSlot() == 2) {
                    // furnace -> player

                    // TODO
                }
            } else if (nmsPlayer.activeContainer instanceof ContainerDispenser) {
                // TODO should be really easy
            } else if (nmsPlayer.activeContainer instanceof ContainerPlayer) {
                // only crafting, no armor (for now)

                 if (clickReceived.getSlot() == 0 && clickedInventory.get(0) != null) {
                    // result -> player
                     craftAll.accept(
                         // no `!!`? :wilted_rose:
                         Objects.requireNonNull(clickedInventory.get(0)),
                         new InventoryProxy(InventoryProxy.Type.CRAFTING, ((ContainerPlayer)nmsPlayer.activeContainer).craftInventory, nmsPlayer, 0),
                         4
                     );

                    // TODO
                 }

            }
        } else if (clickReceived.isRightClick()) {

        }
    }

    public void registerListener(Listener listener) {
        getServer().getPluginManager().registerEvents(listener, this);
    }

    private void transferFromSlot(int slot, InventoryProxy from, InventoryProxy to) {
        val itemStack = from.get(slot);
        if (itemStack == null) return;

        // cannot be more than one, as you cannot have more than one in slot, duh
        val remaining = to.add(itemStack);
        assert remaining.size() < 2;

        if (remaining.isEmpty()) {
            from.set(slot, null);
        } else {
            val newItem = itemStack.clone();
            newItem.setAmount(newItem.getAmount() - remaining.get(0).getAmount());

            from.set(slot, newItem);
        }
    }

    /**
     * All packet level stuff has hotbar as last 9 slots, but inventory access is cursed and 0..8 is hotbar,
     * while counting 9+ from the top row...
     * This turns that packet stuff into inv access bullshit <3
     */
    private int translatePlayerInventory(int indexWithoutPacketOffset) {
        return (indexWithoutPacketOffset + 9) % 36;
    }

    private interface EndMySuffering<A, B, C> {
        void accept(A a, B b, C c);

        default EndMySuffering<A, B, C> andThen(EndMySuffering<? super A, ? super B, ? super C> e) {
            Objects.requireNonNull(e);
            return (f, g, h) -> {
                this.accept(f, g, h);
                e.accept(f, g, h);
            };
        }
    }
}
