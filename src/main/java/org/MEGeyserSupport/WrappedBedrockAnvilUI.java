package org.MEGeyserSupport;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_20_R2.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.PrepareAnvilEvent;
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
    private static HashMap<Inventory, WrappedBedrockAnvilUI> wrappedBedrockAnvilUIs = new HashMap<>();
    public static List<Inventory> noClose = new ArrayList<>();
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
        return ((Byte)(ItemUtils.itemGetNbtPath(item, "isBannerItem")))==0;
    }

    private ItemStack bannerItem(){
        ItemStack result = new ItemStack(Material.GRAY_STAINED_GLASS_PANE, 1);
        ItemMeta rItemMeta = result.getItemMeta();
        rItemMeta.setDisplayName(" ");
        result.setItemMeta(rItemMeta);
        result = ItemUtils.itemSetNbtPath(result, "isBannerItem", true);
        return result;
    }

    private static boolean isRenameItem(ItemStack item){
        return ((Byte)(ItemUtils.itemGetNbtPath(item, "isRenameItem")))==0;
    }

    private ItemStack renameItem(){
        ItemStack result = new ItemStack(Material.NAME_TAG, 1);
        ItemMeta rItemMeta = result.getItemMeta();
        rItemMeta.setDisplayName(" ");
        rItemMeta.setLore(List.of("Click to rename item"));
        result.setItemMeta(rItemMeta);
        result = ItemUtils.itemSetNbtPath(result, "isRenameItem", true);
        return result;
    }

    public WrappedBedrockAnvilUI(Player player){
        this.inventory = Bukkit.createInventory(player, InventoryType.HOPPER, "Anvil");
        this.player = player;
        inventory.setItem(0, renameItem());
        inventory.setItem(3, bannerItem());
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

            ItemStack renameItem = renameItem();
            ItemMeta renameItemMeta = renameItem.getItemMeta();
            renameItemMeta.setDisplayName(renameText);
            renameItem.setItemMeta(renameItemMeta);
            inventory.setItem(0, renameItem);
            player.openInventory(inventory);
        });
    }

    private void updateResult(){
        ServerPlayer splayer = ((CraftPlayer) player).getHandle();
        int nCC = splayer.nextContainerCounter();
        net.minecraft.world.entity.player.Inventory inv = splayer.getInventory();
        ContainerLevelAccess containerAccess = ContainerLevelAccess.create(splayer.level(), splayer.blockPosition());
        AnvilMenu menu = new AnvilMenu(nCC, inv, containerAccess);
        menu.checkReachable = false;
        menu.setTitle(Component.literal("Anvil"));
        InventoryView view = menu.getBukkitView();
        System.out.println(inventory.getItem(0));
        System.out.println(inventory.getItem(1));
        System.out.println(inventory.getItem(2));
        System.out.println(inventory.getItem(3));
        System.out.println(inventory.getItem(4));
        noClose.add(view.getTopInventory());

        Bukkit.getScheduler().runTaskLater(MEGeyserSupport.getThis(), () -> {
            ItemStack a = inventory.getItem(1);
            if (a != null){
                a = inventory.getItem(1).clone();
            }
            ItemStack b = inventory.getItem(2);
            if (b != null){
                b = inventory.getItem(2).clone();
            }
            view.setItem(0, a);
            view.setItem(1, b);
            player.closeInventory();
        }, 1);


        Bukkit.getScheduler().runTaskLater(MEGeyserSupport.getThis(), () -> {
            player.openInventory(view);
        }, 2);

        Bukkit.getScheduler().runTaskLater(MEGeyserSupport.getThis(), () -> {
            ItemStack result = view.getItem(2);
            if (result != null){
                result = result.clone();
            }
            inventory.setItem(4,result);
            view.setItem(0, new ItemStack(Material.AIR));
            view.setItem(1, new ItemStack(Material.AIR));
            view.setItem(2, new ItemStack(Material.AIR));
            player.openInventory(inventory);
        }, 3);
    }

    public void onUpdate(){
        updateResult();
        if (isRenaming()) rename();

    }

    public void onResultTake(){
        ItemStack result = inventory.getItem(4);
        if (result != null){
            result = result.clone();
        }
        inventory.setItem(0, renameItem());
        inventory.setItem(1, new ItemStack(Material.AIR));
        inventory.setItem(2, new ItemStack(Material.AIR));
        inventory.setItem(3, bannerItem());
        inventory.setItem(4, new ItemStack(Material.AIR));
        ItemStack finalResult = result;
        Bukkit.getScheduler().runTaskLater(MEGeyserSupport.getThis(), () -> {
            player.setItemOnCursor(finalResult);
        }, 2);
    }


}
