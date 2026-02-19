package com.islandium.core.api.region;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registre centralisé pour bypasser le flag item-pickup des régions.
 * Les plugins (ex: prison) peuvent enregistrer des joueurs qui doivent
 * pouvoir ramasser des items même si le flag de la région l'interdit.
 */
public final class PickupBypassRegistry {

    private static final Set<UUID> bypassedPlayers = ConcurrentHashMap.newKeySet();

    private PickupBypassRegistry() {}

    /**
     * Ajoute un joueur au bypass (peut ramasser des items partout).
     */
    public static void addBypass(UUID playerUuid) {
        bypassedPlayers.add(playerUuid);
    }

    /**
     * Retire un joueur du bypass.
     */
    public static void removeBypass(UUID playerUuid) {
        bypassedPlayers.remove(playerUuid);
    }

    /**
     * Vérifie si un joueur a le bypass actif.
     */
    public static boolean hasBypass(UUID playerUuid) {
        return bypassedPlayers.contains(playerUuid);
    }

    /**
     * Nettoie tous les bypass (ex: shutdown).
     */
    public static void clear() {
        bypassedPlayers.clear();
    }
}
