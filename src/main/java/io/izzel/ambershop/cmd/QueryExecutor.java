package io.izzel.ambershop.cmd;

import com.google.common.collect.ImmutableList;
import com.google.common.reflect.TypeToken;
import com.google.inject.Inject;
import io.izzel.amber.commons.i18n.AmberLocale;
import io.izzel.amber.commons.i18n.args.Arg;
import io.izzel.ambershop.cmd.condition.Condition;
import io.izzel.ambershop.data.ShopDataSource;
import io.izzel.ambershop.data.ShopRecord;
import io.izzel.ambershop.util.AmberTasks;
import lombok.SneakyThrows;
import lombok.val;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.service.pagination.PaginationList;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.world.World;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class QueryExecutor {

    private static final List<String> queries = ImmutableList.of("q_id", "q_world", "q_price");

    @SneakyThrows
    private static List<ShopRecord> queryByArgs(CommandContext args, ShopDataSource ds) {
        val conditions = queries.stream()
            .filter(args::hasAny)
            .map(args::<Condition>getOne)
            .map(Optional::get)
            .collect(Collectors.toList());
        val queryList = conditions.stream().filter(Condition::query).map(Condition::get).collect(Collectors.toList());
        val tests = conditions.stream().filter(Condition::test);
        return ds.fetchRecordBy(queryList).get()
            .stream()
            .filter(it -> tests.allMatch(condition -> condition.test(it)))
            .collect(Collectors.toList());
    }

    @Inject RemoveExecutor remove;
    @Inject SetExecutor set;
    @Inject ListExecutor list;

    public static class RemoveExecutor implements CommandExecutor {

        @Inject private AmberTasks tasks;
        @Inject private ShopDataSource ds;
        @Inject private AmberLocale locale;

        @Override
        public CommandResult execute(CommandSource src, CommandContext args) {
            val copy = new CommandContext();
            copy.applySnapshot(args.createSnapshot());
            val uid = src instanceof Player ? ((Player) src).getUniqueId() : null;
            tasks.async().submit(() -> {
                val records = queryByArgs(copy, ds);
                val stream = records.stream().filter(it -> src.hasPermission(it.owner.equals(uid) ? "ambershop.user.remove" : "ambershop.admin.remove"));
                stream.map(ds::removeRecord).forEach(it -> { try { it.get(); } catch (Exception ignored) { } });
                locale.to(src, "commands.query.remove", stream.count());
                return null;
            });
            return CommandResult.success();
        }

    }

    public static class SetExecutor implements CommandExecutor {

        @Inject private AmberTasks tasks;
        @Inject private ShopDataSource ds;
        @Inject private AmberLocale locale;

        @Override
        public CommandResult execute(CommandSource src, CommandContext args) {
            val copy = new CommandContext();
            copy.applySnapshot(args.createSnapshot());
            val uid = src instanceof Player ? ((Player) src).getUniqueId() : null;
            val priceUser = src.hasPermission("ambershop.user.setprice");
            val priceAdmin = src.hasPermission("ambershop.admin.setprice");
            val ownerUser = src.hasPermission("ambershop.user.setowner");
            val ownerAdmin = src.hasPermission("ambershop.admin.setowner");
            val unlimited = src.hasPermission("ambershop.admin.unlimited");
            tasks.async().submit(() -> {
                val records = queryByArgs(copy, ds);
                records.forEach(it -> {
                    copy.<Double>getOne("s_price").filter(x -> priceUser && it.owner.equals(uid) || priceAdmin)
                        .ifPresent(it::setPrice);
                    copy.<User>getOne("s_owner").filter(x -> ownerUser && it.owner.equals(uid) || ownerAdmin)
                        .map(User::getUniqueId)
                        .ifPresent(it::setOwner);
                    copy.<Boolean>getOne("s_unlimited").filter(x -> unlimited)
                        .ifPresent(it::setUnlimited);
                });
                records.stream().map(ds::updateRecord)
                    .forEach(it -> { try { it.get(); } catch (Exception ignored) { } });
                locale.to(src, "commands.query.set", records.size());
                return null;
            });
            return CommandResult.success();
        }

    }

    public static class ListExecutor implements CommandExecutor {

        @Inject private AmberTasks tasks;
        @Inject private ShopDataSource ds;
        @Inject private AmberLocale locale;

        @Override
        public CommandResult execute(CommandSource src, CommandContext args) {
            val copy = new CommandContext();
            copy.applySnapshot(args.createSnapshot());
            tasks.async().submit(() -> {
                val result = queryByArgs(copy, ds).stream()
                    .map(record -> locale.getAs("trade.record-info", TypeToken.of(Text.class), record.id, Arg.user(record.owner), record.price,
                        Sponge.getServer().getWorld(record.world).map(World::getName).orElse(record.world.toString()),
                        Instant.ofEpochMilli(record.createTime).toString(),
                        record.isUnlimited() ? Arg.ref("trade.types.unlimited") : "", record.x, record.y, record.z, record.getItemType(),
                        Arg.ref(record.price < 0 ? "trade.types.sell" : "trade.types.buy")))
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .collect(Collectors.toList());
                val page = PaginationList.builder();
                page.contents(result);
                page.sendTo(src);
                return null;
            });
            return CommandResult.success();
        }

    }
}
