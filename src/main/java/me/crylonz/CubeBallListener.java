package me.crylonz;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.util.Vector;

import java.util.concurrent.atomic.AtomicReference;

import static java.lang.Double.max;
import static java.lang.Double.min;
import static java.lang.Math.abs;
import static me.crylonz.CubeBall.*;

public class CubeBallListener implements Listener {

    @EventHandler
    public void blockChangeEvent(EntityChangeBlockEvent e) {
        for (Match match : matches.values()) {
            Material block = match.getData().cubeBallBlock;
            if (e.getTo().equals(block)) {
                if (e.getEntityType() == EntityType.FALLING_BLOCK) {
                    e.setCancelled(true);

                    Ball ballData = fetchBallContacting(e.getBlock().getLocation());

                    if (ballData != null) {
                        String ballId = ballData.getId();

                        if (ballData.getBall() != null) {
                            Vector velocity = ballData.getBall().getVelocity();
                            double zVelocity = abs(velocity.getZ()) / 1.5;
                            double xVelocity = abs(velocity.getX()) / 1.5;
                            double maxZX = max(zVelocity, xVelocity);

                            velocity.setY(min(maxZX, 0.5));

                            destroyBall(ballId);
                            generateBall(block, ballId, e.getEntity().getLocation(), ballData.getLastVelocity());

                            ballData = balls.get(ballId);
                            ballData.getBall().setVelocity(velocity);

                            if (abs(velocity.getX() + velocity.getY() + velocity.getZ()) <= 0.001 || velocity.getY() < 0.025) {
                                ballData.getBall().setVelocity(ballData.getBall().getVelocity().zero());
                                ballData.getBall().setGravity(false);
                            } else {
                                if (abs(velocity.getX() + velocity.getY() + velocity.getZ()) > 0.1) {
                                    ballData.getBall().getWorld().playSound(ballData.getBall().getLocation(), Sound.BLOCK_WOOL_HIT, 10, 1);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @EventHandler
    public static void onPlayerJoin(PlayerJoinEvent e) {
        Player player = e.getPlayer();
        for (Match match : matches.values()) {
            match.replacePlayer(player);
        }
    }

    @EventHandler
    public static void onSwapItem(PlayerSwapHandItemsEvent event) {
        for (Match match : matches.values()) {
            if (match.getMatchState() == MatchState.IN_PROGRESS && match.containsPlayer(event.getPlayer())) {
                int cd = match.getData().dashCooldown;
                if (cd <= 0) break;
                if (!cooldown.containsKey(event.getPlayer().getUniqueId())) {
                    cooldown.put(event.getPlayer().getUniqueId(), System.currentTimeMillis() + cd * 1000L);
                    launch(event.getPlayer(), 2);
                    event.setCancelled(true);
                }
                break;
            }
        }
    }

    @EventHandler
    public static void onPlayerLeave(PlayerQuitEvent event) {
        cooldown.remove(event.getPlayer().getUniqueId());
    }

    private Ball fetchBallContacting(Location location) {

        AtomicReference<Ball> ballTrigger = new AtomicReference<>();
        balls.forEach((id, ball) -> {
            if (ball.getBall().getWorld().equals(location.getWorld()) &&
                    ball.getBall().getLocation().distanceSquared(location) < 2) {
                ballTrigger.set(ball);
            }
        });
        return ballTrigger.get();
    }
}
