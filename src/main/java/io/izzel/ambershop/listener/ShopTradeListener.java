package io.izzel.ambershop.listener;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.izzel.ambershop.conf.AmberConfManager;
import io.izzel.ambershop.conf.AmberLocale;
import io.izzel.ambershop.data.ShopDataSource;
import io.izzel.ambershop.data.ShopRecord;
import io.izzel.ambershop.trade.Trades;
import io.izzel.ambershop.util.AmberTasks;
import io.izzel.ambershop.util.Util;
import lombok.val;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.type.HandTypes;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.gamemode.GameModes;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.block.InteractBlockEvent;
import org.spongepowered.api.event.filter.IsCancelled;
import org.spongepowered.api.event.filter.cause.First;
import org.spongepowered.api.event.filter.type.Include;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.service.user.UserStorageService;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.util.Tristate;

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
        if (block.getState().getType().equals(BlockTypes.CHEST) && ((!player.getItemInHand(HandTypes.MAIN_HAND).isPresent()) ||
                player.getItemInHand(HandTypes.MAIN_HAND).get().getType() == ItemTypes.AIR ||
                player.getItemInHand(HandTypes.MAIN_HAND).get().getType() == ItemTypes.NONE)
        ) {
            val location = block.getLocation();
            if (!location.isPresent()) return;
            ds.getByLocation(location.get()).ifPresent(rec -> tasks.async().submit(() -> {
                locale.shopInfo(rec).forEach(player::sendMessage);
                if (!player.getUniqueId().equals(rec.owner)) {
                    player.sendMessage(locale.getText(rec.price < 0 ? "trade.input-sell" : "trade.input-buy"));
                    val opt = tasks.inputChat(player, cm.get().shopSettings.inputExpireTime, TimeUnit.SECONDS,
                            Util::isInteger, p -> p.sendMessage(locale.getText("trade.format-err"))).get()
                            .flatMap(Util::asInteger);
                    if (!opt.isPresent()) {
                        player.sendMessage(locale.getText("trade.expire"));
                    } else {
                        val num = opt.get();
                        if (num <= 0) {
                            player.sendMessage(locale.getText("trade.non-negative"));
                            return null;
                        }
                        val text = Trades.playerShopTrade(player, rec, num, rec.price < 0).performTransaction().text;
                        player.sendMessage(locale.getText(text));
                        display.addBlockChange(rec);
                    }
                } else {
                    player.sendMessage(manage(rec));
                }
                return null;
            }));
        }
    }

    private Text manage(ShopRecord record) {
        val builder = Text.builder();
        builder.append(Text.builder().append(locale.getText("trade.manage.remove")).onClick(TextActions.executeCallback(cs -> {
            if (cs.hasPermission("ambershop.user.remove")) {
                record.getLocation().getBlock().get(Keys.DIRECTION).ifPresent(direction -> display.reset(record.getLocation(), direction));
                tasks.async().submit(() -> {
                    cs.sendMessage(ds.removeRecord(record).get().reason());
                    return null;
                });
            }
        })).build());
        builder.append(Text.of(" "));
        builder.append(Text.builder().append(locale.getText("trade.manage.setprice")).onClick(TextActions.executeCallback(cs -> {
            if (cs instanceof Player && cs.hasPermission("ambershop.user.setprice")) tasks.async().submit(() -> {
                cs.sendMessage(locale.getText("trade.manage.setprice-input"));
                val input = tasks.inputNumber(((Player) cs), cm.get().shopSettings.inputExpireTime, TimeUnit.SECONDS).get();
                if (input.isPresent()) {
                    val num = input.get();
                    record.setPrice(num);
                    cs.sendMessage(ds.updateRecord(record).get().reason());
                }
                return null;
            });
        })).build());
        builder.append(Text.of(" "));
        builder.append(Text.builder().append(locale.getText("trade.manage.setowner")).onClick(TextActions.executeCallback(cs -> {
            if (cs instanceof Player && cs.hasPermission("ambershop.user.setowner")) tasks.async().submit(() -> {
                cs.sendMessage(locale.getText("trade.manage.setowner-input"));
                val input = tasks.input(((Player) cs), cm.get().shopSettings.inputExpireTime, TimeUnit.SECONDS,
                        s -> {
                            val user = Sponge.getServiceManager().provideUnchecked(UserStorageService.class).get(s);
                            return user.isPresent() && user.get().hasPermission("ambershop.user.create");
                        }, p -> p.sendMessage(locale.getText("commands.setowner.fail.no-perm")),
                        s -> s.flatMap(name -> Sponge.getServiceManager().provideUnchecked(UserStorageService.class).get(name))).get();
                if (input.isPresent()) {
                    val user = input.get();
                    if (user.hasPermission("ambershop.user.create")) {
                        record.setOwner(user.getUniqueId());
                        cs.sendMessage(ds.updateRecord(record).get().reason());
                    } else cs.sendMessage(locale.getText("commands.setowner.fail.no-perm"));
                }
                return null;
            });
        })).build());
        builder.append(Text.of(" "));
        builder.append(Text.builder().append(locale.getText("trade.manage.setunlimited")).onClick(TextActions.executeCallback(cs -> {
            if (cs instanceof Player && cs.hasPermission("ambershop.admin.unlimited")) tasks.async().submit(() -> {
                record.setUnlimited(!record.isUnlimited());
                cs.sendMessage(ds.updateRecord(record).get().reason());
                return null;
            });
        })).build());
        return builder.build();
    }

}
