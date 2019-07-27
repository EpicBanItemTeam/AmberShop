package io.izzel.ambershop.listener;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.izzel.amber.commons.i18n.AmberLocale;
import io.izzel.ambershop.conf.AmberConfManager;
import io.izzel.ambershop.data.ShopDataSource;
import io.izzel.ambershop.data.ShopRecord;
import io.izzel.ambershop.util.AmberTasks;
import io.izzel.ambershop.util.Util;
import lombok.val;
import org.spongepowered.api.block.tileentity.carrier.TileEntityCarrier;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.type.HandTypes;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.gamemode.GameModes;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.block.InteractBlockEvent;
import org.spongepowered.api.event.filter.cause.First;
import org.spongepowered.api.event.filter.type.Include;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.world.Location;

import java.util.concurrent.TimeUnit;

@Singleton
public class ShopCreateListener {

    @Inject private AmberLocale locale;
    @Inject private AmberTasks tasks;
    @Inject private AmberConfManager cm;
    @Inject private ShopDataSource ds;

    @Include(InteractBlockEvent.Primary.class)
    @Listener(order = Order.LAST)
    public void onCreate(InteractBlockEvent event, @First Player player) {
        if (player.gameMode().get().equals(GameModes.CREATIVE)) return; // as creative players should not create a shop
        if (cm.get().shopSettings.createOnlySneaking && !player.get(Keys.IS_SNEAKING).orElse(false)) return; // #5
        if (!player.hasPermission("ambershop.user.create")) return;
        val block = event.getTargetBlock();
        if (!block.getLocation().flatMap(Location::getTileEntity).filter(TileEntityCarrier.class::isInstance).isPresent()) // should be a chest
            return;
        val loc = block.getLocation().get();
        if (ds.getByLocation(loc).isPresent()) return;
        val opt = player.getItemInHand(HandTypes.MAIN_HAND);
        if (!opt.isPresent() || opt.get().getType() == ItemTypes.AIR || opt.get().getType() == ItemTypes.NONE)
            return;  // should be something in hand
        val item = opt.get().copy();
        item.setQuantity(1);
        tasks.async().submit(() -> {
            val created = ds.getByPlayer(player).get().size();
            val max = player.getOption("ambershop.max-shop").flatMap(Util::asInteger).orElse(cm.get().shopSettings.maxShops);
            if (max == -1 || max > created) {
                locale.to(player, "trade.input-price", item);
                val input = tasks.inputNumber(player, cm.get().shopSettings.inputExpireTime, TimeUnit.SECONDS).get();
                input.ifPresent(price -> tasks.sync().submit(() -> {
                    if (loc.getTileEntity().filter(TileEntityCarrier.class::isInstance).isPresent())
                        tasks.async().submit(() -> {
                            val record = ShopRecord.of(player, loc, price);
                            record.setItemType(item);
                            val csr = ds.addRecord(record).get();
                            locale.to(player, "commands.create.success", csr.id);
                            return null;
                        });
                }));
            } else locale.to(player, "trade.limit-exceeded");
            return null;
        });
    }

}
