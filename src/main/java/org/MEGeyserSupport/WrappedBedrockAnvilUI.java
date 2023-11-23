package org.MEGeyserSupport;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import org.MEGeyserSupport.utils.ItemStackUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_20_R2.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.itemutils.ItemUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

public class WrappedBedrockAnvilUI{
    public Inventory inventory;
    public static List<Inventory> noClose = new ArrayList<>();
    private static HashMap<Inventory, WrappedBedrockAnvilUI> wrappedBedrockAnvilUIs = new HashMap<>();
    private ItemStack itemA = new ItemStack(Material.AIR);
    private ItemStack itemB = new ItemStack(Material.AIR);
    private ItemStack result = new ItemStack(Material.AIR);
    Player player;
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
            callback.accept(this.renameText);
            players.remove(playerUUID);
        }
    }

    public static boolean isWrappedBedrockAnvilUI(Inventory inventory){
        return wrappedBedrockAnvilUIs.containsKey(inventory);
    }

    public static WrappedBedrockAnvilUI getWrappedBedrockAnvilUI(Inventory inventory){
        return wrappedBedrockAnvilUIs.get(inventory);
    }

    private static boolean isBannerItem(ItemStack item){
        Byte isBannerItem = ((Byte)(ItemUtils.itemGetNbtPath(item, "isBannerItem")));
        if (isBannerItem == null) return false;
        return (isBannerItem==0);
    }

    private ItemStack newBannerItem(){
        ItemStack result = new ItemStack(Material.GRAY_STAINED_GLASS_PANE, 1);
        ItemMeta rItemMeta = result.getItemMeta();
        rItemMeta.setDisplayName(" ");
        result.setItemMeta(rItemMeta);
        result = ItemUtils.itemSetNbtPath(result, "isBannerItem", true);
        return result;
    }

    private static boolean isRenameItem(ItemStack item){
        Byte isRenameItem = ((Byte)(ItemUtils.itemGetNbtPath(item, "isRenameItem")));
        if (isRenameItem == null) return false;
        return (isRenameItem==0);
    }

    private ItemStack newRenameItem(){
        ItemStack result = new ItemStack(Material.NAME_TAG, 1);
        ItemMeta rItemMeta = result.getItemMeta();
        rItemMeta.setDisplayName(" ");
        rItemMeta.setLore(List.of("Click to rename item"));
        result.setItemMeta(rItemMeta);
        result = ItemUtils.itemSetNbtPath(result, "isRenameItem", true);
        return result;
    }

    private void clearItem(int slot){
        if (inventory.getItem(slot) == null) {
            inventory.setItem(slot, new ItemStack(Material.AIR));
            return;
        }

        if (inventory.getItem(slot).getType().equals(Material.AIR)) {
            return;
        }

        player.getInventory().addItem(inventory.getItem(slot));
        inventory.setItem(slot, new ItemStack(Material.AIR));
    }

    private void setResult(ItemStack item){
        if (item == null) {
            inventory.setItem(4, new ItemStack(Material.AIR));
            return;
        }

        item = item.clone();

        if (item.getType().equals(Material.AIR)) {
            return;
        }

        inventory.setItem(4, item);
        result = item;
    }

    private void frameworkUpdate(){
        clearItem(0);
        clearItem(3);
        inventory.setItem(0, newRenameItem());
        inventory.setItem(3, newBannerItem());
    }

    public WrappedBedrockAnvilUI(Player player){
        this.inventory = Bukkit.createInventory(player, InventoryType.HOPPER, "Anvil");
        this.player = player;
        frameworkUpdate();
        player.closeInventory();
        player.openInventory(inventory);
        wrappedBedrockAnvilUIs.put(inventory, this);
    }

    private boolean isRenaming(){
        boolean isTaken = false;

        if (inventory.getItem(0) == null){
            isTaken = true;
        }

        if (WrappedBedrockAnvilUI.isRenameItem(inventory.getItem(0))){
            isTaken = true;
        }

        return isTaken;
    }

    private void rename(){
        new RenameUI(player, (renameText)->{
            player.closeInventory();
            player.sendMessage("Please type the new name of the item in chat");
            if (renameText.replaceAll(" ", "").isEmpty()) return;
            if (renameText.contains("§")) return;
            if (renameText.contains("\n")) return;

            ItemStack renameItem = newRenameItem();
            ItemMeta renameItemMeta = renameItem.getItemMeta();
            renameItemMeta.setDisplayName(renameText);
            renameItem.setItemMeta(renameItemMeta);
            inventory.setItem(0, renameItem);
            player.openInventory(inventory);
        });
    }

    private void updateResult(){
        MinecraftAnvilAPI vanillaAnvil = new MinecraftAnvilAPI(player);

        Bukkit.getScheduler().runTaskLater(MEGeyserSupport.getThis(), () -> {
            vanillaAnvil.setItemA(itemA);
            vanillaAnvil.setItemB(itemB);
        }, 1);


        Bukkit.getScheduler().runTaskLater(MEGeyserSupport.getThis(), vanillaAnvil::open, 2);

        Bukkit.getScheduler().runTaskLater(MEGeyserSupport.getThis(), () -> {
            setResult(vanillaAnvil.getResultPreview());
            vanillaAnvil.clear();
            player.openInventory(inventory);
            noClose.remove(vanillaAnvil.getInventory());
        }, 3);
    }

    public void onUpdate(){
//        if (isRenaming()) rename();
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
                    || action.equals(InventoryAction.SWAP_WITH_CURSOR)
                    || action.equals(InventoryAction.HOTBAR_SWAP)
                )
        ) return;

        ItemStack currentResult = inventory.getItem(4);
        if (currentResult == null) currentResult = new ItemStack(Material.AIR);

        MinecraftAnvilAPI vanillaAnvil = new MinecraftAnvilAPI(player);
        vanillaAnvil.setItemA(itemA);
        vanillaAnvil.setItemB(itemB);

        player.closeInventory();
        Bukkit.getScheduler().runTaskLater(MEGeyserSupport.getThis(), ()->{
            vanillaAnvil.open();
        }, 1);
        Bukkit.getScheduler().runTaskLater(MEGeyserSupport.getThis(), ()->{
            inventory.clear();
            vanillaAnvil.onResultClick(clickType, action);
        }, 2);
        Bukkit.getScheduler().runTaskLater(MEGeyserSupport.getThis(), ()->{
            frameworkUpdate();
            player.openInventory(inventory);
        }, 3);

//        inventory.setItem(0, renameItem());
//        inventory.setItem(1, new ItemStack(Material.AIR));
//        inventory.setItem(2, new ItemStack(Material.AIR));
//        inventory.setItem(3, bannerItem());
//        inventory.setItem(4, new ItemStack(Material.AIR));
//        ItemStack finalResult = result;
//        Bukkit.getScheduler().runTaskLater(MEGeyserSupport.getThis(), () -> {
//            player.setItemOnCursor(finalResult);
//        }, 2);
    }


}
