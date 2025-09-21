package io.reallmerry.rStudio;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.reflect.StructureModifier;
import com.google.common.collect.ImmutableSet;
import io.reallmerry.rStudio.log.FileLogger;
import org.bukkit.entity.Player;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class PacketListenerManager {

    private final PacketAnalyser plugin;
    private final FileLogger fileLogger;
    private final Map<UUID, String> loggingTargets = new ConcurrentHashMap<>();
    private PacketAdapter packetAdapter;
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    public PacketListenerManager(PacketAnalyser plugin, FileLogger fileLogger) {
        this.plugin = plugin;
        this.fileLogger = fileLogger;
    }

    public File startLogging(Player player, String packetFilter) {
        loggingTargets.put(player.getUniqueId(), packetFilter);
        return fileLogger.openSession(player, packetFilter);
    }

    public void stopLogging(UUID playerUuid) {
        loggingTargets.remove(playerUuid);
        fileLogger.closeSession(playerUuid);
    }

    public void stopAllLogging() {
        for (UUID uuid : loggingTargets.keySet()) {
            fileLogger.closeSession(uuid);
        }
        loggingTargets.clear();
    }

    public boolean isLogging(UUID playerUuid) {
        return loggingTargets.containsKey(playerUuid);
    }

    public void registerListeners() {
        final Set<PacketType> supportedPackets = StreamSupport.stream(PacketType.values().spliterator(), false)
                .filter(PacketType::isSupported)
                .collect(ImmutableSet.toImmutableSet());

        this.packetAdapter = new PacketAdapter(this.plugin, ListenerPriority.NORMAL, supportedPackets) {
            @Override
            public void onPacketReceiving(PacketEvent event) {
                handlePacket(event, "C->S");
            }

            @Override
            public void onPacketSending(PacketEvent event) {
                handlePacket(event, "S->C");
            }
        };
        ProtocolLibrary.getProtocolManager().addPacketListener(this.packetAdapter);
    }

    public void unregisterListeners() {
        ProtocolLibrary.getProtocolManager().removePacketListener(this.packetAdapter);
    }

    private void handlePacket(PacketEvent event, String direction) {
        if (event.getPacketType().getProtocol() != PacketType.Protocol.PLAY) {
            return;
        }

        Player player = event.getPlayer();
        if (player == null || !player.isOnline()) {
            return;
        }

        if (loggingTargets.containsKey(player.getUniqueId())) {
            String filter = loggingTargets.get(player.getUniqueId());
            String packetName = event.getPacketType().name();

            // *** НАЧАЛО ИСПРАВЛЕНИЯ ***
            // Теперь проверяем, является ли строка пустой, а не null
            if (filter.isEmpty() || packetName.equalsIgnoreCase(filter)) {
                // *** КОНЕЦ ИСПРАВЛЕНИЯ ***
                String timestamp = TIME_FORMATTER.format(LocalDateTime.now());
                String packetDetails = getPacketDetails(event.getPacket());
                String logMessage = String.format("[%s] [%s] %s %s", timestamp, direction, packetName, packetDetails);
                fileLogger.log(player.getUniqueId(), logMessage);
            }
        }
    }

    private String getPacketDetails(PacketContainer packet) {
        StringBuilder details = new StringBuilder("{");
        try {
            appendFields(details, "Booleans", packet.getBooleans());
            appendFields(details, "Bytes", packet.getBytes());
            appendFields(details, "Shorts", packet.getShorts());
            appendFields(details, "Integers", packet.getIntegers());
            appendFields(details, "Longs", packet.getLongs());
            appendFields(details, "Floats", packet.getFloat());
            appendFields(details, "Doubles", packet.getDoubles());
            appendFields(details, "Strings", packet.getStrings());
            appendFields(details, "Byte[]", packet.getByteArrays());
        } catch (Exception e) {
            plugin.getLogger().warning("Could not fully inspect packet " + packet.getType().name() + ": " + e.getMessage());
            details.append(" (Inspection Error)");
        }

        if (details.length() > 1) {
            details.setLength(details.length() - 2);
        }

        details.append(" }");
        return details.toString();
    }

    private <T> void appendFields(StringBuilder builder, String fieldType, StructureModifier<T> modifier) {
        int size = modifier.size();
        if (size > 0) {
            builder.append(String.format(" %s: (", fieldType));
            for (int i = 0; i < size; i++) {
                T value = modifier.readSafely(i);
                String valueStr = (value instanceof byte[]) ? "byte[" + ((byte[]) value).length + "]" : String.valueOf(value);
                builder.append(String.format("%d: %s, ", i, valueStr));
            }
            builder.setLength(builder.length() - 2);
            builder.append("), ");
        }
    }
}