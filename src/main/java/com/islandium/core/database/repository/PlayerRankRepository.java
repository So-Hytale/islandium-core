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
 * Repository pour les ranks des joueurs.
 */
public class PlayerRankRepository {

    private final SQLExecutor sql;

    public PlayerRankRepository(@NotNull SQLExecutor sql) {
        this.sql = sql;
    }

    /**
     * Obtient les IDs des ranks d'un joueur (non expires).
     */
    public CompletableFuture<Set<Integer>> findRankIdsByPlayer(@NotNull UUID playerUuid) {
        long now = System.currentTimeMillis();
        return sql.queryList(
                """
                SELECT rank_id FROM essentials_player_ranks
                WHERE player_uuid = ? AND (expires_at IS NULL OR expires_at > ?)
                """,
                rs -> {
                    try {
                        return rs.getInt("rank_id");
                    } catch (SQLException e) {
                        throw new RuntimeException(e);
                    }
                },
                playerUuid.toString(),
                now
        ).thenApply(HashSet::new);
    }

    /**
     * Ajoute un rank a un joueur.
     */
    public CompletableFuture<Void> add(
            @NotNull UUID playerUuid,
            int rankId,
            @Nullable Long expiresAt,
            @Nullable UUID assignedBy
    ) {
        long now = System.currentTimeMillis();
        return sql.execute("""
            INSERT INTO essentials_player_ranks (player_uuid, rank_id, assigned_at, expires_at, assigned_by)
            VALUES (?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE
                expires_at = VALUES(expires_at),
                assigned_by = VALUES(assigned_by),
                assigned_at = VALUES(assigned_at)
        """,
                playerUuid.toString(),
                rankId,
                now,
                expiresAt,
                assignedBy != null ? assignedBy.toString() : null
        );
    }

    /**
     * Retire un rank d'un joueur.
     */
    public CompletableFuture<Void> remove(@NotNull UUID playerUuid, int rankId) {
        return sql.execute(
                "DELETE FROM essentials_player_ranks WHERE player_uuid = ? AND rank_id = ?",
                playerUuid.toString(),
                rankId
        );
    }

    /**
     * Verifie si un joueur a un rank specifique.
     */
    public CompletableFuture<Boolean> exists(@NotNull UUID playerUuid, int rankId) {
        long now = System.currentTimeMillis();
        return sql.queryExists(
                """
                SELECT 1 FROM essentials_player_ranks
                WHERE player_uuid = ? AND rank_id = ? AND (expires_at IS NULL OR expires_at > ?)
                """,
                playerUuid.toString(),
                rankId,
                now
        );
    }

    /**
     * Supprime tous les ranks d'un joueur.
     */
    public CompletableFuture<Void> deleteAllByPlayer(@NotNull UUID playerUuid) {
        return sql.execute(
                "DELETE FROM essentials_player_ranks WHERE player_uuid = ?",
                playerUuid.toString()
        );
    }

    /**
     * Supprime les ranks expires.
     */
    public CompletableFuture<Integer> deleteExpired() {
        long now = System.currentTimeMillis();
        return sql.queryInt(
                "DELETE FROM essentials_player_ranks WHERE expires_at IS NOT NULL AND expires_at <= ?",
                now
        );
    }

    /**
     * Obtient les UUIDs des joueurs ayant un rank specifique.
     */
    public CompletableFuture<Set<UUID>> findPlayersByRankId(int rankId) {
        long now = System.currentTimeMillis();
        return sql.queryList(
                """
                SELECT player_uuid FROM essentials_player_ranks
                WHERE rank_id = ? AND (expires_at IS NULL OR expires_at > ?)
                """,
                rs -> {
                    try {
                        return UUID.fromString(rs.getString("player_uuid"));
                    } catch (SQLException e) {
                        throw new RuntimeException(e);
                    }
                },
                rankId,
                now
        ).thenApply(HashSet::new);
    }
}
