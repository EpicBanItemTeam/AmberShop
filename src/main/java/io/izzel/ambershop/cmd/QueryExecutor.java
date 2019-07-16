package io.izzel.ambershop.cmd;

import com.google.inject.Inject;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;

public class QueryExecutor {

    @Inject RemoveExecutor remove;
    @Inject SetExecutor set;
    @Inject ListExecutor list;

    public static class RemoveExecutor implements CommandExecutor {

        @Override
        public CommandResult execute(CommandSource src, CommandContext args) throws CommandException {
            
            return CommandResult.success();
        }

    }

    public static class SetExecutor implements CommandExecutor {

        @Override
        public CommandResult execute(CommandSource src, CommandContext args) throws CommandException {
            return CommandResult.success();
        }

    }

    public static class ListExecutor implements CommandExecutor {

        @Override
        public CommandResult execute(CommandSource src, CommandContext args) throws CommandException {
            return CommandResult.success();
        }
    }
}
