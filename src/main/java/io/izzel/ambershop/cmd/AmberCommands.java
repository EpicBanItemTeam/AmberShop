package io.izzel.ambershop.cmd;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.izzel.ambershop.conf.AmberConfManager;
import io.izzel.ambershop.conf.AmberLocale;
import lombok.val;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.text.Text;

@Singleton
public class AmberCommands {

    @Inject
    public AmberCommands(AmberLocale locale, AmberConfManager cm,
                         CreateShopExecutor cse, RemoveShopExecutor rse,
                         SetUnlimitedExecutor sue, SetOwnerExecutor soe,
                         SetPriceExecutor spe) {
        val executor = CommandSpec.builder();
        if (cm.get().shopSettings.enable) {
            val create = CommandSpec.builder()
                    .description(locale.getText("commands.create.desc"))
                    .extendedDescription(locale.getText("commands.create.extDesc"))
                    .permission("ambershop.user.create")
                    .arguments(GenericArguments.string(Text.of("price")))
                    .executor(cse)
                    .build();
            executor.child(create, "create", "c");
            val remove = CommandSpec.builder()
                    .description(locale.getText("commands.remove.desc"))
                    .extendedDescription(locale.getText("commands.remove.extDesc"))
                    .executor(rse)
                    .build();
            executor.child(remove, "remove", "r");
            val unlimited = CommandSpec.builder()
                    .description(locale.getText("commands.unlimited.desc"))
                    .extendedDescription(locale.getText("commands.unlimited.extDesc"))
                    .permission("ambershop.admin.unlimited")
                    .arguments(GenericArguments.bool(Text.of("unlimited")))
                    .executor(sue)
                    .build();
            executor.child(unlimited, "unlimited", "u");
            val setowner = CommandSpec.builder()
                    .description(locale.getText("commands.setowner.desc"))
                    .extendedDescription(locale.getText("commands.setowner.extDesc"))
                    .arguments(GenericArguments.user(Text.of("owner")))
                    .executor(soe)
                    .build();
            executor.child(setowner, "setowner", "owner", "o");
            val price = CommandSpec.builder()
                    .description(locale.getText("commands.setprice.desc"))
                    .extendedDescription(locale.getText("commands.setprice.extDesc"))
                    .arguments(GenericArguments.doubleNum(Text.of("price")),
                            GenericArguments.optional(GenericArguments.string(Text.of("type"))))
                    .executor(spe)
                    .build();
            executor.child(price, "setprice", "price", "p");
        }
        root = executor.build();
    }

    private CommandSpec root;

    public CommandSpec root() {
        return root;
    }

}
