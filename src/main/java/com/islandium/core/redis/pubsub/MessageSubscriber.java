package com.islandium.core.redis.pubsub;

import com.islandium.core.IslandiumPlugin;
import com.islandium.core.redis.RedisManager;
import com.islandium.core.redis.channel.RedisChannel;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import io.lettuce.core.pubsub.RedisPubSubAdapter;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.logging.Level;

/**
 * Subscriber aux messages Redis.
 */
public class MessageSubscriber extends RedisPubSubAdapter<String, String> {

    private static final Gson GSON = new GsonBuilder().create();

    private final IslandiumPlugin plugin;
    private final RedisManager redisManager;
    private final Map<RedisChannel, Consumer<JsonObject>> handlers = new HashMap<>();

    public MessageSubscriber(@NotNull IslandiumPlugin plugin, @NotNull RedisManager redisManager) {
        this.plugin = plugin;
        this.redisManager = redisManager;
    }

    /**
     * Subscribe to all channels.
     */
    public void subscribeToAllChannels() {
        redisManager.getPubSubConnection().addListener(this);
        redisManager.getPubSubConnection().sync().subscribe(RedisChannel.getAllChannels());
    }

    /**
     * Register a handler for a channel.
     */
    public void registerHandler(@NotNull RedisChannel channel, @NotNull Consumer<JsonObject> handler) {
        handlers.put(channel, handler);
    }

    @Override
    public void message(String channel, String message) {
        try {
            RedisChannel redisChannel = RedisChannel.fromChannel(channel);
            if (redisChannel == null) {
                return;
            }

            JsonObject json = GSON.fromJson(message, JsonObject.class);

            Consumer<JsonObject> handler = handlers.get(redisChannel);
            if (handler != null) {
                handler.accept(json);
            }

            // Handle built-in channels
            handleBuiltInChannels(redisChannel, json);

        } catch (Exception e) {
            plugin.log(Level.WARNING, "Error processing Redis message: " + e.getMessage());
        }
    }

    private void handleBuiltInChannels(RedisChannel channel, JsonObject json) {
        switch (channel) {
            case PLAYER_JOIN -> handlePlayerJoin(json);
            case PLAYER_QUIT -> handlePlayerQuit(json);
            case BROADCAST -> handleBroadcast(json);
            case PRIVATE_MESSAGE -> handlePrivateMessage(json);
            case TELEPORT -> handleTeleport(json);
        }
    }

    private void handlePlayerJoin(JsonObject json) {
        String uuid = json.get("uuid").getAsString();
        String name = json.get("name").getAsString();
        String server = json.get("server").getAsString();

        // Add to online set
        redisManager.sadd("ess:online", uuid);
        redisManager.sadd("ess:server:" + server, uuid);
    }

    private void handlePlayerQuit(JsonObject json) {
        String uuid = json.get("uuid").getAsString();
        String server = json.get("server").getAsString();

        // Remove from online sets
        redisManager.srem("ess:online", uuid);
        redisManager.srem("ess:server:" + server, uuid);
    }

    private void handleBroadcast(JsonObject json) {
        String message = json.get("message").getAsString();
        String permission = json.has("permission") ? json.get("permission").getAsString() : null;

        // Broadcast to local players
        plugin.getPlayerManager().getOnlinePlayersLocal().forEach(player -> {
            if (permission == null || permission.isEmpty() || player.hasPermission(permission)) {
                player.sendMessage(message);
            }
        });
    }

    private void handlePrivateMessage(JsonObject json) {
        String toUuid = json.get("toUuid").getAsString();
        String fromName = json.get("fromName").getAsString();
        String message = json.get("message").getAsString();

        // Find local player and deliver message
        var playerOpt = plugin.getPlayerManager().getOnlinePlayer(java.util.UUID.fromString(toUuid));
        if (playerOpt.isPresent()) {
            var player = playerOpt.get();
            String formatted = plugin.getConfigManager().getMessages()
                    .get("msg.format-received", "player", fromName, "message", message);
            player.sendMessage(formatted);

            // Update last message sender
            String fromUuid = json.get("fromUuid").getAsString();
            plugin.getServiceManager().getMessenger()
                    .setLastMessageSender(player.getUniqueId(), java.util.UUID.fromString(fromUuid));
        }
    }

    private void handleTeleport(JsonObject json) {
        String targetServer = json.get("targetServer").getAsString();

        // Only handle if this is the target server
        if (!targetServer.equalsIgnoreCase(plugin.getServerName())) {
            return;
        }

        String playerUuid = json.get("playerUuid").getAsString();
        String locationStr = json.get("location").getAsString();

        // The player should already be on this server after transfer
        // Teleport them to the specified location
        var playerOpt = plugin.getPlayerManager().getOnlinePlayer(java.util.UUID.fromString(playerUuid));
        if (playerOpt.isPresent()) {
            var location = com.islandium.core.api.location.ServerLocation.deserialize(locationStr);
            playerOpt.get().teleport(location);
        }
    }

}
