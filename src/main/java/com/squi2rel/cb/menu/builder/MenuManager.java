package com.squi2rel.cb.menu.builder;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.UUID;
import java.util.function.Consumer;

public class MenuManager implements Listener {
    private static final HashMap<UUID, MenuContainer<?>> menus = new HashMap<>();
    private static final HashMap<UUID, Consumer<String>> handlers = new HashMap<>();

    private final Plugin plugin;

    private MenuManager(Plugin plugin) {
        this.plugin = plugin;
    }

    public static <T> void registerMenu(Player player, MenuContext<T> menu, T argument) {
        menus.put(player.getUniqueId(), new MenuContainer<>(menu, argument));
    }

    public static void registerChatHandler(Player player, Consumer<String> handler) {
        handlers.put(player.getUniqueId(), handler);
    }

    public static void updateArgument(Player player, Object argument) {
        menus.get(player.getUniqueId()).setArgument(argument);
    }

    public static void closeAll() {
        for (UUID uuid : menus.keySet()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null) continue;
            player.closeInventory();
        }
    }

    public static void init(Plugin plugin) {
        plugin.getServer().getPluginManager().registerEvents(new MenuManager(plugin), plugin);
    }

    @EventHandler
    public void onPlayerLeave(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        menus.remove(uuid);
        handlers.remove(uuid);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        MenuContainer<?> container = menus.get(player.getUniqueId());
        if (container == null) return;
        event.setCancelled(true);
        int slot = event.getRawSlot();
        container.context.handleClick(player, slot, container.argument);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        menus.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        Consumer<String> handler = handlers.remove(player.getUniqueId());
        if (handler == null) return;
        event.setCancelled(true);
        String message = event.getMessage();
        Bukkit.getScheduler().runTask(plugin, () -> handler.accept(message));
    }
}
