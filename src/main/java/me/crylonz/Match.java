package me.crylonz;

import com.github.squi2rel.cb.I18n;
import com.github.squi2rel.cb.MatchData;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.util.Vector;

import java.util.*;

import static me.crylonz.CubeBall.*;
import static me.crylonz.MatchState.*;
import static org.bukkit.Bukkit.getServer;

public class Match {
    public int matchTimer;

    private final String name;
    private final Random rand = new Random();
    private final HashSet<Player> blueTeam = new HashSet<>();
    private final HashSet<Player> redTeam = new HashSet<>();
    private final HashSet<Player> spectatorTeam = new HashSet<>();
    private final HashMap<UUID, Integer> goals = new HashMap<>();
    private MatchState matchState;
    private UUID lastTouchPlayer;
    private int blueScore = 0;
    private int redScore = 0;
    private boolean canceled;
    private final MatchData data;

    public Match(String name, Player player) {
        this(name, MatchData.create(player.getName(), player.getUniqueId()));
    }
    public Match(String name, MatchData config) {
        this.name = name;
        this.data = config;
        matchState = CREATED;
    }

    public void scanNearPlayers(List<Location> spawns, Team team) {
        for (Location spawn : spawns) {
            if (spawn == null) continue;
            World world = Objects.requireNonNull(spawn.getWorld());
            for (Entity entity : world.getNearbyEntities(spawn, 1, 1, 1)) {
                if (entity instanceof Player) {
                    Player player = (Player) entity;
                    if (player.getVehicle() == null) addPlayerToTeam(player, team);
                }
            }
        }
    }

    public void scanPlayer() {
        blueTeam.clear();
        redTeam.clear();
        scanNearPlayers(data.blueTeamSpawns, Team.BLUE);
        scanNearPlayers(data.redTeamSpawns, Team.RED);
        World world = data.ballSpawn.getWorld();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getWorld() != world) continue;
            if (player.getLocation().distance(data.ballSpawn) < 256) {
                if (!blueTeam.contains(player) && !redTeam.contains(player)) addPlayerToTeam(player, Team.SPECTATOR);
            }
        }
        matchState = READY;
    }

    public void start(Player p) {
        if (matchState == READY) {
            reset();
            if (!blueTeam.isEmpty() || !redTeam.isEmpty()) {
                sortSpawns();

                startDelayedRound();
                matchTimer = data.matchDuration;
                matchState = IN_PROGRESS;

                p.sendMessage(I18n.get("match_starting"));
                getAllPlayer(true).forEach(player -> {
                    player.sendMessage(I18n.format("match_started", "min", matchTimer / 60, "sec", matchTimer - ((matchTimer / 60) * 60)));
                    player.sendMessage(I18n.format("max_goals", "max", data.maxGoal <= 0 ? I18n.get("max_goals_unlimited") : data.maxGoal));
                });
            } else {
                p.sendMessage(I18n.get("need_add_players"));
            }
        } else {
            p.sendMessage(I18n.get("match_not_ready"));
        }
    }

    private void sortSpawns() {
        Comparator<Location> sort = Comparator.comparingDouble(l -> l.distance(data.ballSpawn));
        data.blueTeamSpawns.sort(sort);
        data.redTeamSpawns.sort(sort);
    }

    public int[] randomIds(int size, int n) {
        if (size <= 0 || n <= 0) return new int[0];

        int[] result = new int[n];
        int filled = 0;

        while (filled < n) {
            int segmentLen = Math.min(size, n - filled);

            int[] pool = new int[segmentLen];
            for (int i = 0; i < segmentLen; i++) {
                pool[i] = i;
            }

            for (int i = 0; i < segmentLen; i++) {
                int j = i + rand.nextInt(segmentLen - i);
                int tmp = pool[i];
                pool[i] = pool[j];
                pool[j] = tmp;
                result[filled++] = pool[i];
            }
        }
        return result;
    }

    public void teleportTeam(HashSet<Player> team, List<Location> spawns) {
        int[] ids = randomIds(spawns.size(), team.size());
        int i = 0;
        for (Player player : team) {
            player.teleport(getFacingLocation(spawns.get(ids[i++]), data.ballSpawn));
        }
    }

    public static Location getFacingLocation(Location from, Location to) {
        Location loc = from.clone();
        Vector direction = to.toVector().subtract(loc.toVector());
        double dx = direction.getX();
        double dy = direction.getY();
        double dz = direction.getZ();
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float pitch = (float) Math.toDegrees(-Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));
        loc.setYaw(yaw);
        loc.setPitch(pitch);
        return loc;
    }

    private void startDelayedRound() {
        teleportTeam(blueTeam, data.blueTeamSpawns);
        teleportTeam(redTeam, data.redTeamSpawns);

        PotionEffect effect = new PotionEffect(PotionEffectType.SLOWNESS, 80, 255);
        List<Location> allSpawns = getAllSpawns();
        for (Location spawn : allSpawns) surroundWith(spawn, Material.BARRIER);
        getAllPlayer(false).forEach(player -> {
            player.setVelocity(new Vector(0, 0, 0));
            player.addPotionEffect(effect);
        });
        Bukkit.getScheduler().runTaskLater(plugin, () -> sendMessageToAllPlayer(I18n.get("countdown_3"), "", 1, Sound.BLOCK_NOTE_BLOCK_BELL, 1), 20);
        Bukkit.getScheduler().runTaskLater(plugin, () -> sendMessageToAllPlayer(I18n.get("countdown_2"), "", 1, Sound.BLOCK_NOTE_BLOCK_BELL, 1), 40);
        Bukkit.getScheduler().runTaskLater(plugin, () -> sendMessageToAllPlayer(I18n.get("countdown_1"), "", 1, Sound.BLOCK_NOTE_BLOCK_BELL, 1), 60);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            sendMessageToAllPlayer(I18n.get("go"), "", 1, Sound.BLOCK_NOTE_BLOCK_BELL, 2);
            for (Location spawn : getAllSpawns()) surroundWith(spawn, Material.AIR);
            if (canceled) return;
            startRound();
        }, 80);
    }

    private List<Location> getAllSpawns() {
        ArrayList<Location> allSpawns = new ArrayList<>();
        allSpawns.addAll(data.blueTeamSpawns);
        allSpawns.addAll(data.redTeamSpawns);
        return allSpawns;
    }

    private void startRound() {
        matchState = matchTimer > 0 ? IN_PROGRESS : OVERTIME;
        removeBall();
        generateBall(data.cubeBallBlock, name, data.ballSpawn, null);
    }

    private static void surroundWith(Location base, Material block) {
        Block pos = base.getBlock();
        int[][] offsets = {{1, 0}, {0, 1}, {-1, 0}, {0, -1}};
        for (int[] offset : offsets) {
            pos.getRelative(offset[0], 0, offset[1]).setType(block);
            pos.getRelative(offset[0], 1, offset[1]).setType(block);
        }
        pos.getRelative(0, 2, 0).setType(block);
    }

    public void replacePlayer(Player player) {
        replacePlayer(blueTeam, player);
        replacePlayer(redTeam, player);
        replacePlayer(spectatorTeam, player);
    }

    private static void replacePlayer(HashSet<Player> players, Player newPlayer) {
        UUID uuid = newPlayer.getUniqueId();
        Player oldPlayer = null;

        for (Player p : players) {
            if (p.getUniqueId().equals(uuid)) {
                oldPlayer = p;
                break;
            }
        }

        if (oldPlayer != null) {
            players.remove(oldPlayer);
            players.add(newPlayer);
        }
    }


    public void addPlayerToTeam(Player p, Team team) {
        if (p != null) {
            if (team.equals(Team.BLUE)) {
                blueTeam.add(p);
                redTeam.remove(p);
                spectatorTeam.remove(p);
            } else if (team.equals(Team.RED)) {
                redTeam.add(p);
                blueTeam.remove(p);
                spectatorTeam.remove(p);
            } else {
                spectatorTeam.add(p);
                blueTeam.remove(p);
                redTeam.remove(p);
            }
            p.sendMessage(I18n.format("your_team", "team", I18n.get(team == Team.BLUE ? "blue_name" : team == Team.SPECTATOR ? "spectator_name" : "red_name")));
        }
    }

    public void checkGoal(Location ballLocation) {
        if (matchState == IN_PROGRESS || matchState == OVERTIME) {

            int ballX = ballLocation.getBlockX();
            int ballZ = ballLocation.getBlockZ();
            for (Location blockLocation : data.blueTeamGoalBlocks) {
                if (ballX == blockLocation.getBlockX() &&
                        ballZ == blockLocation.getBlockZ()) {
                    goal(Team.RED);
                    return;
                }
            }

            for (Location blockLocation : data.redTeamGoalBlocks) {
                if (ballX == blockLocation.getBlockX() &&
                        ballZ == blockLocation.getBlockZ()) {
                    goal(Team.BLUE);
                    return;
                }
            }
        }
    }

    private void goal(Team team) {
        if (Team.BLUE.equals(team)) {
            blueScore++;
            triggerGoalAnimation(Team.BLUE);

        } else {
            redScore++;
            triggerGoalAnimation(Team.RED);
        }

        if (matchState == IN_PROGRESS && (data.maxGoal <= 0 || (blueScore != data.maxGoal && redScore != data.maxGoal))) {
            sendScoreToPlayer();
            matchState = GOAL;
            getServer().getScheduler().runTaskLater(plugin, this::startDelayedRound, 20 * 3);
        } else {
            matchState = GOAL;
            endMatch();
        }
        destroyBall(name);
    }

    public void spawnFirework(Team team) {
        BukkitScheduler scheduler = getServer().getScheduler();
        for (int i = 0; i < 3; i++) {
            int offset = i * 30;
            scheduler.runTaskLater(plugin, () -> spawnFireworkFor(team), offset + 5);
            scheduler.runTaskLater(plugin, () -> spawnFireworkFor(team), offset + 10);
            scheduler.runTaskLater(plugin, () -> spawnFireworkFor(team), offset + 15);
        }
    }

    public void spawnFireworkFor(Team team) {
        HashSet<Player> players = team == Team.BLUE ? blueTeam : redTeam;
        for (Player player : players) {
            player.getWorld().spawnEntity(player.getLocation(), EntityType.FIREWORK_ROCKET);
        }
    }

    public void endMatch() {
        String title;
        if (getBlueScore() > getRedScore()) {
            title = I18n.get("blue_win");
            spawnFirework(Team.BLUE);
        } else if (getBlueScore() < getRedScore()) {
            title = I18n.get("red_win");
            spawnFirework(Team.RED);
        } else {
            title = I18n.get("overtime");
            setMatchState(OVERTIME);
        }

        String score = ChatColor.BLUE.toString() + getBlueScore() + ChatColor.WHITE + " - " + ChatColor.RED + getRedScore();
        sendMessageToAllPlayer(title, score, 3, Sound.ENTITY_RABBIT_DEATH, 0.5f);

        ArrayList<Map.Entry<UUID, Integer>> list = new ArrayList<>(goals.entrySet());
        list.sort((e1, e2) -> Integer.compare(e2.getValue(), e1.getValue()));
        getAllPlayer(true).forEach(player -> {
            player.sendMessage(I18n.get("game_over"));
            player.sendMessage(I18n.get("goal_rank"));
            player.sendMessage(I18n.format("total_goals", "total", redScore + blueScore));
            int i = 0;
            for (Map.Entry<UUID, Integer> entry : list) {
                player.sendMessage(I18n.format("player_goal",
                        "rank", ++i,
                        "color", (blueTeam.stream().anyMatch(p -> p.getUniqueId().equals(entry.getKey())) ? ChatColor.BLUE : ChatColor.RED),
                        "name", getName(entry.getKey()),
                        "goals", entry.getValue()
                ));
            }
        });

        removeBall();
        reset();
    }

    private static String getName(UUID uuid) {
        if (uuid == null) return "null";
        Player p = Bukkit.getPlayer(uuid);
        return p != null ? p.getDisplayName() : uuid.toString();
    }

    public void reset() {
        setMatchState(READY);
        blueScore = 0;
        redScore = 0;
        goals.clear();
        canceled = false;
    }

    public void cancel() {
        removeBall();
        reset();
        canceled = true;
        for (Player p : getAllPlayer(true)) p.sendMessage(I18n.get("match_canceled"));
    }

    public void sendScoreToPlayer() {
        String title = I18n.format("score_title", "blue", blueScore, "red", redScore);
        String subtitle = I18n.format("score_subtitle", "name", getName(lastTouchPlayer).toUpperCase(), "speed", computeSpeedGoal());
        goals.put(lastTouchPlayer, goals.getOrDefault(lastTouchPlayer, 0) + 1);
        sendMessageToAllPlayer(title, subtitle, 3, Sound.WEATHER_RAIN, 0.5f);
    }

    public double computeSpeedGoal() {

        Ball ball = balls.get(name);

        if (ball != null && ball.getBall() != null) {
            return Math.round((Math.abs((ball.getLastVelocity().getX())) + Math.abs((ball.getLastVelocity().getY())) + Math.abs((ball.getLastVelocity().getZ()))) * 100);
        }
        return 0;
    }

    public void sendMessageToAllPlayer(String title, String subtitle, int duration, Sound sound, float pitch) {
        send(blueTeam, title, subtitle, duration, sound, pitch);
        send(redTeam, title, subtitle, duration, sound, pitch);
        send(spectatorTeam, title, subtitle, duration, sound, pitch);
    }

    private void send(HashSet<Player> team, String title, String subtitle, int duration, Sound sound, float pitch) {
        team.forEach(player -> {
            if (player != null) {
                player.sendTitle(title, subtitle, 1, duration * 20, 1);
                player.playSound(player.getLocation(), sound, 10, pitch);
            }
        });
    }

    public void triggerGoalAnimation(Team team) {
        if (team.equals(Team.BLUE)) {
            data.redTeamGoalBlocks.forEach(block -> {
                Objects.requireNonNull(block.getWorld()).spawnEntity(block.getBlock().getLocation(), EntityType.FIREWORK_ROCKET);
                Objects.requireNonNull(block.getWorld()).playEffect(block.getBlock().getLocation(), Effect.VILLAGER_PLANT_GROW, 3);
            });
        } else {
            data.blueTeamGoalBlocks.forEach(block -> {
                Objects.requireNonNull(block.getWorld()).spawnEntity(block.getBlock().getLocation(), EntityType.FIREWORK_ROCKET);
                Objects.requireNonNull(block.getWorld()).playEffect(block.getBlock().getLocation(), Effect.VILLAGER_PLANT_GROW, 3);
            });

        }
    }

    public String buildTeam() {
        StringBuilder sb = new StringBuilder();
        sb.append(I18n.format("blue_team", "count", this.blueTeam.size())).append('\n');
        this.blueTeam.forEach(player -> {
            if (player != null) {
                sb.append("- ").append(ChatColor.BLUE).append(player.getDisplayName()).append('\n');
            }
        });

        sb.append(I18n.format("red_team", "count", this.redTeam.size())).append('\n');
        this.redTeam.forEach(player -> {
            if (player != null) {
                sb.append("- ").append(ChatColor.RED).append(player.getDisplayName()).append('\n');
            }
        });

        sb.append(I18n.format("spectator_team", "count", this.spectatorTeam.size())).append('\n');
        this.spectatorTeam.forEach(player -> {
            if (player != null) {
                sb.append("- ").append(ChatColor.GREEN).append(player.getDisplayName()).append('\n');
            }
        });
        return sb.toString();
    }

    public String getName() {
        return name;
    }

    public void setLastTouchPlayer(Player lastTouchPlayer) {
        this.lastTouchPlayer = lastTouchPlayer.getUniqueId();
    }

    public HashSet<Player> getBlueTeam() {
        return blueTeam;
    }

    public HashSet<Player> getRedTeam() {
        return redTeam;
    }

    public HashSet<Player> getSpectatorTeam() {
        return spectatorTeam;
    }

    public ArrayList<Player> getAllPlayer(boolean spectator) {
        ArrayList<Player> team = new ArrayList<>();
        team.addAll(getRedTeam());
        team.addAll(getBlueTeam());
        if (spectator) team.addAll(getSpectatorTeam());
        return team;
    }

    public boolean containsPlayer(Player player) {
        if (getRedTeam().contains(player)) return true;
        return getBlueTeam().contains(player);
    }

    public int getBlueScore() {
        return blueScore;
    }

    public int getRedScore() {
        return redScore;
    }

    public MatchState getMatchState() {
        return matchState;
    }

    public void setMatchState(MatchState matchState) {
        this.matchState = matchState;
    }

    public MatchData getData() {
        return data;
    }

    public void removeBall() {
        destroyBall(name);
    }

    public boolean pause() {
        if (matchState == IN_PROGRESS || matchState == OVERTIME) {
            matchState = PAUSED;
            removeBall();
            return true;
        }
        return false;
    }

    public boolean resume() {
        if (matchState == PAUSED) {
            startDelayedRound();
            return true;
        }
        return false;
    }
}
