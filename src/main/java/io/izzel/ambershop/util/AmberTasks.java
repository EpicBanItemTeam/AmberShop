package io.izzel.ambershop.util;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.izzel.amber.commons.i18n.AmberLocale;
import io.izzel.ambershop.AmberShop;
import io.izzel.ambershop.listener.OneTimeChatListener;
import lombok.SneakyThrows;
import lombok.val;
import org.apache.commons.lang3.tuple.Pair;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.message.MessageChannelEvent;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

@Singleton
public class AmberTasks {

    @Inject private AmberShop inst;
    @Inject private AmberLocale locale;

    private ScheduledExecutorService async, sync;

    public void init() {
        async = Executors.newScheduledThreadPool(16);
        sync = Sponge.getScheduler().createSyncExecutor(inst);
    }

    @SneakyThrows
    public void shutdown() {
        async.shutdown();
        sync.shutdown();
    }

    public ScheduledExecutorService async() {
        return async;
    }

    public ScheduledExecutorService sync() {
        return sync;
    }


    public <V> Future<V> input(Player player, long timeout, TimeUnit unit, Function<Optional<String>, ? extends V> mapper) {
        return Util.mapFuture(inputChat(player, timeout, unit, s -> true, p -> {}, p -> locale.to(p, "trade.expire")), mapper);
    }

    public <V> Future<V> input(Player player, long timeout, TimeUnit unit, Predicate<String> predicate, Consumer<Player> consumer, Function<Optional<String>, ? extends V> mapper) {
        return Util.mapFuture(inputChat(player, timeout, unit, predicate, consumer, p -> locale.to(p, "trade.expire")), mapper);
    }

    public <V> Future<V> input(Player player, long timeout, TimeUnit unit, Predicate<String> predicate, Consumer<Player> consumer, Consumer<Player> onTimeout, Function<Optional<String>, ? extends V> mapper) {
        return Util.mapFuture(inputChat(player, timeout, unit, predicate, consumer, onTimeout), mapper);
    }

    public Future<Optional<Double>> inputNumber(Player player, long timeout, TimeUnit unit) {
        return inputNumber(player, timeout, unit, p -> locale.to(p, "trade.expire"));
    }

    public Future<Optional<Double>> inputNumber(Player player, long timeout, TimeUnit unit, Consumer<Player> onTimeout) {
        return Util.mapFuture(inputChat(player, timeout, unit,
            Util::isDouble, p -> locale.to(p, "trade.format-err"), onTimeout), it -> it.flatMap(Util::asDouble));
    }

    public Future<Optional<String>> inputChat(Player player, long timeout, TimeUnit unit) {
        return inputChat(player, timeout, unit, s -> true, p -> {});
    }

    public Future<Optional<String>> inputChat(Player player, long timeout, TimeUnit unit, Predicate<String> predicate, Consumer<Player> consumer) {
        return inputChat(player, timeout, unit, predicate, consumer, p -> locale.to(p, "trade.expire"));
    }

    private Map<UUID, Pair<OneTimeChatListener, ProvidingFutureTask<String>>> chats = new ConcurrentHashMap<>();

    /**
     * @param predicate test if input is valid
     * @param consumer  callback when input do not pass the predicate
     * @param onTimeout callback when input is expired
     */
    public Future<Optional<String>> inputChat(Player player, long timeout, TimeUnit unit, Predicate<String> predicate, Consumer<Player> consumer, Consumer<Player> onTimeout) {
        val uid = player.getUniqueId();
        chats.computeIfPresent(uid, (key, pair) -> {
            Sponge.getEventManager().unregisterListeners(pair.getLeft());
            pair.getRight().cancel(true);
            return null;
        });
        val task = new ProvidingFutureTask<String>(timeout, unit);
        val listener = new OneTimeChatListener(task, player, predicate, consumer);
        chats.put(uid, Pair.of(listener, task));
        Sponge.getEventManager().registerListener(inst, MessageChannelEvent.Chat.class, Order.FIRST, listener);
        sync.schedule(() -> {
            if (task.isCancelled()) return;
            Sponge.getEventManager().unregisterListeners(listener);
            val p = Sponge.getServer().getPlayer(uid);
            if (!task.isDone() && p.isPresent()) onTimeout.accept(p.get());
            chats.remove(uid);
        }, timeout, unit);
        return task;
    }

}
