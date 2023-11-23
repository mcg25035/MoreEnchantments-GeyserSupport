package org.MEGeyserSupport.utils;

import org.bukkit.inventory.ItemStack;

public class ItemStackUtil {
    public static boolean equals(ItemStack a, ItemStack b){
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        boolean sameAmount = a.getAmount() == b.getAmount();
        boolean sameItem = a.isSimilar(b);
        return sameAmount && sameItem;
    }
}
