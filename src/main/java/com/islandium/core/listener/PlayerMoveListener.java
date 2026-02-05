package com.islandium.core.listener;

import com.islandium.core.IslandiumPlugin;
import com.islandium.core.listener.base.IslandiumListener;
import com.hypixel.hytale.event.EventRegistry;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Listener pour les mouvements des joueurs.
 * Utilisé pour la détection d'AFK.
 * NOTE: PlayerMoveEvent n'existe pas dans l'API Hytale,
 * l'AFK devra être géré autrement (via ticks ou autre mécanisme)
 */
public class PlayerMoveListener extends IslandiumListener {

    private final Map<UUID, Long> lastMovement = new ConcurrentHashMap<>();
    private static final long AFK_THRESHOLD_MS = 5 * 60 * 1000; // 5 minutes

    public PlayerMoveListener(@NotNull IslandiumPlugin plugin) {
        super(plugin);
    }

    @Override
    public void register(@NotNull EventRegistry registry) {
        // PlayerMoveEvent n'existe pas dans l'API Hytale
        // TODO: Implémenter via un système de tick pour la détection AFK
    }

    /**
     * Met à jour le dernier mouvement d'un joueur.
     */
    public void updateLastMovement(UUID uuid) {
        lastMovement.put(uuid, System.currentTimeMillis());
    }

    /**
     * Vérifie si un joueur devrait être marqué AFK.
     */
    public boolean shouldBeAfk(UUID uuid) {
        Long last = lastMovement.get(uuid);
        if (last == null) return false;
        return System.currentTimeMillis() - last > AFK_THRESHOLD_MS;
    }

    /**
     * Nettoie les données pour un joueur déconnecté.
     */
    public void cleanup(UUID uuid) {
        lastMovement.remove(uuid);
    }
}
