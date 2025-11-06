package com.squi2rel.cb;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class MatchConfig {

    public Material cubeBallBlock = Material.NETHERITE_BLOCK;
    public int matchDuration = 300;
    public int maxGoal = 0;

    public Location ballSpawn;

    public List<Location> blueTeamGoalBlocks = new ArrayList<>();
    public List<Location> redTeamGoalBlocks = new ArrayList<>();
    public List<Location> blueTeamSpawns = new ArrayList<>();
    public List<Location> redTeamSpawns = new ArrayList<>();

    public void write(ConfigurationSection config) {
        config.set("cubeBallBlock", cubeBallBlock.name());
        config.set("matchDuration", matchDuration);
        config.set("maxGoal", maxGoal);

        config.set("ballSpawn", ballSpawn);
        config.set("blueTeamGoalBlocks", blueTeamGoalBlocks);
        config.set("redTeamGoalBlocks", redTeamGoalBlocks);
        config.set("blueTeamSpawns", blueTeamSpawns);
        config.set("redTeamSpawns", redTeamSpawns);
    }

    public void read(ConfigurationSection config) {
        cubeBallBlock = getMaterial(config.getString("cubeBallBlock"));
        matchDuration = config.getInt("matchDuration");
        maxGoal = config.getInt("maxGoal");

        ballSpawn = config.getSerializable("ballSpawn", Location.class);
        blueTeamGoalBlocks = getLocations(config, "blueTeamGoalBlocks");
        redTeamGoalBlocks = getLocations(config, "redTeamGoalBlocks");
        blueTeamSpawns = getLocations(config, "blueTeamSpawns");
        redTeamSpawns = getLocations(config, "redTeamSpawns");
    }

    public static MatchConfig from(ConfigurationSection config) {
        MatchConfig instance = new MatchConfig();
        instance.read(config);
        return instance;
    }

    public static Material getMaterial(String material) {
        return material == null ? null : Material.matchMaterial(material);
    }

    @SuppressWarnings("unchecked")
    public static ArrayList<Location> getLocations(ConfigurationSection config, String path) {
        return new ArrayList<>((List<Location>) Objects.requireNonNull(config.getList(path)));
    }
}
