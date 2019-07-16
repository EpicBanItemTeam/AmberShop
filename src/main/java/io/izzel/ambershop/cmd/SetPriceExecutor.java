package io.izzel.ambershop.cmd;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.izzel.amber.commons.i18n.AmberLocale;
import io.izzel.ambershop.data.ShopDataSource;
import io.izzel.ambershop.util.AmberTasks;
import io.izzel.ambershop.util.Blocks;
import lombok.val;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.util.annotation.NonnullByDefault;

@Singleton
public class SetPriceExecutor implements CommandExecutor {

    @Inject private AmberLocale locale;
    @Inject private AmberTasks tasks;
    @Inject private ShopDataSource ds;

    @NonnullByDefault
    @Override
    public CommandResult execute(CommandSource src, CommandContext args) {
        val type = args.<String>getOne(Text.of("type")).map(it -> it.equals("buy") ? -1d : 1d).orElse(1d);
        val price = args.<Double>getOne(Text.of("price")).orElse(0d) * type;
        if (src instanceof Player) {
            val player = ((Player) src);
            val opt = Blocks.playerOnCursor(player).flatMap(ds::getByLocation);
            if (opt.isPresent()) {
                val rec = opt.get();
                if (rec.owner.equals(player.getUniqueId()) ? player.hasPermission("ambershop.user.setprice")
                    : player.hasPermission("ambershop.admin.setprice")) {
                    rec.setPrice(price);
                    tasks.async().submit(() -> {
                        val result = ds.updateRecord(rec).get();
                        locale.to(player, result.getPath(), result.getArgs());
                        return null;
                    });
                } else locale.to(player, "commands.setprice.fail.no-perm");
            } else locale.to(player, "commands.setprice.fail.no-shop");
        } else locale.to(src, "commands.setprice.fail.player-only");
        return CommandResult.success();
    }

}
