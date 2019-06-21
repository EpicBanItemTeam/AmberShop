package io.izzel.ambershop.data;

import com.google.inject.ImplementedBy;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.util.*;
import java.util.concurrent.Future;

@ImplementedBy(ShopDataSourceImpl.class)
public interface ShopDataSource {

    void init();

    Future<OperateResult> removeRecord(ShopRecord record);

    Future<OperateResult> updateRecord(ShopRecord newRec);

    Future<OperateResult> moveLocation(ShopRecord old, Location<World> dest);

    Future<List<ShopRecord>> fetchRecordBy(Map<String, String> map);

    Optional<ShopRecord> getByLocation(Location<World> location);

    default Future<List<ShopRecord>> getByPlayer(Player player) {
        return getByUUID(player.getUniqueId());
    }

    Future<List<ShopRecord>> getByUUID(UUID uuid);

    Future<Optional<ShopRecord>> getById(int id);

    Future<ShopRecord> addRecord(ShopRecord record);

    Future<Collection<ShopRecord>> getByChunk(UUID world, int x, int z);

    Future<Void> unloadChunk(UUID world, int x, int z);

}
