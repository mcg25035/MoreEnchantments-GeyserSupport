package org.MEGeyserSupport;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.*;
import org.bukkit.inventory.AnvilInventory;

import java.time.Instant;
import java.util.List;

public class Events implements Listener {
    MEGeyserSupport main = MEGeyserSupport.getThis();
    @EventHandler
    void InventoryClickEvent(InventoryClickEvent e){
        if (!main.isBedrockPlayerWrapped((Player)(e.getWhoClicked()))){
            return;
        }
        
        WrappedBedrockAnvilUI wrappedBedrockAnvilUI = WrappedBedrockAnvilUI.getWrappedBedrockAnvilUI((Player) e.getWhoClicked());

        if (wrappedBedrockAnvilUI == null) return;
        if (e.getClickedInventory()== null) return;

        if (e.getClickedInventory().getType().equals(InventoryType.HOPPER) && wrappedBedrockAnvilUI.inventoryLock){
            e.setResult(Event.Result.DENY);
            return;
        }

        if (e.getClickedInventory().getType().equals(InventoryType.HOPPER)){
            if (wrappedBedrockAnvilUI.lastUpdate != null && wrappedBedrockAnvilUI.lastUpdate.plusMillis(100).isAfter(Instant.now())){
                e.setResult(Event.Result.DENY);
                return;
            }

            wrappedBedrockAnvilUI.lastUpdate = Instant.now();
        }


        boolean isItemInWrappedAnvil = e.getClickedInventory().getType().equals(InventoryType.HOPPER);
        boolean inItemSlot = List.of(1,2).contains(e.getSlot());
        if (isItemInWrappedAnvil && !inItemSlot) e.setResult(Event.Result.DENY);

        if (e.getSlot() == 4 && isItemInWrappedAnvil){
            e.setResult(Event.Result.DENY);
            if (e.getCurrentItem().getType().equals(Material.BARRIER)){
                return;
            }

            Bukkit.getScheduler().runTaskLater(MEGeyserSupport.getThis(), () -> {
                wrappedBedrockAnvilUI.onResultTake(e.getClick(), e.getAction());
            }, 1);
            return;
        }

        if (e.getSlot() == 3 && isItemInWrappedAnvil){
            return;
        }

        if (e.getSlot() == 0 && isItemInWrappedAnvil){
            wrappedBedrockAnvilUI.onRename();
        }

        wrappedBedrockAnvilUI.onUpdate();
    }

    @EventHandler
    void InventoryOpenEvent(InventoryOpenEvent e){
        if (!main.isBedrockPlayerWrapped((Player)(e.getPlayer()))){
            return;
        }

        if (WrappedBedrockAnvilUI.RenameUI.players.containsKey(e.getPlayer().getUniqueId())){
            e.setCancelled(true);
            Bukkit.getScheduler().runTaskLater(MEGeyserSupport.getThis(), () -> e.getPlayer().closeInventory(), 1);
            e.getPlayer().sendMessage("§c"+(String)(MEGeyserSupport.getThis().languageMapping.get("WrongAccessDuringRenaming.Inventory")));
            e.getPlayer().sendMessage("§a"+(String)(MEGeyserSupport.getThis().languageMapping.get("AskForRename")));
            return;
        }


        if (!(e.getInventory().getType().equals(InventoryType.ANVIL))) return;
        if (MinecraftAnvilAPI.playersWithAnvilOpen.contains(e.getPlayer())) return;
        Bukkit.getScheduler().runTaskLater(MEGeyserSupport.getThis(), () -> {
            new WrappedBedrockAnvilUI((Player)(e.getPlayer()));
        }, 1);
    }


    @EventHandler
    void InventoryCloseEvent(InventoryCloseEvent e){
        if (!main.isBedrockPlayerWrapped((Player)(e.getPlayer()))){
            return;
        }

        WrappedBedrockAnvilUI wrappedBedrockAnvilUI = WrappedBedrockAnvilUI.getWrappedBedrockAnvilUI((Player) e.getPlayer());
        if (wrappedBedrockAnvilUI != null){
            if (!WrappedBedrockAnvilUI.RenameUI.players.containsKey(e.getPlayer().getUniqueId())){
                wrappedBedrockAnvilUI.restoreItems();
            }
        }

        if (!(e.getInventory().getType().equals(InventoryType.ANVIL))) return;
        if (!MinecraftAnvilAPI.playersWithAnvilOpen.contains(e.getPlayer())) return;
        for (MinecraftAnvilAPI i : MinecraftAnvilAPI.opening) {
            if (!i.getPlayer().equals(e.getPlayer())) continue;
            i.onUIClosed();
        }
    }

    @EventHandler
    void AsyncPlayerChatEvent(AsyncPlayerChatEvent e){
        if (!main.isBedrockPlayerWrapped(e.getPlayer())){
            return;
        }

        Player player = e.getPlayer();
        if (!WrappedBedrockAnvilUI.RenameUI.players.containsKey(player.getUniqueId())) return;
        WrappedBedrockAnvilUI.RenameUI playerRenameUI = WrappedBedrockAnvilUI.RenameUI.players.get(player.getUniqueId());
        playerRenameUI.onRename(e.getMessage());
        e.setCancelled(true);
    }

    @EventHandler
    void PlayerMoveEvent(PlayerMoveEvent e){
        if (!main.isBedrockPlayerWrapped(e.getPlayer())){
            return;
        }

        if (!WrappedBedrockAnvilUI.RenameUI.players.containsKey(e.getPlayer().getUniqueId())){
            return;
        }
        e.setCancelled(true);
        e.getPlayer().sendMessage("§c"+(String)(MEGeyserSupport.getThis().languageMapping.get("WrongAccessDuringRenaming.Move")));
        e.getPlayer().sendMessage("§a"+(String)(MEGeyserSupport.getThis().languageMapping.get("AskForRename")));

    }

    @EventHandler
    void PlayerCommandPreprocessEvent(PlayerCommandPreprocessEvent e){
        if (!main.isBedrockPlayerWrapped(e.getPlayer())){
            return;
        }

        if (!WrappedBedrockAnvilUI.RenameUI.players.containsKey(e.getPlayer().getUniqueId())){
            return;
        }
        e.setCancelled(true);
        e.getPlayer().sendMessage("§c"+(String)(MEGeyserSupport.getThis().languageMapping.get("WrongAccessDuringRenaming.SendCommand")));
        e.getPlayer().sendMessage("§a"+(String)(MEGeyserSupport.getThis().languageMapping.get("AskForRename")));
    }

    @EventHandler
    void PlayerQuitEvent(PlayerQuitEvent e){
        if (!main.isBedrockPlayerWrapped(e.getPlayer())){
            return;
        }
        
        WrappedBedrockAnvilUI wrappedBedrockAnvilUI = WrappedBedrockAnvilUI.getWrappedBedrockAnvilUI(e.getPlayer());
        if (wrappedBedrockAnvilUI == null) return;
        wrappedBedrockAnvilUI.restoreItems();
        if (WrappedBedrockAnvilUI.RenameUI.players.containsKey(e.getPlayer().getUniqueId())){
            WrappedBedrockAnvilUI.RenameUI.players.remove(e.getPlayer().getUniqueId());
        }

    }

}

