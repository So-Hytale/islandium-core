package com.islandium.core.database;

import com.islandium.core.IslandiumPlugin;
import com.islandium.core.config.MainConfig;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.jetbrains.annotations.NotNull;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

/**
 * Gestionnaire de base de données avec HikariCP.
 */
public class DatabaseManager {

    private final IslandiumPlugin plugin;
    private final MainConfig config;
    private HikariDataSource dataSource;
    private SQLExecutor executor;

    public DatabaseManager(@NotNull IslandiumPlugin plugin, @NotNull MainConfig config) {
        this.plugin = plugin;
        this.config = config;
    }

    /**
     * Connecte à la base de données.
     */
    public CompletableFuture<Void> connect() {
        return CompletableFuture.runAsync(() -> {
            try {
                // Charger le driver MySQL relocalisé explicitement
                // Le driver est relocalisé en com.islandium.libs.mysql par shadowJar
                Class.forName("com.islandium.libs.mysql.cj.jdbc.Driver");

                HikariConfig hikariConfig = new HikariConfig();
                hikariConfig.setDriverClassName("com.islandium.libs.mysql.cj.jdbc.Driver");

                String jdbcUrl = String.format("jdbc:mysql://%s:%d/%s?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC",
                        config.getDatabaseHost(),
                        config.getDatabasePort(),
                        config.getDatabaseName()
                );

                hikariConfig.setJdbcUrl(jdbcUrl);
                hikariConfig.setUsername(config.getDatabaseUsername());
                hikariConfig.setPassword(config.getDatabasePassword());

                hikariConfig.setMaximumPoolSize(config.getDatabasePoolSize());
                hikariConfig.setMinimumIdle(2);
                hikariConfig.setIdleTimeout(300000); // 5 minutes
                hikariConfig.setConnectionTimeout(10000); // 10 seconds
                hikariConfig.setMaxLifetime(600000); // 10 minutes

                hikariConfig.setPoolName("Essentials-Pool");

                // Performance optimizations
                hikariConfig.addDataSourceProperty("cachePrepStmts", "true");
                hikariConfig.addDataSourceProperty("prepStmtCacheSize", "250");
                hikariConfig.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
                hikariConfig.addDataSourceProperty("useServerPrepStmts", "true");
                hikariConfig.addDataSourceProperty("useLocalSessionState", "true");
                hikariConfig.addDataSourceProperty("rewriteBatchedStatements", "true");
                hikariConfig.addDataSourceProperty("cacheResultSetMetadata", "true");
                hikariConfig.addDataSourceProperty("cacheServerConfiguration", "true");
                hikariConfig.addDataSourceProperty("elideSetAutoCommits", "true");
                hikariConfig.addDataSourceProperty("maintainTimeStats", "false");

                this.dataSource = new HikariDataSource(hikariConfig);
                this.executor = new SQLExecutor(this);

                plugin.log(Level.INFO, "Database connection established!");

            } catch (Exception e) {
                plugin.log(Level.SEVERE, "Failed to connect to database: " + e.getMessage());
                throw new RuntimeException("Database connection failed", e);
            }
        });
    }

    /**
     * Déconnecte de la base de données.
     */
    public void disconnect() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            plugin.log(Level.INFO, "Database connection closed.");
        }
    }

    /**
     * Récupère une connexion du pool.
     */
    @NotNull
    public Connection getConnection() throws SQLException {
        if (dataSource == null || dataSource.isClosed()) {
            throw new SQLException("Database connection pool is not available!");
        }
        return dataSource.getConnection();
    }

    /**
     * @return l'exécuteur SQL async
     */
    @NotNull
    public SQLExecutor getExecutor() {
        return executor;
    }

    /**
     * Vérifie si la connexion est active.
     */
    public boolean isConnected() {
        return dataSource != null && !dataSource.isClosed();
    }

    /**
     * Exécute les migrations de base de données.
     */
    public CompletableFuture<Void> runMigrations() {
        return executor.execute("""
            -- Table joueurs (vanish, godMode, location sont gérés nativement par Hytale)
            CREATE TABLE IF NOT EXISTS essentials_players (
                uuid CHAR(36) PRIMARY KEY,
                username VARCHAR(16) NOT NULL,
                balance DECIMAL(15,2) DEFAULT 0.00,
                first_login BIGINT NOT NULL,
                last_login BIGINT NOT NULL,
                last_server VARCHAR(64),
                INDEX idx_username (username)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
        """).thenCompose(v -> executor.execute("""
            -- Table homes
            CREATE TABLE IF NOT EXISTS essentials_homes (
                id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
                player_uuid CHAR(36) NOT NULL,
                name VARCHAR(32) NOT NULL,
                server VARCHAR(64) NOT NULL,
                world VARCHAR(64) NOT NULL,
                x DOUBLE NOT NULL,
                y DOUBLE NOT NULL,
                z DOUBLE NOT NULL,
                yaw FLOAT NOT NULL,
                pitch FLOAT NOT NULL,
                created_at BIGINT NOT NULL,
                UNIQUE KEY uk_player_home (player_uuid, name),
                INDEX idx_player (player_uuid)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
        """)).thenCompose(v -> executor.execute("""
            -- Table warps
            CREATE TABLE IF NOT EXISTS essentials_warps (
                name VARCHAR(32) PRIMARY KEY,
                server VARCHAR(64) NOT NULL,
                world VARCHAR(64) NOT NULL,
                x DOUBLE NOT NULL,
                y DOUBLE NOT NULL,
                z DOUBLE NOT NULL,
                yaw FLOAT NOT NULL,
                pitch FLOAT NOT NULL,
                permission VARCHAR(128),
                created_by CHAR(36),
                created_at BIGINT NOT NULL
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
        """)).thenCompose(v -> executor.execute("""
            -- Table spawn
            CREATE TABLE IF NOT EXISTS essentials_spawn (
                server VARCHAR(64) PRIMARY KEY,
                world VARCHAR(64) NOT NULL,
                x DOUBLE NOT NULL,
                y DOUBLE NOT NULL,
                z DOUBLE NOT NULL,
                yaw FLOAT NOT NULL,
                pitch FLOAT NOT NULL
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
        """)).thenCompose(v -> executor.execute("""
            -- Table kits
            CREATE TABLE IF NOT EXISTS essentials_kits (
                name VARCHAR(32) PRIMARY KEY,
                cooldown_seconds INT UNSIGNED DEFAULT 0,
                permission VARCHAR(128),
                items TEXT NOT NULL,
                created_at BIGINT NOT NULL
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
        """)).thenCompose(v -> executor.execute("""
            -- Table usage kits
            CREATE TABLE IF NOT EXISTS essentials_kit_cooldowns (
                player_uuid CHAR(36) NOT NULL,
                kit_name VARCHAR(32) NOT NULL,
                last_used BIGINT NOT NULL,
                PRIMARY KEY (player_uuid, kit_name)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
        """)).thenCompose(v -> executor.execute("""
            -- Table punitions
            CREATE TABLE IF NOT EXISTS essentials_punishments (
                id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
                player_uuid CHAR(36) NOT NULL,
                type ENUM('BAN', 'MUTE', 'KICK') NOT NULL,
                reason TEXT,
                punisher_uuid CHAR(36),
                created_at BIGINT NOT NULL,
                expires_at BIGINT,
                revoked TINYINT(1) DEFAULT 0,
                revoked_by CHAR(36),
                revoked_at BIGINT,
                INDEX idx_player_type (player_uuid, type),
                INDEX idx_active (player_uuid, type, revoked, expires_at)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
        """)).thenCompose(v -> executor.execute("""
            -- Table transactions
            CREATE TABLE IF NOT EXISTS essentials_transactions (
                id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
                from_uuid CHAR(36),
                to_uuid CHAR(36),
                amount DECIMAL(15,2) NOT NULL,
                type ENUM('TRANSFER', 'ADMIN_SET', 'ADMIN_ADD', 'ADMIN_REMOVE') NOT NULL,
                description VARCHAR(255),
                created_at BIGINT NOT NULL,
                INDEX idx_from (from_uuid),
                INDEX idx_to (to_uuid)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
        """)).thenCompose(v -> executor.execute("""
            -- Table ranks (groupes de permissions)
            CREATE TABLE IF NOT EXISTS essentials_ranks (
                id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
                name VARCHAR(32) NOT NULL UNIQUE,
                display_name VARCHAR(64) NOT NULL,
                prefix VARCHAR(64),
                color VARCHAR(16) DEFAULT '#FFFFFF',
                priority INT DEFAULT 0,
                parent_id INT UNSIGNED,
                is_default TINYINT(1) DEFAULT 0,
                created_at BIGINT NOT NULL,
                FOREIGN KEY (parent_id) REFERENCES essentials_ranks(id) ON DELETE SET NULL,
                INDEX idx_priority (priority),
                INDEX idx_default (is_default)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
        """)).thenCompose(v -> executor.execute("""
            -- Table permissions des ranks
            CREATE TABLE IF NOT EXISTS essentials_rank_permissions (
                rank_id INT UNSIGNED NOT NULL,
                permission VARCHAR(128) NOT NULL,
                value TINYINT(1) DEFAULT 1,
                PRIMARY KEY (rank_id, permission),
                FOREIGN KEY (rank_id) REFERENCES essentials_ranks(id) ON DELETE CASCADE
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
        """)).thenCompose(v -> executor.execute("""
            -- Table ranks des joueurs
            CREATE TABLE IF NOT EXISTS essentials_player_ranks (
                player_uuid CHAR(36) NOT NULL,
                rank_id INT UNSIGNED NOT NULL,
                assigned_at BIGINT NOT NULL,
                expires_at BIGINT,
                assigned_by CHAR(36),
                PRIMARY KEY (player_uuid, rank_id),
                FOREIGN KEY (rank_id) REFERENCES essentials_ranks(id) ON DELETE CASCADE,
                INDEX idx_player (player_uuid),
                INDEX idx_expires (expires_at)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
        """)).thenCompose(v -> executor.execute("""
            -- Table permissions personnelles des joueurs
            CREATE TABLE IF NOT EXISTS essentials_player_permissions (
                player_uuid CHAR(36) NOT NULL,
                permission VARCHAR(128) NOT NULL,
                value TINYINT(1) DEFAULT 1,
                expires_at BIGINT,
                PRIMARY KEY (player_uuid, permission),
                INDEX idx_player (player_uuid),
                INDEX idx_expires (expires_at)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
        """)).thenRun(() -> plugin.log(Level.INFO, "Database migrations completed!"));
    }
}
