package org.nezxenka.strictkits.command.api;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.nezxenka.strictkits.util.command.Completions;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public abstract class SubCommandRouter implements TabExecutor {

    private final Map<String, SubCommand> subCommands = new LinkedHashMap<>();

    protected final void register(SubCommand subCommand) {
        subCommands.put(subCommand.name(), subCommand);
    }

    @Override
    public final boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            onEmpty(sender);
            return true;
        }
        SubCommand subCommand = find(args[0]);
        if (subCommand == null) {
            onUnknown(sender, args);
        } else {
            subCommand.execute(sender, tail(args));
        }
        return true;
    }

    @Override
    public final List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length <= 1) {
            return completeFirst(sender, args.length == 0 ? "" : args[0]);
        }
        SubCommand subCommand = find(args[0]);
        return subCommand == null ? completeUnknown(sender, args) : subCommand.complete(sender, tail(args));
    }

    protected abstract void onEmpty(CommandSender sender);

    protected abstract void onUnknown(CommandSender sender, String[] args);

    protected List<String> completeFirst(CommandSender sender, String prefix) {
        return Completions.filter(subCommands.keySet(), prefix);
    }

    protected List<String> completeUnknown(CommandSender sender, String[] args) {
        return Collections.emptyList();
    }

    private SubCommand find(String name) {
        return subCommands.get(name.toLowerCase(Locale.ROOT));
    }

    private static String[] tail(String[] args) {
        return Arrays.copyOfRange(args, 1, args.length);
    }
}
