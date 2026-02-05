package com.islandium.core.api.moderation;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Représente une punition (ban, mute, kick).
 */
public interface Punishment {

    /**
     * @return l'ID de la punition
     */
    int getId();

    /**
     * @return l'UUID du joueur puni
     */
    @NotNull
    UUID getPlayerUuid();

    /**
     * @return le type de punition
     */
    @NotNull
    PunishmentType getType();

    /**
     * @return la raison de la punition
     */
    @Nullable
    String getReason();

    /**
     * Alias for getReason() (record-style accessor).
     */
    @Nullable
    default String reason() {
        return getReason();
    }

    /**
     * @return l'UUID du punisseur (null si console)
     */
    @Nullable
    UUID getPunisherUuid();

    /**
     * @return le timestamp de création
     */
    long getCreatedAt();

    /**
     * @return le timestamp d'expiration (null si permanent)
     */
    @Nullable
    Long getExpiresAt();

    /**
     * Alias for getExpiresAt() (record-style accessor).
     */
    @Nullable
    default Long expiresAt() {
        return getExpiresAt();
    }

    /**
     * @return true si la punition a été révoquée
     */
    boolean isRevoked();

    /**
     * @return l'UUID de celui qui a révoqué (si révoqué)
     */
    @Nullable
    UUID getRevokedBy();

    /**
     * @return le timestamp de révocation (si révoqué)
     */
    @Nullable
    Long getRevokedAt();

    /**
     * Vérifie si la punition est permanente.
     */
    default boolean isPermanent() {
        return getExpiresAt() == null;
    }

    /**
     * Vérifie si la punition est active (non révoquée et non expirée).
     */
    default boolean isActive() {
        if (isRevoked()) return false;
        Long expires = getExpiresAt();
        return expires == null || expires > System.currentTimeMillis();
    }

    /**
     * Calcule le temps restant en millisecondes.
     *
     * @return le temps restant, ou -1 si permanent, ou 0 si expiré
     */
    default long getRemainingTime() {
        if (!isActive()) return 0;
        Long expires = getExpiresAt();
        if (expires == null) return -1;
        return Math.max(0, expires - System.currentTimeMillis());
    }
}
