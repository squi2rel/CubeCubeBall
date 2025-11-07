package com.github.squi2rel.cb.menu.builder;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;

@SuppressWarnings("unused")
public abstract class MenuContext<T> {
    protected MenuContext<?> parent;
    protected Object parentArgument;
    protected T argument;
    protected boolean autoClose = true;

    public void setParent(MenuContext<?> parent) {
        this.parent = parent;
        parentArgument = parent.argument;
    }

    public void openParent(Player player) {
        parent.send(player, parentArgument);
    }

    public T getArgument() {
        return argument;
    }

    public void setArgument(T argument) {
        this.argument = argument;
    }

    public void setAutoClose(boolean autoClose) {
        this.autoClose = autoClose;
    }

    public boolean isAutoClose() {
        return autoClose && (parent == null || parent.isAutoClose());
    }

    @SuppressWarnings("unchecked")
    void send(Player player, Object argument) {
        sendTo(player, (T) argument);
    }

    public void sendTo(Player player) {
        sendTo(player, getArgument());
    }

    abstract public void handleClick(Player player, int slot, int hotbar, Object argument, ClickType type);

    abstract public void sendTo(Player player, T argument);
}
