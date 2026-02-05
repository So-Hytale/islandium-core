package com.islandium.core.redis.channel;

import org.jetbrains.annotations.NotNull;

/**
 * Énumération des channels Redis.
 */
public enum RedisChannel {

    /**
     * Téléportations cross-server.
     */
    TELEPORT("ess:teleport"),

    /**
     * Messages privés.
     */
    PRIVATE_MESSAGE("ess:msg"),

    /**
     * Mise à jour des données joueur.
     */
    PLAYER_UPDATE("ess:player:update"),

    /**
     * Connexion joueur.
     */
    PLAYER_JOIN("ess:player:join"),

    /**
     * Déconnexion joueur.
     */
    PLAYER_QUIT("ess:player:quit"),

    /**
     * Messages staff.
     */
    STAFF("ess:staff"),

    /**
     * Broadcasts globaux.
     */
    BROADCAST("ess:broadcast");

    private final String channel;

    RedisChannel(@NotNull String channel) {
        this.channel = channel;
    }

    @NotNull
    public String getChannel() {
        return channel;
    }

    /**
     * Récupère un channel par son nom.
     */
    public static RedisChannel fromChannel(@NotNull String channel) {
        for (RedisChannel rc : values()) {
            if (rc.channel.equals(channel)) {
                return rc;
            }
        }
        return null;
    }

    /**
     * @return tous les noms de channels
     */
    public static String[] getAllChannels() {
        RedisChannel[] values = values();
        String[] channels = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            channels[i] = values[i].channel;
        }
        return channels;
    }
}
