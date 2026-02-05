package com.islandium.core.service.permission;

import com.islandium.core.api.permission.Rank;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cache pour les ranks et permissions des joueurs.
 */
public class PermissionCache {

    // Cache des ranks par nom
    private final Map<String, RankImpl> ranksByName = new ConcurrentHashMap<>();

    // Cache des ranks par ID
    private final Map<Integer, RankImpl> ranksById = new ConcurrentHashMap<>();

    // Cache des permissions joueurs avec expiration
    private final Map<UUID, CachedPlayerPermissions> playerCache = new ConcurrentHashMap<>();

    // Duree de vie du cache joueur en millisecondes (5 minutes)
    private static final long PLAYER_CACHE_TTL = 5 * 60 * 1000;

    /**
     * Charge tous les ranks dans le cache.
     */
    public void loadRanks(@NotNull List<RankImpl> ranks) {
        ranksByName.clear();
        ranksById.clear();

        for (RankImpl rank : ranks) {
            ranksByName.put(rank.getName().toLowerCase(), rank);
            ranksById.put(rank.getId(), rank);
        }

        // Resoudre les references parent
        resolveParentReferences();
    }

    /**
     * Resout les references parent pour l'heritage.
     */
    private void resolveParentReferences() {
        for (RankImpl rank : ranksByName.values()) {
            if (rank.getParentId() != null) {
                RankImpl parent = ranksById.get(rank.getParentId());
                rank.setParent(parent);
            } else {
                rank.setParent(null);
            }
        }
    }

    /**
     * Ajoute ou met a jour un rank dans le cache.
     */
    public void putRank(@NotNull RankImpl rank) {
        ranksByName.put(rank.getName().toLowerCase(), rank);
        ranksById.put(rank.getId(), rank);
        resolveParentReferences();
    }

    /**
     * Retire un rank du cache.
     */
    public void removeRank(@NotNull String name) {
        RankImpl rank = ranksByName.remove(name.toLowerCase());
        if (rank != null) {
            ranksById.remove(rank.getId());
            resolveParentReferences();
        }
    }

    /**
     * Obtient un rank par nom.
     */
    @Nullable
    public RankImpl getRankByName(@NotNull String name) {
        return ranksByName.get(name.toLowerCase());
    }

    /**
     * Obtient un rank par ID.
     */
    @Nullable
    public RankImpl getRankById(int id) {
        return ranksById.get(id);
    }

    /**
     * Obtient le rank par defaut.
     */
    @Nullable
    public RankImpl getDefaultRank() {
        return ranksByName.values().stream()
                .filter(RankImpl::isDefault)
                .findFirst()
                .orElse(null);
    }

    /**
     * Obtient tous les ranks tries par priorite.
     */
    @NotNull
    public List<RankImpl> getAllRanks() {
        List<RankImpl> ranks = new ArrayList<>(ranksByName.values());
        ranks.sort(Comparator.comparingInt(RankImpl::getPriority).reversed());
        return ranks;
    }

    /**
     * Verifie si un rank existe.
     */
    public boolean hasRank(@NotNull String name) {
        return ranksByName.containsKey(name.toLowerCase());
    }

    /**
     * Cache les permissions d'un joueur.
     */
    public void cachePlayer(@NotNull UUID uuid, @NotNull PlayerPermissionsImpl permissions) {
        playerCache.put(uuid, new CachedPlayerPermissions(permissions, System.currentTimeMillis()));
    }

    /**
     * Obtient les permissions cachees d'un joueur.
     */
    @Nullable
    public PlayerPermissionsImpl getPlayerCache(@NotNull UUID uuid) {
        CachedPlayerPermissions cached = playerCache.get(uuid);
        if (cached == null) {
            return null;
        }

        // Verifier si le cache a expire
        if (System.currentTimeMillis() - cached.cachedAt > PLAYER_CACHE_TTL) {
            playerCache.remove(uuid);
            return null;
        }

        return cached.permissions;
    }

    /**
     * Invalide le cache d'un joueur.
     */
    public void invalidatePlayer(@NotNull UUID uuid) {
        playerCache.remove(uuid);
    }

    /**
     * Invalide le cache d'un rank.
     */
    public void invalidateRank(@NotNull String name) {
        // Invalider tous les joueurs car ils pourraient avoir ce rank
        playerCache.clear();
    }

    /**
     * Vide tous les caches.
     */
    public void clear() {
        ranksByName.clear();
        ranksById.clear();
        playerCache.clear();
    }

    /**
     * Nettoie les entries expirees du cache joueur.
     */
    public void cleanupExpired() {
        long now = System.currentTimeMillis();
        playerCache.entrySet().removeIf(entry ->
                now - entry.getValue().cachedAt > PLAYER_CACHE_TTL
        );
    }

    /**
     * Obtient les ranks a partir d'une liste d'IDs.
     */
    @NotNull
    public Set<Rank> getRanksByIds(@NotNull Set<Integer> ids) {
        Set<Rank> ranks = new HashSet<>();
        for (Integer id : ids) {
            RankImpl rank = ranksById.get(id);
            if (rank != null) {
                ranks.add(rank);
            }
        }
        return ranks;
    }

    /**
     * Wrapper pour les permissions cachees avec timestamp.
     */
    private record CachedPlayerPermissions(PlayerPermissionsImpl permissions, long cachedAt) {}
}
