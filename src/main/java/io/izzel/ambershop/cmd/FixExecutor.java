package io.izzel.ambershop.cmd;

import com.google.inject.Inject;
import io.izzel.amber.commons.i18n.AmberLocale;
import io.izzel.ambershop.data.ShopDataSource;
import io.izzel.ambershop.util.AmberTasks;
import lombok.val;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;

public class FixExecutor implements CommandExecutor {

    @Inject private AmberLocale locale;
    @Inject private AmberTasks tasks;
    @Inject private ShopDataSource ds;

    @Override
    public CommandResult execute(CommandSource src, CommandContext args) {
        tasks.async().submit(() -> {
            val result = ds.fixAll().get();
            locale.to(src, result.getPath(), result.getArgs());
            return null;
        });
        return CommandResult.success();
    }

}
