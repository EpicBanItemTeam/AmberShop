package io.izzel.ambershop.data;

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.Futures;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import gnu.trove.map.hash.TLongObjectHashMap;
import gnu.trove.map.hash.TShortObjectHashMap;
import io.izzel.ambershop.conf.AmberLocale;
import io.izzel.ambershop.listener.DisplayListener;
import io.izzel.ambershop.util.AmberTasks;
import io.izzel.ambershop.util.Blocks;
import lombok.Cleanup;
import lombok.SneakyThrows;
import lombok.val;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.sql.Statement;
import java.util.*;
import java.util.concurrent.Future;

@Singleton
class ShopDataSourceImpl implements ShopDataSource {

    @Inject private AmberTasks tasks;
    @Inject private Storage storage;
    @Inject private AmberLocale locale;
    @Inject private DisplayListener signDisplay;
    // EcCol 用不起，shadow 进来太大了

    private Map<UUID, TLongObjectHashMap<TShortObjectHashMap<ShopRecord>>> shops = new HashMap<>();

    private TShortObjectHashMap<ShopRecord> chunk(UUID world, int x, int z) {
        if (!shops.containsKey(world)) shops.put(world, new TLongObjectHashMap<>());
        val map = shops.get(world);
        val chunk = Blocks.toLong(x, z);
        if (!map.containsKey(chunk)) map.put(chunk, new TShortObjectHashMap<>());
        return map.get(chunk);
    }

    @SneakyThrows
    @Override
    public void init() {
        storage.init();
        locale.info("shop-loaded");
    }

    @Override
    public Future<OperateResult> removeRecord(ShopRecord record) {
        return tasks.async().submit(() -> {
            try (val conn = storage.connection();
                 val stmt = conn.prepareStatement("delete from ambershop_shops where id = ?;")) {

                stmt.setInt(1, record.id);
                if (stmt.executeUpdate() == 0) {
                    return new ResultFail(locale.getText("shop-delete-nothing"));
                }

                tasks.sync().submit(() -> {
                    chunk(record.world, record.x >> 4, record.z >> 4)
                            .remove(Blocks.toShort(record.x, record.y, record.z));
                });

                return new ResultSuccess(locale.getText("shop-deleted"));
            } catch (Exception e) {
                return new ResultFail(locale.getText("sql-error", e));
            }
        });
    }

    @Override
    public Future<OperateResult> updateRecord(ShopRecord newRec) {
        return tasks.async().submit(() -> {
            try (val conn = storage.connection();
                 val stmt = conn.prepareStatement(
                         "update ambershop_shops " +
                                 "set create_time = ?," +
                                 "    owner       = ?," +
                                 "    world       = ?," +
                                 "    chunk       = ?," +
                                 "    pos         = ?," +
                                 "    price       = ?," +
                                 "    meta        = ?" +
                                 "where id = ?;")) {
                newRec.writeResultSet(stmt);
                stmt.setInt(8, newRec.id);
                if (stmt.executeUpdate() == 0) {
                    return new ResultFail(locale.getText("shop-update-nothing"));
                }

                tasks.sync().submit(() -> {
                    chunk(newRec.world, newRec.x >> 4, newRec.z >> 4)
                            .put(Blocks.toShort(newRec.x, newRec.y, newRec.z), newRec);
                    signDisplay.addBlockChange(newRec);
                });

                return new ResultSuccess(locale.getText("shop-updated"));
            } catch (Exception e) {
                return new ResultFail(locale.getText("sql-error", e));
            }
        });
    }

    @Override
    public Future<OperateResult> moveLocation(ShopRecord old, Location<World> dest) {
        chunk(old.world, old.x >> 4, old.z >> 4).remove(Blocks.toShort(old.x, old.y, old.z));
        old.x = dest.getBlockX();
        old.y = dest.getBlockY();
        old.z = dest.getBlockZ();
        old.world = dest.getExtent().getUniqueId();
        return updateRecord(old);
    }

    @Override
    public Future<List<ShopRecord>> fetchRecordBy(Map<String, String> map) {
        return Futures.immediateCheckedFuture(ImmutableList.of());
    }

    @SneakyThrows
    @Override
    public Optional<ShopRecord> getByLocation(Location<World> loc) {
        if (Sponge.getServer().isMainThread())
            return Optional.ofNullable(chunk(loc.getExtent().getUniqueId(), loc.getBlockX() >> 4, loc.getBlockZ() >> 4)
                    .get(Blocks.toShort(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ())));
        else
            return Optional.ofNullable(tasks.sync().submit(() -> chunk(loc.getExtent().getUniqueId(),
                    loc.getBlockX() >> 4, loc.getBlockZ() >> 4)
                    .get(Blocks.toShort(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ()))).get());
    }

    @Override
    public Future<List<ShopRecord>> getByUUID(UUID uuid) {
        return tasks.async().submit(() -> {
            @Cleanup val conn = storage.connection();
            @Cleanup val stmt = conn.prepareStatement("select * from ambershop_shops where owner = ?;");
            stmt.setString(1, uuid.toString());
            @Cleanup val rs = stmt.executeQuery();
            val builder = ImmutableList.<ShopRecord>builder();
            while (rs.next()) builder.add(ShopRecord.readResultSet(rs));
            return builder.build();
        });
    }

    @SneakyThrows
    @Override
    public Future<Optional<ShopRecord>> getById(int id) {
        return tasks.async().submit(() -> {
            @Cleanup val conn = storage.connection();
            @Cleanup val stmt = conn.prepareStatement("select * from ambershop_shops where id = ? limit 1;");
            stmt.setInt(1, id);
            @Cleanup val rs = stmt.executeQuery();
            if (rs.next()) return Optional.of(ShopRecord.readResultSet(rs));
            else return Optional.empty();
        });
    }

    @SneakyThrows
    @Override
    public Future<ShopRecord> addRecord(ShopRecord rec) {
        rec.id = -1;
        return tasks.async().submit(() -> {
            @Cleanup val conn = storage.connection();
            @Cleanup val stmt = conn.prepareStatement(
                    "insert into ambershop_shops (create_time, owner, world, chunk, pos, price, meta) " +
                            "values (?, ?, ?, ?, ?, ?, ?);",
                    Statement.RETURN_GENERATED_KEYS);
            rec.writeResultSet(stmt);
            stmt.executeUpdate();
            @Cleanup val rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                rec.id = rs.getInt(1);
                tasks.sync().execute(() -> {
                    chunk(rec.world, rec.x >> 4, rec.z >> 4).put(Blocks.toShort(rec.x, rec.y, rec.z), rec);
                    signDisplay.addBlockChange(rec);
                });
                return rec;
            } else return rec;
        });
    }

    @Override
    public Future<Collection<ShopRecord>> getByChunk(UUID world, int x, int z) {
        val chunk = Blocks.toLong(x, z);
        val worldMap = shops.get(world);
        if (worldMap != null && worldMap.containsKey(chunk))
            return Futures.immediateCheckedFuture(Collections.unmodifiableCollection(worldMap.get(chunk).valueCollection()));
        return tasks.async().submit(() -> {
            @Cleanup val conn = storage.connection();
            @Cleanup val stmt = conn.prepareStatement(
                    "select * from ambershop_shops where chunk = ?;",
                    Statement.RETURN_GENERATED_KEYS);
            stmt.setLong(1, chunk);
            @Cleanup val rs = stmt.executeQuery();
            val map = new TShortObjectHashMap<ShopRecord>();
            while (rs.next()) {
                val record = ShopRecord.readResultSet(rs);
                map.put(Blocks.toShort(record.x, record.y, record.z), record);
            }
            if (map.size() != 0) {
                tasks.sync().submit(() -> chunk(world, x, z).putAll(map));
                return Collections.unmodifiableCollection(map.valueCollection());
            } else return ImmutableList.of();
        });
    }

    @Override
    public Future<Void> unloadChunk(UUID world, int x, int z) {
        if (Sponge.getServer().isMainThread()) {
            val map = shops.get(world);
            if (map != null) map.remove(Blocks.toLong(x, z));
            return Futures.immediateCheckedFuture(null);
        }
        return tasks.sync().submit(() -> {
            shops.get(world).remove(Blocks.toLong(x, z));
            return null;
        });
    }

}
