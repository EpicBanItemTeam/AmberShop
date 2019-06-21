package io.izzel.ambershop.cmd;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.izzel.ambershop.conf.AmberLocale;
import io.izzel.ambershop.data.ShopDataSource;
import io.izzel.ambershop.data.ShopRecord;
import io.izzel.ambershop.util.AmberTasks;
import io.izzel.ambershop.util.Blocks;
import lombok.val;
import org.spongepowered.api.block.tileentity.carrier.TileEntityCarrier;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.util.annotation.NonnullByDefault;

import java.util.concurrent.TimeUnit;

@Singleton
class CreateShopExecutor implements CommandExecutor {

    @Inject private AmberLocale locale;
    @Inject private AmberTasks tasks;
    @Inject private ShopDataSource ds;

    @NonnullByDefault
    @Override
    public CommandResult execute(CommandSource src, CommandContext args) {
        val price = args.<Double>getOne("price").get();
        if (src instanceof Player) {
            val player = ((Player) src);
            val opt = Blocks.playerOnCursor(player);
            if (opt.isPresent()) {
                val te = opt.get().getTileEntity().filter(it -> it instanceof TileEntityCarrier);
                if (te.isPresent()) {
                    val tec = (TileEntityCarrier) te.get();
                    val item = tec.getInventory().peek();
                    if (item.isPresent()) {
                        tasks.async().execute(() -> {
                            try {
                                val osr = ds.getByLocation(opt.get());
                                if (osr.isPresent()) {
                                    player.sendMessage(locale.getText("commands.create.fail.exist-shop"));
                                } else {
                                    val sr = ShopRecord.of(player, opt.get(), price);
                                    sr.setItemType(item.get());
                                    val csr = ds.addRecord(sr).get();
                                    player.sendMessage(locale.getText("commands.create.success", csr.id));
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        });
                    } else player.sendMessage(locale.getText("commands.create.fail.no-itemName"));
                } else player.sendMessage(locale.getText("commands.create.fail.not-chest"));
            } else player.sendMessage(locale.getText("commands.create.fail.no-block"));
        } else src.sendMessage(locale.getText("commands.create.fail.player-only"));
        return CommandResult.success();
    }

}
