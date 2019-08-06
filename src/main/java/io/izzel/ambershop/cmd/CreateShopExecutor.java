package io.izzel.ambershop.cmd;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.izzel.amber.commons.i18n.AmberLocale;
import io.izzel.ambershop.conf.AmberConfManager;
import io.izzel.ambershop.data.ShopDataSource;
import io.izzel.ambershop.data.ShopRecord;
import io.izzel.ambershop.module.EbiModule;
import io.izzel.ambershop.util.AmberTasks;
import io.izzel.ambershop.util.Blocks;
import io.izzel.ambershop.util.Util;
import lombok.val;
import org.spongepowered.api.block.tileentity.carrier.TileEntityCarrier;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.util.annotation.NonnullByDefault;

@Singleton
class CreateShopExecutor implements CommandExecutor {

    @Inject private AmberLocale locale;
    @Inject private AmberTasks tasks;
    @Inject private ShopDataSource ds;
    @Inject private AmberConfManager cm;
    @Inject private EbiModule ebiModule;

    @NonnullByDefault
    @Override
    public CommandResult execute(CommandSource src, CommandContext args) {
        val price = args.<Double>getOne("price");
        if (src instanceof Player) {
            val player = ((Player) src);
            val opt = Blocks.playerOnCursor(player);
            if (opt.isPresent()) {
                val te = opt.get().getTileEntity().filter(TileEntityCarrier.class::isInstance);
                if (te.isPresent()) {
                    val tec = (TileEntityCarrier) te.get();
                    val item = tec.getInventory().peek();
                    if (item.isPresent()) {
                        if (ebiModule.checkCreate(item.get(), player.getWorld(), player)) {
                            tasks.async().submit(() -> {
                                val created = ds.getByPlayer(player).get().size();
                                val max = player.getOption("ambershop.max-shop").flatMap(Util::asInteger)
                                    .orElse(cm.get().shopSettings.maxShops);
                                if (max == -1 || max > created) {
                                    try {
                                        val osr = ds.getByLocation(opt.get());
                                        if (osr.isPresent()) {
                                            locale.to(player, "commands.create.fail.exist-shop");
                                        } else {
                                            val sr = ShopRecord.of(player, opt.get(), price.orElse(1D));
                                            sr.setItemType(item.get());
                                            val result = ds.addRecord(sr).get();
                                            locale.to(player, result.getPath(), result.getArgs());
                                        }
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                } else locale.to(player, "trade.limit-exceeded");
                                return null;
                            });
                        } else locale.to(player, "commands.create.fail.blacklist.create");
                    } else locale.to(player, "commands.create.fail.no-item");
                } else locale.to(player, "commands.create.fail.not-chest");
            } else locale.to(player, "commands.create.fail.no-block");
        } else locale.to(src, "commands.create.fail.player-only");
        return CommandResult.success();
    }

}
