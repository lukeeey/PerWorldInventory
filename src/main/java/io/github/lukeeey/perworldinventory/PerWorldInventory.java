package io.github.lukeeey.perworldinventory;

import cn.nukkit.Player;
import cn.nukkit.Server;
import cn.nukkit.event.Listener;
import cn.nukkit.inventory.PlayerInventory;
import cn.nukkit.item.Item;
import cn.nukkit.level.Level;
import cn.nukkit.nbt.NBTIO;
import cn.nukkit.nbt.tag.CompoundTag;
import cn.nukkit.nbt.tag.ListTag;
import cn.nukkit.plugin.PluginBase;
import cn.nukkit.utils.TextFormat;
import com.google.common.collect.Maps;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PerWorldInventory extends PluginBase implements Listener {
    private final Map<Player,  Map<String, Int2ObjectMap<Item>>> loadedInventories = new HashMap<>();
    private final ObjectList<Player> loading = new ObjectArrayList<>();

    private final Map<String, String> bundledWorlds = new HashMap<>();

    private File inventoryFile;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        getServer().getPluginManager().registerEvents(new EventListener(this), this);

        parseBundledWorlds();

        inventoryFile = new File(getDataFolder(), "inventories");
        if (!inventoryFile.exists()) {
            inventoryFile.mkdirs();
        }
    }

    @Override
    public void onDisable() {
        saveAllInventories();
    }

    private void parseBundledWorlds() {
        getConfig().getSection("bundled-worlds").forEach((mainWorld, subWorldsSection) -> {
            List<String> subWorlds = (List<String>) subWorldsSection;
            subWorlds.forEach(childWorld -> bundledWorlds.put(childWorld, getParentWorld(mainWorld)));
        });
    }

    public String getParentWorld(String world) {
        return bundledWorlds.getOrDefault(world, world);
    }

    public Int2ObjectMap<Item> getInventory(Player player, Level level) {
        return loadedInventories.get(player).get(getParentWorld(level.getFolderName()));
    }

    public void storeInventory(Player player, Level level) {
        Int2ObjectMap<Item> contents = new Int2ObjectOpenHashMap<>();
        player.getInventory().getContents().forEach(contents::put);

        if (contents.isEmpty()) {
            loadedInventories.get(player).remove(getParentWorld(level.getFolderName()));
        } else {
            Map<String, Int2ObjectMap<Item>> lvlMap = new HashMap<>();
            lvlMap.put(getParentWorld(level.getFolderName()), contents);

            if (loadedInventories.containsKey(player)) {
                loadedInventories.get(player).forEach(lvlMap::put);
            }

            loadedInventories.put(player, lvlMap);
        }
    }

    public void load(Player player) {
        File file = new File(inventoryFile, player.getName().toLowerCase() + ".dat");

        if (file.exists()) {
            getServer().getScheduler().scheduleAsyncTask(this, new LoadInventoryTask(player, file));
            loading.add(player);
        }
    }

    public boolean isLoading(Player player) {
        return loading.contains(player);
    }

    public void onAbortLoading(Player player) {
        loading.remove(player);
    }

    public void onLoadInventory(Player player, Map<String, Int2ObjectMap<Item>> contents) {
        Map<String, Int2ObjectMap<Item>> lvlMap = new HashMap<>();
        contents.forEach(lvlMap::put);

        loading.remove(player);
        loadedInventories.put(player, lvlMap);

        setInventory(player, player.getLevel());
    }

    public void setInventory(Player player, Level level) {
        Int2ObjectMap<Item> contents = getInventory(player, level);
        PlayerInventory inventory = player.getInventory();

        inventory.clearAll();

        if (contents != null) {
            contents.forEach((slot, item) -> inventory.setItem(slot, item, false));
        }

        inventory.sendContents(player);
    }

    public void save(Player player) {
        save(player, false);
    }

    public void save(Player player, boolean unset) {
        File playerDataFile = new File(inventoryFile, player.getName() + ".dat");

        if (loadedInventories.containsKey(player)) {
            CompoundTag tag = new CompoundTag();

            loadedInventories.get(player).forEach((level, contents) -> {
                ListTag<CompoundTag> inventoryTag = new ListTag<>(level);
                contents.forEach((slot, item) -> {
                    inventoryTag.add(NBTIO.putItemHelper(item, slot));
                });
                tag.putList(inventoryTag);
            });

            try {
                NBTIO.write(tag, playerDataFile);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else if (playerDataFile.exists()) {
            playerDataFile.delete();
        }

        if (unset) {
            loadedInventories.remove(player);
        }
    }

    public void saveAllInventories() {
        loadedInventories.keySet().forEach(this::save);
    }
}
