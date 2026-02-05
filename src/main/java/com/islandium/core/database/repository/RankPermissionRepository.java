package com.islandium.core.database.repository;

import com.islandium.core.database.SQLExecutor;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * Repository pour les permissions des ranks.
 */
public class RankPermissionRepository {

    private final SQLExecutor sql;

    public RankPermissionRepository(@NotNull SQLExecutor sql) {
        this.sql = sql;
    }

    /**
     * Obtient toutes les permissions d'un rank.
     */
    public CompletableFuture<Set<String>> findByRankId(int rankId) {
        return sql.queryList(
                "SELECT permission FROM essentials_rank_permissions WHERE rank_id = ? AND value = 1",
                rs -> {
                    try {
                        return rs.getString("permission");
                    } catch (SQLException e) {
                        throw new RuntimeException(e);
                    }
                },
                rankId
        ).thenApply(HashSet::new);
    }

    /**
     * Ajoute une permission a un rank.
     */
    public CompletableFuture<Void> add(int rankId, @NotNull String permission) {
        return sql.execute("""
            INSERT INTO essentials_rank_permissions (rank_id, permission, value)
            VALUES (?, ?, 1)
            ON DUPLICATE KEY UPDATE value = 1
        """, rankId, permission);
    }

    /**
     * Retire une permission d'un rank.
     */
    public CompletableFuture<Void> remove(int rankId, @NotNull String permission) {
        return sql.execute(
                "DELETE FROM essentials_rank_permissions WHERE rank_id = ? AND permission = ?",
                rankId,
                permission
        );
    }

    /**
     * Verifie si un rank a une permission.
     */
    public CompletableFuture<Boolean> exists(int rankId, @NotNull String permission) {
        return sql.queryExists(
                "SELECT 1 FROM essentials_rank_permissions WHERE rank_id = ? AND permission = ? AND value = 1",
                rankId,
                permission
        );
    }

    /**
     * Supprime toutes les permissions d'un rank.
     */
    public CompletableFuture<Void> deleteAllByRankId(int rankId) {
        return sql.execute(
                "DELETE FROM essentials_rank_permissions WHERE rank_id = ?",
                rankId
        );
    }

    /**
     * Compte les permissions d'un rank.
     */
    public CompletableFuture<Integer> countByRankId(int rankId) {
        return sql.queryInt(
                "SELECT COUNT(*) FROM essentials_rank_permissions WHERE rank_id = ? AND value = 1",
                rankId
        );
    }
}
