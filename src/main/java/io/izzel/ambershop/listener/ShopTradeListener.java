package io.izzel.ambershop.listener;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.izzel.amber.commons.i18n.AmberLocale;
import io.izzel.amber.commons.i18n.args.Arg;
import io.izzel.ambershop.conf.AmberConfManager;
import io.izzel.ambershop.data.ShopDataSource;
import io.izzel.ambershop.data.ShopRecord;
import io.izzel.ambershop.trade.Trades;
import io.izzel.ambershop.util.AmberTasks;
import io.izzel.ambershop.util.Util;
import lombok.val;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.tileentity.carrier.TileEntityCarrier;
import org.spongepowered.api.data.type.HandTypes;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.gamemode.GameModes;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.block.InteractBlockEvent;
import org.spongepowered.api.event.filter.IsCancelled;
import org.spongepowered.api.event.filter.cause.First;
import org.spongepowered.api.event.filter.type.Include;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.util.Tristate;
import org.spongepowered.api.world.Location;

import java.util.concurrent.TimeUnit;

@Singleton
public class ShopTradeListener {

    @Inject private AmberLocale locale;
    @Inject private AmberTasks tasks;
    @Inject private AmberConfManager cm;
    @Inject private ShopDataSource ds;
    @Inject private DisplayListener display;

    @IsCancelled(Tristate.UNDEFINED)
    @Listener
    @Include(InteractBlockEvent.Primary.class)
    public void onTrade(InteractBlockEvent event, @First Player player) {
        if (player.gameMode().get().equals(GameModes.CREATIVE)) return; // as creative players should not buy or sell
        val block = event.getTargetBlock();
        val handEmpty = event.getCause().first(DisplayListener.class).isPresent(); // clicking sign do not need empty hand
        if (block.getLocation().flatMap(Location::getTileEntity).filter(TileEntityCarrier.class::isInstance).isPresent() // #10
            && (handEmpty || (!player.getItemInHand(HandTypes.MAIN_HAND).isPresent()) ||
            player.getItemInHand(HandTypes.MAIN_HAND).get().getType() == ItemTypes.AIR ||
            player.getItemInHand(HandTypes.MAIN_HAND).get().getType() == ItemTypes.NONE)
        ) {
            val location = block.getLocation();
            if (!location.isPresent()) return;
            ds.getByLocation(location.get()).ifPresent(rec -> tasks.async().submit(() -> {
                locale.to(player, "trade.shop-info", Arg.user(rec.owner), rec.getItemType(), rec.getStock(), Math.abs(rec.price),
                    Arg.ref(rec.price < 0 ? "trade.types.sell" : "trade.types.buy"));
                if (!player.getUniqueId().equals(rec.owner)) {
                    locale.to(player, rec.price < 0 ? "trade.input-sell" : "trade.input-buy");
                    val opt = tasks.inputChat(player, cm.get().shopSettings.inputExpireTime, TimeUnit.SECONDS,
                        Util::isInteger, p -> locale.to(p, "trade.format-err")).get()
                        .flatMap(Util::asInteger);
                    if (opt.isPresent()) {
                        val num = opt.get();
                        if (num > 0) {
                            val result = Trades.playerShopTrade(player, rec, num, rec.price < 0).performTransaction();
                            locale.to(player, result.getPath(), result.getArgs());
                            display.addBlockChange(rec);
                        } else locale.to(player, "trade.non-negative");
                    } else locale.to(player, "trade.expire");
                } else manage(player, rec);
                return null;
            }));
        }
    }

    private void manage(Player player, ShopRecord record) {
        locale.to(player, "trade.manage.info", record.id, -record.price, Arg.ref("trade.manage.button.price").withCallback(cs -> {
            if (cs.hasPermission("ambershop.user.setprice")) tasks.async().submit(() -> {
                locale.to(cs, "trade.manage.input.price");
                tasks.inputNumber(((Player) cs), cm.get().shopSettings.inputExpireTime, TimeUnit.SECONDS).get()
                    .ifPresent(price -> tasks.sync().execute(() -> Sponge.getCommandManager()
                        .process(cs, String.format("ambershop query -i %s s --p=%s", record.id, price))));
                return null;
            });
        }), Arg.ref("trade.manage.button.owner").withCallback(cs -> {
            if (cs.hasPermission("ambershop.user.setowner")) tasks.async().submit(() -> {
                locale.to(cs, "trade.manage.input.owner");
                tasks.inputChat(((Player) cs), cm.get().shopSettings.inputExpireTime, TimeUnit.SECONDS).get()
                    .ifPresent(s -> tasks.sync().execute(() -> Sponge.getCommandManager()
                        .process(cs, String.format("ambershop query -i %s s --o=%s", record.id, s))));
                return null;
            });
        }), player.hasPermission("ambershop.admin.unlimited") ? Arg.ref("trade.manage.button.unlimited") : "", !record.isUnlimited());
    }

}
