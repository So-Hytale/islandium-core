package com.islandium.core.listener;

import com.islandium.core.api.location.ServerLocation;
import com.islandium.core.api.moderation.PunishmentType;
import com.islandium.core.api.player.IslandiumPlayer;
import com.islandium.core.IslandiumPlugin;
import com.islandium.core.listener.base.IslandiumListener;
import com.hypixel.hytale.event.EventRegistry;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.player.PlayerConnectEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Listener pour la connexion des joueurs.
 */
public class PlayerJoinListener extends IslandiumListener {

    public PlayerJoinListener(@NotNull IslandiumPlugin plugin) {
        super(plugin);
    }

    @Override
    public void register(@NotNull EventRegistry registry) {
        registry.register(PlayerConnectEvent.class, this::onPlayerConnect);
    }

    private void onPlayerConnect(PlayerConnectEvent event) {
        var playerRef = event.getPlayerRef();
        var uuid = playerRef.getUuid();
        var name = playerRef.getUsername();
        Player hytalePlayer = event.getPlayer();

        // Vérifier si le joueur est banni
        plugin.getServiceManager().getModerationService()
            .getActivePunishment(uuid, PunishmentType.BAN)
            .thenAccept(punishmentOpt -> {
                if (punishmentOpt.isPresent()) {
                    var punishment = punishmentOpt.get();
                    String reason = punishment.reason() != null ? punishment.reason() : "Aucune raison";
                    String message = punishment.expiresAt() != null
                        ? "Vous êtes banni jusqu'au " + punishment.expiresAt() + ": " + reason
                        : "Vous êtes banni définitivement: " + reason;
                    // Disconnect via packet handler
                    playerRef.getPacketHandler().disconnect(message);
                    return;
                }

                // Charger les données du joueur
                loadPlayerData(uuid, name, playerRef, hytalePlayer);
            });
    }

    private void loadPlayerData(java.util.UUID uuid, String name, com.hypixel.hytale.server.core.universe.PlayerRef playerRef, @Nullable Player hytalePlayer) {
        plugin.getPlayerManager().handlePlayerJoin(uuid, name, playerRef)
            .thenCompose(essentialsPlayer -> {
                // Synchroniser les permissions avec le système natif Hytale
                return plugin.getServiceManager().getPermissionServiceImpl()
                    .syncWithNativeSystem(uuid)
                    .thenApply(v -> essentialsPlayer);
            })
            .thenCompose(essentialsPlayer -> {
                // Charger les cooldowns de kits du joueur
                return plugin.getServiceManager().getKitService()
                    .loadPlayerCooldowns(uuid)
                    .thenApply(v -> essentialsPlayer);
            })
            .thenCompose(essentialsPlayer -> {
                // Publier l'événement de connexion sur Redis
                return plugin.getRedisManager().getPublisher()
                    .publishPlayerJoin(uuid.toString(), essentialsPlayer.getName(), plugin.getServerName())
                    .thenApply(v -> essentialsPlayer);
            })
            .thenAccept(essentialsPlayer -> {
                // Le joueur est chargé et prêt
                plugin.log(java.util.logging.Level.FINE, "Player " + name + " data loaded and permissions synced");

                // Téléporter au spawn et gérer le message de bienvenue + kits
                handleSpawnAndWelcome(essentialsPlayer, hytalePlayer);
            })
            .exceptionally(ex -> {
                plugin.log(java.util.logging.Level.SEVERE, "Failed to load player data for " + name + ": " + ex.getMessage());
                return null;
            });
    }

    /**
     * Gère la téléportation au spawn, le message de bienvenue et les kits de première connexion.
     */
    private void handleSpawnAndWelcome(IslandiumPlayer player, @Nullable Player hytalePlayer) {
        // TODO: Remettre isFirstJoin quand le système sera validé
        // boolean isNewPlayer = isFirstJoin(player);

        // Téléporter au spawn à chaque connexion
        ServerLocation spawn = plugin.getSpawnService().getSpawn();
        plugin.log(java.util.logging.Level.INFO, "[JoinTP] spawn=" + spawn + " hytalePlayer=" + (hytalePlayer != null ? "OK" : "NULL"));
        if (spawn != null) {
            plugin.getTeleportService().teleportInstant(player, spawn);
            player.sendMessage(plugin.getMessages().getMessagePrefixed("spawn.teleported"));
            plugin.log(java.util.logging.Level.INFO, "[JoinTP] teleportInstant called for " + player.getName());
        } else {
            plugin.log(java.util.logging.Level.WARNING, "[JoinTP] No spawn set! Cannot teleport " + player.getName());
        }

        // Donner les kits de première connexion (avec délai pour laisser l'inventaire se charger)
        if (hytalePlayer != null) {
            new Thread(() -> {
                try {
                    Thread.sleep(3000); // Attendre que l'inventaire soit prêt
                    plugin.getServiceManager().getKitService().giveFirstJoinKits(hytalePlayer);
                    plugin.log(java.util.logging.Level.INFO, "Gave first-join kits to player " + player.getName());
                } catch (Exception e) {
                    plugin.log(java.util.logging.Level.WARNING, "Failed to give first-join kits to " + player.getName() + ": " + e.getMessage());
                }
            }, "FirstJoinKit-" + player.getName()).start();
        }
    }

    /**
     * Vérifie si c'est la première connexion du joueur.
     */
    private boolean isFirstJoin(IslandiumPlayer player) {
        // Le joueur est nouveau si firstLogin == lastLogin (même timestamp)
        // ou si la différence est très faible (< 5 secondes)
        long diff = Math.abs(player.getLastLogin() - player.getFirstLogin());
        return diff < 5000; // Moins de 5 secondes de différence
    }

    /**
     * Envoie un message à tous les joueurs en ligne.
     */
    private void broadcastMessage(com.hypixel.hytale.server.core.Message message) {
        try {
            // Utiliser le PlayerManager d'Islandium pour obtenir les joueurs en ligne
            for (IslandiumPlayer onlinePlayer : plugin.getPlayerManager().getOnlinePlayersLocal()) {
                onlinePlayer.sendMessage(message);
            }
        } catch (Exception e) {
            plugin.log(java.util.logging.Level.WARNING, "Failed to broadcast welcome message: " + e.getMessage());
        }
    }
}
