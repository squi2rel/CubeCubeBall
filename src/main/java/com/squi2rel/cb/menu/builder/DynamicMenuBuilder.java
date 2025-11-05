package com.squi2rel.cb.menu.builder;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class DynamicMenuBuilder<T> {
    private String title;
    private int row;
    private List<MenuItem<T>> items;
    private DynamicMenuBuilderHandler<T> builder;
    private String prefix = "", lorePrefix = "";
    private boolean refreshRequested = true;

    public DynamicMenuBuilder(String title, int row, DynamicMenuBuilderHandler<T> builder) {
        this.title = title;
        this.row = row;
        this.builder = builder;
        clear();
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }

    public void setRow(int row) {
        this.row = row;
        clear();
    }

    public int getRow() {
        return row;
    }

    public void setBuilder(DynamicMenuBuilderHandler<T> builder) {
        this.builder = builder;
    }

    public DynamicMenuBuilderHandler<T> getBuilder() {
        return builder;
    }

    @SuppressWarnings("unchecked")
    public void clear() {
        items = Arrays.asList(new MenuItem[row * 9]);
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

    public void requestRefresh() {
        refreshRequested = true;
    }

    public DynamicMenuContext<T> build() {
        return new DynamicMenuContext<>(this);
    }

    public static class DynamicMenuContext<T> extends MenuContext<T> {
        private final DynamicMenuBuilder<T> builder;
        private Inventory inventory;
        private List<MenuItem<T>> items;

        public DynamicMenuContext(DynamicMenuBuilder<T> builder) {
            this.builder = builder;
        }

        public void refresh(Player player, T argument) {
            builder.clear();
            builder.builder.handle(builder, player, argument);
            Inventory inventory = Bukkit.createInventory(null, builder.row * 9, builder.title);
            for (int i = 0; i < builder.items.size(); i++) {
                MenuItem<T> item = builder.items.get(i);
                if (item == null) continue;
                ItemStack stack = new ItemStack(item.item());
                ItemMeta meta = Objects.requireNonNull(stack.getItemMeta());
                meta.setDisplayName(ChatColor.RESET + builder.prefix + item.name());
                String desc = item.desc();
                if (desc != null) meta.setLore(Arrays.stream(desc.split("\n")).map(s -> ChatColor.RESET + builder.lorePrefix + s).collect(Collectors.toList()));
                stack.setItemMeta(meta);
                inventory.setItem(i, stack);
            }
            this.items = new ArrayList<>(builder.items);
            this.inventory = inventory;
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
            if (builder.refreshRequested) {
                builder.refreshRequested = false;
                sendTo(player, this.argument);
            }
        }

        @Override
        public void sendTo(Player player, T argument) {
            refresh(player, argument);
            builder.refreshRequested = false;
            player.openInventory(inventory);
            MenuManager.registerMenu(player, this, argument);
        }
    }

    public interface DynamicMenuBuilderHandler<T> {
        void handle(DynamicMenuBuilder<T> builder, Player player, T argument);
    }
}
