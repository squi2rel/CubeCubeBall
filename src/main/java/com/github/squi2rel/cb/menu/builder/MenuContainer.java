package com.github.squi2rel.cb.menu.builder;

class MenuContainer<T> {
    final MenuContext<T> context;
    T argument;
    boolean isClosed;

    MenuContainer(MenuContext<T> context, T argument) {
        this.context = context;
        this.argument = argument;
    }

    @SuppressWarnings("unchecked")
    void setArgument(Object argument) {
        this.argument = (T) argument;
    }
}
