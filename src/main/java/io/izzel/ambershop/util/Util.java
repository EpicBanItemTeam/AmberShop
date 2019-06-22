package io.izzel.ambershop.util;

import lombok.experimental.UtilityClass;

import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;

@UtilityClass
public class Util {

    public boolean isDouble(String str) {
        try {
            Double.parseDouble(str);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public Optional<Double> asDouble(String str) {
        try {
            return Optional.of(Double.parseDouble(str));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public boolean isInteger(String str) {
        try {
            Integer.parseInt(str);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public Optional<Integer> asInteger(String str) {
        try {
            return Optional.of(Integer.parseInt(str));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public <U, V> Future<V> mapFuture(Future<U> future, Function<? super U, ? extends V> mapper) {
        return new Future<V>() {
            @Override
            public boolean cancel(boolean mayInterruptIfRunning) {
                return future.cancel(mayInterruptIfRunning);
            }

            @Override
            public boolean isCancelled() {
                return future.isCancelled();
            }

            @Override
            public boolean isDone() {
                return future.isDone();
            }

            @Override
            public V get() throws InterruptedException, ExecutionException {
                return mapper.apply(future.get());
            }

            @Override
            public V get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
                return mapper.apply(future.get(timeout, unit));
            }
        };
    }

}
