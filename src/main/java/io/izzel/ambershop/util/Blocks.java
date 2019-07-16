package io.izzel.ambershop.util;

import lombok.experimental.UtilityClass;
import lombok.val;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.util.blockray.BlockRay;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.util.Optional;

@UtilityClass
public class Blocks {

    private final int X_MASK = 0xf000;
    private final int Y_MASK = 0x0ff0;
    private final int Z_MASK = 0x000f;

    public short toShort(int x, int y, int z) {
        return (short) (((x << 12) & X_MASK) | ((y << 4) & Y_MASK) | (z & Z_MASK));
    }

    public int[] coord(long chunk, short pos) {
        val cz = (int) (chunk & 0xffffffffL);
        val cx = (int) (chunk >>> 32);
        val posi = ((int) pos);
        val px = (posi >>> 12) & 0xf;
        val pz = posi & 0xf;
        val y = (posi >>> 4) & 0xff;
        val x = (cx << 4) | px;
        val z = (cz << 4) | pz;
        return new int[]{x, y, z};
    }

    public long toLong(int x, int z) {
        return (((long) x) << 32) | (((long) z) & 0xffffffffL);
    }

    public Optional<Location<World>> playerOnCursor(Player player) {
        val blockRay = BlockRay.from(player)
            .stopFilter(BlockRay.continueAfterFilter(BlockRay.onlyAirFilter(), 1))
            .distanceLimit(6)
            .build();
        val end = blockRay.end();
        if (end.isPresent()) {
            val hit = end.get();
            val loc = hit.getLocation();
            return Optional.of(loc);
        } else {
            return Optional.empty();
        }
    }

}
