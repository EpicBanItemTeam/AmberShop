package io.izzel.ambershop.util;

import io.izzel.ambershop.AmberShop;
import io.izzel.ambershop.trade.TransactionResult;
import lombok.experimental.UtilityClass;
import lombok.val;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.cause.EventContext;
import org.spongepowered.api.event.cause.EventContextKeys;
import org.spongepowered.api.service.economy.EconomyService;
import org.spongepowered.api.service.economy.transaction.ResultType;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import java.util.regex.Pattern;

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
            return Optional.of(BigDecimal.valueOf(Double.parseDouble(str)).setScale(2, RoundingMode.HALF_UP).doubleValue());
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


    // this is a lot faster than the internal formatter
    public String replace(String template, Object... args) {
        if (args.length == 0 || template.length() == 0) {
            return template;
        }
        val arr = template.toCharArray();
        val stringBuilder = new StringBuilder(template.length());
        for (int i = 0; i < arr.length; i++) {
            if (arr[i] == '{' && Character.isDigit(arr[Math.min(i + 1, arr.length - 1)])
                    && arr[Math.min(i + 1, arr.length - 1)] - '0' < args.length
                    && arr[Math.min(i + 2, arr.length - 1)] == '}'
                    && args[arr[i + 1] - '0'] != null) {
                stringBuilder.append(args[arr[i + 1] - '0']);
                i += 2;
            } else {
                stringBuilder.append(arr[i]);
            }
        }
        return stringBuilder.toString();
    }

    public TransactionResult performEconomy(UUID uuid, BigDecimal price, boolean isWithdraw) {
        val eco = Sponge.getServiceManager().provideUnchecked(EconomyService.class);
        val playerAcc = eco.getOrCreateAccount(uuid).get();
        val ctx = EventContext.builder().add(EventContextKeys.PLUGIN, AmberShop.SINGLETON.container).build();
        val cause = Cause.of(ctx, AmberShop.SINGLETON.container);
        val currency = eco.getDefaultCurrency();
        val result = isWithdraw ? playerAcc.withdraw(currency, price, cause) : playerAcc.deposit(currency, price, cause);
        return result.getResult() == ResultType.SUCCESS ? TransactionResult.SUCCESS : TransactionResult.ECONOMY_ISSUE;
    }

}
