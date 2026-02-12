package com.islandium.core.database.repository;

import com.islandium.core.database.SQLExecutor;
import com.islandium.core.server.ServerData;
import org.jetbrains.annotations.NotNull;

import java.sql.ResultSet;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Repository pour les serveurs.
 */
public class ServerRepository extends AbstractRepository<ServerData, String> {

    public ServerRepository(@NotNull SQLExecutor sql) {
        super(sql);
    }

    @Override
    protected String getTableName() {
        return "essentials_servers";
    }

    @Override
    public CompletableFuture<Optional<ServerData>> findById(@NotNull String name) {
        return sql.queryOne(
            "SELECT * FROM essentials_servers WHERE name = ?",
            this::mapRow,
            name
        );
    }

    public CompletableFuture<Optional<ServerData>> findByName(@NotNull String name) {
        return findById(name);
    }

    @Override
    public CompletableFuture<List<ServerData>> findAll() {
        return sql.queryList(
            "SELECT * FROM essentials_servers ORDER BY name",
            this::mapRow
        );
    }

    @Override
    public CompletableFuture<ServerData> save(@NotNull ServerData entity) {
        return sql.execute("""
            INSERT INTO essentials_servers (name, host, port, display_name, created_at)
            VALUES (?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE
                host = VALUES(host),
                port = VALUES(port),
                display_name = VALUES(display_name)
            """,
            entity.getName(),
            entity.getHost(),
            entity.getPort(),
            entity.getDisplayName(),
            entity.getCreatedAt()
        ).thenApply(v -> entity);
    }

    @Override
    public CompletableFuture<Boolean> deleteById(@NotNull String name) {
        return sql.execute(
            "DELETE FROM essentials_servers WHERE name = ?",
            name
        ).thenApply(v -> true);
    }

    public CompletableFuture<Boolean> deleteByName(@NotNull String name) {
        return deleteById(name);
    }

    @Override
    public CompletableFuture<Boolean> existsById(@NotNull String name) {
        return sql.queryExists(
            "SELECT 1 FROM essentials_servers WHERE name = ?",
            name
        );
    }

    @Override
    public CompletableFuture<Long> count() {
        return sql.queryLong("SELECT COUNT(*) FROM essentials_servers");
    }

    private ServerData mapRow(ResultSet rs) {
        try {
            return new ServerData(
                rs.getString("name"),
                rs.getString("host"),
                rs.getInt("port"),
                rs.getString("display_name"),
                rs.getLong("created_at")
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to map server row", e);
        }
    }
}
