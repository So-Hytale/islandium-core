package com.islandium.core.api.permission;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;
import java.util.UUID;

/**
 * Représente les permissions d'un joueur.
 */
public interface PlayerPermissions {

    /**
     * @return l'UUID du joueur
     */
    @NotNull
    UUID getPlayerUuid();

    /**
     * @return les ranks du joueur
     */
    @NotNull
    Set<Rank> getRanks();

    /**
     * @return le rank principal (priorité la plus haute)
     */
    @Nullable
    Rank getPrimaryRank();

    /**
     * Vérifie si le joueur a un rank spécifique.
     */
    boolean hasRank(@NotNull String rankName);

    /**
     * @return les permissions personnelles (hors ranks)
     */
    @NotNull
    Set<String> getPersonalPermissions();

    /**
     * @return toutes les permissions (ranks + personnelles)
     */
    @NotNull
    Set<String> getAllPermissions();

    /**
     * Vérifie si le joueur a une permission (avec wildcards).
     */
    boolean hasPermission(@NotNull String permission);
}
