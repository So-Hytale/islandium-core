package com.islandium.core.redis.pubsub;

import com.islandium.core.redis.RedisManager;
import com.islandium.core.redis.channel.RedisChannel;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Publie des messages sur les channels Redis.
 */
public class MessagePublisher {

    private static final Gson GSON = new GsonBuilder().create();

    private final RedisManager redisManager;

    public MessagePublisher(@NotNull RedisManager redisManager) {
        this.redisManager = redisManager;
    }

    /**
     * Publie un message JSON sur un channel.
     */
    public CompletableFuture<Long> publish(@NotNull RedisChannel channel, @NotNull Object data) {
        String json = GSON.toJson(data);
        return redisManager.async().publish(channel.getChannel(), json).toCompletableFuture();
    }

    /**
     * Publie un message JSON sur un channel avec une map.
     */
    public CompletableFuture<Long> publish(@NotNull RedisChannel channel, @NotNull Map<String, Object> data) {
        String json = GSON.toJson(data);
        return redisManager.async().publish(channel.getChannel(), json).toCompletableFuture();
    }

    /**
     * Publie un message string sur un channel.
     */
    public CompletableFuture<Long> publishRaw(@NotNull RedisChannel channel, @NotNull String message) {
        return redisManager.async().publish(channel.getChannel(), message).toCompletableFuture();
    }

    /**
     * Publie une demande de téléportation cross-server.
     */
    public CompletableFuture<Long> publishTeleport(
            @NotNull String playerUuid,
            @NotNull String targetServer,
            @NotNull String location
    ) {
        return publish(RedisChannel.TELEPORT, Map.of(
                "playerUuid", playerUuid,
                "targetServer", targetServer,
                "location", location
        ));
    }

    /**
     * Publie un message privé.
     */
    public CompletableFuture<Long> publishPrivateMessage(
            @NotNull String fromUuid,
            @NotNull String fromName,
            @NotNull String toUuid,
            @NotNull String message
    ) {
        return publish(RedisChannel.PRIVATE_MESSAGE, Map.of(
                "fromUuid", fromUuid,
                "fromName", fromName,
                "toUuid", toUuid,
                "message", message
        ));
    }

    /**
     * Publie une mise à jour de joueur.
     */
    public CompletableFuture<Long> publishPlayerUpdate(@NotNull String uuid, @NotNull String data) {
        return publish(RedisChannel.PLAYER_UPDATE, Map.of(
                "uuid", uuid,
                "data", data
        ));
    }

    /**
     * Publie une connexion de joueur.
     */
    public CompletableFuture<Long> publishPlayerJoin(@NotNull String uuid, @NotNull String name, @NotNull String server) {
        return publish(RedisChannel.PLAYER_JOIN, Map.of(
                "uuid", uuid,
                "name", name,
                "server", server
        ));
    }

    /**
     * Publie une déconnexion de joueur.
     */
    public CompletableFuture<Long> publishPlayerQuit(@NotNull String uuid, @NotNull String server) {
        return publish(RedisChannel.PLAYER_QUIT, Map.of(
                "uuid", uuid,
                "server", server
        ));
    }

    /**
     * Publie un message staff.
     */
    public CompletableFuture<Long> publishStaffMessage(@NotNull String message) {
        return publish(RedisChannel.STAFF, Map.of(
                "message", message
        ));
    }

    /**
     * Publie un broadcast global.
     */
    public CompletableFuture<Long> publishBroadcast(@NotNull String message, String permission) {
        return publish(RedisChannel.BROADCAST, Map.of(
                "message", message,
                "permission", permission != null ? permission : ""
        ));
    }
}
