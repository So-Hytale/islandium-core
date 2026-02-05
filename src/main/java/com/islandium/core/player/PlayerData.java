package com.islandium.core.player;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Données d'un joueur stockées en base de données.
 *
 * Note: Les données suivantes sont gérées nativement par Hytale et ne sont PAS stockées ici:
 * - Position/Location -> PlayerConfigData.lastSavedPosition
 * - Vanish -> HiddenPlayersManager
 * - God Mode -> Invulnerable component
 * - Fly -> MovementManager.canFly
 * - Permissions -> PermissionsModule
 */
public class PlayerData {

    private final UUID uuid;
    private String username;
    private BigDecimal balance;
    private final long firstLogin;
    private long lastLogin;
    private String lastServer;

    public PlayerData(
            @NotNull UUID uuid,
            @NotNull String username,
            @NotNull BigDecimal balance,
            long firstLogin,
            long lastLogin,
            @Nullable String lastServer
    ) {
        this.uuid = uuid;
        this.username = username;
        this.balance = balance;
        this.firstLogin = firstLogin;
        this.lastLogin = lastLogin;
        this.lastServer = lastServer;
    }

    /**
     * Crée un nouveau PlayerData pour un nouveau joueur.
     */
    public static PlayerData createNew(@NotNull UUID uuid, @NotNull String username, @NotNull BigDecimal startingBalance) {
        long now = System.currentTimeMillis();
        return new PlayerData(
                uuid,
                username,
                startingBalance,
                now,
                now,
                null
        );
    }

    // === Getters (record-style) ===

    @NotNull
    public UUID uuid() { return uuid; }
    @NotNull
    public UUID getUuid() { return uuid; }

    @NotNull
    public String username() { return username; }
    @NotNull
    public String getUsername() { return username; }

    @NotNull
    public BigDecimal balance() { return balance; }
    @NotNull
    public BigDecimal getBalance() { return balance; }

    public long firstLogin() { return firstLogin; }
    public long getFirstLogin() { return firstLogin; }

    public long lastLogin() { return lastLogin; }
    public long getLastLogin() { return lastLogin; }

    @Nullable
    public String lastServer() { return lastServer; }
    @Nullable
    public String getLastServer() { return lastServer; }

    // === Setters ===

    public void setUsername(@NotNull String username) {
        this.username = username;
    }

    public void setBalance(@NotNull BigDecimal balance) {
        this.balance = balance;
    }

    public void setLastLogin(long lastLogin) {
        this.lastLogin = lastLogin;
    }

    public void setLastServer(@Nullable String lastServer) {
        this.lastServer = lastServer;
    }

    // === Balance operations ===

    public void addBalance(@NotNull BigDecimal amount) {
        this.balance = this.balance.add(amount);
    }

    public boolean removeBalance(@NotNull BigDecimal amount) {
        if (balance.compareTo(amount) < 0) {
            return false;
        }
        this.balance = this.balance.subtract(amount);
        return true;
    }

    public boolean hasBalance(@NotNull BigDecimal amount) {
        return balance.compareTo(amount) >= 0;
    }
}
