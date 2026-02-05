package com.islandium.core.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Configuration principale (config.yml).
 */
public class MainConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final Path path;
    private ConfigData data;

    public MainConfig(@NotNull Path path) {
        this.path = path;
    }

    public void load() throws IOException {
        if (Files.exists(path)) {
            String content = Files.readString(path);
            this.data = GSON.fromJson(content, ConfigData.class);
        } else {
            this.data = createDefault();
            save();
        }
    }

    public void save() throws IOException {
        Files.writeString(path, GSON.toJson(data));
    }

    private ConfigData createDefault() {
        ConfigData config = new ConfigData();
        config.serverName = "server-1";

        config.database = new DatabaseConfig();
        config.database.host = "localhost";
        config.database.port = 3306;
        config.database.database = "essentials";
        config.database.username = "root";
        config.database.password = "password";
        config.database.poolSize = 10;

        config.redis = new RedisConfig();
        config.redis.host = "localhost";
        config.redis.port = 6379;
        config.redis.password = "";
        config.redis.database = 4;

        config.homes = new HomesConfig();
        config.homes.maxHomes = new HashMap<>();
        config.homes.maxHomes.put("default", 3);
        config.homes.maxHomes.put("vip", 5);
        config.homes.maxHomes.put("premium", 10);

        config.economy = new EconomyConfig();
        config.economy.startingBalance = 100.0;
        config.economy.currencySymbol = "$";
        config.economy.currencyName = "coins";

        config.teleport = new TeleportConfig();
        config.teleport.warmupSeconds = 3;
        config.teleport.cooldownSeconds = 5;
        config.teleport.tpaExpireSeconds = 60;

        config.servers = new ServersConfig();
        config.servers.servers = new HashMap<>();
        config.servers.servers.put("lobby", new ServerInfo("lobby.example.com", 25565, "Lobby"));
        config.servers.servers.put("survival", new ServerInfo("survival.example.com", 25565, "Survival"));
        config.servers.servers.put("minigames", new ServerInfo("minigames.example.com", 25565, "Mini-Jeux"));

        return config;
    }

    // === Getters ===

    @NotNull
    public String getServerName() {
        return data.serverName;
    }

    // Database
    @NotNull
    public String getDatabaseHost() {
        return data.database.host;
    }

    public int getDatabasePort() {
        return data.database.port;
    }

    @NotNull
    public String getDatabaseName() {
        return data.database.database;
    }

    @NotNull
    public String getDatabaseUsername() {
        return data.database.username;
    }

    @NotNull
    public String getDatabasePassword() {
        return data.database.password;
    }

    public int getDatabasePoolSize() {
        return data.database.poolSize;
    }

    // Redis
    @NotNull
    public String getRedisHost() {
        return data.redis.host;
    }

    public int getRedisPort() {
        return data.redis.port;
    }

    @NotNull
    public String getRedisPassword() {
        return data.redis.password;
    }

    public int getRedisDatabase() {
        return data.redis.database;
    }

    // Homes
    public int getMaxHomes(@NotNull String group) {
        return data.homes.maxHomes.getOrDefault(group, data.homes.maxHomes.getOrDefault("default", 3));
    }

    // Economy
    @NotNull
    public BigDecimal getStartingBalance() {
        return BigDecimal.valueOf(data.economy.startingBalance);
    }

    @NotNull
    public String getCurrencySymbol() {
        return data.economy.currencySymbol;
    }

    @NotNull
    public String getCurrencyName() {
        return data.economy.currencyName;
    }

    // Teleport
    public int getTeleportWarmup() {
        return data.teleport.warmupSeconds;
    }

    public int getTeleportCooldown() {
        return data.teleport.cooldownSeconds;
    }

    public int getTpaExpireSeconds() {
        return data.teleport.tpaExpireSeconds;
    }

    // Servers
    @NotNull
    public Map<String, ServerInfo> getServers() {
        return data.servers != null && data.servers.servers != null
            ? data.servers.servers
            : new HashMap<>();
    }

    public ServerInfo getServer(@NotNull String name) {
        return getServers().get(name.toLowerCase());
    }

    // === Inner classes for JSON structure ===

    private static class ConfigData {
        String serverName;
        DatabaseConfig database;
        RedisConfig redis;
        HomesConfig homes;
        EconomyConfig economy;
        TeleportConfig teleport;
        ServersConfig servers;
    }

    private static class DatabaseConfig {
        String host;
        int port;
        String database;
        String username;
        String password;
        int poolSize;
    }

    private static class RedisConfig {
        String host;
        int port;
        String password;
        int database;
    }

    private static class HomesConfig {
        Map<String, Integer> maxHomes;
    }

    private static class EconomyConfig {
        double startingBalance;
        String currencySymbol;
        String currencyName;
    }

    private static class TeleportConfig {
        int warmupSeconds;
        int cooldownSeconds;
        int tpaExpireSeconds;
    }

    private static class ServersConfig {
        Map<String, ServerInfo> servers;
    }

    public static class ServerInfo {
        public String host;
        public int port;
        public String displayName;

        public ServerInfo() {}

        public ServerInfo(String host, int port, String displayName) {
            this.host = host;
            this.port = port;
            this.displayName = displayName;
        }
    }
}
