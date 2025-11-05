package com.squi2rel.cb.menu.builder;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class MenuBuilder<T> {
    private final String title;
    private final int row;
    private final List<MenuItem<T>> items;
    private String prefix = "", lorePrefix = "";

    @SuppressWarnings("unchecked")
    public MenuBuilder(String title, int row, Consumer<MenuBuilder<T>> builder) {
        this.title = title;
        this.row = row;
        items = Arrays.asList(new MenuItem[row * 9]);
        builder.accept(this);
    }

    public void setSlot(int column, int row, Material item, String name, String desc, MenuHandler<T> action) {
        items.set(row * 9 + column, new MenuItem<>(item, name, desc, action));
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public void setLorePrefix(String prefix) {
        this.lorePrefix = prefix;
    }

    public StaticMenuContext<T> build() {
        Inventory inventory = Bukkit.createInventory(null, row * 9, title);
        for (int i = 0; i < items.size(); i++) {
            MenuItem<T> item = items.get(i);
            if (item == null) continue;
            Material type = item.item();
            if (type == null || type.isAir()) continue;
            ItemStack stack = new ItemStack(type);
            ItemMeta meta = Objects.requireNonNull(stack.getItemMeta());
            meta.setDisplayName(ChatColor.RESET + prefix + item.name());
            String desc = item.desc();
            if (desc != null) meta.setLore(Arrays.stream(desc.split("\n")).map(s -> ChatColor.RESET + lorePrefix + s).collect(Collectors.toList()));
            stack.setItemMeta(meta);
            inventory.setItem(i, stack);
        }
        return new StaticMenuContext<>(inventory, items);
    }

    public static class StaticMenuContext<T> extends MenuContext<T> {
        private final Inventory inventory;
        private final List<MenuItem<T>> items;

        public StaticMenuContext(Inventory inventory, List<MenuItem<T>> items) {
            this.inventory = inventory;
            this.items = items;
        }

        @Override
        @SuppressWarnings("unchecked")
        public void handleClick(Player player, int slot, Object argument) {
            if (slot < 0 || slot >= items.size()) return;
            MenuItem<T> item = items.get(slot);
            if (item == null) return;
            this.argument = (T) argument;
            MenuHandler<T> action = item.action();
            if (action == null) return;
            action.handle(player, this);
            if (this.argument != argument) MenuManager.updateArgument(player, this.argument);
        }

        public void sendTo(Player player, T argument) {
            player.openInventory(inventory);
            MenuManager.registerMenu(player, this, argument);
        }
    }
}
