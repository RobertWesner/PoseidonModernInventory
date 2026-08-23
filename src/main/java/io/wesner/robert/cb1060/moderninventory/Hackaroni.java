package io.wesner.robert.cb1060.moderninventory;

import lombok.SneakyThrows;
import lombok.val;
import net.minecraft.server.Container;
import net.minecraft.server.IInventory;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public class Hackaroni {
    @SneakyThrows({NoSuchFieldException.class, IllegalAccessException.class})
    public static IInventory getInventoryBypassPrivate(Container container) {
        val field = container.getClass().getDeclaredField("a");
        field.setAccessible(true);

        return (IInventory)field.get(container);
    }

    public static net.minecraft.server.@Nullable ItemStack nmsiffy(@Nullable ItemStack itemStack) {
        if (itemStack == null) return null;

        return new net.minecraft.server.ItemStack(itemStack.getTypeId(), itemStack.getAmount(), itemStack.getDurability());
    }
}
