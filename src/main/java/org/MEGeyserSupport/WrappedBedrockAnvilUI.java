package org.MEGeyserSupport;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import org.MEGeyserSupport.utils.ItemStackUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_20_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.itemutils.ItemUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

public class WrappedBedrockAnvilUI{
    public Inventory inventory;
    public static List<Inventory> noClose = new ArrayList<>();
    private static HashMap<Player, WrappedBedrockAnvilUI> wrappedBedrockAnvilUIs = new HashMap<>();
    private String renameText = "";
    private ItemStack itemA = new ItemStack(Material.AIR);
    private ItemStack itemB = new ItemStack(Material.AIR);
    private ItemStack result = newEmptyItem();
    public boolean inventoryLock = false;
    public Instant lastUpdate = null;
    public Player player;
    public class RenameUI {
        public static HashMap<UUID, RenameUI> players = new HashMap<>();
        public UUID playerUUID;
        public String renameText;
        public Consumer<String> callback;
        public RenameUI(Player player, Consumer<String> callback){
            this.playerUUID = player.getUniqueId();
            players.put(playerUUID, this);
            this.callback = callback;
        }

        public void onRename(String renameText){
            this.renameText = renameText;
            callback.accept(this.renameText);
            players.remove(playerUUID);
        }
    }
    public static WrappedBedrockAnvilUI getWrappedBedrockAnvilUI(Player player){
        return wrappedBedrockAnvilUIs.get(player);
    }

    private static boolean isBannerItem(ItemStack item){
        Byte isBannerItem = ((Byte)(ItemUtils.itemGetNbtPath(item, "isBannerItem")));
        if (isBannerItem == null) return false;
        return (isBannerItem==0);
    }

    private ItemStack newBannerItem(int lvl, boolean isTooExpensive){
        ItemStack result = new ItemStack(Material.GRAY_STAINED_GLASS_PANE, 1);
        ItemMeta rItemMeta = result.getItemMeta();
        boolean isLevelEnough = player.getLevel() >= lvl;
        if (isTooExpensive) isLevelEnough = false;
        String colorPrefix = isLevelEnough ? "§a" : "§c";
        rItemMeta.setDisplayName("§e"+(String)(MEGeyserSupport.getThis().languageMapping.get("LevelCost")+"§f: "+colorPrefix+lvl));
        if (isTooExpensive) rItemMeta.setDisplayName("§c"+(String)(MEGeyserSupport.getThis().languageMapping.get("TooExpensive")));
        if (lvl == -1) rItemMeta.setDisplayName(" ");
        result.setItemMeta(rItemMeta);
        result = ItemUtils.itemSetNbtPath(result, "isBannerItem", true);
        return result;
    }

    private ItemStack newRenameItem(String itemName){
        ItemStack result = new ItemStack(Material.NAME_TAG, 1);
        ItemMeta rItemMeta = result.getItemMeta();
        rItemMeta.setDisplayName("§e[§f"+(String)(MEGeyserSupport.getThis().languageMapping.get("ClickToRename")+"§e]"));
        rItemMeta.setLore(List.of("§f"+itemName));
        result.setItemMeta(rItemMeta);
        result = ItemUtils.itemSetNbtPath(result, "isRenameItem", true);
        return result;
    }

    private ItemStack newEmptyItem(){
        ItemStack result = new ItemStack(Material.BARRIER, 1);
        ItemMeta itemMeta = result.getItemMeta();
        itemMeta.setDisplayName(" ");
        result.setItemMeta(itemMeta);
        return result;
    }

    private void setupRenameItem(String itemName){
        ItemStack renameItem = newRenameItem(itemName);
        inventory.setItem(0, renameItem);
    }

    private void setResult(ItemStack item){
        if (item == null) {
            item = new ItemStack(Material.AIR);
        }

        if (item.getType().equals(Material.AIR)){
            item = newEmptyItem();
        }

        item = item.clone();

        result = item;
        inventory.setItem(4, result);
    }

    private void frameworkUpdate(){
        setupRenameItem(renameText);
        inventory.setItem(3, newBannerItem(-1, false));
        inventory.setItem(4, result);
    }

    public WrappedBedrockAnvilUI(Player player){
        this.inventory = Bukkit.createInventory(player, InventoryType.HOPPER, "§e"+(String)(MEGeyserSupport.getThis().languageMapping.get("Anvil")));
        this.player = player;
        frameworkUpdate();
        player.closeInventory();
        player.openInventory(inventory);
        wrappedBedrockAnvilUIs.put(player, this);
    }

    private void rename(){
        new RenameUI(player, (renameText)->{
            this.renameText = renameText;
            setupRenameItem(renameText);
            Bukkit.getScheduler().runTask(MEGeyserSupport.getThis(), ()->player.openInventory(inventory));
            Bukkit.getScheduler().runTask(MEGeyserSupport.getThis(), this::updateResult);
        });
        player.sendMessage("§a"+(String)(MEGeyserSupport.getThis().languageMapping.get("AskForRename")));
        player.closeInventory();
    }

    private void updateResult(){
        MinecraftAnvilAPI vanillaAnvil = new MinecraftAnvilAPI(player);
        Bukkit.getScheduler().runTaskLater(MEGeyserSupport.getThis(), () -> {
            vanillaAnvil.setItemA(itemA);
            vanillaAnvil.setItemB(itemB);
            if (!vanillaAnvil.setRename(renameText)){
                renameText = "";
                setupRenameItem(renameText);
            }
        }, 1);

        Bukkit.getScheduler().runTaskLater(MEGeyserSupport.getThis(), () -> {
            vanillaAnvil.setRename(renameText);
        }, 3);

        Bukkit.getScheduler().runTaskLater(MEGeyserSupport.getThis(), () -> {
            setResult(vanillaAnvil.getResultPreview());
            int lvl = vanillaAnvil.getRepairCost();
            boolean isTooExpensive = vanillaAnvil.isTooExpensive();
            inventory.setItem(3, newBannerItem(lvl, isTooExpensive));
            vanillaAnvil.clear();
            noClose.remove(vanillaAnvil.getInventory());
        }, 4);
    }

    public void onRename(){
        rename();
    }

    public void onUpdate(){
        Bukkit.getScheduler().runTaskLater(MEGeyserSupport.getThis(), () -> {
            ItemStack currentItemA = inventory.getItem(1);
            ItemStack currentItemB = inventory.getItem(2);
            if (currentItemA == null) currentItemA = new ItemStack(Material.AIR);
            if (currentItemB == null) currentItemB = new ItemStack(Material.AIR);

            boolean itemUpdated = !ItemStackUtil.equals(itemA, currentItemA) || !ItemStackUtil.equals(itemB, currentItemB);
            itemA = currentItemA.clone();
            itemB = currentItemB.clone();
            if (itemUpdated) updateResult();
        }, 1);
    }

    public void onResultTake(ClickType clickType, InventoryAction action){
        if (
                !(
                    action.equals(InventoryAction.PICKUP_ALL)
                    || action.equals(InventoryAction.PICKUP_HALF)
                    || action.equals(InventoryAction.PICKUP_ONE)
                    || action.equals(InventoryAction.PICKUP_SOME)
                    || action.equals(InventoryAction.COLLECT_TO_CURSOR)
                    || action.equals(InventoryAction.MOVE_TO_OTHER_INVENTORY)
                )
        ) return;

        MinecraftAnvilAPI vanillaAnvil = new MinecraftAnvilAPI(player);
        vanillaAnvil.setItemA(itemA);
        vanillaAnvil.setItemB(itemB);
        inventory.clear(1);
        inventory.clear(2);
        inventory.clear(4);
        result = newEmptyItem();

        inventoryLock = true;
        Bukkit.getScheduler().runTaskLater(MEGeyserSupport.getThis(), ()->{
            vanillaAnvil.setRename(renameText);
        }, 2);
        Bukkit.getScheduler().runTaskLater(MEGeyserSupport.getThis(), ()->{
            vanillaAnvil.onResultClick(clickType, action);
        }, 3);
        Bukkit.getScheduler().runTaskLater(MEGeyserSupport.getThis(), ()->{
            frameworkUpdate();
            itemA = vanillaAnvil.getItemA();
            itemB = vanillaAnvil.getItemB();
            inventory.setItem(1, itemA);
            inventory.setItem(2, itemB);
            setResult(vanillaAnvil.getResultPreview());
        }, 4);
        Bukkit.getScheduler().runTaskLater(MEGeyserSupport.getThis(), ()->{
            inventoryLock = false;
        }, 7);

    }

    public void restoreItems(){
        if (inventoryLock){
            return;
        }
        HashMap<Integer, ItemStack> leftItems = player.getInventory().addItem(itemA);
        for (ItemStack item : leftItems.values()){
            player.getWorld().dropItem(player.getLocation(), item);
        }
        leftItems = player.getInventory().addItem(itemB);
        for (ItemStack item : leftItems.values()){
            player.getWorld().dropItem(player.getLocation(), item);
        }
        wrappedBedrockAnvilUIs.remove(player);
    }


}
