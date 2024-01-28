package org.MEGeyserSupport;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.craftbukkit.v1_20_R3.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_20_R3.inventory.CraftInventoryAnvil;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Field;
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
        int playerLevel = player.getLevel();
        if (player.getGameMode().equals(GameMode.SURVIVAL) && !isTooExpensive()){
            playerLevel -= getRepairCost();
        }
        InventoryClickEvent event = new InventoryClickEvent(view, InventoryType.SlotType.RESULT, 2, clickType, action);
        Bukkit.getPluginManager().callEvent(event);
        int finalPlayerLevel = playerLevel;
        Bukkit.getScheduler().runTaskLater(MEGeyserSupport.getThis(),()->{
            HashMap<Integer, ItemStack> leftItems = player.getInventory().addItem(player.getItemOnCursor());
            for (ItemStack item : leftItems.values()){
                player.getWorld().dropItem(player.getLocation(), item);
            }
            player.setItemOnCursor(new ItemStack(Material.AIR));
            if (finalPlayerLevel >= 0){
                player.setLevel(finalPlayerLevel);
            }
        },1);
    }

    public boolean setRename(String rename){
        if (rename.contains("§")) rename = rename.replace("§", "");
        if (rename.isBlank()) return false;
        if (rename.isEmpty()) return false;
        try {
            CraftInventoryAnvil anvil = (CraftInventoryAnvil) inventory;
            Field field = CraftInventoryAnvil.class.getDeclaredField("container");
            field.setAccessible(true);
            AnvilMenu container = (AnvilMenu) field.get(anvil);
            container.setItemName(rename);
            return true;
        }
        catch (Exception e){
            return false;
        }
    }

    public void open(){
        opened = true;
        playersWithAnvilOpen.add(player);
        opening.add(this);
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

    public int getRepairCost(){
        return inventory.getRepairCost();
    }

    public boolean isTooExpensive(){
        return inventory.getRepairCost() > inventory.getMaximumRepairCost();
    }

    public ItemStack getItemA(){
        return inventory.getItem(0);
    }

    public ItemStack getItemB(){
        return inventory.getItem(1);
    }

}
