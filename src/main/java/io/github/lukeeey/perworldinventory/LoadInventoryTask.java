package io.github.lukeeey.perworldinventory;

import cn.nukkit.Player;
import cn.nukkit.Server;
import cn.nukkit.item.Item;
import cn.nukkit.nbt.NBTIO;
import cn.nukkit.nbt.tag.CompoundTag;
import cn.nukkit.nbt.tag.ListTag;
import cn.nukkit.scheduler.AsyncTask;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class LoadInventoryTask extends AsyncTask {
    private final UUID playerUuid;
    private final String playerName;
    private final File filePath;

    public LoadInventoryTask(Player player, File filePath) {
        this.playerUuid = player.getUniqueId();
        this.playerName = player.getName().toLowerCase();
        this.filePath = filePath;
    }

    @Override
    public void onRun() {
        try {
            CompoundTag rootTag = NBTIO.read(filePath);
            Map<String, Int2ObjectMap<Item>> result = new HashMap<>();

            rootTag.getAllTags().forEach(tag -> {
                if (tag instanceof ListTag) {
                    ListTag<CompoundTag> listTag = (ListTag<CompoundTag>) tag;
                    String levelName = listTag.getName();

                    Int2ObjectMap<Item> contents = new Int2ObjectOpenHashMap<>();
                    listTag.getAll().forEach(itemTag -> {
                        contents.put(itemTag.getByte("Slot"), NBTIO.getItemHelper(itemTag));
                    });

                    result.put(levelName, contents);
                }
                setResult(result);
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onCompletion(Server server) {
        Player player = server.getPlayer(playerUuid).orElse(null);
        PerWorldInventory plugin = (PerWorldInventory) server.getPluginManager().getPlugin("PerWorldInventory");

        if (player == null) {
            plugin.onAbortLoading(player);
        } else {
            plugin.onLoadInventory(player, (Map<String, Int2ObjectMap<Item>>) getResult());
        }
    }
}
