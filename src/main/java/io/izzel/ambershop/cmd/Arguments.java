package io.izzel.ambershop.cmd;

import com.google.common.collect.ImmutableList;
import io.izzel.ambershop.util.Util;
import lombok.val;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.ArgumentParseException;
import org.spongepowered.api.command.args.CommandArgs;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.text.Text;

import javax.annotation.Nullable;
import java.util.List;

public class Arguments {

    public static CommandElement num(String key) {
        return new FormattedDouble(Text.of(key));
    }

    private static class FormattedDouble extends CommandElement {

        FormattedDouble(@Nullable Text key) {
            super(key);
        }

        @Nullable
        @Override
        protected Object parseValue(CommandSource source, CommandArgs args) throws ArgumentParseException {
            try {
                val next = args.next();
                return Util.asDouble(next).orElseThrow(Exception::new);
            } catch (Exception e) {
                throw args.createError(Text.of(e));
            }
        }

        @Override
        public List<String> complete(CommandSource src, CommandArgs args, CommandContext context) {
            return ImmutableList.of();
        }

        @Override
        public Text getUsage(CommandSource src) {
            return Text.of("#.00");
        }

    }

}
