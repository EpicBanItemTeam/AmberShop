package io.izzel.ambershop.util;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.izzel.ambershop.AmberShop;
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
import java.util.function.Predicate;

@Singleton
public class AmberTasks {

    @Inject private AmberShop inst;

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

    public Future<Optional<String>> inputChat(Player player, long timeout, TimeUnit unit) {
        val task = new ProvidingFutureTask<String>(timeout, unit);
        val listener = new OneTimeChatListener(task, player);
        Sponge.getEventManager().registerListener(inst, MessageChannelEvent.Chat.class, Order.FIRST, listener);
        sync.schedule(() -> Sponge.getEventManager().unregisterListeners(listener), timeout, unit);
        return task;
    }

    /**
     * @param predicate test if input is valid
     * @param consumer  callback when input do not pass the predicate
     */
    public Future<Optional<String>> inputChat(Player player, long timeout, TimeUnit unit, Predicate<String> predicate, Consumer<Player> consumer) {
        val task = new ProvidingFutureTask<String>(timeout, unit);
        val listener = new OneTimeChatListener(task, player, predicate, consumer);
        Sponge.getEventManager().registerListener(inst, MessageChannelEvent.Chat.class, Order.FIRST, listener);
        sync.schedule(() -> Sponge.getEventManager().unregisterListeners(listener), timeout, unit);
        return task;
    }

}
