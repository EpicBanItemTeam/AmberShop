package io.izzel.ambershop.listener;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.izzel.ambershop.conf.AmberConfManager;
import io.izzel.ambershop.conf.AmberLocale;
import io.izzel.ambershop.data.ShopDataSource;
import io.izzel.ambershop.util.AmberTasks;
import lombok.val;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.block.ChangeBlockEvent;
import org.spongepowered.api.event.filter.type.Include;

@Singleton
public class ShopRemoveListener {

    @Inject private AmberLocale locale;
    @Inject private ShopDataSource dataSource;
    @Inject private AmberConfManager conf;
    @Inject private AmberTasks tasks;
    @Inject private DisplayListener display;

    @Include({ChangeBlockEvent.Break.class, ChangeBlockEvent.Modify.class})
    @Listener(order = Order.LAST, beforeModifications = true)
    public void onBreak(ChangeBlockEvent event) {
        event.getTransactions().forEach(transaction -> {
            val origin = transaction.getOriginal();
            val direction = origin.get(Keys.DIRECTION);
            if (!direction.isPresent()) return;
            val pos = transaction.getOriginal().getLocation();
            if (pos.isPresent()) {
                val loc = pos.get();
                val rec = dataSource.getByLocation(loc);
                if (rec.isPresent()) {
                    val first = event.getCause().first(Player.class);
                    if (first.isPresent()) {
                        val player = first.get();
                        if (player.getUniqueId().equals(rec.get().owner) ? player.hasPermission("ambershop.user.remove")
                                : player.hasPermission("ambershop.admin.remove")) {
                            display.reset(loc, direction.get()); // reset sign
                            tasks.async().submit(() -> {
                                val result = dataSource.removeRecord(rec.get()).get();
                                player.sendMessage(result.reason());
                                return null;
                            });
                        } else {
                            if (conf.get().shopSettings.protectShops) {
                                event.setCancelled(true);
                                player.sendMessage(locale.getText("trade.protect"));
                            }
                        }
                    } else {
                        if (conf.get().shopSettings.protectShops) {
                            event.setCancelled(true);
                        } else {
                            display.reset(loc, direction.get());
                            dataSource.removeRecord(rec.get());
                        }
                    }
                }
            }
        });
    }

}
