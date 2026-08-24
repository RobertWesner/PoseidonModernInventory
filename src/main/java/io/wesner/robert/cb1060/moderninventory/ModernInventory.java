package io.wesner.robert.cb1060.moderninventory;

import io.wesner.robert.cb1060.moderninventory.adapter.ModernInventoryAdapterInterface;
import io.wesner.robert.cb1060.moderninventory.adapter.PoseidonV1PacketAdapter;
import lombok.val;
import net.minecraft.server.*;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.logging.Logger;

// TODO: transactions? would be important! (and transaction event)

@NullMarked
public class ModernInventory extends JavaPlugin {
    @SuppressWarnings("NotNullFieldNotInitialized")
    public static ModernInventory plugin;

    public static HashSet<Material> fuel = new HashSet<>(
        Arrays.asList(
            Material.WOOD,
            Material.SAPLING,
            // not used as fuel by shift clicking in modern!
            // Material.LOG,
            Material.NOTE_BLOCK,
            Material.BOOKSHELF,
            Material.WOOD_STAIRS,
            Material.CHEST,
            Material.WORKBENCH,
            Material.SIGN_POST,
            Material.WOODEN_DOOR,
            Material.WALL_SIGN,
            Material.WOOD_PLATE,
            Material.JUKEBOX,
            Material.FENCE,
            Material.LOCKED_CHEST,
            Material.TRAP_DOOR,
            Material.COAL,
            Material.STICK,
            Material.LAVA_BUCKET
        )
    );

    public static HashSet<Material> smeltable = new HashSet<>(
        Arrays.asList(
            Material.COBBLESTONE,
            Material.SAND,
            Material.GOLD_ORE,
            Material.IRON_ORE,
            Material.LOG,
            Material.CACTUS,
            Material.PORK,
            Material.CLAY_BALL,
            Material.RAW_FISH
        )
    );

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
        val playerInventory = new InventoryProxy(
            InventoryProxy.Type.PLAYER,
            player.getInventory(),
            player,
            0
        );

        // I loathe Java. Thank you for giving us BiConsumer instead of arbitrary length...
        EndMySuffering<ItemStack, InventoryProxy, Integer> craftAll = ( result, grid, gridSize) -> {
            // this only works because there is no vintage story style amount>1 crafting!
            val amount = Arrays.stream(grid.getInventory().getContents())
                .filter(Objects::nonNull)
                .mapToInt(ItemStack::getAmount)
                .min()
                .orElse(0);

            if (
                playerInventory.getInventory().firstEmpty() == -1
                    && playerInventory
                    .getInventory()
                    .all(result.getType()).entrySet().stream()
                    .noneMatch((entry) -> entry.getValue().getAmount() > result.getType().getMaxStackSize() - result.getAmount())
            ) {
                return;
            }

            // loop seems stupid here, but it is much safer in almost full inventories!
            int crafted;
            for (crafted = 0; crafted < amount; crafted++) {
                val remainder = playerInventory.add(result);

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
                        clickedInventory,
                        translatePlayerInventory(clickReceived.getSlot() - 10),
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

                    val item = clickedInventory.get(translatePlayerInventory(clickReceived.getSlot() - 3));
                    int targetSlot;
                    assert item != null;
                    if (smeltable.contains(item.getType())) {
                        targetSlot = 0;
                    } else if (fuel.contains(item.getType())) {
                        targetSlot = 1;
                    } else {
                        return;
                    }

                    transferFromSlot(
                        clickedInventory,
                        translatePlayerInventory(clickReceived.getSlot() - 3),
                        new InventoryProxy(
                            InventoryProxy.Type.FURNACE,
                            Hackaroni.getInventoryBypassPrivate(nmsPlayer.activeContainer),
                            player,
                            0
                        ),
                        targetSlot
                    );
                } else if (clickReceived.getSlot() == 2) {
                    // furnace -> player
                    // already works in vanilla
                }
            } else if (nmsPlayer.activeContainer instanceof ContainerDispenser) {
                if (clickedInventory.getType() == InventoryProxy.Type.PLAYER) {
                    // player -> dispenser
                    transferFromSlot(
                        clickedInventory,
                        translatePlayerInventory(clickReceived.getSlot() - 9),
                        new InventoryProxy(
                            InventoryProxy.Type.DISPENSER,
                            Hackaroni.getInventoryBypassPrivate(nmsPlayer.activeContainer),
                            player,
                            0
                        )
                    );
                } else {
                    // dispenser -> player
                    transferFromSlot(
                        clickedInventory,
                        clickReceived.getSlot(),
                        playerInventory
                    );
                }
            } else if (nmsPlayer.activeContainer instanceof ContainerPlayer) {
                // only crafting, no armor (for now)
                 if (clickReceived.getSlot() == 0 && clickedInventory.get(0) != null) {
                    // result -> player
                     craftAll.accept(
                         Objects.requireNonNull(clickedInventory.get(0)),
                         new InventoryProxy(InventoryProxy.Type.CRAFTING, ((ContainerPlayer)nmsPlayer.activeContainer).craftInventory, nmsPlayer, 0),
                         4
                     );
                 }
            }
        } else if (clickReceived.isRightClick()) {

        }
    }

    public void registerListener(Listener listener) {
        getServer().getPluginManager().registerEvents(listener, this);
    }

    private void transferFromSlot(InventoryProxy from, int slotFrom, InventoryProxy to, @Nullable Integer slotTo) {
        val itemStack = from.get(slotFrom);
        if (itemStack == null) return;

        // cannot be more than one, as you cannot have more than one in slot, duh
        Map<Integer, ItemStack> remaining = new HashMap<>();
        if (slotTo == null) {
            remaining = to.add(itemStack);
            assert remaining.size() < 2;
        } else {
            val target = to.get(slotTo);
            if (target == null) {
                to.set(slotTo, itemStack);
            } else if (target.getType() == itemStack.getType() && target.getData() == itemStack.getData()) {
                val availableSpace = itemStack.getType().getMaxStackSize() - target.getAmount();
                val filledTarget = target.clone();
                filledTarget.setAmount(Math.min(target.getAmount() + itemStack.getAmount(), itemStack.getMaxStackSize()));
                to.set(slotTo, filledTarget);
                if (itemStack.getAmount() > availableSpace) {
                    val remainder = itemStack.clone();
                    remainder.setAmount(itemStack.getAmount() - availableSpace);
                    remaining.put(0, remainder);
                }
            } else {
                return;
            }
        }

        if (remaining.isEmpty()) {
            from.set(slotFrom, null);
        } else {
            val newItem = itemStack.clone();
            newItem.setAmount(newItem.getAmount() - remaining.get(0).getAmount());

            from.set(slotFrom, newItem);
        }
    }

    private void transferFromSlot(InventoryProxy from, int slot, InventoryProxy to) {
        transferFromSlot(from, slot, to, null);
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
