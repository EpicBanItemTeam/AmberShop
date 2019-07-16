package io.izzel.ambershop.cmd;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.izzel.amber.commons.i18n.AmberLocale;
import io.izzel.ambershop.conf.AmberConfManager;
import lombok.val;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.args.ChildCommandElementExecutor;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.text.Text;

import static io.izzel.ambershop.cmd.Arguments.num;
import static io.izzel.ambershop.cmd.condition.Condition.id;
import static io.izzel.ambershop.cmd.condition.Condition.world;
import static org.spongepowered.api.command.args.GenericArguments.*;

@Singleton
public class AmberCommands {

    @Inject
    public AmberCommands(AmberLocale locale, AmberConfManager cm,
                         CreateShopExecutor cse, RemoveShopExecutor rse,
                         SetUnlimitedExecutor sue, SetOwnerExecutor soe,
                         SetPriceExecutor spe, QueryExecutor qe) {
        val executor = CommandSpec.builder();
        if (cm.get().shopSettings.enable) {
            val create = CommandSpec.builder()
                .permission("ambershop.user.create")
                .arguments(num("price"))
                .executor(cse)
                .build();
            executor.child(create, "create", "c");
            val remove = CommandSpec.builder()
                .executor(rse)
                .build();
            executor.child(remove, "remove", "r");
            val unlimited = CommandSpec.builder()
                .permission("ambershop.admin.unlimited")
                .arguments(bool(Text.of("unlimited")))
                .executor(sue)
                .build();
            executor.child(unlimited, "unlimited", "u");
            val setowner = CommandSpec.builder()
                .arguments(user(Text.of("owner")))
                .executor(soe)
                .build();
            executor.child(setowner, "setowner", "owner", "o");
            val price = CommandSpec.builder()
                .arguments(num("price"),
                    optional(string(Text.of("type"))))
                .executor(spe)
                .build();
            executor.child(price, "setprice", "price", "p");
            {
                val queryRemove = CommandSpec.builder()
                    .permission("ambershop.user.query.remove")
                    .arguments()
                    .executor(qe.remove)
                    .build();
                val querySet = CommandSpec.builder()
                    .permission("ambershop.user.query.set")
                    .arguments(
                        flags()
                            .valueFlag(requiringPermission(bool(Text.of("s_unlimited")), "ambershop.admin.unlimited"), "-unlimited", "u")
                            .valueFlag(requiringPermission(num("s_price"), "ambershop.user.setprice"), "-price", "p")
                            .valueFlag(requiringPermission(user(Text.of("s_owner")), "ambershop.user.setowner"), "-owner", "o")
                            .buildWith(none())
                    )
                    .executor(qe.set)
                    .build();
                val queryList = CommandSpec.builder()
                    .permission("ambershop.user.query.list")
                    .executor(qe.list)
                    .build();
                val queryChild = new ChildCommandElementExecutor(null, null, false);
                queryChild.register(queryRemove, "remove", "r");
                queryChild.register(querySet, "set", "s");
                queryChild.register(queryList, "list", "l");
                val query = CommandSpec.builder()
                    .permission("ambershop.user.query")
                    .arguments(
                        flags()
                            .valueFlag(requiringPermission(id("q_id"), "ambershop.user.query.id"), "-id", "i")
                            .valueFlag(requiringPermission(world("q_world"), "ambershop.user.query.world"), "-world", "w")
                            .buildWith(queryChild)
                    )
                    .executor(queryChild)
                    .build();
                executor.child(query, "query", "q");
            }
            val reload = CommandSpec.builder()
                .permission("ambershop.admin.reload")
                .executor((src, args) -> {
                    try {
                        locale.reload();
                        cm.reload();
                        locale.to(src, "commands.reload.complete");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return CommandResult.success();
                })
                .build();
            executor.child(reload, "reload");
        }
        root = executor.build();
    }

    private CommandSpec root;

    public CommandSpec root() {
        return root;
    }

}
