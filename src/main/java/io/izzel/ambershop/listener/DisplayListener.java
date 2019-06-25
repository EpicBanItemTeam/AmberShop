package io.izzel.ambershop.listener;

import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.izzel.ambershop.AmberShop;
import io.izzel.ambershop.conf.AmberConfManager;
import io.izzel.ambershop.conf.AmberLocale;
import io.izzel.ambershop.data.ShopDataSource;
import io.izzel.ambershop.data.ShopRecord;
import io.izzel.ambershop.mixin.AmberPlayer;
import io.izzel.ambershop.util.AmberTasks;
import io.izzel.ambershop.util.Blocks;
import lombok.SneakyThrows;
import lombok.val;
import lombok.var;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.block.tileentity.carrier.Chest;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.type.HandTypes;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.SpongeEventFactory;
import org.spongepowered.api.event.block.InteractBlockEvent;
import org.spongepowered.api.event.filter.cause.First;
import org.spongepowered.api.event.filter.type.Include;
import org.spongepowered.api.event.item.inventory.InteractInventoryEvent;
import org.spongepowered.api.event.network.ClientConnectionEvent;
import org.spongepowered.api.item.inventory.type.CarriedInventory;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.service.user.UserStorageService;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.serializer.TextSerializers;
import org.spongepowered.api.util.Direction;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Singleton
public class DisplayListener {

    private static final Pattern PATTERN = Pattern.compile("(.*?)(%shop_[^%]*%)|(.+)");

    @Inject private AmberConfManager conf;
    @Inject private AmberLocale locale;
    @Inject private AmberTasks tasks;
    @Inject private ShopDataSource ds;
    @Inject private AmberShop instance;
    @Inject private ShopTradeListener trade;

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

    @Include({InteractBlockEvent.Primary.class, InteractBlockEvent.Secondary.class})
    @Listener(order = Order.LAST, beforeModifications = true)
    public void onSignTradeAndUpdate(InteractBlockEvent event, @First Player player) {
        if (conf.get().shopSettings.displaySign) {
            if (event.getTargetBlock().getState().getType() == BlockTypes.AIR) { // generated signs
                val loc = event.getTargetBlock().getLocation();
                if (!loc.isPresent()) return;
                val opt = ((AmberPlayer) player).getSign(loc.get());
                if (opt.isPresent()) {
                    val direction = opt.get();
                    val chestLoc = loc.get().sub(direction.asBlockOffset());
                    ds.getByLocation(chestLoc).ifPresent(rec -> {
                        if (event instanceof InteractBlockEvent.Secondary) { // right click for updating sign info
                            event.setCancelled(true);
                            Optional.ofNullable(map.get(player.getUniqueId())).ifPresent(task -> {
                                val lines = task.makeSignLines(rec);
                                tasks.sync().submit(() -> ((AmberPlayer) player).sendSign(loc.get(), direction, lines));
                            });
                        } else if (event instanceof InteractBlockEvent.Primary) { // left click for trading
                            val newEvent = SpongeEventFactory.createInteractBlockEventPrimaryMainHand(event.getCause().with(this),
                                    HandTypes.MAIN_HAND, Optional.empty(), chestLoc.getBlock().snapshotFor(chestLoc), direction);
                            trade.onTrade(newEvent, player);
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
            if (!location.getTileEntity().filter(it -> it instanceof Chest).isPresent()) return;
            val block = location.getBlock();
            if (conf.get().shopSettings.displayItem) {
                val itemLoc = location.add(0.5, 1.2, 0.5);
                ((AmberPlayer) player).sendDroppedItem(itemLoc, record.getItemType().createStack());
            }
            if (conf.get().shopSettings.displaySign) {
                val direction = block.get(Keys.DIRECTION);
                if (!direction.isPresent()) return;
                val sign = location.add(direction.get().asBlockOffset());
                if (sign.getBlockType() != BlockTypes.AIR) return;
                ((AmberPlayer) player).sendSign(sign, direction.get(), makeSignLines(record));
            }
        }

        private List<Text> makeSignLines(ShopRecord record) {
            return conf.get().shopSettings.signInfo.stream().map(it -> {
                val ret = Text.builder();

                val matcher = PATTERN.matcher(it);
                while (matcher.find()) {
                    if (matcher.group(3) == null) {
                        val text = matcher.group(1);
                        ret.append(TextSerializers.FORMATTING_CODE.deserialize(text));
                        val placeholder = matcher.group(2);
                        switch (placeholder) {
                            case "%shop_item%":
                                val stack = record.getItemType().createStack();
                                ret.append(locale.itemName(stack));
                                break;
                            case "%shop_owner%":
                                ret.append(Text.of(Sponge.getServiceManager().provideUnchecked(UserStorageService.class)
                                        .get(record.owner).map(User::getName).orElse("")));
                                break;
                            case "%shop_price%":
                                ret.append(Text.of(String.valueOf(Math.abs(record.price))));
                                break;
                            case "%shop_type%":
                                ret.append(locale.getText(record.price < 0 ? "trade.type.sell" : "trade.type.buy"));
                                break;
                            case "%shop_stock%":
                                ret.append(Text.of(record.isUnlimited() ?
                                        locale.getText("trade.type.unlimited") : String.valueOf(record.getStock())));
                                break;
                            default:
                        }
                    } else {
                        val text = matcher.group(3);
                        ret.append(TextSerializers.FORMATTING_CODE.deserialize(text));
                    }
                }

                return ret.build();
            }).collect(Collectors.toList());
        }

        private void resetDisplay(Location<World> location, Direction direction) {
            if (conf.get().shopSettings.displaySign) {
                val sign = location.add(direction.asBlockOffset());
                Sponge.getServer().getPlayer(playerUid)
                        .ifPresent(player -> ((AmberPlayer) player).resetSign(sign));
            }
            if (conf.get().shopSettings.displayItem) {
                val itemLoc = location.add(0.5, 1.2, 0.5);
                Sponge.getServer().getPlayer(playerUid)
                        .ifPresent(player -> ((AmberPlayer) player).resetDroppedItem(itemLoc));
            }
        }

        private int distance(int ax, int ay, int bx, int by) {
            return Math.max(Math.abs(ax - bx), Math.abs(ay - by));
        }

    }

}
