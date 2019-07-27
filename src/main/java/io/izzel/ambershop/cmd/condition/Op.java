package io.izzel.ambershop.cmd.condition;

import com.google.common.collect.ImmutableMap;
import lombok.RequiredArgsConstructor;

import java.util.Map;
import java.util.function.BiFunction;

public interface Op extends BiFunction<Number, Number, Boolean> {

    Map<String, Op> INTS = ImmutableMap.<String, Op>builder()
        .put("=", Impl.of("=", (a, b) -> a.longValue() == b.longValue()))
        .put(">=", Impl.of(">=", (a, b) -> a.longValue() >= b.longValue()))
        .put(">", Impl.of(">", (a, b) -> a.longValue() > b.longValue()))
        .put("<=", Impl.of("<=", (a, b) -> a.longValue() <= b.longValue()))
        .put("<", Impl.of("<", (a, b) -> a.longValue() < b.longValue()))
        .put("!=", Impl.of("!=", (a, b) -> !a.equals(b)))
        .build();

    Map<String, Op> DECIMALS = ImmutableMap.<String, Op>builder()
        .put("=", Impl.of("=", (a, b) -> a.doubleValue() == b.doubleValue()))
        .put(">=", Impl.of(">=", (a, b) -> a.doubleValue() >= b.doubleValue()))
        .put(">", Impl.of(">", (a, b) -> a.doubleValue() > b.doubleValue()))
        .put("<=", Impl.of("<=", (a, b) -> a.doubleValue() <= b.doubleValue()))
        .put("<", Impl.of("<", (a, b) -> a.doubleValue() < b.doubleValue()))
        .put("!=", Impl.of("!=", (a, b) -> !a.equals(b)))
        .build();

    static Op ofInt(String op) {
        return INTS.get(op);
    }

    static Op ofDecimal(String op) {
        return DECIMALS.get(op);
    }

}

@RequiredArgsConstructor(staticName = "of")
class Impl implements Op {

    private final String name;
    private final Op underlying;

    @Override
    public Boolean apply(Number number, Number number2) {
        return underlying.apply(number, number2);
    }

    @Override
    public String toString() {
        return "Op(" + name + ")";
    }

}
