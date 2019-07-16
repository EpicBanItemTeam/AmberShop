package io.izzel.ambershop.cmd.condition;

import io.izzel.ambershop.data.ShopRecord;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.text.Text;

import java.util.function.Predicate;
import java.util.function.Supplier;

public abstract class Condition implements Predicate<ShopRecord>, Supplier<String> {

    public abstract boolean query();

    public abstract boolean test();

    public static CommandElement id(String key) {
        return new ExpCondition.Parser(Text.of(key), "id", ShopRecord::getId);
    }

    public static CommandElement world(String key) {
        return new WorldCondition.Parser(Text.of(key));
    }

}
