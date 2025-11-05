package com.squi2rel.cb.menu;

import com.squi2rel.cb.I18n;
import com.squi2rel.cb.menu.builder.*;
import me.crylonz.CubeBall;
import me.crylonz.Match;
import me.crylonz.MatchState;
import org.bukkit.Material;

import java.util.ArrayList;

import static org.bukkit.Material.*;

public class SettingsMenu {

    public static DynamicMenuBuilder<Match> desc = new DynamicMenuBuilder<>(I18n.get("menu_desc_title"), 6, (builder, player, match) -> {
        builder.setSlot(0, 0, getState(match.getMatchState()), match.getName(), null, null);
        builder.setSlot(8, 0, BARRIER, I18n.get("menu_desc_delete"), null, (p, v) -> {
            CubeBall.matches.remove(match.getName());
            if (CubeBall.current == match) CubeBall.current = null;
            CubeBall.save();
            v.openParent(p);
            p.sendMessage(I18n.get("match_removed"));
        });
        builder.setSlot(1, 1, RED_WOOL, "redspawn", null, (p, v) -> {
        });
        builder.setSlot(3, 1, BLUE_WOOL, "bluespawn", null, (p, v) -> {
        });
        builder.setSlot(5, 1, RED_CONCRETE, "redgoal", null, (p, v) -> {
        });
        builder.setSlot(7, 1, BLUE_CONCRETE, "bluegoal", null, (p, v) -> {
        });
        builder.setSlot(1, 3, EMERALD_BLOCK, "ballspawn", null, (p, v) -> {
        });
        builder.setSlot(3, 3, match.getConfig().cubeBallBlock, "ballblock", null, (p, v) -> {
            p.sendMessage("material_name");
            p.closeInventory();
            MenuManager.registerChatHandler(p, s -> {
                Material m = Material.matchMaterial(s.toUpperCase());
                if (m == null || !m.isBlock()) {
                    p.sendMessage("invalid_material");
                    v.sendTo(p, v.getArgument());
                }
                match.getConfig().cubeBallBlock = m;
                v.sendTo(p, v.getArgument());
            });
        });
        builder.setSlot(5, 3, PLAYER_HEAD, "scanplayer", null, (p, v) -> {
        });
        builder.setSlot(7, 3, GREEN_WOOL, "start!", null, (p, v) -> {
        });
        builder.setSlot(4, 5, ARROW, I18n.get("menu_back"), null, (p, v) -> v.openParent(p));
    });

    public static DynamicMenuBuilder<Integer> list = new DynamicMenuBuilder<>(I18n.get("menu_list_title"), 6, (builder, player, page) -> {
        int col = 0, row = 0;
        int maxPerPage = 5 * 9;
        ArrayList<Match> values = new ArrayList<>(CubeBall.matches.values());
        for (Match match : values.subList(page * maxPerPage, Math.min((page + 1) * maxPerPage, values.size()))) {
            MatchState state = match.getMatchState();
            builder.setSlot(col, row, getState(state), match.getName(), null, (p, v) -> {
                DynamicMenuBuilder.DynamicMenuContext<Match> menu = desc.build();
                menu.setParent(v);
                menu.sendTo(p, match);
            });
            if (++col == 8) {
                col = 0;
                row += 1;
            }
        }
        builder.setSlot(4, 5, ARROW, I18n.get("menu_back"), null, (p, v) -> v.openParent(p));
    });

    public static MenuContext<Void> settings = new MenuBuilder<Void>(I18n.get("menu_title"), 6, builder -> {
        builder.setSlot(0, 0, GREEN_CONCRETE, I18n.get("menu_new"), null, (p, v) -> {
            p.sendMessage(I18n.get("menu_new_name"));
            p.closeInventory();
            MenuManager.registerChatHandler(p, s -> {
                Match m = new Match(s);
                CubeBall.matches.put(s, m);
                CubeBall.save();
                p.sendMessage(I18n.get("menu_new_success"));
                DynamicMenuBuilder.DynamicMenuContext<Match> menu = desc.build();
                menu.setParent(v);
                menu.sendTo(p, m);
            });
        });
        builder.setSlot(0, 1, BOOK, I18n.get("menu_list"), null, (p, v) -> {
            DynamicMenuBuilder.DynamicMenuContext<Integer> menu = list.build();
            menu.setParent(v);
            menu.sendTo(p, 0);
        });
    }).build();

    private static Material getState(MatchState state) {
        return state == MatchState.CREATED ? GRAY_WOOL : state == MatchState.READY ? YELLOW_WOOL : GREEN_WOOL;
    }
}
