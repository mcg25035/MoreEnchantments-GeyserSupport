package org.MEGeyserSupport;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.AnvilInventory;

public class Events implements Listener {

    @EventHandler
    void PrepareAnvilEvent(PrepareAnvilEvent e){
        System.out.println("triggered");

    }

    @EventHandler
    void InventoryClickEvent(InventoryClickEvent e){
        if (MinecraftAnvilAPI.isAnvilOpening(e.getInventory())) return;
        WrappedBedrockAnvilUI wrappedBedrockAnvilUI = WrappedBedrockAnvilUI.getWrappedBedrockAnvilUI(e.getInventory());
        if (wrappedBedrockAnvilUI == null) return;
        if (e.getSlot() == 4 && e.getSlotType().equals(InventoryType.SlotType.CONTAINER)){
//            e.getWhoClicked().setItemOnCursor(new ItemStack(Material.AIR));
//            e.setCurrentItem(new ItemStack(Material.AIR));
            e.setResult(Event.Result.DENY);
//            e.setCancelled(true);
//            System.out.println("cancelled");
            Bukkit.getScheduler().runTaskLater(MEGeyserSupport.getThis(), () -> {
                wrappedBedrockAnvilUI.onResultTake(e.getClick(), e.getAction());
            }, 1);
//            MinecraftAnvilAPI anvil = new MinecraftAnvilAPI((Player) e.getWhoClicked());
//            Bukkit.getScheduler().runTaskLater(MEGeyserSupport.getThis(), anvil::open, 10);
            return;
        }
        wrappedBedrockAnvilUI.onUpdate();
    }

    @EventHandler
    void InventoryOpenEvent(InventoryOpenEvent e){
        System.out.println("playersWithAnvilOpen");
        System.out.println(e.getInventory().getType());
        if (!(e.getInventory().getType().equals(InventoryType.ANVIL))) return;
//        if (WrappedBedrockAnvilUI.noClose.contains(e.getInventory())) return;
        if (MinecraftAnvilAPI.playersWithAnvilOpen.contains(e.getPlayer())) return;
        for (Player i : MinecraftAnvilAPI.playersWithAnvilOpen){
            System.out.println("playersWithAnvilOpen");
            System.out.println(i.getName());
        }
        Bukkit.getScheduler().runTaskLater(MEGeyserSupport.getThis(), () -> {
            WrappedBedrockAnvilUI wrappedBedrockAnvilUI = new WrappedBedrockAnvilUI((Player)(e.getPlayer()));
        }, 1);
    }


    @EventHandler
    void InventoryCloseEvent(InventoryCloseEvent e){
        System.out.println("playersWithAnvilClose");
        System.out.println(e.getInventory().getType());
        if (!(e.getInventory().getType().equals(InventoryType.ANVIL))) return;
        if (!MinecraftAnvilAPI.playersWithAnvilOpen.contains(e.getPlayer())) return;
        for (MinecraftAnvilAPI i : MinecraftAnvilAPI.opening) {
            System.out.println("playersWithAnvilClose");
            System.out.println(i.getPlayer().getName());
            if (!i.getPlayer().equals(e.getPlayer())) {
                continue;
            }
            i.onUIClosed();
        }
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

