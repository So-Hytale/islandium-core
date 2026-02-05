package com.islandium.core.service.permission;

import com.hypixel.hytale.server.core.permissions.provider.PermissionProvider;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Provider de permissions personnalisé qui synchronise le système Islandium (BDD)
 * avec le système de permissions natif Hytale.
 *
 * Ce provider est enregistré auprès de PermissionsModule.get().addProvider()
 * et permet à Hytale de voir les permissions stockées dans notre BDD.
 */
public class IslandiumPermissionProvider implements PermissionProvider {

    private static final String PROVIDER_NAME = "IslandiumPermissionProvider";

    // Cache local des permissions synchronisées
    private final Map<UUID, Set<String>> userPermissions = new ConcurrentHashMap<>();
    private final Map<UUID, Set<String>> userGroups = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> groupPermissions = new ConcurrentHashMap<>();

    @Override
    @Nonnull
    public String getName() {
        return PROVIDER_NAME;
    }

    // ========================================
    // Permissions utilisateur
    // ========================================

    @Override
    public void addUserPermissions(@Nonnull UUID uuid, @Nonnull Set<String> permissions) {
        userPermissions.computeIfAbsent(uuid, k -> new HashSet<>()).addAll(permissions);
    }

    @Override
    public void removeUserPermissions(@Nonnull UUID uuid, @Nonnull Set<String> permissions) {
        Set<String> perms = userPermissions.get(uuid);
        if (perms != null) {
            perms.removeAll(permissions);
        }
    }

    @Override
    public Set<String> getUserPermissions(@Nonnull UUID uuid) {
        return new HashSet<>(userPermissions.getOrDefault(uuid, Collections.emptySet()));
    }

    /**
     * Définit toutes les permissions d'un utilisateur (remplace les existantes).
     */
    public void setUserPermissions(@NotNull UUID uuid, @NotNull Set<String> permissions) {
        userPermissions.put(uuid, new HashSet<>(permissions));
    }

    /**
     * Supprime toutes les permissions d'un utilisateur.
     */
    public void clearUserPermissions(@NotNull UUID uuid) {
        userPermissions.remove(uuid);
        userGroups.remove(uuid);
    }

    // ========================================
    // Groupes (Ranks)
    // ========================================

    @Override
    public void addUserToGroup(@Nonnull UUID uuid, @Nonnull String group) {
        userGroups.computeIfAbsent(uuid, k -> new HashSet<>()).add(group.toLowerCase());
    }

    @Override
    public void removeUserFromGroup(@Nonnull UUID uuid, @Nonnull String group) {
        Set<String> groups = userGroups.get(uuid);
        if (groups != null) {
            groups.remove(group.toLowerCase());
        }
    }

    @Override
    public Set<String> getGroupsForUser(@Nonnull UUID uuid) {
        return new HashSet<>(userGroups.getOrDefault(uuid, Collections.emptySet()));
    }

    /**
     * Définit tous les groupes d'un utilisateur (remplace les existants).
     */
    public void setUserGroups(@NotNull UUID uuid, @NotNull Set<String> groups) {
        Set<String> lowerGroups = new HashSet<>();
        for (String group : groups) {
            lowerGroups.add(group.toLowerCase());
        }
        userGroups.put(uuid, lowerGroups);
    }

    // ========================================
    // Permissions de groupe
    // ========================================

    @Override
    public void addGroupPermissions(@Nonnull String group, @Nonnull Set<String> permissions) {
        groupPermissions.computeIfAbsent(group.toLowerCase(), k -> new HashSet<>()).addAll(permissions);
    }

    @Override
    public void removeGroupPermissions(@Nonnull String group, @Nonnull Set<String> permissions) {
        Set<String> perms = groupPermissions.get(group.toLowerCase());
        if (perms != null) {
            perms.removeAll(permissions);
        }
    }

    @Override
    public Set<String> getGroupPermissions(@Nonnull String group) {
        return new HashSet<>(groupPermissions.getOrDefault(group.toLowerCase(), Collections.emptySet()));
    }

    /**
     * Définit toutes les permissions d'un groupe (remplace les existantes).
     */
    public void setGroupPermissions(@NotNull String group, @NotNull Set<String> permissions) {
        groupPermissions.put(group.toLowerCase(), new HashSet<>(permissions));
    }

    // ========================================
    // Utilitaires
    // ========================================

    /**
     * Vérifie si un utilisateur est chargé dans le provider.
     */
    public boolean isUserLoaded(@NotNull UUID uuid) {
        return userPermissions.containsKey(uuid) || userGroups.containsKey(uuid);
    }

    /**
     * Vide tout le cache du provider.
     */
    public void clearAll() {
        userPermissions.clear();
        userGroups.clear();
        groupPermissions.clear();
    }

    /**
     * Obtient le nombre d'utilisateurs chargés.
     */
    public int getLoadedUserCount() {
        Set<UUID> allUsers = new HashSet<>();
        allUsers.addAll(userPermissions.keySet());
        allUsers.addAll(userGroups.keySet());
        return allUsers.size();
    }
}
