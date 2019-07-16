package io.izzel.ambershop.data;

import com.google.common.io.ByteStreams;
import io.izzel.ambershop.util.Blocks;
import io.izzel.ambershop.util.Inventories;
import io.netty.buffer.Unpooled;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.tileentity.carrier.TileEntityCarrier;
import org.spongepowered.api.data.persistence.DataFormats;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Data
public final class ShopRecord {

    @NonNull public int id;

    @NonNull public long createTime;

    @NonNull public UUID owner;

    @NonNull public UUID world;

    @NonNull public int x, y, z;

    @NonNull public double price;

    @NonNull public byte[] meta;

    private boolean unlimited = false; // 1

    private ItemStackSnapshot itemType; // 2

    public void setItemType(ItemStack stack) {
        val copy = stack.copy();
        copy.setQuantity(1);
        itemType = stack.createSnapshot();
    }

    public Location<World> getLocation() {
        return new Location<>(Sponge.getServer().getWorld(world).get(), x, y, z);
    }

    public int getStock() {
        return getLocation().getTileEntity()
            .filter(TileEntityCarrier.class::isInstance)
            .map(TileEntityCarrier.class::cast)
            .map(TileEntityCarrier::getInventory)
            .map(it -> {
                if (price > 0) {
                    return Inventories.count(it, itemType.createStack());
                } else {
                    return Inventories.empty(it, itemType.createStack());
                }
            }).orElse(0);
    }

    @SneakyThrows
    private void writeMeta() {
        val buf = Unpooled.buffer();
        buf.writeShort(1).writeBoolean(unlimited);
        buf.writeShort(2);
        val stream = new ByteArrayOutputStream();
        DataFormats.NBT.writeTo(stream, itemType.toContainer());
        buf.writeInt(stream.size());
        buf.writeBytes(stream.toByteArray(), 0, stream.size());
        buf.writeShort(0);
        meta = new byte[buf.writerIndex()];
        buf.readBytes(meta, 0, meta.length);
    }

    @SneakyThrows
    private void readMeta() {
        val buf = Unpooled.wrappedBuffer(meta);
        @SuppressWarnings("UnusedAssignment")
        var id = 0;
        while ((id = buf.readShort()) != 0) {
            switch (id) {
                case 1:
                    unlimited = buf.readBoolean();
                    break;
                case 2:
                    val len = buf.readInt();
                    val container = DataFormats.NBT.readFrom(ByteStreams.limit(new InputStream() {
                        @Override
                        public int read() {
                            return buf.readUnsignedByte();
                        }
                    }, len));
                    itemType = Sponge.getDataManager().deserialize(ItemStackSnapshot.class, container).get();
                    break;
            }
        }
    }

    @SneakyThrows
    public void writeResultSet(PreparedStatement stmt) {
        stmt.setLong(1, createTime);
        stmt.setString(2, owner.toString());
        stmt.setString(3, world.toString());
        val chunk = Blocks.toLong(x >> 4, z >> 4);
        val pos = Blocks.toShort(x, y, z);
        stmt.setLong(4, chunk);
        stmt.setShort(5, pos);
        stmt.setDouble(6, price);
        writeMeta();
        stmt.setBlob(7, new ByteArrayInputStream(meta));
    }

    @SneakyThrows
    public static ShopRecord readResultSet(ResultSet rs) {
        val chunk = rs.getLong("chunk");
        val pos = rs.getShort("pos");
        val coord = Blocks.coord(chunk, pos);
        val rec = new ShopRecord(
            rs.getInt("id"),
            rs.getLong("create_time"),
            UUID.fromString(rs.getString("owner")),
            UUID.fromString(rs.getString("world")),
            coord[0],
            coord[1],
            coord[2],
            rs.getDouble("price"),
            ByteStreams.toByteArray(rs.getBlob("meta").getBinaryStream())
        );
        rec.readMeta();
        return rec;
    }

    public static ShopRecord of(Player player, Location<World> location, double price) {
        return new ShopRecord(-1,
            System.currentTimeMillis(),
            player.getUniqueId(),
            location.getExtent().getUniqueId(),
            location.getBlockX(),
            location.getBlockY(),
            location.getBlockZ(),
            price,
            new byte[0]
        );
    }

}
