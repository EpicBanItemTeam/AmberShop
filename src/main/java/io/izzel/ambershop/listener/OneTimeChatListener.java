package io.izzel.ambershop.listener;

import io.izzel.ambershop.AmberShop;
import io.izzel.ambershop.conf.AmberLocale;
import io.izzel.ambershop.util.ProvidingFutureTask;
import lombok.val;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.EventListener;
import org.spongepowered.api.event.message.MessageChannelEvent;
import org.spongepowered.api.util.annotation.NonnullByDefault;

import java.lang.ref.WeakReference;
import java.util.function.Consumer;
import java.util.function.Predicate;

@NonnullByDefault
public class OneTimeChatListener implements EventListener<MessageChannelEvent.Chat> {

    private final ProvidingFutureTask<String> task;

    private final WeakReference<Player> player;

    private final Predicate<String> predicate;

    private final Consumer<Player> consumer;

    public OneTimeChatListener(ProvidingFutureTask<String> task, Player player) {
        this(task, player, s -> true, p -> {});
    }

    public OneTimeChatListener(ProvidingFutureTask<String> task, Player player, Predicate<String> predicate, Consumer<Player> consumer) {
        this.task = task;
        this.player = new WeakReference<>(player);
        this.predicate = predicate;
        this.consumer = consumer;
    }

    @Override
    public void handle(MessageChannelEvent.Chat event) {
        if (event.isCancelled()) return;
        val sender = event.getCause().first(Player.class);
        val dest = player.get();
        if (dest == null) {
            Sponge.getEventManager().unregisterListeners(this);
            return;
        }
        if (sender.isPresent()) {
            val p = sender.get();
            if (p.getUniqueId().equals(dest.getUniqueId())) {
                event.setCancelled(true);
                if (predicate.test(event.getRawMessage().toPlainSingle())) {
                    task.provide(event.getRawMessage().toPlainSingle());
                    Sponge.getEventManager().unregisterListeners(this);
                } else consumer.accept(p);
            }
        }
    }

}
