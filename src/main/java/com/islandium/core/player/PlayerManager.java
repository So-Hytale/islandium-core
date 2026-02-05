package com.islandium.core.player;

import com.islandium.core.api.player.IslandiumPlayer;
import com.islandium.core.api.player.PlayerProvider;
import com.islandium.core.IslandiumPlugin;
import com.islandium.core.database.repository.PlayerRepository;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gestionnaire des joueurs.
 */
public class PlayerManager implements PlayerProvider {

    private final IslandiumPlugin plugin;
    private final PlayerRepository repository;

    // Cache des joueurs online sur ce serveur
    private final Map<UUID, IslandiumPlayerImpl> onlinePlayers = new ConcurrentHashMap<>();

    // Cache par nom
    private final Map<String, UUID> nameToUuid = new ConcurrentHashMap<>();

    public PlayerManager(@NotNull IslandiumPlugin plugin) {
        this.plugin = plugin;
        this.repository = plugin.getServiceManager().getPlayerRepository();
    }

    // === PlayerProvider Implementation ===

    @Override
    public CompletableFuture<Optional<IslandiumPlayer>> getPlayer(@NotNull UUID uuid) {
        // Check local cache first
        IslandiumPlayerImpl cached = onlinePlayers.get(uuid);
        if (cached != null) {
            return CompletableFuture.completedFuture(Optional.of(cached));
        }

        // Load from database
        return repository.findById(uuid).thenApply(opt ->
                opt.map(data -> new IslandiumPlayerImpl(plugin, data, null))
        );
    }

    @Override
    public CompletableFuture<Optional<IslandiumPlayer>> getPlayer(@NotNull String name) {
        // Check name cache
        UUID uuid = nameToUuid.get(name.toLowerCase());
        if (uuid != null) {
            return getPlayer(uuid);
        }

        // Load from database
        return repository.findByUsername(name).thenApply(opt ->
                opt.map(data -> new IslandiumPlayerImpl(plugin, data, null))
        );
    }

    @Override
    @NotNull
    public Optional<IslandiumPlayer> getOnlinePlayer(@NotNull UUID uuid) {
        return Optional.ofNullable(onlinePlayers.get(uuid));
    }

    @Override
    @NotNull
    public Optional<IslandiumPlayer> getOnlinePlayer(@NotNull String name) {
        UUID uuid = nameToUuid.get(name.toLowerCase());
        return uuid != null ? Optional.ofNullable(onlinePlayers.get(uuid)) : Optional.empty();
    }

    @Override
    @NotNull
    public Collection<IslandiumPlayer> getOnlinePlayersLocal() {
        return Collections.unmodifiableCollection(onlinePlayers.values());
    }

    @Override
    @NotNull
    public CompletableFuture<Collection<IslandiumPlayer>> getOnlinePlayers() {
        return CompletableFuture.completedFuture(Collections.unmodifiableCollection(onlinePlayers.values()));
    }

    @Override
    public int getOnlineCount() {
        return onlinePlayers.size();
    }

    @Override
    public CompletableFuture<Integer> getNetworkOnlineCount() {
        return plugin.getRedisManager().scard("ess:online")
                .thenApply(Long::intValue);
    }

    @Override
    public CompletableFuture<Boolean> isOnlineNetwork(@NotNull UUID uuid) {
        return plugin.getRedisManager().sismember("ess:online", uuid.toString());
    }

    @Override
    public CompletableFuture<Optional<String>> getPlayerServer(@NotNull UUID uuid) {
        // Check all server sets to find the player
        return plugin.getRedisManager().get("ess:player:" + uuid + ":server")
                .thenApply(Optional::ofNullable);
    }

    @Override
    public CompletableFuture<IslandiumPlayer> getOrCreate(@NotNull UUID uuid, @NotNull String name) {
        return repository.findById(uuid).thenCompose(opt -> {
            if (opt.isPresent()) {
                return CompletableFuture.completedFuture(
                        new IslandiumPlayerImpl(plugin, opt.get(), null)
                );
            } else {
                // Create new player
                PlayerData newData = PlayerData.createNew(
                        uuid,
                        name,
                        plugin.getConfigManager().getMainConfig().getStartingBalance()
                );
                return repository.save(newData).thenApply(saved ->
                        new IslandiumPlayerImpl(plugin, saved, null)
                );
            }
        });
    }

    // === Player lifecycle ===

    /**
     * Called when a player joins the server.
     */
    public CompletableFuture<IslandiumPlayerImpl> handlePlayerJoin(
            @NotNull UUID uuid,
            @NotNull String name,
            @NotNull Object hytalePlayer // PlayerRef or Player from Hytale API
    ) {
        return getOrCreate(uuid, name).thenApply(player -> {
            IslandiumPlayerImpl impl = (IslandiumPlayerImpl) player;
            impl.setHytalePlayer(hytalePlayer);

            // Update cache
            onlinePlayers.put(uuid, impl);
            nameToUuid.put(name.toLowerCase(), uuid);

            // Update last login
            impl.getData().setLastLogin(System.currentTimeMillis());
            impl.getData().setLastServer(plugin.getServerName());

            // Save to database
            impl.save();

            // Publish to Redis
            plugin.getRedisManager().getPublisher().publishPlayerJoin(
                    uuid.toString(),
                    name,
                    plugin.getServerName()
            );

            return impl;
        });
    }

    /**
     * Called when a player leaves the server.
     */
    public void handlePlayerQuit(@NotNull UUID uuid) {
        IslandiumPlayerImpl player = onlinePlayers.remove(uuid);
        if (player != null) {
            nameToUuid.remove(player.getName().toLowerCase());

            // Save player data
            player.save();

            // Publish to Redis
            plugin.getRedisManager().getPublisher().publishPlayerQuit(
                    uuid.toString(),
                    plugin.getServerName()
            );
        }
    }

    /**
     * Saves all online players.
     */
    public CompletableFuture<Void> saveAll() {
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (IslandiumPlayerImpl player : onlinePlayers.values()) {
            futures.add(player.save());
        }
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
    }

    // === Additional methods for commands/listeners ===

    /**
     * Loads a player into the cache.
     */
    public CompletableFuture<IslandiumPlayerImpl> loadPlayer(@NotNull UUID uuid, @NotNull String name) {
        return getOrCreate(uuid, name).thenApply(player -> {
            IslandiumPlayerImpl impl = (IslandiumPlayerImpl) player;
            onlinePlayers.put(uuid, impl);
            nameToUuid.put(name.toLowerCase(), uuid);

            impl.getData().setLastLogin(System.currentTimeMillis());
            impl.getData().setLastServer(plugin.getServerName());
            impl.save();

            return impl;
        });
    }

    /**
     * Unloads a player from the cache.
     */
    public CompletableFuture<Void> unloadPlayer(@NotNull UUID uuid) {
        IslandiumPlayerImpl player = onlinePlayers.remove(uuid);
        if (player != null) {
            nameToUuid.remove(player.getName().toLowerCase());
            return player.save();
        }
        return CompletableFuture.completedFuture(null);
    }

    /**
     * Gets a player's UUID by name.
     */
    public CompletableFuture<Optional<UUID>> getPlayerUUID(@NotNull String name) {
        UUID cached = nameToUuid.get(name.toLowerCase());
        if (cached != null) {
            return CompletableFuture.completedFuture(Optional.of(cached));
        }
        return repository.findByUsername(name).thenApply(opt -> opt.map(PlayerData::uuid));
    }

    /**
     * Gets a player's name by UUID.
     */
    public CompletableFuture<Optional<String>> getPlayerName(@NotNull UUID uuid) {
        IslandiumPlayerImpl cached = onlinePlayers.get(uuid);
        if (cached != null) {
            return CompletableFuture.completedFuture(Optional.of(cached.getName()));
        }
        return repository.findById(uuid).thenApply(opt -> opt.map(PlayerData::username));
    }

    /**
     * Gets offline player data.
     */
    public CompletableFuture<Optional<PlayerData>> getOfflinePlayerData(@NotNull String name) {
        return repository.findByUsername(name);
    }
}
