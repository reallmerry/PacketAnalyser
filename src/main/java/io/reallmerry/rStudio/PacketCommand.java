package io.reallmerry.rStudio;

import io.reallmerry.rStudio.log.FileLogger;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.io.File;

public class PacketCommand implements CommandExecutor {

    private final PacketListenerManager listenerManager;
    private final FileLogger fileLogger;
    private final MiniMessage mm = MiniMessage.miniMessage();

    public PacketCommand(PacketListenerManager listenerManager, FileLogger fileLogger) {
        this.listenerManager = listenerManager;
        this.fileLogger = fileLogger;
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("packetanalyser.using")) {
            sender.sendMessage(mm.deserialize("<red>You do not have permission to use this command.</red>"));
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(mm.deserialize("<gold>Usage: <white>/packets <start|stop|list> ...</white></gold>"));
            return true;
        }

        String action = args[0].toLowerCase();

        if ("list".equals(action)) {
            try {
                File packetListFile = fileLogger.generatePacketListFile();
                sender.sendMessage(mm.deserialize("<green>Successfully generated packet list!</green>"));
                sender.sendMessage(mm.deserialize("<gray>File saved to: <white>" + packetListFile.getPath().replace("\\", "/") + "</white></gray>"));
            } catch (Exception e) {
                sender.sendMessage(mm.deserialize("<red>An error occurred while generating the packet list. See console for details.</red>"));
                e.printStackTrace();
            }
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(mm.deserialize("<gold>Usage: <white>/packets <start|stop> <player> [packet_type]</white></gold>"));
            return true;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(mm.deserialize("<red>Player <white>" + args[1] + "</white> not found.</red>"));
            return true;
        }

        if ("start".equals(action)) {
            // *** НАЧАЛО ИСПРАВЛЕНИЯ ***
            String packetFilter = (args.length > 2) ? args[2].toUpperCase() : ""; // Используем "" вместо null
            File logFile = listenerManager.startLogging(target, packetFilter);

            // Проверяем, что файл был успешно создан
            if (logFile == null) {
                sender.sendMessage(mm.deserialize("<red>Failed to start logging session. Check the console for errors.</red>"));
                return true;
            }

            String filterMessage = (!packetFilter.isEmpty()) ? " filtering for <white>" + packetFilter + "</white>" : "";
            // *** КОНЕЦ ИСПРАВЛЕНИЯ ***

            sender.sendMessage(mm.deserialize("<green>Started logging packets for <white>" + target.getName() + "</white>" + filterMessage + ".</green>"));
            sender.sendMessage(mm.deserialize("<gray>Log file: <white>" + logFile.getPath().replace("\\", "/") + "</white></gray>"));

        } else if ("stop".equals(action)) {
            if (listenerManager.isLogging(target.getUniqueId())) {
                listenerManager.stopLogging(target.getUniqueId());
                sender.sendMessage(mm.deserialize("<red>Stopped logging packets for <white>" + target.getName() + "</white>.</red>"));
            } else {
                sender.sendMessage(mm.deserialize("<yellow>Logging for <white>" + target.getName() + "</white> was not enabled.</yellow>"));
            }
        } else {
            sender.sendMessage(mm.deserialize("<red>Unknown action. Use 'start', 'stop', or 'list'.</red>"));
        }

        return true;
    }
}