package io.izzel.ambershop.listener;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.izzel.ambershop.data.ShopDataSource;
import io.izzel.ambershop.util.AmberTasks;
import lombok.val;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.world.chunk.LoadChunkEvent;
import org.spongepowered.api.event.world.chunk.UnloadChunkEvent;

@Singleton
public class ChunkListener {

    @Inject private ShopDataSource dataSource;
    @Inject private AmberTasks tasks;

    @Listener
    public void onLoad(LoadChunkEvent event) {
        val chunk = event.getTargetChunk().getPosition();
        tasks.async().submit(() ->
            dataSource.getByChunk(event.getTargetChunk().getWorld().getUniqueId(), chunk.getX(), chunk.getZ()));
    }

    @Listener
    public void onUnload(UnloadChunkEvent event) {
        val chunk = event.getTargetChunk().getPosition();
        dataSource.unloadChunk(event.getTargetChunk().getWorld().getUniqueId(), chunk.getX(), chunk.getZ());
    }

}
