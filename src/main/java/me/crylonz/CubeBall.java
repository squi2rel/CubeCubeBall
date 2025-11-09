package me.crylonz;

import com.github.squi2rel.cb.CCBCommand;
import com.github.squi2rel.cb.I18n;
import com.github.squi2rel.cb.MatchData;
import com.github.squi2rel.cb.menu.builder.MenuManager;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bstats.bukkit.Metrics;
import org.bukkit.*;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

import java.util.*;

import static java.lang.Math.abs;
import static me.crylonz.MatchState.*;

public class CubeBall extends JavaPlugin {
    public static Plugin plugin;

    public static HashMap<String, Ball> balls = new HashMap<>();
    public static HashMap<String, Match> matches = new HashMap<>();
    public static HashMap<UUID, Long> cooldown = new HashMap<>();

    public static int maxMatchPerPlayer;

    public static void generateBall(Material material, String id, Location location, Vector lastVelocity) {

        if (balls.get(id) != null) {
            throw new IllegalStateException("Same ID cannot be put on the same ball");
        }

        BlockData blockData = Bukkit.createBlockData(material);
        FallingBlock block = Objects.requireNonNull(location.getWorld()).spawnFallingBlock(location, blockData);
        block.setMetadata("ballID", new FixedMetadataValue(plugin, id));
        block.setGlowing(true);
        block.setDropItem(false);
        block.setInvulnerable(true);

        Ball ball = new Ball();
        ball.setId(id);
        ball.setBall(block);

        if (lastVelocity != null) {
            ball.getLastVelocity().setX(0);
            ball.getLastVelocity().setZ(0);
        }

        ball.setPlayerCollisionTick(0);
        balls.put(id, ball);
    }

    public static void destroyBall(String id) {
        if (balls.get(id) != null && balls.get(id).getBall() != null) {
            balls.get(id).getBall().remove();
            balls.remove(id);
        }
    }

    public void onEnable() {
        plugin = this;

        saveDefaultConfig();

        String lang = getConfig().getString("language", "en");
        I18n.init(this, lang);

        MenuManager.init(this);

        ConfigurationSection section = getConfig().getConfigurationSection("matches");
        if (section == null) section = new MemoryConfiguration();
        for (String key : Objects.requireNonNull(section).getKeys(false)) {
            matches.put(key, new Match(key, MatchData.from(section.getConfigurationSection(key))));
        }

        maxMatchPerPlayer = getConfig().getInt("maxMatchPerPlayer", 3);

        getServer().getPluginManager().registerEvents(new CubeBallListener(), this);

        new Metrics(this, 17634);

        launchRepeatingTask();

        Objects.requireNonNull(getCommand("ccb")).setExecutor(new CCBCommand());
    }

    public void onDisable() {
        MenuManager.closeAll();

        save();

        balls.forEach((key, value) -> {
            if (value.getBall() != null) {
                value.getBall().remove();
            }
        });
        getServer().getScheduler().cancelTasks(this);
    }

    public static void save() {
        ConfigurationSection section = new MemoryConfiguration();
        for (Map.Entry<String, Match> match : matches.entrySet()) {
            MemoryConfiguration m = new MemoryConfiguration();
            match.getValue().getData().write(m);
            section.set(match.getKey(), m);
        }
        plugin.getConfig().set("matches", section);
        plugin.saveConfig();
    }

    public static void launch(Player player, double power) {
        Vector direction = player.getLocation().getDirection().normalize();
        direction.setY(0.2);
        Vector velocity = direction.multiply(power);
        player.setVelocity(velocity);
    }

    private void launchRepeatingTask() {

        getServer().getScheduler().runTaskTimer(this, () -> {

            cooldown.entrySet().removeIf(entry -> {
                long targetTime = entry.getValue();
                boolean b = System.currentTimeMillis() > targetTime;
                Player p = Bukkit.getPlayer(entry.getKey());
                if (p != null) p.spigot().sendMessage(ChatMessageType.ACTION_BAR, getDashCooldownText(b, targetTime));
                return b;
            });

            for (Match match : matches.values()) {
                if (match.getMatchState().equals(IN_PROGRESS)) {
                    int matchTimer = --match.matchTimer;

                    if (matchTimer % 60 == 0 && matchTimer > 0) {
                        match.getAllPlayer(true).forEach(player -> {
                            if (player != null) {
                                player.sendMessage(I18n.format("match_time_left_min", "min", matchTimer / 60));
                            }
                        });
                    }
                    if (matchTimer == 30 || matchTimer == 15 || matchTimer <= 10 && matchTimer > 0) {
                        match.getAllPlayer(true).forEach(player -> {
                            if (player != null) {
                                player.sendMessage(I18n.format("match_time_left_sec", "sec", matchTimer));
                            }
                        });
                    }
                    if (matchTimer <= 0) {
                        match.endMatch();
                        if (match.getMatchState() != OVERTIME) {
                            match.setMatchState(READY);
                        }
                    }
                } else {
                    for (Player player : match.getAllPlayer(false)) {
                        cooldown.remove(player.getUniqueId());
                    }
                }
            }
        }, 0, 20);


        getServer().getScheduler().runTaskTimer(this, () -> {

            for (Map.Entry<String, Ball> entry : balls.entrySet()) {
                Ball ballData = entry.getValue();
                if (ballData.getBall() != null) {
                    ballData.getBall().setTicksLived(1);

                    Match match = matches.get(ballData.getId());

                    ballData.getBall().getNearbyEntities(10, 10, 10)
                            .stream().filter(entity -> entity instanceof Player)
                            .forEach(p -> {
                                Player player = (Player) p;
                                // if player is colliding the ball
                                if (player.getLocation().distance(ballData.getBall().getLocation()) < 1 || (
                                        player.getLocation().distance(ballData.getBall().getLocation()) < 2.5 &&
                                                Math.floor(ballData.getBall().getLocation().getX()) == Math.floor(player.getLocation().getX()) &&
                                                Math.floor(ballData.getBall().getLocation().getZ()) == Math.floor(player.getLocation().getZ()))) {

                                    // compute velocity to the ball
                                    Vector velocity = getVector(player, ballData);

                                    // apply ball trajectory
                                    ballData.getBall().setVelocity(velocity);
                                    ballData.getBall().setGravity(true);
                                    ballData.getBall().getWorld().playSound(ballData.getBall().getLocation(), Sound.BLOCK_STONE_HIT, 10, 1);
                                    ballData.setPlayerCollisionTick(0);

                                    if (match != null) {
                                        match.setLastTouchPlayer(player);
                                    }
                                }
                            });

                    //compute bouncing on other blocks
                    if (ballData.getPlayerCollisionTick() > 3) {

                        boolean zBouncing = abs(ballData.getLastVelocity().getZ()) - abs(ballData.getBall().getVelocity().getZ()) > 0.2 && ballData.getBall().getVelocity().getZ() == 0;
                        boolean xBouncing = abs(ballData.getLastVelocity().getX()) - abs(ballData.getBall().getVelocity().getX()) > 0.2 && ballData.getBall().getVelocity().getX() == 0;
                        boolean yBouncing = abs(ballData.getLastVelocity().getY()) - abs(ballData.getBall().getVelocity().getY()) > 0.2 && ballData.getBall().getVelocity().getY() == 0;

                        if (zBouncing) {
                            ballData.getBall().setVelocity(ballData.getBall().getVelocity().setZ(-ballData.getLastVelocity().getZ()));
                            ballData.getBall().getVelocity().setZ(-ballData.getLastVelocity().getZ());
                            ballData.getBall().getWorld().playSound(ballData.getBall().getLocation(), Sound.BLOCK_WOOL_HIT, 10, 1);
                        }
                        if (xBouncing) {
                            ballData.getBall().setVelocity(ballData.getBall().getVelocity().setX(-ballData.getLastVelocity().getX()));
                            ballData.getBall().getVelocity().setX(-ballData.getLastVelocity().getX());
                            ballData.getBall().getWorld().playSound(ballData.getBall().getLocation(), Sound.BLOCK_WOOL_HIT, 10, 1);
                        }
                        if (yBouncing) {
                            ballData.getBall().setGravity(true);
                            ballData.getBall().setVelocity(ballData.getBall().getVelocity().setY(-ballData.getLastVelocity().getY()));
                            ballData.getBall().getWorld().playSound(ballData.getBall().getLocation(), Sound.BLOCK_WOOL_HIT, 10, 1);
                        }
                    }

                    if (match != null) {
                        match.checkGoal(ballData.getBall().getLocation());
                    }

                    ballData.setLastVelocity(ballData.getBall().getVelocity().clone());
                    ballData.setPlayerCollisionTick(ballData.getPlayerCollisionTick() + 1);
                }
            }
        }, 0, 2);
    }

    private static TextComponent getDashCooldownText(boolean b, long targetTime) {
        if (b) return new TextComponent(I18n.get("dash_ready"));
        return new TextComponent(I18n.format("dash_cooldown", "time", (int) ((targetTime - System.currentTimeMillis()) / 1000.0 + 1)));
    }

    private static Vector getVector(Player player, Ball ballData) {
        double yVelocity = 0.15;
        double xzMul = 1;

        if (player.isSneaking()) {
            yVelocity = 0.3;
            xzMul = 3.5;
        } else if (player.isSprinting()) {
            yVelocity = 0.25;
        }

        Vector velocity = ballData.getBall().getVelocity();
        velocity.setY(ballData.getBall().getVelocity().getY() + yVelocity + player.getVelocity().getY() / 2);
        velocity.setX(ballData.getBall().getVelocity().getX() + (player.getLocation().getDirection().getX() / 2) * xzMul);
        velocity.setZ(ballData.getBall().getVelocity().getZ() + (player.getLocation().getDirection().getZ() / 2) * xzMul);

        // if player is not moving, create bouncing on it
        if (abs(player.getVelocity().getX() + player.getVelocity().getY() + player.getVelocity().getZ()) == 0) {
            velocity.setY(0);
            velocity.setX(0);
            velocity.setZ(0);
        }
        return velocity;
    }
}


