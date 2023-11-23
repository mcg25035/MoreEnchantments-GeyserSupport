package org.MEGeyserSupport;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Sound;
import org.bukkit.craftbukkit.v1_20_R2.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class MinecraftAnvilAPI
{
    public static Set<MinecraftAnvilAPI> opening = new HashSet<>();
    public static Set<Player> playersWithAnvilOpen = new HashSet<>();
    private Player player;
    private AnvilInventory inventory;
    private InventoryView view;
    private boolean opened = false;

    public static boolean isAnvilOpening(Inventory anvil){
        return opening.contains(anvil);
    }
    public MinecraftAnvilAPI(Player player){
        this.player = player;
        ServerPlayer splayer = ((CraftPlayer) player).getHandle();
        int nCC = splayer.nextContainerCounter();
        net.minecraft.world.entity.player.Inventory inv = splayer.getInventory();
        ContainerLevelAccess containerAccess = ContainerLevelAccess.create(splayer.level(), splayer.blockPosition());
        AnvilMenu menu = new AnvilMenu(nCC, inv, containerAccess);
        menu.checkReachable = false;
        menu.setTitle(Component.literal("Anvil"));
        view = menu.getBukkitView();
        inventory = (AnvilInventory) view.getTopInventory();
    }

    public void setItemA(org.bukkit.inventory.ItemStack item){
        inventory.setItem(0, item);
    }

    public void setItemB(org.bukkit.inventory.ItemStack item){
        inventory.setItem(1, item);
    }

    public ItemStack getResultPreview(){
        return inventory.getItem(2);
    }

    public Inventory getInventory(){
        return inventory;
    }

    public InventoryView getView(){
        return view;
    }

    public Player getPlayer(){
        return player;
    }

    public void onResultClick(ClickType clickType, InventoryAction action){
        if (player.getGameMode().equals(GameMode.SURVIVAL)){
            player.setLevel(player.getLevel() - inventory.getRepairCost());
        }
        InventoryClickEvent event = new InventoryClickEvent(view, InventoryType.SlotType.RESULT, 2, clickType, action);
        Bukkit.getPluginManager().callEvent(event);
        player.setItemOnCursor(event.getCursor());
//        player.
//        player.getLocation().getWorld().playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 1.0F, (float)(1.035 - Math.random() * 0.15));

    }

    public void open(){
        opened = true;
        playersWithAnvilOpen.add(player);
        opening.add(this);
        for (MinecraftAnvilAPI i : opening){
            System.out.println("opening");
            System.out.println(i.player.getName());
        }
        player.openInventory(view);

    }

    public void onUIClosed(){
        if (!opened) return;
        opening.remove(this);
        playersWithAnvilOpen.remove(player);
        opened = false;
    }

    public void close(){
        playersWithAnvilOpen.remove(player);
        player.closeInventory();
    }

    public void clear(){
        inventory.clear();
    }
}
