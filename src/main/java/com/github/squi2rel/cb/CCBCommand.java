package com.github.squi2rel.cb;

import com.github.squi2rel.cb.menu.SettingsMenu;
import com.github.squi2rel.cb.menu.builder.MenuManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class CCBCommand implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player)) return true;
        Player player = (Player) sender;
        MenuManager.openMenu(player, () -> SettingsMenu.settings.sendTo(player));
        return true;
    }
}
