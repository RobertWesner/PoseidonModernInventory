package io.wesner.robert.cb1060.moderninventory;

import io.wesner.robert.cb1060.moderninventory.adapter.ModernInventoryAdapterInterface;
import io.wesner.robert.cb1060.moderninventory.adapter.PoseidonV1PacketAdapter;
import lombok.val;
import net.minecraft.server.*;
import org.bukkit.ChatColor;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.craftbukkit.inventory.CraftInventory;
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

        val player = ((CraftPlayer)clickReceived.getPlayer()).getHandle();
        val world = clickReceived.getPlayer().getWorld();
        val proxy = new Packet102Translator(clickReceived.getContainer(), player).proxy(clickReceived.getSlot());

        if (clickReceived.isShift()) {
            if (player.activeContainer instanceof ContainerChest) {
                // Chests already work natively so uhhhh
            } else if (player.activeContainer instanceof ContainerWorkbench) {
                // workbenches only need player->grid and result->player (ALL stacks!)

                if (proxy.getType() == InventoryProxy.Type.PLAYER) {
                    // player -> grid
                    addFromProxy(translatePlayerInventory(clickReceived.getSlot() - 10), proxy, ((ContainerWorkbench) player.activeContainer).craftInventory);
                } else if (clickReceived.getSlot() == 0 && proxy.get(0) != null) {
                    // result -> player
                    val grid = new CraftInventory(((ContainerWorkbench) player.activeContainer).craftInventory);
                    val gridProxy = new InventoryProxy(InventoryProxy.Type.CRAFTING, grid.getInventory(), 0, player);

                    val rawResult = proxy.get(0);
                    assert rawResult != null; // I miss kotlin so damn bad

                    // this only works because there is no vintage story style amount>1 crafting!
                    val amount = Arrays.stream(grid.getContents())
                        .filter(Objects::nonNull)
                        .mapToInt(ItemStack::getAmount)
                        .min()
                        .orElse(0);

                    val playerInventory = clickReceived.getPlayer().getInventory();
                    val result = new ItemStack(rawResult.id, rawResult.count, (short)rawResult.damage);

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
                                world.dropItemNaturally(clickReceived.getPlayer().getLocation(), it)
                            );

                            break;
                        }
                    }

                    for (int i = 0; i < 9; i++) {
                        val g = gridProxy.get(i);
                        if (g == null) continue;

                        // cannot be lower than that!
                        if (g.count == crafted) {
                            gridProxy.set(i, null);

                            continue;
                        }

                        val clone = g.cloneItemStack();
                        clone.count -= crafted;
                        gridProxy.set(i, clone);
                    }

                    //noinspection deprecation
                    clickReceived.getPlayer().updateInventory();
                }
            } else if (player.activeContainer instanceof ContainerFurnace) {
                if (proxy.getType() == InventoryProxy.Type.PLAYER) {
                    // player -> furnace
                    // TODO: differentiate between fuel and smeltable!!! also wood in both depending on which one is full?
                    val tef = ((TileEntityFurnace)Hackaroni.getInventoryBypassPrivate(player.activeContainer));
                    val block = world.getBlockAt(tef.x, tef.y, tef.z);

                    // TODO
                } else if (clickReceived.getSlot() == 2) {
                    // furnace -> player

                    // TODO
                }
            } else if (player.activeContainer instanceof ContainerDispenser) {
                // TODO should be really easy
            } else if (player.activeContainer instanceof ContainerPlayer) {
                // only crafting, no armor (for now)

                // TODO
            }
        } else if (clickReceived.isRightClick()) {

        }
    }

    public void registerListener(Listener listener) {
        getServer().getPluginManager().registerEvents(listener, this);
    }

    private void addFromProxy(int slot, InventoryProxy fromProxy, IInventory toInventory) {
        val rawItem = fromProxy.get(slot);
        assert rawItem != null;
        val itemStack = new ItemStack(rawItem.id, rawItem.count, (short)rawItem.damage);

        val remaining = new CraftInventory(toInventory).addItem(itemStack);
        if (remaining.isEmpty()) {
            fromProxy.set(slot, null);
        } else {
            val newItem = rawItem.cloneItemStack();
            newItem.count -= remaining.get(0).getAmount();

            fromProxy.set(slot, newItem);
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
}
