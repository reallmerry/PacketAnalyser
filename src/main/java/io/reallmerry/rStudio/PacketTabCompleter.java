package io.reallmerry.rStudio;

import com.comphenix.protocol.PacketType;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class PacketTabCompleter implements TabCompleter {

    private static final List<String> ACTIONS = Arrays.asList("start", "stop", "list");
    private static final List<String> PACKET_NAMES = StreamSupport.stream(PacketType.values().spliterator(), false)
            .map(PacketType::name)
            .sorted()
            .collect(Collectors.toList());

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (!sender.hasPermission("packetanalyser.use")) {
            return Collections.emptyList();
        }

        final List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            StringUtil.copyPartialMatches(args[0], ACTIONS, completions);
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("start") || args[0].equalsIgnoreCase("stop")) {
                List<String> playerNames = Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .collect(Collectors.toList());
                StringUtil.copyPartialMatches(args[1], playerNames, completions);
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("start")) {
            StringUtil.copyPartialMatches(args[2], PACKET_NAMES, completions);
        }

        return completions;
    }
}