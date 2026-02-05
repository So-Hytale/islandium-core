package com.islandium.core.service.messaging;

import com.islandium.core.api.messaging.CrossServerMessenger;
import com.islandium.core.IslandiumPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implémentation du service de messagerie cross-server.
 */
public class CrossServerMessengerImpl implements CrossServerMessenger {

    private final IslandiumPlugin plugin;

    // Map: player UUID -> last message sender UUID
    private final Map<UUID, UUID> lastMessageSenders = new ConcurrentHashMap<>();

    public CrossServerMessengerImpl(@NotNull IslandiumPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public CompletableFuture<Boolean> sendPrivateMessage(
            @NotNull UUID fromUuid,
            @NotNull UUID toUuid,
            @NotNull String message
    ) {
        var fromPlayerOpt = plugin.getPlayerManager().getOnlinePlayer(fromUuid);
        if (fromPlayerOpt.isEmpty()) {
            return CompletableFuture.completedFuture(false);
        }
        var fromPlayer = fromPlayerOpt.get();

        // Check if target is online locally
        var toPlayerOpt = plugin.getPlayerManager().getOnlinePlayer(toUuid);
        if (toPlayerOpt.isPresent()) {
            // Local delivery
            var toPlayer = toPlayerOpt.get();
            String formatted = plugin.getConfigManager().getMessages()
                    .get("msg.format-received", "player", fromPlayer.getName(), "message", message);
            toPlayer.sendMessage(formatted);
            setLastMessageSender(toUuid, fromUuid);
            return CompletableFuture.completedFuture(true);
        }

        // Cross-server delivery via Redis
        return plugin.getPlayerManager().isOnlineNetwork(toUuid).thenApply(isOnline -> {
            if (!isOnline) {
                return false;
            }

            plugin.getRedisManager().getPublisher().publishPrivateMessage(
                    fromUuid.toString(),
                    fromPlayer.getName(),
                    toUuid.toString(),
                    message
            );

            return true;
        });
    }

    @Override
    public void broadcast(@NotNull String message) {
        // Send to local players
        plugin.getPlayerManager().getOnlinePlayersLocal().forEach(player ->
                player.sendMessage(message)
        );

        // Publish to other servers
        plugin.getRedisManager().getPublisher().publishBroadcast(message, null);
    }

    @Override
    public void broadcast(@NotNull String message, @NotNull String permission) {
        // Send to local players with permission
        plugin.getPlayerManager().getOnlinePlayersLocal().forEach(player -> {
            if (player.hasPermission(permission)) {
                player.sendMessage(message);
            }
        });

        // Publish to other servers
        plugin.getRedisManager().getPublisher().publishBroadcast(message, permission);
    }

    @Override
    public void sendStaffMessage(@NotNull String message) {
        broadcast(message, "essentials.staff");
    }

    @Override
    public UUID getLastMessageSender(@NotNull UUID playerUuid) {
        return lastMessageSenders.get(playerUuid);
    }

    @Override
    public void setLastMessageSender(@NotNull UUID playerUuid, @NotNull UUID lastSenderUuid) {
        lastMessageSenders.put(playerUuid, lastSenderUuid);
    }
}
