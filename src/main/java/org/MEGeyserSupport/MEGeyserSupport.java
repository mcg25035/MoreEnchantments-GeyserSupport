package org.MEGeyserSupport;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.sergiferry.playernpc.api.NPCLib;
import io.netty.util.concurrent.CompleteFuture;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import org.apache.commons.io.FileUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.craftbukkit.v1_20_R2.CraftServer;
import org.bukkit.craftbukkit.v1_20_R2.entity.CraftHumanEntity;
import org.bukkit.craftbukkit.v1_20_R2.entity.CraftPlayer;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.geysermc.api.Geyser;
import org.geysermc.floodgate.api.FloodgateApi;
import org.geysermc.geyser.api.GeyserApi;
import org.moreenchantments.MoreEnchantments;
import org.moreenchantments.books.MoneyMendingBook;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;


public final class MEGeyserSupport extends JavaPlugin {
    public File pluginDir = getDataFolder();
    public HashMap<String, Object> config = new HashMap<>();
    public HashMap<String, String> languageMapping = new HashMap<>();
    public String prefix = "";
    public Method isBedrockPlayer;
    public void checkPluginFile() throws IOException {

        if (!pluginDir.exists()){
            pluginDir.mkdirs();
        }
        if (!pluginDir.isDirectory()){
            pluginDir.delete();
            pluginDir.mkdirs();
        }
        File configFileReal = pluginDir.toPath().resolve("config.yml").toFile();
        if (!configFileReal.exists()){
            URL configFileTemplate = getClass().getResource("/config.yml");
            assert configFileTemplate != null;
            FileUtils.copyURLToFile(configFileTemplate,configFileReal);
        }
        if (!configFileReal.isFile()){
            boolean ignored = configFileReal.delete();
            URL configFileTemplate = getClass().getResource("/config.yml");
            assert configFileTemplate != null;
            FileUtils.copyURLToFile(configFileTemplate,configFileReal);
        }
    }

    public static MEGeyserSupport getThis(){
        return (MEGeyserSupport) (Bukkit.getPluginManager().getPlugin("MoreEnchantments-GeyserSupport"));
    }

    public static boolean isBedrockPlayerByGeyser(Player player){
        return Geyser.api().isBedrockPlayer(player.getUniqueId());
    }

    public static boolean isBedrockPlayerByFloodgate(Player player){
        return FloodgateApi.getInstance().isFloodgatePlayer(player.getUniqueId());
    }

    public static boolean isBedrockPlayerByPrefix(Player player){
        return player.getName().startsWith(getThis().prefix);
    }

    public boolean isBedrockPlayerWrapped(Player player){
        try{
            return (boolean) isBedrockPlayer.invoke(this, player);
        }
        catch (Exception ignored){}
        return false;
    }

    @Override
    public void onEnable(){
        try{
            checkPluginFile();
        }
        catch (Exception ignored){}

        File configFile = pluginDir.toPath().resolve("config.yml").toFile();
        InputStream inputStream = null;

        try {
            inputStream = new FileInputStream(configFile);
        } catch (Exception e) {}
        this.config = (new Yaml()).load(inputStream);

        try {
            InputStream textSource = this.getClass().getClassLoader().getResourceAsStream("langs/"+config.get("lang")+".json");
            String fileData = new String(textSource.readAllBytes(), StandardCharsets.UTF_8);
            JsonObject langJson = new Gson().fromJson(fileData , JsonObject.class);
            Map<String, JsonElement> langJsonElement = langJson.asMap();
            for (String key : langJsonElement.keySet()){
                String value = langJsonElement.get(key).getAsString();
                if (value == null) continue;
                languageMapping.put(key, value);
            }
        } catch (Exception ignored) {}

        if (config.get("getPlayerTypeBy") == null){
            throw new RuntimeException("Invalid config.yml");
        }
        else if (config.get("getPlayerTypeBy").equals(0)){
            if (Bukkit.getPluginManager().getPlugin("Geyser-Spigot") == null){
                throw new RuntimeException("Geyser-Spigot not found");
            }
            try {
                isBedrockPlayer = this.getClass().getDeclaredMethod("isBedrockPlayerByGeyser", Player.class);
            }
            catch (Exception ignored){}
        }
        else if (config.get("getPlayerTypeBy").equals(1)){
            if (Bukkit.getPluginManager().getPlugin("floodgate") == null){
                throw new RuntimeException("floodgate not found");
            }
            try {
                isBedrockPlayer = this.getClass().getDeclaredMethod("isBedrockPlayerByFloodgate", Player.class);
            }
            catch (Exception ignored){}
        }
        else if (config.get("getPlayerTypeBy").equals(2)){
            if (config.get("prefix") == null){
                throw new RuntimeException("Invalid config.yml");
            }
            if (config.get("prefix").equals("")){
                throw new RuntimeException("Invalid config.yml");
            }
            prefix = (String) config.get("prefix");
            try {
                isBedrockPlayer = this.getClass().getDeclaredMethod("isBedrockPlayerByPrefix", Player.class);
            }
            catch (Exception ignored){}
        }
        else{
            throw new RuntimeException("Invalid config.yml");
        }

        Bukkit.getServer().getPluginManager().registerEvents(new Events(), this);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
