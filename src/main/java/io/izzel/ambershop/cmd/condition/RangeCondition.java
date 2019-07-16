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
import java.util.function.Function;
import java.util.regex.Pattern;

@RequiredArgsConstructor
public class RangeCondition extends Condition {

    private final ExpCondition left, right;

    @Override
    public boolean query() {
        return true;
    }

    @Override
    public boolean test() {
        return true;
    }

    @Override
    public boolean test(ShopRecord record) {
        return left.test(record) && right.test(record);
    }

    @Override
    public String get() {
        return String.format("(%s and %s)", left.get(), right.get());
    }

    public static class Parser extends CommandElement {

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
                val range = RANGE.matcher(next);
                if (range.find()) {
                    val leftInclusive = range.group(1).equals("[");
                    val leftValue = Long.parseLong(range.group(2));
                    val rightValue = Long.parseLong(range.group(3));
                    val rightInclusive = range.group(4).equals("]");
                    if (leftValue > rightValue)
                        throw args.createError(Text.of(next, "L>R: " + leftValue + " " + rightValue));
                    if (leftValue == rightValue && (!leftInclusive || !rightInclusive))
                        throw args.createError(Text.of(next));
                    val leftOp = leftInclusive ? ">=" : ">";
                    val rightOp = leftInclusive ? "<=" : "<";
                    return new RangeCondition(
                        new ExpCondition(name, getter, leftOp, Op.ofInt(leftOp), leftValue),
                        new ExpCondition(name, getter, rightOp, Op.ofInt(rightOp), leftValue)
                    );
                }
                throw args.createError(Text.of("Not a range: " + next));
            } catch (Exception ex) {
                throw args.createError(Text.of(ex.toString()));
            }
        }

        @Override
        public List<String> complete(CommandSource src, CommandArgs args, CommandContext context) {
            return ImmutableList.of();
        }
    }
}
