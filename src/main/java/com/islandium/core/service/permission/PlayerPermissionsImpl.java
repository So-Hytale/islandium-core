package com.islandium.core.service.permission;

import com.islandium.core.api.permission.PlayerPermissions;
import com.islandium.core.api.permission.Rank;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Implementation de PlayerPermissions.
 */
public class PlayerPermissionsImpl implements PlayerPermissions {

    private final UUID playerUuid;
    private final Set<Rank> ranks;
    private final Set<String> personalPermissions;

    public PlayerPermissionsImpl(@NotNull UUID playerUuid) {
        this.playerUuid = playerUuid;
        this.ranks = new HashSet<>();
        this.personalPermissions = new HashSet<>();
    }

    public PlayerPermissionsImpl(
            @NotNull UUID playerUuid,
            @NotNull Set<Rank> ranks,
            @NotNull Set<String> personalPermissions
    ) {
        this.playerUuid = playerUuid;
        this.ranks = new HashSet<>(ranks);
        this.personalPermissions = new HashSet<>(personalPermissions);
    }

    @Override
    @NotNull
    public UUID getPlayerUuid() {
        return playerUuid;
    }

    @Override
    @NotNull
    public Set<Rank> getRanks() {
        return new HashSet<>(ranks);
    }

    public void addRank(@NotNull Rank rank) {
        ranks.add(rank);
    }

    public void removeRank(@NotNull Rank rank) {
        ranks.remove(rank);
    }

    public void setRanks(@NotNull Set<Rank> ranks) {
        this.ranks.clear();
        this.ranks.addAll(ranks);
    }

    @Override
    @Nullable
    public Rank getPrimaryRank() {
        return ranks.stream()
                .max(Comparator.comparingInt(Rank::getPriority))
                .orElse(null);
    }

    @Override
    public boolean hasRank(@NotNull String rankName) {
        return ranks.stream()
                .anyMatch(r -> r.getName().equalsIgnoreCase(rankName));
    }

    @Override
    @NotNull
    public Set<String> getPersonalPermissions() {
        return new HashSet<>(personalPermissions);
    }

    public void addPersonalPermission(@NotNull String permission) {
        personalPermissions.add(permission);
    }

    public void removePersonalPermission(@NotNull String permission) {
        personalPermissions.remove(permission);
    }

    public void setPersonalPermissions(@NotNull Set<String> permissions) {
        this.personalPermissions.clear();
        this.personalPermissions.addAll(permissions);
    }

    @Override
    @NotNull
    public Set<String> getAllPermissions() {
        Set<String> all = new HashSet<>(personalPermissions);
        for (Rank rank : ranks) {
            all.addAll(rank.getAllPermissions());
        }
        return all;
    }

    @Override
    public boolean hasPermission(@NotNull String permission) {
        // Verifier les permissions personnelles d'abord
        if (checkPermission(personalPermissions, permission)) {
            return true;
        }

        // Verifier les permissions des ranks
        for (Rank rank : ranks) {
            if (rank.hasPermission(permission)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Verifie une permission dans un set avec support des wildcards.
     */
    private boolean checkPermission(@NotNull Set<String> permissions, @NotNull String permission) {
        // Permission exacte
        if (permissions.contains(permission)) {
            return true;
        }

        // Wildcard global
        if (permissions.contains("*")) {
            return true;
        }

        // Wildcards partiels
        String[] parts = permission.split("\\.");
        StringBuilder wildcard = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) {
                wildcard.append(".");
            }
            wildcard.append(parts[i]);
            if (permissions.contains(wildcard + ".*")) {
                return true;
            }
        }

        return false;
    }

    @Override
    public String toString() {
        return "PlayerPermissionsImpl{" +
                "playerUuid=" + playerUuid +
                ", ranks=" + ranks.size() +
                ", personalPermissions=" + personalPermissions.size() +
                '}';
    }
}
