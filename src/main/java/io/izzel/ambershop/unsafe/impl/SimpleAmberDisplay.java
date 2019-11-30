package io.izzel.ambershop.unsafe.impl;

import com.google.inject.Singleton;
import io.izzel.ambershop.unsafe.AmberDisplay;
import lombok.val;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.play.server.*;
import net.minecraft.tileentity.TileEntitySign;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.util.Direction;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;
import org.spongepowered.common.text.SpongeTexts;

import java.util.*;

@Singleton
public class SimpleAmberDisplay implements AmberDisplay {

    private Map<UUID, Map<Location<World>, Direction>> signMap = new HashMap<>();

    @Override
    public void sendSign(Player player, Location<World> location, Direction direction, List<Text> lines) {
        val connection = ((EntityPlayerMP) player).connection;
        val blockState = BlockTypes.WALL_SIGN.getDefaultState().with(Keys.DIRECTION, direction).get();
        player.sendBlockChange(location.getBlockPosition(), blockState);
        val sign = new TileEntitySign();
        val blockPos = new BlockPos(location.getX(), location.getY(), location.getZ());
        sign.setPos(blockPos);
        for (int i = 0; i < sign.signText.length; i++) {
            Text line = lines.size() > i ? lines.get(i) : Text.EMPTY;
            sign.signText[i] = SpongeTexts.toComponent(line);
        }
        val compound = new NBTTagCompound();
        sign.writeToNBT(compound);
        val packet = new SPacketUpdateTileEntity(blockPos, 9, compound);
        connection.sendPacket(packet);
        signMap.computeIfAbsent(player.getUniqueId(), any -> new HashMap<>()).put(location, direction);
    }

    @Override
    public void resetSign(Player player, Location<World> location) {
        player.resetBlockChange(location.getBlockPosition());
        signMap.computeIfAbsent(player.getUniqueId(), any -> new HashMap<>()).remove(location);
    }

    private Map<UUID, Map<Location<World>, Integer>> eidMap = new HashMap<>();

    @Override
    public void sendDroppedItem(Player player, Location<World> location, ItemStack stack) {
        removeOld(player, location);
        val connection = ((EntityPlayerMP) player).connection;
        @SuppressWarnings("ConstantConditions")
        val entityItem = new EntityItem(((WorldServer) location.getExtent()),
            location.getX(), location.getY(), location.getZ(), ((net.minecraft.item.ItemStack) (Object) stack));
        entityItem.setNoDespawn();
        val newId = entityItem.getEntityId();
        eidMap.computeIfAbsent(player.getUniqueId(), any -> new HashMap<>()).put(location, newId);
        val spawn = new SPacketSpawnObject(entityItem, 2, 1);// type:2 Item data:1 magic value
        connection.sendPacket(spawn); // spawn
        val velocity = new SPacketEntityVelocity(newId, 0, 0, 0);
        connection.sendPacket(velocity); // velocity
        val metadata = new SPacketEntityMetadata(newId, entityItem.getDataManager(), false);
        connection.sendPacket(metadata); // metadata
    }

    private void removeOld(Player player, Location<World> location) {
        val connection = ((EntityPlayerMP) player).connection;
        val lastId = eidMap.computeIfAbsent(player.getUniqueId(), any -> new HashMap<>()).remove(location);
        if (lastId != null) {
            val packet = new SPacketDestroyEntities(lastId);
            connection.sendPacket(packet);
        }
    }

    @Override
    public void resetDroppedItem(Player player, Location<World> location) {
        removeOld(player, location);
    }

    @Override
    public Optional<Direction> getSign(Player player, Location<World> location) {
        return Optional.ofNullable(signMap.computeIfAbsent(player.getUniqueId(), any -> new HashMap<>()).get(location));
    }

}
