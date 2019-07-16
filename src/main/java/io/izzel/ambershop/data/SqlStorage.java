package io.izzel.ambershop.data;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.izzel.amber.commons.i18n.AmberLocale;
import io.izzel.ambershop.conf.AmberConfManager;
import io.izzel.ambershop.util.AmberTasks;
import lombok.SneakyThrows;
import lombok.val;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.service.sql.SqlService;

import java.sql.Connection;
import java.util.List;
import java.util.concurrent.Future;

@Singleton
class SqlStorage implements Storage {

    private static final String CURRENT_VERSION = "1";

    @Inject private AmberConfManager cm;
    @Inject private AmberLocale locale;
    @Inject private AmberTasks tasks;

    private SqlService service;

    @Override
    public Future<List<ShopRecord>> allRecords() {
        return tasks.async().submit(() -> {
            val builder = ImmutableList.<ShopRecord>builder();
            try (val conn = connection();
                 val stmt = conn.prepareStatement("select * from ambershop_shops;");
                 val rs = stmt.executeQuery()) {
                while (rs.next()) {
                    builder.add(ShopRecord.readResultSet(rs));
                }
            }
            return builder.build();
        });
    }

    @SneakyThrows
    @Override
    public void init() {
        service = Sponge.getServiceManager().provide(SqlService.class).orElseThrow(Exception::new);
        ensureDbVersion();
    }

    @SneakyThrows
    private void ensureDbVersion() {
        try (val conn = connection()) {
            try (val stmt = conn.createStatement()) {
                stmt.addBatch("create table if not exists ambershop_info" +
                    "(" +
                    "    `version` varchar(16) primary key not null," +
                    "    `date`    bigint(19)              not null" +
                    ");");
                stmt.addBatch("insert into ambershop_info " +
                    "select * " +
                    "from (" +
                    "         select " + CURRENT_VERSION + ", " + System.currentTimeMillis() +
                    "     ) x " +
                    "where not exists(select * from ambershop_info);");
                stmt.addBatch("create table if not exists ambershop_shops" +
                    "(" +
                    "    `id`          int auto_increment primary key not null," +
                    "    `create_time` bigint                         not null," +
                    "    `owner`       varchar(36)                    not null," +
                    "    `world`       varchar(36)                    not null," +
                    "    `chunk`       bigint                         not null," +
                    "    `pos`         smallint                       not null," +
                    "    `price`       double                         not null," +
                    "    `meta`        blob" +
                    ");");
                stmt.executeBatch();
            }
            try (val stmt = conn.prepareStatement("select * from ambershop_info;")) {
                val set = stmt.executeQuery();
                if (set.next()) {
                    val version = set.getString("version");
                    if (!version.equals(CURRENT_VERSION)) updateDb();
                    else locale.log("db-connect");
                }
            }
        }
    }

    private void updateDb() {

    }

    @Override
    @SneakyThrows
    public Connection connection() {
        return service.getDataSource(cm.get().jdbcUrl).getConnection();
    }

}
