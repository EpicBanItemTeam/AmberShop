package io.izzel.ambershop.cmd.condition;

import io.izzel.ambershop.data.ShopRecord;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.val;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.ArgumentParseException;
import org.spongepowered.api.command.args.CommandArgs;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.world.World;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@ToString
@RequiredArgsConstructor
public class WorldCondition extends Condition {

    private final List<UUID> uuid;

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
        return uuid.contains(record.world);
    }

    @Override
    public String get() {
        if (uuid.size() == 1)
            return String.format("world = %s", uuid.get(0).toString());
        else if (uuid.size() > 1) {
            return String.format("world in (%s)", uuid.stream().map(Object::toString).collect(Collectors.joining(",")));
        } else throw new IllegalArgumentException();
    }

    public static class Parser extends CommandElement {

        protected Parser(@Nullable Text key) {
            super(key);
        }

        @Nullable
        @Override
        protected Object parseValue(CommandSource source, CommandArgs args) throws ArgumentParseException {
            val next = args.next();
            val list = Arrays.stream(next.split("\\|"))
                .map(Sponge.getServer()::getWorld)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(World::getUniqueId)
                .collect(Collectors.toList());
            if (list.isEmpty()) throw args.createError(Text.of(next));
            return new WorldCondition(list);
        }

        @Override
        public List<String> complete(CommandSource src, CommandArgs args, CommandContext context) {
            return Sponge.getServer().getWorlds().stream().map(World::getName).collect(Collectors.toList());
        }

    }
}
