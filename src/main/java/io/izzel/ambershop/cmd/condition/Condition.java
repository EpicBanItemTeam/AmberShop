package io.izzel.ambershop.cmd.condition;

import io.izzel.ambershop.data.ShopRecord;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.text.Text;

import java.util.function.Predicate;
import java.util.function.Supplier;

public abstract class Condition implements Predicate<ShopRecord>, Supplier<String> {

    public abstract boolean query();

    public abstract boolean test();

    public static CommandElement id(String key) {
        return GenericArguments.firstParsing(new ExpCondition.Parser(Text.of(key), "id", ShopRecord::getId),
            new RangeCondition.Parser(Text.of(key), "id", ShopRecord::getId));
    }

    public static CommandElement world(String key) {
        return new WorldCondition.Parser(Text.of(key));
    }

    public static CommandElement price(String key) {
        return GenericArguments.firstParsing(new ExpCondition.Parser(Text.of(key), "price", ShopRecord::getPrice),
            new RangeCondition.Parser(Text.of(key), "price", ShopRecord::getPrice));
    }

    public static CommandElement locX(String key) {
        return GenericArguments.firstParsing(new ExpCondition.Parser(Text.of(key), "x", ShopRecord::getX),
            new RangeCondition.Parser(Text.of(key), "x", ShopRecord::getX));
    }

    public static CommandElement locY(String key) {
        return GenericArguments.firstParsing(new ExpCondition.Parser(Text.of(key), "y", ShopRecord::getY),
            new RangeCondition.Parser(Text.of(key), "y", ShopRecord::getY));
    }

    public static CommandElement locZ(String key) {
        return GenericArguments.firstParsing(new ExpCondition.Parser(Text.of(key), "z", ShopRecord::getZ),
            new RangeCondition.Parser(Text.of(key), "z", ShopRecord::getZ));
    }

}
