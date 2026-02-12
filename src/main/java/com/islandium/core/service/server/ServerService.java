package com.islandium.core.service.server;

import com.islandium.core.IslandiumPlugin;
import com.islandium.core.database.repository.ServerRepository;
import com.islandium.core.server.ServerData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service de gestion des serveurs avec cache local.
 */
public class ServerService {

    private final IslandiumPlugin plugin;
    private final ServerRepository repository;
    private final Map<String, ServerData> cache = new ConcurrentHashMap<>();

    public ServerService(@NotNull IslandiumPlugin plugin, @NotNull ServerRepository repository) {
        this.plugin = plugin;
        this.repository = repository;
    }

    /**
     * Charge tous les serveurs depuis la BDD dans le cache.
     */
    public CompletableFuture<Void> loadServers() {
        return repository.findAll().thenAccept(servers -> {
            cache.clear();
            for (ServerData server : servers) {
                cache.put(server.getName().toLowerCase(), server);
            }
            System.out.println("[ISLANDIUM] Loaded " + cache.size() + " servers from database");
        });
    }

    /**
     * Retourne tous les serveurs (depuis le cache).
     */
    @NotNull
    public Map<String, ServerData> getServers() {
        return Collections.unmodifiableMap(cache);
    }

    /**
     * Retourne un serveur par nom (depuis le cache).
     */
    @Nullable
    public ServerData getServer(@NotNull String name) {
        return cache.get(name.toLowerCase());
    }

    /**
     * Ajoute un nouveau serveur.
     */
    public CompletableFuture<ServerData> addServer(@NotNull String name, @NotNull String host, int port, @NotNull String displayName) {
        ServerData data = new ServerData(name.toLowerCase(), host, port, displayName, System.currentTimeMillis());
        return repository.save(data).thenApply(saved -> {
            cache.put(saved.getName().toLowerCase(), saved);
            return saved;
        });
    }

    /**
     * Met a jour un serveur existant.
     */
    public CompletableFuture<ServerData> updateServer(@NotNull ServerData data) {
        return repository.save(data).thenApply(saved -> {
            cache.put(saved.getName().toLowerCase(), saved);
            return saved;
        });
    }

    /**
     * Supprime un serveur.
     */
    public CompletableFuture<Boolean> removeServer(@NotNull String name) {
        return repository.deleteByName(name.toLowerCase()).thenApply(result -> {
            cache.remove(name.toLowerCase());
            return result;
        });
    }

    /**
     * Verifie si un serveur existe.
     */
    public boolean exists(@NotNull String name) {
        return cache.containsKey(name.toLowerCase());
    }
}
