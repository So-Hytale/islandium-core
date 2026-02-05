package com.islandium.core.api.permission;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Service de gestion des permissions et ranks.
 */
public interface PermissionService {

    // ========================================
    // Gestion des Ranks
    // ========================================

    /**
     * Récupère un rank par son nom.
     */
    CompletableFuture<Optional<Rank>> getRank(@NotNull String name);

    /**
     * Récupère un rank par son ID.
     */
    CompletableFuture<Optional<Rank>> getRankById(int id);

    /**
     * @return tous les ranks
     */
    CompletableFuture<List<Rank>> getAllRanks();

    /**
     * @return le rank par défaut
     */
    CompletableFuture<Optional<Rank>> getDefaultRank();

    /**
     * Crée un nouveau rank.
     */
    CompletableFuture<Rank> createRank(
            @NotNull String name,
            @NotNull String displayName,
            @Nullable String prefix,
            @Nullable String color,
            int priority
    );

    /**
     * Supprime un rank.
     */
    CompletableFuture<Boolean> deleteRank(@NotNull String name);

    /**
     * Met à jour un rank.
     */
    CompletableFuture<Rank> updateRank(@NotNull Rank rank);

    /**
     * Définit le parent d'un rank.
     */
    CompletableFuture<Void> setRankParent(@NotNull String rankName, @Nullable String parentName);

    /**
     * Définit le rank par défaut.
     */
    CompletableFuture<Void> setDefaultRank(@NotNull String rankName);

    // ========================================
    // Permissions de Rank
    // ========================================

    /**
     * Ajoute une permission à un rank.
     */
    CompletableFuture<Void> addRankPermission(@NotNull String rankName, @NotNull String permission);

    /**
     * Retire une permission d'un rank.
     */
    CompletableFuture<Void> removeRankPermission(@NotNull String rankName, @NotNull String permission);

    /**
     * @return les permissions d'un rank
     */
    CompletableFuture<Set<String>> getRankPermissions(@NotNull String rankName);

    // ========================================
    // Gestion des Joueurs
    // ========================================

    /**
     * @return les permissions d'un joueur
     */
    CompletableFuture<PlayerPermissions> getPlayerPermissions(@NotNull UUID playerUuid);

    /**
     * Ajoute un rank à un joueur.
     */
    CompletableFuture<Void> addPlayerRank(
            @NotNull UUID playerUuid,
            @NotNull String rankName,
            @Nullable Long expiresAt,
            @Nullable UUID assignedBy
    );

    /**
     * Retire un rank d'un joueur.
     */
    CompletableFuture<Void> removePlayerRank(@NotNull UUID playerUuid, @NotNull String rankName);

    /**
     * @return les ranks d'un joueur
     */
    CompletableFuture<Set<Rank>> getPlayerRanks(@NotNull UUID playerUuid);

    /**
     * @return les joueurs ayant un rank spécifique
     */
    CompletableFuture<Set<UUID>> getPlayersWithRank(@NotNull String rankName);

    // ========================================
    // Permissions Personnelles
    // ========================================

    /**
     * Ajoute une permission personnelle à un joueur.
     */
    CompletableFuture<Void> addPlayerPermission(
            @NotNull UUID playerUuid,
            @NotNull String permission,
            @Nullable Long expiresAt
    );

    /**
     * Retire une permission personnelle d'un joueur.
     */
    CompletableFuture<Void> removePlayerPermission(@NotNull UUID playerUuid, @NotNull String permission);

    /**
     * @return les permissions personnelles d'un joueur
     */
    CompletableFuture<Set<String>> getPlayerPersonalPermissions(@NotNull UUID playerUuid);

    // ========================================
    // Vérification
    // ========================================

    /**
     * Vérifie si un joueur a une permission.
     */
    CompletableFuture<Boolean> hasPermission(@NotNull UUID playerUuid, @NotNull String permission);

    // ========================================
    // Cache
    // ========================================

    /**
     * Invalide le cache d'un joueur.
     */
    void invalidatePlayerCache(@NotNull UUID playerUuid);

    /**
     * Invalide le cache d'un rank.
     */
    void invalidateRankCache(@NotNull String rankName);

    /**
     * Recharge toutes les données.
     */
    CompletableFuture<Void> reloadAll();
}
