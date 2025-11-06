package com.squi2rel.cb.menu.builder;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;

@SuppressWarnings("unused")
class MenuBuilderBase {
    protected String title;
    protected int row;
    protected String prefix = "", lorePrefix = "";
    protected boolean autoClose = true;

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public void setLorePrefix(String prefix) {
        this.lorePrefix = prefix;
    }

    public void setAutoClose(boolean autoClose) {
        this.autoClose = autoClose;
    }

    protected static <T> void setMenuItem(MenuItem<T> item, Inventory inventory, int i) {
        if (item == null) return;
        Material type = item.getItem();
        if (type == null || type.isAir()) return;
        ItemStack stack = new ItemStack(item.getItem());
        ItemMeta meta = Objects.requireNonNull(stack.getItemMeta());
        meta.setDisplayName(ChatColor.RESET + item.getPrefix() + item.getName());
        String desc = item.getDesc();
        if (desc != null) meta.setLore(Arrays.stream(desc.split("\n")).map(s -> ChatColor.RESET + item.getLorePrefix() + s).collect(Collectors.toList()));
        stack.setItemMeta(meta);
        inventory.setItem(i, stack);
    }
}
