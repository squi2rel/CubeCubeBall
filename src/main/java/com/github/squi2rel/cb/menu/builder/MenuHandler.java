package com.github.squi2rel.cb.menu.builder;

import org.bukkit.entity.Player;

public interface MenuHandler<T> {
    void handle(Player player, MenuContext<T> context);

    interface HotbarMenuHandler<T> {
        void handle(Player player, MenuContext<T> context, int slot);
    }
}
