package io.izzel.ambershop.cmd;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.izzel.ambershop.conf.AmberLocale;
import io.izzel.ambershop.data.ShopDataSource;
import io.izzel.ambershop.listener.DisplayListener;
import io.izzel.ambershop.util.AmberTasks;
import io.izzel.ambershop.util.Blocks;
import lombok.val;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.util.annotation.NonnullByDefault;

@Singleton
public class RemoveShopExecutor implements CommandExecutor {

    @Inject private AmberLocale locale;
    @Inject private AmberTasks tasks;
    @Inject private ShopDataSource ds;
    @Inject private DisplayListener display;

    @NonnullByDefault
    @Override
    public CommandResult execute(CommandSource src, CommandContext args) {
        if (src instanceof Player) {
            val player = ((Player) src);
            val loc = Blocks.playerOnCursor(player);
            val opt = loc.flatMap(ds::getByLocation);
            if (opt.isPresent()) {
                val rec = opt.get();
                if (player.getUniqueId().equals(rec.owner) ? player.hasPermission("ambershop.user.remove") :
                        player.hasPermission("ambershop.admin.remove")) {
                    display.reset(loc.get(), loc.get().getBlock().get(Keys.DIRECTION).get());
                    tasks.async().submit(() -> {
                        player.sendMessage(ds.removeRecord(rec).get().reason());
                        return null;
                    });
                } else player.sendMessage(locale.getText("commands.remove.fail.no-perm"));
            } else player.sendMessage(locale.getText("commands.remove.fail.no-shop"));
        } else src.sendMessage(locale.getText("commands.remove.fail.player-only"));
        return CommandResult.success();
    }
}
