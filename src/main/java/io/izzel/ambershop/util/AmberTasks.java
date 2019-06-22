package io.izzel.ambershop.util;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.izzel.ambershop.AmberShop;
import io.izzel.ambershop.conf.AmberLocale;
import io.izzel.ambershop.listener.OneTimeChatListener;
import lombok.SneakyThrows;
import lombok.val;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.message.MessageChannelEvent;
import org.spongepowered.api.scheduler.SpongeExecutorService;

import java.util.Optional;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

@Singleton
public class AmberTasks {

    @Inject private AmberShop inst;
    @Inject private AmberLocale locale;

    private SpongeExecutorService async, sync;

    public void init() {
        async = Sponge.getScheduler().createAsyncExecutor(inst);
        sync = Sponge.getScheduler().createSyncExecutor(inst);
    }

    @SneakyThrows
    public void shutdown() {
        async.shutdown();
        sync.shutdown();
    }

    public SpongeExecutorService async() {
        return async;
    }

    public SpongeExecutorService sync() {
        return sync;
    }


    public <V> Future<V> input(Player player, long timeout, TimeUnit unit, Function<Optional<String>, ? extends V> mapper) {
        return Util.mapFuture(inputChat(player, timeout, unit, s -> true, p -> {}, p -> p.sendMessage(locale.getText("trade.expire"))), mapper);
    }

    public <V> Future<V> input(Player player, long timeout, TimeUnit unit, Predicate<String> predicate, Consumer<Player> consumer, Function<Optional<String>, ? extends V> mapper) {
        return Util.mapFuture(inputChat(player, timeout, unit, predicate, consumer, p -> p.sendMessage(locale.getText("trade.expire"))), mapper);
    }

    public <V> Future<V> input(Player player, long timeout, TimeUnit unit, Predicate<String> predicate, Consumer<Player> consumer, Consumer<Player> onTimeout, Function<Optional<String>, ? extends V> mapper) {
        return Util.mapFuture(inputChat(player, timeout, unit, predicate, consumer, onTimeout), mapper);
    }

    public Future<Optional<Double>> inputNumber(Player player, long timeout, TimeUnit unit) {
        return inputNumber(player, timeout, unit, p -> p.sendMessage(locale.getText("trade.expire")));
    }

    public Future<Optional<Double>> inputNumber(Player player, long timeout, TimeUnit unit, Consumer<Player> onTimeout) {
        return Util.mapFuture(inputChat(player, timeout, unit,
                Util::isDouble, p -> p.sendMessage(locale.getText("trade.format-err")), onTimeout), it -> it.flatMap(Util::asDouble));
    }

    public Future<Optional<String>> inputChat(Player player, long timeout, TimeUnit unit) {
        return inputChat(player, timeout, unit, s -> true, p -> {});
    }

    public Future<Optional<String>> inputChat(Player player, long timeout, TimeUnit unit, Predicate<String> predicate, Consumer<Player> consumer) {
        return inputChat(player, timeout, unit, predicate, consumer, p -> p.sendMessage(locale.getText("trade.expire")));
    }

    /**
     * @param predicate test if input is valid
     * @param consumer  callback when input do not pass the predicate
     * @param onTimeout callback when input is expired
     */
    public Future<Optional<String>> inputChat(Player player, long timeout, TimeUnit unit, Predicate<String> predicate, Consumer<Player> consumer, Consumer<Player> onTimeout) {
        val task = new ProvidingFutureTask<String>(timeout, unit);
        val listener = new OneTimeChatListener(task, player, predicate, consumer);
        Sponge.getEventManager().registerListener(inst, MessageChannelEvent.Chat.class, Order.FIRST, listener);
        val uid = player.getUniqueId();
        sync.schedule(() -> {
            Sponge.getEventManager().unregisterListeners(listener);
            val p = Sponge.getServer().getPlayer(uid);
            if (!task.isDone() && p.isPresent()) onTimeout.accept(p.get());
        }, timeout, unit);
        return task;
    }

}
