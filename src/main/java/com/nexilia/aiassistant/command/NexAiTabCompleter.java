package com.nexilia.aiassistant.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.List;

public class NexAiTabCompleter implements TabCompleter {

    private static final List<String> SUBCOMMANDS = List.of("reindex", "reload");

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            for (String s : SUBCOMMANDS) {
                if (s.startsWith(args[0].toLowerCase())) {
                    out.add(s);
                }
            }
        }
        return out;
    }
}
