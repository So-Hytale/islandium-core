package com.islandium.core.database.repository;

import com.islandium.core.database.SQLExecutor;
import com.islandium.core.service.permission.RankImpl;
import org.jetbrains.annotations.NotNull;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Repository pour les ranks.
 */
public class RankRepository extends AbstractRepository<RankImpl, Integer> {

    public RankRepository(@NotNull SQLExecutor sql) {
        super(sql);
    }

    @Override
    protected String getTableName() {
        return "essentials_ranks";
    }

    @Override
    public CompletableFuture<Optional<RankImpl>> findById(@NotNull Integer id) {
        return sql.queryOne(
                "SELECT * FROM essentials_ranks WHERE id = ?",
                this::mapRow,
                id
        );
    }

    public CompletableFuture<Optional<RankImpl>> findByName(@NotNull String name) {
        return sql.queryOne(
                "SELECT * FROM essentials_ranks WHERE name = ?",
                this::mapRow,
                name
        );
    }

    public CompletableFuture<Optional<RankImpl>> findDefault() {
        return sql.queryOne(
                "SELECT * FROM essentials_ranks WHERE is_default = 1 LIMIT 1",
                this::mapRow
        );
    }

    @Override
    public CompletableFuture<List<RankImpl>> findAll() {
        return sql.queryList(
                "SELECT * FROM essentials_ranks ORDER BY priority DESC",
                this::mapRow
        );
    }

    public CompletableFuture<List<String>> findAllNames() {
        return sql.queryList(
                "SELECT name FROM essentials_ranks ORDER BY priority DESC",
                rs -> {
                    try {
                        return rs.getString("name");
                    } catch (SQLException e) {
                        throw new RuntimeException(e);
                    }
                }
        );
    }

    @Override
    public CompletableFuture<RankImpl> save(@NotNull RankImpl entity) {
        if (entity.getId() > 0) {
            // Update
            return sql.execute("""
                UPDATE essentials_ranks SET
                    display_name = ?,
                    prefix = ?,
                    color = ?,
                    priority = ?,
                    parent_id = ?,
                    is_default = ?
                WHERE id = ?
            """,
                    entity.getDisplayName(),
                    entity.getPrefix(),
                    entity.getColor(),
                    entity.getPriority(),
                    entity.getParentId(),
                    entity.isDefault() ? 1 : 0,
                    entity.getId()
            ).thenApply(v -> entity);
        } else {
            // Insert
            return sql.executeAndGetId("""
                INSERT INTO essentials_ranks (name, display_name, prefix, color, priority, parent_id, is_default, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """,
                    entity.getName(),
                    entity.getDisplayName(),
                    entity.getPrefix(),
                    entity.getColor(),
                    entity.getPriority(),
                    entity.getParentId(),
                    entity.isDefault() ? 1 : 0,
                    entity.getCreatedAt()
            ).thenApply(id -> {
                entity.setId(id.intValue());
                return entity;
            });
        }
    }

    @Override
    public CompletableFuture<Boolean> deleteById(@NotNull Integer id) {
        return sql.execute(
                "DELETE FROM essentials_ranks WHERE id = ?",
                id
        ).thenApply(v -> true).exceptionally(e -> false);
    }

    public CompletableFuture<Boolean> deleteByName(@NotNull String name) {
        return sql.execute(
                "DELETE FROM essentials_ranks WHERE name = ?",
                name
        ).thenApply(v -> true).exceptionally(e -> false);
    }

    @Override
    public CompletableFuture<Boolean> existsById(@NotNull Integer id) {
        return sql.queryExists(
                "SELECT 1 FROM essentials_ranks WHERE id = ?",
                id
        );
    }

    public CompletableFuture<Boolean> existsByName(@NotNull String name) {
        return sql.queryExists(
                "SELECT 1 FROM essentials_ranks WHERE name = ?",
                name
        );
    }

    @Override
    public CompletableFuture<Long> count() {
        return sql.queryLong("SELECT COUNT(*) FROM essentials_ranks");
    }

    public CompletableFuture<Void> clearDefault() {
        return sql.execute("UPDATE essentials_ranks SET is_default = 0 WHERE is_default = 1");
    }

    public CompletableFuture<Void> setDefault(@NotNull String name) {
        return clearDefault().thenCompose(v ->
                sql.execute("UPDATE essentials_ranks SET is_default = 1 WHERE name = ?", name)
        );
    }

    public CompletableFuture<Void> setParent(@NotNull String rankName, Integer parentId) {
        return sql.execute(
                "UPDATE essentials_ranks SET parent_id = ? WHERE name = ?",
                parentId,
                rankName
        );
    }

    private RankImpl mapRow(ResultSet rs) {
        try {
            Integer parentId = rs.getObject("parent_id") != null ? rs.getInt("parent_id") : null;

            return new RankImpl(
                    rs.getInt("id"),
                    rs.getString("name"),
                    rs.getString("display_name"),
                    rs.getString("prefix"),
                    rs.getString("color"),
                    rs.getInt("priority"),
                    parentId,
                    rs.getBoolean("is_default"),
                    rs.getLong("created_at")
            );
        } catch (SQLException e) {
            throw new RuntimeException("Failed to map rank", e);
        }
    }
}
