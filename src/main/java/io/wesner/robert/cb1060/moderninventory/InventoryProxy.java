package io.wesner.robert.cb1060.moderninventory;

import lombok.Getter;
import lombok.val;
import net.minecraft.server.EntityPlayer;
import net.minecraft.server.IInventory;
import net.minecraft.server.Packet103SetSlot;
import org.bukkit.Material;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.craftbukkit.inventory.CraftInventory;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;

/**
 * Convenient access to inventories with packet level updates!
 * Also fixes the completely brain-dead decision of Bukkit to return AIR items instead of null, which crashes clients.
 */
@NullMarked
public class InventoryProxy {
    @Getter
    private final Type type;
    private final IInventory nmsInventory;
    private final CraftInventory inventory;
    private final EntityPlayer player;

    /**
     * Only used for sending update packets.
     */
    private final int packetOffset;

    // Java generics are utter ass, why the fuck can't I just have OR generics/unions? Not even in modern, wtf?
    // Can't even put a `permits` hack in here because java8...
    // GUESS SIX FUCKING OVERLOADS IT IS!
    private InventoryProxy(Type type, Object inventory, Object player, int packetOffset) {
        this.type = type;

        if (inventory instanceof IInventory) {
            this.nmsInventory = (IInventory)inventory;
            this.inventory = new CraftInventory(this.nmsInventory);
        } else {
            this.inventory = (CraftInventory)inventory;
            this.nmsInventory = this.inventory.getInventory();
        }

        if (player instanceof EntityPlayer) {
            this.player = (EntityPlayer)player;
        } else {
            this.player = ((CraftPlayer)player).getHandle();
        }

        this.packetOffset = packetOffset;
    }

    public InventoryProxy(Type type, IInventory inventory, EntityPlayer player, int packetOffset) { this(type, (Object)inventory, player, packetOffset); }
    public InventoryProxy(Type type, Inventory inventory, EntityPlayer player, int packetOffset) { this(type, (Object)inventory, player, packetOffset); }
    public InventoryProxy(Type type, IInventory inventory, Player player, int packetOffset) { this(type, (Object)inventory, player, packetOffset); }
    public InventoryProxy(Type type, Inventory inventory, Player player, int packetOffset) { this(type, (Object)inventory, player, packetOffset); }
    public InventoryProxy(Type type, IInventory inventory, CraftPlayer player, int packetOffset) { this(type, (Object)inventory, player, packetOffset); }
    public InventoryProxy(Type type, Inventory inventory, CraftPlayer player, int packetOffset) { this(type, (Object)inventory, player, packetOffset); }

    public @Nullable ItemStack get(int index) {
        // TODO: check index in range

        val item = inventory.getItem(index);
        // fuck you!
        if (item != null && item.getType() == Material.AIR) {
            return null;
        }

        return item;
    }

    public void set(int index, @Nullable ItemStack item) {
        // TODO: check index in range

        inventory.setItem(index, item);
        nmsInventory.update();
        player.netServerHandler.sendPacket(
            new Packet103SetSlot(player.activeContainer.windowId, packetOffset + index, Hackaroni.nmsiffy(item))
        );
    }

    public HashMap<Integer, ItemStack> add(ItemStack item) {
        return new CraftInventory(nmsInventory).addItem(item);
    }

    public enum Type {
        PLAYER,
        CRAFTING,
        CHEST,
        WORKBENCH,
        FURNACE,
        DISPENSER,
    }
}
