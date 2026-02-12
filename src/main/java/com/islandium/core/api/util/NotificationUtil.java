package com.islandium.core.api.util;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.io.PacketHandler;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Utilitaire centralise pour envoyer des notifications visuelles (toast) aux joueurs.
 * Utilise l'API native NotificationUtil de Hytale.
 */
public final class NotificationUtil {

    private NotificationUtil() {}

    /**
     * Envoie une notification visuelle a un joueur.
     */
    public static void send(@NotNull Player player, @NotNull NotificationType type, @NotNull String message) {
        send(player, type, message, null);
    }

    /**
     * Envoie une notification visuelle a un joueur avec sous-titre.
     */
    public static void send(@NotNull Player player, @NotNull NotificationType type,
                            @NotNull String message, @Nullable String subtitle) {
        try {
            PlayerRef playerRef = Universe.get().getPlayer(player.getUuid());
            if (playerRef == null) return;

            sendToRef(playerRef, type, message, subtitle);
        } catch (Exception e) {
            // Fallback silencieux
        }
    }

    /**
     * Envoie une notification visuelle via un PlayerRef.
     */
    public static void send(@NotNull PlayerRef playerRef, @NotNull NotificationType type, @NotNull String message) {
        send(playerRef, type, message, null);
    }

    /**
     * Envoie une notification visuelle via un PlayerRef avec sous-titre.
     */
    public static void send(@NotNull PlayerRef playerRef, @NotNull NotificationType type,
                            @NotNull String message, @Nullable String subtitle) {
        try {
            sendToRef(playerRef, type, message, subtitle);
        } catch (Exception e) {
            // Fallback silencieux
        }
    }

    /**
     * Envoie une notification depuis un CommandContext.
     * Fallback vers le chat si le sender n'est pas un joueur.
     */
    public static void send(@NotNull CommandContext ctx, @NotNull NotificationType type, @NotNull String message) {
        send(ctx, type, message, null);
    }

    /**
     * Envoie une notification depuis un CommandContext avec sous-titre.
     * Fallback vers le chat si le sender n'est pas un joueur.
     */
    public static void send(@NotNull CommandContext ctx, @NotNull NotificationType type,
                            @NotNull String message, @Nullable String subtitle) {
        if (ctx.isPlayer()) {
            send(ctx.senderAs(Player.class), type, message, subtitle);
        } else {
            // Console fallback : envoyer dans le chat
            ctx.sendMessage(ColorUtil.parse(message));
        }
    }

    private static void sendToRef(@NotNull PlayerRef playerRef, @NotNull NotificationType type,
                                  @NotNull String message, @Nullable String subtitle) {
        PacketHandler packetHandler = playerRef.getPacketHandler();
        if (packetHandler == null) return;

        Message primaryMsg = Message.raw(message).color(type.getColor());
        Message secondaryMsg = subtitle != null ? Message.raw(subtitle) : Message.raw("");

        com.hypixel.hytale.server.core.util.NotificationUtil.sendNotification(
            packetHandler,
            primaryMsg,
            secondaryMsg,
            type.getStyle()
        );
    }
}
