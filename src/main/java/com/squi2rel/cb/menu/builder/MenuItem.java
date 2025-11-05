package com.squi2rel.cb.menu.builder;

import org.bukkit.Material;

public final class MenuItem<T> {
    private final Material item;
    private final String name;
    private final String desc;
    private final MenuHandler<T> action;

    public MenuItem(Material item, String name, String desc, MenuHandler<T> action) {
        this.item = item;
        this.name = name;
        this.desc = desc;
        this.action = action;
    }

    public Material item() {
        return item;
    }

    public String name() {
        return name;
    }

    public String desc() {
        return desc;
    }

    public MenuHandler<T> action() {
        return action;
    }
}
