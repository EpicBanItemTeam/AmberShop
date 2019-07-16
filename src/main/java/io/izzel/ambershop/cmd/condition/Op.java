package io.izzel.ambershop.cmd.condition;

import com.google.common.collect.ImmutableMap;

import java.util.Map;
import java.util.function.BiFunction;

public interface Op extends BiFunction<Number, Number, Boolean> {

    Map<String, Op> INTS = ImmutableMap.<String, Op>builder()
        .put("=", Object::equals)
        .put(">=", (a, b) -> a.longValue() >= b.longValue())
        .put(">", (a, b) -> a.longValue() > b.longValue())
        .put("<=", (a, b) -> a.longValue() <= b.longValue())
        .put("<", (a, b) -> a.longValue() < b.longValue())
        .put("!=", (a, b) -> !a.equals(b))
        .build();

    Map<String, Op> DECIMALS = ImmutableMap.<String, Op>builder()
        .put("=", Object::equals)
        .put(">=", (a, b) -> a.doubleValue() >= b.doubleValue())
        .put(">", (a, b) -> a.doubleValue() > b.doubleValue())
        .put("<=", (a, b) -> a.doubleValue() <= b.doubleValue())
        .put("<", (a, b) -> a.doubleValue() < b.doubleValue())
        .put("!=", (a, b) -> !a.equals(b))
        .build();

    static Op ofInt(String op) {
        return INTS.get(op);
    }

    static Op ofDecimal(String op) {
        return DECIMALS.get(op);
    }

}
