package com.islandium.core.database;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * Exécuteur SQL asynchrone avec helpers.
 */
public class SQLExecutor {

    private final DatabaseManager databaseManager;

    public SQLExecutor(@NotNull DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    /**
     * Exécute une requête SQL (INSERT, UPDATE, DELETE, CREATE).
     */
    public CompletableFuture<Void> execute(@NotNull String sql, Object... params) {
        return CompletableFuture.runAsync(() -> {
            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                setParameters(stmt, params);
                stmt.executeUpdate();

            } catch (SQLException e) {
                throw new RuntimeException("SQL execution failed: " + sql, e);
            }
        });
    }

    /**
     * Exécute une requête SQL et retourne l'ID généré.
     */
    public CompletableFuture<Long> executeAndGetId(@NotNull String sql, Object... params) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

                setParameters(stmt, params);
                stmt.executeUpdate();

                try (ResultSet rs = stmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        return rs.getLong(1);
                    }
                    return -1L;
                }

            } catch (SQLException e) {
                throw new RuntimeException("SQL execution failed: " + sql, e);
            }
        });
    }

    /**
     * Exécute une requête SELECT et retourne un seul résultat.
     */
    public <T> CompletableFuture<Optional<T>> queryOne(
            @NotNull String sql,
            @NotNull Function<ResultSet, T> mapper,
            Object... params
    ) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                setParameters(stmt, params);

                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return Optional.ofNullable(mapper.apply(rs));
                    }
                    return Optional.empty();
                }

            } catch (SQLException e) {
                throw new RuntimeException("SQL query failed: " + sql, e);
            }
        });
    }

    /**
     * Exécute une requête SELECT et retourne une liste de résultats.
     */
    public <T> CompletableFuture<List<T>> queryList(
            @NotNull String sql,
            @NotNull Function<ResultSet, T> mapper,
            Object... params
    ) {
        return CompletableFuture.supplyAsync(() -> {
            List<T> results = new ArrayList<>();

            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                setParameters(stmt, params);

                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        T item = mapper.apply(rs);
                        if (item != null) {
                            results.add(item);
                        }
                    }
                }

            } catch (SQLException e) {
                throw new RuntimeException("SQL query failed: " + sql, e);
            }

            return results;
        });
    }

    /**
     * Exécute une requête SELECT et retourne un seul entier.
     */
    public CompletableFuture<Integer> queryInt(@NotNull String sql, Object... params) {
        return queryOne(sql, rs -> {
            try {
                return rs.getInt(1);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }, params).thenApply(opt -> opt.orElse(0));
    }

    /**
     * Exécute une requête SELECT et retourne un seul long.
     */
    public CompletableFuture<Long> queryLong(@NotNull String sql, Object... params) {
        return queryOne(sql, rs -> {
            try {
                return rs.getLong(1);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }, params).thenApply(opt -> opt.orElse(0L));
    }

    /**
     * Exécute une requête SELECT et retourne un boolean.
     */
    public CompletableFuture<Boolean> queryExists(@NotNull String sql, Object... params) {
        return queryOne(sql, rs -> true, params).thenApply(Optional::isPresent);
    }

    /**
     * Exécute un INSERT ... ON DUPLICATE KEY UPDATE.
     */
    public CompletableFuture<Void> upsert(@NotNull String sql, Object... params) {
        return execute(sql, params);
    }

    /**
     * Exécute un batch d'opérations.
     */
    public CompletableFuture<int[]> executeBatch(@NotNull String sql, @NotNull List<Object[]> batchParams) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                for (Object[] params : batchParams) {
                    setParameters(stmt, params);
                    stmt.addBatch();
                }

                return stmt.executeBatch();

            } catch (SQLException e) {
                throw new RuntimeException("SQL batch execution failed: " + sql, e);
            }
        });
    }

    /**
     * Définit les paramètres d'un PreparedStatement.
     */
    private void setParameters(PreparedStatement stmt, Object[] params) throws SQLException {
        for (int i = 0; i < params.length; i++) {
            Object param = params[i];
            int index = i + 1;

            if (param == null) {
                stmt.setNull(index, Types.NULL);
            } else if (param instanceof String s) {
                stmt.setString(index, s);
            } else if (param instanceof Integer n) {
                stmt.setInt(index, n);
            } else if (param instanceof Long l) {
                stmt.setLong(index, l);
            } else if (param instanceof Double d) {
                stmt.setDouble(index, d);
            } else if (param instanceof Float f) {
                stmt.setFloat(index, f);
            } else if (param instanceof Boolean b) {
                stmt.setBoolean(index, b);
            } else if (param instanceof java.math.BigDecimal bd) {
                stmt.setBigDecimal(index, bd);
            } else {
                stmt.setObject(index, param);
            }
        }
    }
}
