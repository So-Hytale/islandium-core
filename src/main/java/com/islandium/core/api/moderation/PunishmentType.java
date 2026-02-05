package com.islandium.core.api.moderation;

/**
 * Types de punitions.
 */
public enum PunishmentType {

    /**
     * Bannissement (ne peut plus se connecter).
     */
    BAN,

    /**
     * Mute (ne peut plus parler).
     */
    MUTE,

    /**
     * Kick (expulsion du serveur).
     */
    KICK
}
