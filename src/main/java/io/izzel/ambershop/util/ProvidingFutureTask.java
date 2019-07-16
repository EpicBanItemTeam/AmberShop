package io.izzel.ambershop.util;

import org.spongepowered.api.util.annotation.NonnullByDefault;

import java.util.Optional;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

@NonnullByDefault
public class ProvidingFutureTask<T> extends FutureTask<Optional<T>> {

    private final ArrayBlockingQueue<T> queue;

    private final long timeout;
    private final TimeUnit unit;
    private boolean done;

    public ProvidingFutureTask(long timeout, TimeUnit unit) {
        super(Optional::empty);
        this.timeout = timeout;
        this.unit = unit;
        queue = new ArrayBlockingQueue<>(1);
    }

    @Override
    public Optional<T> get() throws InterruptedException {
        return Optional.ofNullable(queue.poll(timeout, unit));
    }

    @Override
    public Optional<T> get(long timeout, TimeUnit unit) throws InterruptedException {
        return Optional.ofNullable(queue.poll(timeout, unit));
    }

    @Override
    public boolean isDone() {
        return done;
    }

    public void provide(T value) {
        done = done || queue.offer(value);
    }

}
