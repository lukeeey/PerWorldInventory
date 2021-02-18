package io.github.lukeeey.perworldinventory;

import cn.nukkit.Player;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.EventPriority;
import cn.nukkit.event.Listener;
import cn.nukkit.event.entity.EntityLevelChangeEvent;
import cn.nukkit.event.inventory.InventoryTransactionEvent;
import cn.nukkit.event.player.PlayerJoinEvent;
import cn.nukkit.event.player.PlayerLoginEvent;
import cn.nukkit.event.player.PlayerQuitEvent;
import cn.nukkit.level.Level;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class EventListener implements Listener {
    private final PerWorldInventory plugin;

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onLevelChange(EntityLevelChangeEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();

            if (player.isCreative() || player.isSpectator()) {
                return;
            }
            Level origin = event.getOrigin();
            Level target = event.getTarget();

            plugin.storeInventory(player, origin);
            if (player.hasPermission("per-world-inventory.bypass")) {
                return;
            }

            if (plugin.getParentWorld(origin.getFolderName()).equalsIgnoreCase(plugin.getParentWorld(target.getFolderName()))) {
                return;
            }

            plugin.setInventory(player, target);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        plugin.save(event.getPlayer(), true);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onLogin(PlayerLoginEvent event) {
        Player player = event.getPlayer();
        if (player.isCreative() || player.isSpectator() || player.hasPermission("per-world-inventory.bypass")) {
            return;
        }
        plugin.load(player);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryTransaction(InventoryTransactionEvent event) {
        if (plugin.isLoading(event.getTransaction().getSource())) {
            event.setCancelled(true);
        }
    }
}
