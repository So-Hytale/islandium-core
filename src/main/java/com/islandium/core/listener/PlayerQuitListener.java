package com.islandium.core.listener;

import com.islandium.core.IslandiumPlugin;
import com.islandium.core.listener.base.IslandiumListener;
import com.hypixel.hytale.event.EventRegistry;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;
import java.util.logging.Level;

/**
 * Listener pour la déconnexion des joueurs.
 */
public class PlayerQuitListener extends IslandiumListener {

    public PlayerQuitListener(@NotNull IslandiumPlugin plugin) {
        super(plugin);
    }

    @Override
    public void register(@NotNull EventRegistry registry) {
        registry.register(PlayerDisconnectEvent.class, this::onPlayerDisconnect);
    }

    private void onPlayerDisconnect(PlayerDisconnectEvent event) {
        PlayerRef playerRef = event.getPlayerRef();
        if (playerRef == null) {
            return;
        }

        UUID uuid = playerRef.getUuid();
        String name = playerRef.getUsername();

        // Sauvegarder et décharger le joueur
        plugin.getPlayerManager().unloadPlayer(uuid)
            .thenCompose(v -> {
                // Publier l'événement de déconnexion sur Redis
                return plugin.getRedisManager().getPublisher()
                    .publishPlayerQuit(uuid.toString(), plugin.getServerName());
            })
            .exceptionally(ex -> {
                plugin.log(Level.SEVERE, "Failed to save player data for " + name + ": " + ex.getMessage());
                return null;
            });
    }
}
