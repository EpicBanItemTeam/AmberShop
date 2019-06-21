package io.izzel.ambershop.mixin.impl;

import io.izzel.ambershop.mixin.AmberPlayer;
import lombok.val;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.play.server.*;
import net.minecraft.tileentity.TileEntitySign;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.util.Direction;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.common.item.inventory.util.ItemStackUtil;
import org.spongepowered.common.text.SpongeTexts;
import org.spongepowered.common.world.WorldUtil;

import java.util.*;

@Mixin(EntityPlayerMP.class)
public abstract class MixinAmberPlayer implements AmberPlayer, Player {

    @Shadow public NetHandlerPlayServer connection;

    private Map<Location<World>, Direction> signMap = new HashMap<>();

    @Override
    public void sendSign(Location<World> location, Direction direction, List<Text> lines) {
        val blockState = BlockTypes.WALL_SIGN.getDefaultState().with(Keys.DIRECTION, direction).get();
        sendBlockChange(location.getBlockPosition(), blockState);
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
        signMap.put(location, direction);
    }

    @Override
    public void resetSign(Location<World> location) {
        resetBlockChange(location.getBlockPosition());
        signMap.remove(location);
    }

    private Map<Location<World>, Integer> eidMap = new HashMap<>();

    @Override
    public void sendDroppedItem(Location<World> location, ItemStack stack) {
        val lastId = eidMap.remove(location);
        if (lastId != null) destroyEntity(lastId);
        val entityItem = new EntityItem(WorldUtil.asNative(location.getExtent()),
                location.getX(), location.getY(), location.getZ(), ItemStackUtil.toNative(stack));
        val newId = entityItem.getEntityId();
        eidMap.put(location, newId);
        val spawn = new SPacketSpawnObject(entityItem, 2, 1);// type:2 Item data:1 magic value
        connection.sendPacket(spawn); // spawn
        val velocity = new SPacketEntityVelocity(newId, 0, 0, 0);
        connection.sendPacket(velocity); // velocity
        val metadata = new SPacketEntityMetadata(newId, entityItem.getDataManager(), false);
        connection.sendPacket(metadata); // metadata
    }

    @Override
    public void resetDroppedItem(Location<World> location) {
        val id = eidMap.remove(location);
        if (id != null) destroyEntity(id);
    }

    @Override
    public Optional<Direction> getSign(Location<World> location) {
        return Optional.ofNullable(signMap.get(location));
    }

    private void destroyEntity(int eid) {
        val packet = new SPacketDestroyEntities(eid);
        connection.sendPacket(packet);
    }

}
