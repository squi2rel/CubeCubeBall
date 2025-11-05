package com.squi2rel.cb.menu.builder;

public class MenuContainer<T> {
    public final MenuContext<T> context;
    public T argument;

    public MenuContainer(MenuContext<T> context, T argument) {
        this.context = context;
        this.argument = argument;
    }

    @SuppressWarnings("unchecked")
    public void setArgument(Object argument) {
        this.argument = (T) argument;
    }
}
