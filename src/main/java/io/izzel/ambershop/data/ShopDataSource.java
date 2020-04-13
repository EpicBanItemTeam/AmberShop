package io.izzel.ambershop.data;

import com.google.inject.ImplementedBy;
import io.izzel.ambershop.util.OperationResult;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

@ImplementedBy(ShopDataSourceImpl.class)
public interface ShopDataSource {

    void init();

    Future<OperationResult> removeRecord(ShopRecord record);

    Future<OperationResult> updateRecord(ShopRecord newRec);

    Future<OperationResult> moveLocation(ShopRecord old, Location<World> dest);

    Future<List<ShopRecord>> fetchRecordBy(List<String> map);

    Optional<ShopRecord> getByLocation(Location<World> location);

    default Future<List<ShopRecord>> getByPlayer(Player player) {
        return getByUUID(player.getUniqueId());
    }

    Future<List<ShopRecord>> getByUUID(UUID uuid);

    Future<Optional<ShopRecord>> getById(int id);

    Future<OperationResult> addRecord(ShopRecord record);

    CompletableFuture<Collection<ShopRecord>> getByChunk(UUID world, int x, int z);

    Future<Void> unloadChunk(UUID world, int x, int z);

    Future<OperationResult> fixAll();

}
