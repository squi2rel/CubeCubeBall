package com.squi2rel.cb.menu.builder;

import org.bukkit.entity.Player;

public abstract class MenuContext<T> {
    protected MenuContext<?> parent;
    protected Object parentArgument;
    protected T argument;

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

    @SuppressWarnings("unchecked")
    protected void send(Player player, Object argument) {
        sendTo(player, (T) argument);
    }

    public void sendTo(Player player) {
        sendTo(player, null);
    }

    abstract public void handleClick(Player player, int slot, Object argument);

    abstract public void sendTo(Player player, T argument);
}
