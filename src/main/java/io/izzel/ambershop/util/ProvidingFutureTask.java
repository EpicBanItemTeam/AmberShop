package io.izzel.ambershop.util;

import lombok.val;
import org.spongepowered.api.util.annotation.NonnullByDefault;

import java.util.Optional;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

@NonnullByDefault
public class ProvidingFutureTask<T> extends FutureTask<Optional<T>> {

    private static final Object CANCEL = new Object();

    private final ArrayBlockingQueue<T> queue;

    private final long timeout;
    private final TimeUnit unit;
    private boolean done = false, cancel = false;

    public ProvidingFutureTask(long timeout, TimeUnit unit) {
        super(Optional::empty);
        this.timeout = timeout;
        this.unit = unit;
        queue = new ArrayBlockingQueue<>(1);
    }

    @Override
    public Optional<T> get() throws InterruptedException {
        val poll = queue.poll(timeout, unit);
        return Optional.ofNullable(poll == CANCEL ? null : poll);
    }

    @Override
    public Optional<T> get(long timeout, TimeUnit unit) throws InterruptedException {
        val poll = queue.poll(timeout, unit);
        return Optional.ofNullable(poll == CANCEL ? null : poll);
    }

    @Override
    public boolean isDone() {
        return done;
    }

    public void provide(T value) {
        done = (done || cancel || queue.offer(value));
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return cancel = (done || cancel || queue.offer((T) CANCEL));
    }

    @Override
    public boolean isCancelled() {
        return cancel;
    }
}
