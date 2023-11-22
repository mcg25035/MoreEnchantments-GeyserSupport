package org.MEGeyserSupport;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_20_R2.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class Events implements Listener {

    @EventHandler
    void PrepareAnvilEvent(PrepareAnvilEvent e){
        System.out.println("triggered");

    }

    @EventHandler
    void InventoryClickEvent(InventoryClickEvent e){
        if (WrappedBedrockAnvilUI.noClose.contains(e.getInventory())) return;
        if (!e.getSlotType().equals(InventoryType.SlotType.CONTAINER)) return;
        WrappedBedrockAnvilUI wrappedBedrockAnvilUI = WrappedBedrockAnvilUI.getWrappedBedrockAnvilUI(e.getInventory());
        if (wrappedBedrockAnvilUI == null) return;
        if (e.getSlot() == 4){
            Bukkit.getScheduler().runTaskLater(MEGeyserSupport.getThis(), () -> {
                wrappedBedrockAnvilUI.onResultTake();
            }, 1);
        }
        wrappedBedrockAnvilUI.onUpdate();
    }

    @EventHandler
    void InventoryOpenEvent(InventoryOpenEvent e){
        if (!(e.getInventory() instanceof AnvilInventory)) return;
        if (WrappedBedrockAnvilUI.noClose.contains(e.getInventory())) return;
        Bukkit.getScheduler().runTaskLater(MEGeyserSupport.getThis(), () -> {
            WrappedBedrockAnvilUI wrappedBedrockAnvilUI = new WrappedBedrockAnvilUI((Player)(e.getPlayer()));
        }, 1);
    }

    @EventHandler
    void AsyncPlayerChatEvent(AsyncPlayerChatEvent e){
        Player player = e.getPlayer();
        if (!WrappedBedrockAnvilUI.RenameUI.players.containsKey(player.getUniqueId())) return;
        WrappedBedrockAnvilUI.RenameUI playerRenameUI = WrappedBedrockAnvilUI.RenameUI.players.get(player.getUniqueId());
        playerRenameUI.onRename(e.getMessage());
        e.setCancelled(true);
    }


}

