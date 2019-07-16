package io.izzel.ambershop.cmd.condition;

import com.google.common.collect.ImmutableList;
import io.izzel.ambershop.data.ShopRecord;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.ArgumentParseException;
import org.spongepowered.api.command.args.CommandArgs;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.text.Text;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.regex.Pattern;

@RequiredArgsConstructor
public class ExpCondition extends Condition {

    private final String name;
    private final Function<ShopRecord, Number> getter;
    private final String opName;
    private final Op op;
    private final long num;

    @Override
    public boolean test(ShopRecord record) {
        return op.apply(getter.apply(record), num);
    }

    @Override
    public String get() {
        return String.format("%s %s %d", name, opName, num);
    }

    @Override
    public boolean query() {
        return true;
    }

    @Override
    public boolean test() {
        return true;
    }

    public static class Parser extends CommandElement {

        private static final Pattern EXP = Pattern.compile("(=|>|<|>=|<=|!=)([+\\-]?\\d+)");
        private static final Pattern RANGE = Pattern.compile("([\\[(])([+\\-]?\\d+)?,([+\\-]?\\d+)?([])])");
        private String name;
        private Function<ShopRecord, Number> getter;

        protected Parser(@Nullable Text key, String name, Function<ShopRecord, Number> getter) {
            super(key);
            this.name = name;
            this.getter = getter;
        }

        @Nullable
        @Override
        protected Object parseValue(CommandSource source, CommandArgs args) throws ArgumentParseException {
            val next = args.next();
            try {
                return new ExpCondition(name, getter, "=", Op.ofInt("="), Long.parseLong(next));
            } catch (Exception e) {
                try {
                    val exp = EXP.matcher(next);
                    if (exp.find()) {
                        val op = exp.group(1);
                        val num = Long.parseLong(exp.group(2));
                        return new ExpCondition(name, getter, op, Objects.requireNonNull(Op.ofInt(op)), num);
                    }
                    throw args.createError(Text.of("Not a condition: " + next));
                } catch (Exception ex) {
                    throw args.createError(Text.of(ex.toString()));
                }
            }
        }

        @Override
        public List<String> complete(CommandSource src, CommandArgs args, CommandContext context) {
            return ImmutableList.of();
        }
    }

}
