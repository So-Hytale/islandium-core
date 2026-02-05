package com.islandium.core.database.repository;

import com.islandium.core.database.SQLExecutor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Repository pour les permissions personnelles des joueurs.
 */
public class PlayerPermissionRepository {

    private final SQLExecutor sql;

    public PlayerPermissionRepository(@NotNull SQLExecutor sql) {
        this.sql = sql;
    }

    /**
     * Obtient toutes les permissions personnelles d'un joueur (non expirees).
     */
    public CompletableFuture<Set<String>> findByPlayer(@NotNull UUID playerUuid) {
        long now = System.currentTimeMillis();
        return sql.queryList(
                """
                SELECT permission FROM essentials_player_permissions
                WHERE player_uuid = ? AND value = 1 AND (expires_at IS NULL OR expires_at > ?)
                """,
                rs -> {
                    try {
                        return rs.getString("permission");
                    } catch (SQLException e) {
                        throw new RuntimeException(e);
                    }
                },
                playerUuid.toString(),
                now
        ).thenApply(HashSet::new);
    }

    /**
     * Ajoute une permission personnelle a un joueur.
     */
    public CompletableFuture<Void> add(
            @NotNull UUID playerUuid,
            @NotNull String permission,
            @Nullable Long expiresAt
    ) {
        return sql.execute("""
            INSERT INTO essentials_player_permissions (player_uuid, permission, value, expires_at)
            VALUES (?, ?, 1, ?)
            ON DUPLICATE KEY UPDATE value = 1, expires_at = VALUES(expires_at)
        """,
                playerUuid.toString(),
                permission,
                expiresAt
        );
    }

    /**
     * Retire une permission personnelle d'un joueur.
     */
    public CompletableFuture<Void> remove(@NotNull UUID playerUuid, @NotNull String permission) {
        return sql.execute(
                "DELETE FROM essentials_player_permissions WHERE player_uuid = ? AND permission = ?",
                playerUuid.toString(),
                permission
        );
    }

    /**
     * Verifie si un joueur a une permission personnelle.
     */
    public CompletableFuture<Boolean> exists(@NotNull UUID playerUuid, @NotNull String permission) {
        long now = System.currentTimeMillis();
        return sql.queryExists(
                """
                SELECT 1 FROM essentials_player_permissions
                WHERE player_uuid = ? AND permission = ? AND value = 1 AND (expires_at IS NULL OR expires_at > ?)
                """,
                playerUuid.toString(),
                permission,
                now
        );
    }

    /**
     * Supprime toutes les permissions personnelles d'un joueur.
     */
    public CompletableFuture<Void> deleteAllByPlayer(@NotNull UUID playerUuid) {
        return sql.execute(
                "DELETE FROM essentials_player_permissions WHERE player_uuid = ?",
                playerUuid.toString()
        );
    }

    /**
     * Supprime les permissions expirees.
     */
    public CompletableFuture<Integer> deleteExpired() {
        long now = System.currentTimeMillis();
        return sql.queryInt(
                "DELETE FROM essentials_player_permissions WHERE expires_at IS NOT NULL AND expires_at <= ?",
                now
        );
    }

    /**
     * Compte les permissions personnelles d'un joueur.
     */
    public CompletableFuture<Integer> countByPlayer(@NotNull UUID playerUuid) {
        long now = System.currentTimeMillis();
        return sql.queryInt(
                """
                SELECT COUNT(*) FROM essentials_player_permissions
                WHERE player_uuid = ? AND value = 1 AND (expires_at IS NULL OR expires_at > ?)
                """,
                playerUuid.toString(),
                now
        );
    }
}
