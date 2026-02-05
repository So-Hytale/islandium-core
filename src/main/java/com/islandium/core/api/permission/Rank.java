package com.islandium.core.api.permission;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

/**
 * Représente un rang de permissions.
 */
public interface Rank {

    /**
     * @return l'ID du rank
     */
    int getId();

    /**
     * @return le nom unique du rank
     */
    @NotNull
    String getName();

    /**
     * @return le nom d'affichage
     */
    @NotNull
    String getDisplayName();

    /**
     * @return le préfixe de chat
     */
    @Nullable
    String getPrefix();

    /**
     * @return la couleur hex
     */
    @NotNull
    String getColor();

    /**
     * @return la priorité (plus élevé = plus important)
     */
    int getPriority();

    /**
     * @return le rank parent (héritage)
     */
    @Nullable
    Rank getParent();

    /**
     * @return l'ID du parent
     */
    @Nullable
    Integer getParentId();

    /**
     * @return true si c'est le rank par défaut
     */
    boolean isDefault();

    /**
     * @return le timestamp de création
     */
    long getCreatedAt();

    /**
     * @return les permissions directes (sans héritage)
     */
    @NotNull
    Set<String> getDirectPermissions();

    /**
     * @return toutes les permissions (avec héritage)
     */
    @NotNull
    Set<String> getAllPermissions();

    /**
     * Vérifie si le rank a une permission (avec wildcards).
     */
    boolean hasPermission(@NotNull String permission);
}
