package com.islandium.core.redis;

import com.islandium.core.IslandiumPlugin;
import com.islandium.core.config.MainConfig;
import com.islandium.core.redis.channel.RedisChannel;
import com.islandium.core.redis.pubsub.MessagePublisher;
import com.islandium.core.redis.pubsub.MessageSubscriber;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisAsyncCommands;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

/**
 * Gestionnaire Redis avec Lettuce.
 */
public class RedisManager {

    private final IslandiumPlugin plugin;
    private final MainConfig config;

    private RedisClient client;
    private StatefulRedisConnection<String, String> connection;
    private StatefulRedisPubSubConnection<String, String> pubSubConnection;

    private MessagePublisher publisher;
    private MessageSubscriber subscriber;

    public RedisManager(@NotNull IslandiumPlugin plugin, @NotNull MainConfig config) {
        this.plugin = plugin;
        this.config = config;
    }

    /**
     * Connecte à Redis.
     */
    public CompletableFuture<Void> connect() {
        return CompletableFuture.runAsync(() -> {
            try {
                RedisURI.Builder uriBuilder = RedisURI.builder()
                        .withHost(config.getRedisHost())
                        .withPort(config.getRedisPort())
                        .withDatabase(config.getRedisDatabase())
                        .withTimeout(Duration.ofSeconds(10));

                String password = config.getRedisPassword();
                if (password != null && !password.isEmpty()) {
                    uriBuilder.withPassword(password.toCharArray());
                }

                RedisURI uri = uriBuilder.build();

                this.client = RedisClient.create(uri);
                this.connection = client.connect();
                this.pubSubConnection = client.connectPubSub();

                // Create publisher and subscriber
                this.publisher = new MessagePublisher(this);
                this.subscriber = new MessageSubscriber(plugin, this);

                // Subscribe to all channels
                subscribeToChannels();

                plugin.log(Level.INFO, "Redis connection established!");

            } catch (Exception e) {
                plugin.log(Level.SEVERE, "Failed to connect to Redis: " + e.getMessage());
                throw new RuntimeException("Redis connection failed", e);
            }
        });
    }

    /**
     * Déconnecte de Redis.
     */
    public void disconnect() {
        try {
            if (pubSubConnection != null) {
                pubSubConnection.close();
            }
            if (connection != null) {
                connection.close();
            }
            if (client != null) {
                client.shutdown();
            }
            plugin.log(Level.INFO, "Redis connection closed.");
        } catch (Exception e) {
            plugin.log(Level.WARNING, "Error closing Redis connection: " + e.getMessage());
        }
    }

    /**
     * Subscribe to all Redis channels.
     */
    private void subscribeToChannels() {
        subscriber.subscribeToAllChannels();
    }

    // === Commands ===

    /**
     * @return commandes synchrones
     */
    @NotNull
    public RedisCommands<String, String> sync() {
        return connection.sync();
    }

    /**
     * @return commandes asynchrones
     */
    @NotNull
    public RedisAsyncCommands<String, String> async() {
        return connection.async();
    }

    /**
     * @return la connexion pub/sub
     */
    @NotNull
    public StatefulRedisPubSubConnection<String, String> getPubSubConnection() {
        return pubSubConnection;
    }

    /**
     * @return le publisher
     */
    @NotNull
    public MessagePublisher getPublisher() {
        return publisher;
    }

    /**
     * @return le subscriber
     */
    @NotNull
    public MessageSubscriber getSubscriber() {
        return subscriber;
    }

    // === Helpers ===

    /**
     * Set a value with expiration.
     */
    public CompletableFuture<Void> setEx(@NotNull String key, @NotNull String value, long seconds) {
        return async().setex(key, seconds, value).toCompletableFuture().thenApply(v -> null);
    }

    /**
     * Get a value.
     */
    public CompletableFuture<String> get(@NotNull String key) {
        return async().get(key).toCompletableFuture();
    }

    /**
     * Delete a key.
     */
    public CompletableFuture<Long> del(@NotNull String key) {
        return async().del(key).toCompletableFuture();
    }

    /**
     * Add to a set.
     */
    public CompletableFuture<Long> sadd(@NotNull String key, String... values) {
        return async().sadd(key, values).toCompletableFuture();
    }

    /**
     * Remove from a set.
     */
    public CompletableFuture<Long> srem(@NotNull String key, String... values) {
        return async().srem(key, values).toCompletableFuture();
    }

    /**
     * Check if member of set.
     */
    public CompletableFuture<Boolean> sismember(@NotNull String key, @NotNull String value) {
        return async().sismember(key, value).toCompletableFuture();
    }

    /**
     * Get set members.
     */
    public CompletableFuture<java.util.Set<String>> smembers(@NotNull String key) {
        return async().smembers(key).toCompletableFuture();
    }

    /**
     * Get set size.
     */
    public CompletableFuture<Long> scard(@NotNull String key) {
        return async().scard(key).toCompletableFuture();
    }

    /**
     * Vérifie si la connexion est active.
     */
    public boolean isConnected() {
        return connection != null && connection.isOpen();
    }
}
