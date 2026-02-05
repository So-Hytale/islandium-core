package com.islandium.core.database.repository;

import com.islandium.core.database.SQLExecutor;
import com.islandium.core.player.PlayerData;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Repository pour les données des joueurs.
 */
public class PlayerRepository extends AbstractRepository<PlayerData, UUID> {

    public PlayerRepository(@NotNull SQLExecutor sql) {
        super(sql);
    }

    @Override
    protected String getTableName() {
        return "essentials_players";
    }

    @Override
    public CompletableFuture<Optional<PlayerData>> findById(@NotNull UUID id) {
        return sql.queryOne(
                "SELECT * FROM essentials_players WHERE uuid = ?",
                this::mapRow,
                id.toString()
        );
    }

    public CompletableFuture<Optional<PlayerData>> findByUsername(@NotNull String username) {
        return sql.queryOne(
                "SELECT * FROM essentials_players WHERE username = ?",
                this::mapRow,
                username
        );
    }

    @Override
    public CompletableFuture<List<PlayerData>> findAll() {
        return sql.queryList(
                "SELECT * FROM essentials_players",
                this::mapRow
        );
    }

    @Override
    public CompletableFuture<PlayerData> save(@NotNull PlayerData entity) {
        // Note: vanish, godMode, location sont gérés nativement par Hytale
        return sql.execute("""
            INSERT INTO essentials_players (uuid, username, balance, first_login, last_login, last_server)
            VALUES (?, ?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE
                username = VALUES(username),
                balance = VALUES(balance),
                last_login = VALUES(last_login),
                last_server = VALUES(last_server)
        """,
                entity.getUuid().toString(),
                entity.getUsername(),
                entity.getBalance(),
                entity.getFirstLogin(),
                entity.getLastLogin(),
                entity.getLastServer()
        ).thenApply(v -> entity);
    }

    @Override
    public CompletableFuture<Boolean> deleteById(@NotNull UUID id) {
        return sql.execute(
                "DELETE FROM essentials_players WHERE uuid = ?",
                id.toString()
        ).thenApply(v -> true).exceptionally(e -> false);
    }

    @Override
    public CompletableFuture<Boolean> existsById(@NotNull UUID id) {
        return sql.queryExists(
                "SELECT 1 FROM essentials_players WHERE uuid = ?",
                id.toString()
        );
    }

    @Override
    public CompletableFuture<Long> count() {
        return sql.queryLong("SELECT COUNT(*) FROM essentials_players");
    }

    public CompletableFuture<Void> updateBalance(@NotNull UUID uuid, @NotNull BigDecimal balance) {
        return sql.execute(
                "UPDATE essentials_players SET balance = ? WHERE uuid = ?",
                balance,
                uuid.toString()
        );
    }

    public CompletableFuture<Void> updateLastLogin(@NotNull UUID uuid, long lastLogin, @NotNull String server) {
        return sql.execute(
                "UPDATE essentials_players SET last_login = ?, last_server = ? WHERE uuid = ?",
                lastLogin,
                server,
                uuid.toString()
        );
    }

    public CompletableFuture<List<UUID>> getTopByBalance(int limit) {
        return sql.queryList(
                "SELECT uuid FROM essentials_players ORDER BY balance DESC LIMIT ?",
                rs -> {
                    try {
                        return UUID.fromString(rs.getString("uuid"));
                    } catch (SQLException e) {
                        throw new RuntimeException(e);
                    }
                },
                limit
        );
    }

    private PlayerData mapRow(ResultSet rs) {
        try {
            // Note: vanish, godMode, location sont gérés nativement par Hytale
            return new PlayerData(
                    UUID.fromString(rs.getString("uuid")),
                    rs.getString("username"),
                    rs.getBigDecimal("balance"),
                    rs.getLong("first_login"),
                    rs.getLong("last_login"),
                    rs.getString("last_server")
            );
        } catch (SQLException e) {
            throw new RuntimeException("Failed to map player data", e);
        }
    }
}
