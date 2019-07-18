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
        return ds.fetchRecordBy(queryList).get().stream().filter(it -> tests.allMatch(condition -> condition.test(it))).collect(Collectors.toList());
    }

    @Inject RemoveExecutor remove;
    @Inject SetExecutor set;
    @Inject ListExecutor list;

    public static class RemoveExecutor implements CommandExecutor {

        @Override
        public CommandResult execute(CommandSource src, CommandContext args) {

            return CommandResult.success();
        }

    }

    public static class SetExecutor implements CommandExecutor {

        @Override
        public CommandResult execute(CommandSource src, CommandContext args) {
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
                        record.isUnlimited() ? Arg.ref("trade.types.unlimited") : "", record.x, record.y, record.z, record.getItemType()))
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
