package io.wesner.robert.cb1060.moderninventory;

import lombok.SneakyThrows;
import lombok.val;
import net.minecraft.server.Container;
import net.minecraft.server.IInventory;

public class Hackaroni {
    @SneakyThrows({NoSuchFieldException.class, IllegalAccessException.class})
    public static IInventory getInventoryBypassPrivate(Container container) {
        val field = container.getClass().getDeclaredField("a");
        field.setAccessible(true);

        return (IInventory)field.get(container);
    }
}
