package com.github.squi2rel.cb.util;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

import java.util.*;

public class BlockScanUtil {
    private static final BlockFace[] HORIZONTAL = {
            BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST
    };

    public static List<Location> scanXZ(Location start, int maxResults) {
        if (start == null) return Collections.emptyList();

        World world = start.getWorld();
        if (world == null) return Collections.emptyList();

        Block startBlock = start.getBlock();
        Material target = startBlock.getType();
        int startY = startBlock.getY();

        List<Location> result = new ArrayList<>();
        Deque<Block> queue = new ArrayDeque<>();
        Set<Long> visited = new HashSet<>();

        queue.add(startBlock);
        visited.add(encode(startBlock));

        while (!queue.isEmpty()) {
            if (maxResults > 0 && result.size() >= maxResults) break;

            Block b = queue.poll();
            if (b.getType() != target || b.getY() != startY) continue;

            result.add(b.getLocation());

            for (BlockFace face : HORIZONTAL) {
                Block nb = b.getRelative(face);

                long code = encode(nb);
                if (visited.contains(code)) continue;
                visited.add(code);

                if (nb.getType() == target) {
                    queue.add(nb);
                }
            }
        }

        return result;
    }

    private static long encode(Block b) {
        int x = b.getX();
        int y = b.getY();
        int z = b.getZ();
        long lx = x ^ 0x80000000;
        long ly = y & 0xFFFF;
        long lz = z ^ 0x80000000;
        return (lx << 32) | (ly << 16) ^ lz;
    }
}
