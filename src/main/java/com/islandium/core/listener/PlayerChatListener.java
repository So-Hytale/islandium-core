package com.islandium.core.listener;

import com.islandium.core.api.moderation.PunishmentType;
import com.islandium.core.api.permission.PlayerPermissions;
import com.islandium.core.api.permission.Rank;
import com.islandium.core.api.player.PlayerState;
import com.islandium.core.IslandiumPlugin;
import com.islandium.core.listener.base.IslandiumListener;
import com.hypixel.hytale.event.EventRegistry;
import com.hypixel.hytale.server.core.event.events.player.PlayerChatEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.Message;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

/**
 * Listener pour le chat des joueurs.
 * Note: PlayerChatEvent implémente IAsyncEvent<String>, utilise registerAsyncGlobal
 */
public class PlayerChatListener extends IslandiumListener {

    public PlayerChatListener(@NotNull IslandiumPlugin plugin) {
        super(plugin);
    }

    @Override
    public void register(@NotNull EventRegistry registry) {
        // PlayerChatEvent est un IAsyncEvent<String>, donc on utilise registerAsyncGlobal
        registry.registerAsyncGlobal(PlayerChatEvent.class, this::onPlayerChat);
    }

    private CompletableFuture<PlayerChatEvent> onPlayerChat(CompletableFuture<PlayerChatEvent> eventFuture) {
        return eventFuture.thenCompose(event -> {
            PlayerRef senderRef = event.getSender();

            var uuid = senderRef.getUuid();

            // Vérifier si le joueur est muté
            return plugin.getServiceManager().getModerationService()
                .getActivePunishment(uuid, PunishmentType.MUTE)
                .thenCompose(punishmentOpt -> {
                    if (punishmentOpt.isPresent()) {
                        event.setCancelled(true);
                        var punishment = punishmentOpt.get();

                        // Formater le temps restant si temporaire
                        String timeMsg;
                        if (punishment.expiresAt() != null) {
                            long remaining = punishment.expiresAt() - System.currentTimeMillis();
                            timeMsg = formatDuration(remaining);
                        } else {
                            timeMsg = "permanent";
                        }

                        String reason = punishment.reason() != null ? punishment.reason() : "Aucune raison specifiee";

                        // Message principal en rouge
                        senderRef.sendMessage(Message.join(
                            Message.raw("✖ ").color("#FF5555"),
                            Message.raw("Vous etes mute!").color("#FF5555")
                        ));
                        // Raison
                        senderRef.sendMessage(Message.join(
                            Message.raw("Raison: ").color("#AAAAAA"),
                            Message.raw(reason).color("#FFFFFF")
                        ));
                        // Durée
                        senderRef.sendMessage(Message.join(
                            Message.raw("Duree restante: ").color("#AAAAAA"),
                            Message.raw(timeMsg).color("#FFFFFF")
                        ));

                        return CompletableFuture.completedFuture(event);
                    }

                    // Retirer l'état AFK si le joueur parle
                    plugin.getPlayerManager().getOnlinePlayer(uuid).ifPresent(essPlayer -> {
                        if (essPlayer.hasState(PlayerState.AFK)) {
                            essPlayer.removeState(PlayerState.AFK);
                        }
                    });

                    // Charger les permissions du joueur pour obtenir son rang
                    return plugin.getServiceManager().getPermissionService()
                        .getPlayerPermissions(uuid)
                        .thenApply(perms -> {
                            applyRankFormat(event, senderRef, perms);
                            return event;
                        });
                });
        });
    }

    /**
     * Applique le format du chat avec le préfixe et la couleur du rang.
     * Format: [Prefix] Pseudo: Message (avec couleurs)
     */
    private void applyRankFormat(PlayerChatEvent event, PlayerRef senderRef, PlayerPermissions perms) {
        Rank primaryRank = perms.getPrimaryRank();

        // Définir un formatter personnalisé pour le message
        event.setFormatter((playerRef, msg) -> {
            String username = playerRef.getUsername();

            if (primaryRank != null) {
                // Utiliser directement la chaîne hex comme Hytale le fait
                String colorHex = normalizeColor(primaryRank.getColor());
                String prefix = primaryRank.getPrefix();

                if (prefix != null && !prefix.isEmpty()) {
                    // Format: [Prefix] Pseudo: Message
                    // Prefix et pseudo en couleur du rang, message en blanc
                    return Message.join(
                        Message.raw(prefix + " ").color(colorHex),
                        Message.raw(username).color(colorHex),
                        Message.raw(": " + msg).color("#FFFFFF")
                    );
                } else {
                    // Pas de prefix, juste pseudo coloré
                    return Message.join(
                        Message.raw(username).color(colorHex),
                        Message.raw(": " + msg).color("#FFFFFF")
                    );
                }
            }

            // Pas de rang, message par défaut
            return Message.raw(username + ": " + msg);
        });
    }

    /**
     * Normalise une couleur hex pour s'assurer qu'elle commence par #.
     */
    private String normalizeColor(String hexColor) {
        if (hexColor == null || hexColor.isEmpty()) {
            return "#FFFFFF";
        }
        // S'assurer que la couleur commence par #
        return hexColor.startsWith("#") ? hexColor : "#" + hexColor;
    }

    /**
     * Formate une durée en millisecondes en texte lisible.
     */
    private String formatDuration(long millis) {
        if (millis <= 0) {
            return "expire";
        }

        long seconds = millis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        if (days > 0) {
            return days + "j " + (hours % 24) + "h " + (minutes % 60) + "m";
        } else if (hours > 0) {
            return hours + "h " + (minutes % 60) + "m " + (seconds % 60) + "s";
        } else if (minutes > 0) {
            return minutes + "m " + (seconds % 60) + "s";
        } else {
            return seconds + "s";
        }
    }
}
