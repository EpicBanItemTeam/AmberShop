package io.izzel.ambershop.listener;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import com.google.common.reflect.TypeToken;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.izzel.amber.commons.i18n.AmberLocale;
import io.izzel.amber.commons.i18n.args.Arg;
import io.izzel.ambershop.AmberShop;
import io.izzel.ambershop.conf.AmberConfManager;
import io.izzel.ambershop.data.ShopDataSource;
import io.izzel.ambershop.data.ShopRecord;
import io.izzel.ambershop.unsafe.AmberDisplay;
import io.izzel.ambershop.util.AmberTasks;
import io.izzel.ambershop.util.Blocks;
import lombok.SneakyThrows;
import lombok.val;
import lombok.var;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.block.tileentity.carrier.Chest;
import org.spongepowered.api.block.tileentity.carrier.TileEntityCarrier;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.type.HandTypes;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.SpongeEventFactory;
import org.spongepowered.api.event.block.InteractBlockEvent;
import org.spongepowered.api.event.filter.IsCancelled;
import org.spongepowered.api.event.filter.cause.First;
import org.spongepowered.api.event.filter.type.Include;
import org.spongepowered.api.event.item.inventory.InteractInventoryEvent;
import org.spongepowered.api.event.network.ClientConnectionEvent;
import org.spongepowered.api.item.inventory.type.CarriedInventory;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.TranslatableText;
import org.spongepowered.api.util.Direction;
import org.spongepowered.api.util.Tristate;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

@Singleton
public class DisplayListener {

    @Inject private AmberConfManager conf;
    @Inject private AmberLocale locale;
    @Inject private AmberTasks tasks;
    @Inject private ShopDataSource ds;
    @Inject private AmberShop instance;
    @Inject private AmberDisplay display;
    private ShopTradeListener trade; // No Guice when AmberShop#<init>

    public ShopTradeListener trade() {
        return trade == null ? trade = AmberShop.INJECTOR.getInstance(ShopTradeListener.class) : trade;
    }

    @Listener
    public void onJoin(ClientConnectionEvent.Join event) {
        val task = new PlayerChunkChangeTask(event.getTargetEntity().getUniqueId(), event.getTargetEntity().getWorld().getUniqueId());
        map.put(event.getTargetEntity().getUniqueId(), task);
        Task.builder().execute(task).delayTicks(20).intervalTicks(40).submit(instance);
    }

    @Include(InteractInventoryEvent.Close.class)
    @Listener
    public void onChestClose(InteractInventoryEvent event) {
        if (event.getTargetInventory() instanceof CarriedInventory) {
            val carrier = ((CarriedInventory) event.getTargetInventory()).getCarrier();
            if (carrier.isPresent() && carrier.get() instanceof Chest) {
                val chest = ((Chest) carrier.get());
                val loc = chest.getLocation();
                ds.getByLocation(loc).ifPresent(this::addBlockChange);
            }
        }
    }

    @IsCancelled(Tristate.UNDEFINED)
    @Include({InteractBlockEvent.Primary.class, InteractBlockEvent.Secondary.class})
    @Listener(order = Order.LAST, beforeModifications = true)
    public void onSignTradeAndUpdate(InteractBlockEvent event, @First Player player) {
        if (conf.get().shopSettings.displaySign) {
            if (event.getTargetBlock().getState().getType() == BlockTypes.AIR) { // generated signs
                val loc = event.getTargetBlock().getLocation();
                if (!loc.isPresent()) return;
                val opt = display.getSign(player, loc.get());
                if (opt.isPresent()) {
                    val direction = opt.get();
                    val chestLoc = loc.get().sub(direction.asBlockOffset());
                    ds.getByLocation(chestLoc).ifPresent(rec -> {
                        if (event instanceof InteractBlockEvent.Secondary) { // right click for updating sign info
                            event.setCancelled(true);
                            Optional.ofNullable(map.get(player.getUniqueId())).ifPresent(task -> {
                                val lines = task.makeSignLines(rec);
                                tasks.sync().submit(() -> display.sendSign(player, loc.get(), direction, lines));
                            });
                        } else if (event instanceof InteractBlockEvent.Primary && !event.isCancelled()) { // left click for trading
                            val newEvent = SpongeEventFactory.createInteractBlockEventPrimaryMainHand(event.getCause().with(this),
                                HandTypes.MAIN_HAND, Optional.empty(), chestLoc.getBlock().snapshotFor(chestLoc), direction);
                            trade().onTrade(newEvent, player);
                        }
                    });
                }
            }
        }
    }

    public void addBlockChange(ShopRecord record) {
        for (PlayerChunkChangeTask task : map.values()) {
            task.set.add(record);
        }
    }

    public void reset(Location<World> location, Direction direction) {
        for (PlayerChunkChangeTask task : map.values()) {
            task.resetDisplay(location, direction);
        }
    }

    private final Map<UUID, PlayerChunkChangeTask> map = new ConcurrentHashMap<>();

    private class PlayerChunkChangeTask implements Consumer<Task> {

        private final UUID playerUid;

        private UUID lastWorld;
        private long lastChunk;

        private final Set<ShopRecord> set = Sets.newConcurrentHashSet();

        private PlayerChunkChangeTask(UUID uuid, UUID world) {
            playerUid = uuid;
            lastWorld = world;
            lastChunk = Blocks.toLong(Integer.MAX_VALUE, Integer.MIN_VALUE); // as player 'usually' cant be at such a coordinate
        }

        @SneakyThrows
        @Override
        public void accept(Task task) {
            val player = Sponge.getServer().getPlayer(playerUid);
            if (!player.isPresent()) { // the player log out
                map.remove(playerUid);
                task.cancel();
                return;
            }
            val loc = player.get().getLocation();
            val lx = ((int) (lastChunk >>> 32));
            val lz = (int) (lastChunk & 0xffffffffL);
            val ncx = loc.getBlockX() >> 4;
            val ncz = loc.getBlockZ() >> 4;
            if (!lastWorld.equals(loc.getExtent().getUniqueId()))
                lastChunk = Blocks.toLong(Integer.MAX_VALUE, Integer.MIN_VALUE); // reset location
            if (lastChunk != Blocks.toLong(ncx, ncz)) {
                for (var x = -1; x <= 1; ++x) {
                    for (var z = -1; z <= 1; ++z) { // visit neighbors
                        val cx = x + ncx;
                        val cz = z + ncz;
                        if (distance(cx, cz, lx, lz) > 1) { // chunks not displayed
                            val records = ds.getByChunk(loc.getExtent().getUniqueId(), cx, cz).get();
                            for (ShopRecord record : records) {
                                display(record, player.get());
                            }
                        }
                    }
                }
            }
            val iterator = set.iterator();
            while (iterator.hasNext()) {
                display(iterator.next(), player.get());
                iterator.remove();
            }
            lastChunk = Blocks.toLong(ncx, ncz);
            lastWorld = loc.getExtent().getUniqueId();
        }

        @SneakyThrows
        private void display(ShopRecord record, Player player) {
            val location = record.getLocation();
            if (!location.getExtent().getUniqueId().equals(player.getWorld().getUniqueId())) return;
            if (!location.getTileEntity().filter(TileEntityCarrier.class::isInstance).isPresent()) return; // #10
            val block = location.getBlock();
            if (conf.get().shopSettings.displayItem) {
                val itemLoc = location.add(0.5, 1.2, 0.5);
                display.sendDroppedItem(player, itemLoc, record.getItemType().createStack());
            }
            if (conf.get().shopSettings.displaySign) {
                val direction = block.get(Keys.DIRECTION);
                if (!direction.isPresent()) return;
                val sign = location.add(direction.get().asBlockOffset());
                if (sign.getBlockType() != BlockTypes.AIR) return;
                display.sendSign(player, sign, direction.get(), makeSignLines(record));
            }
        }

        private List<Text> makeSignLines(ShopRecord record) {
            return locale.getAs("trade.display-sign", ImmutableList.of(), new TypeToken<List<Text>>() {},
                Arg.user(record.owner),
                Math.abs(record.price),
                record.isUnlimited() ? Arg.ref("trade.types.unlimited") : record.getStock(),
                record.getItemType().get(Keys.DISPLAY_NAME)
                    .orElseGet(() -> TranslatableText.builder(record.getItemType().getTranslation()).build()),
                Arg.ref(record.price < 0 ? "trade.types.sell" : "trade.types.buy"));
        }

        private void resetDisplay(Location<World> location, Direction direction) {
            if (conf.get().shopSettings.displaySign) {
                val sign = location.add(direction.asBlockOffset());
                Sponge.getServer().getPlayer(playerUid).filter(it -> it.getWorld().getUniqueId().equals(location.getExtent().getUniqueId()))
                    .ifPresent(player -> display.resetSign(player, sign));
            }
            if (conf.get().shopSettings.displayItem) {
                val itemLoc = location.add(0.5, 1.2, 0.5);
                Sponge.getServer().getPlayer(playerUid).filter(it -> it.getWorld().getUniqueId().equals(location.getExtent().getUniqueId()))
                    .ifPresent(player -> display.resetDroppedItem(player, itemLoc));
            }
        }

        private int distance(int ax, int ay, int bx, int by) {
            return Math.max(Math.abs(ax - bx), Math.abs(ay - by));
        }

    }

}
