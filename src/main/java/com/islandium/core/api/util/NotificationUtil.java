package com.islandium.core.api.util;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.util.EventTitleUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Utilitaire centralisé pour envoyer des notifications visuelles (toast) aux joueurs.
 * Utilise EventTitleUtil pour afficher un titre à l'écran avec icône et couleur.
 */
public final class NotificationUtil {

    private static final float FADE_IN = 0.2f;
    private static final float FADE_OUT = 0.2f;

    private NotificationUtil() {}

    /**
     * Envoie une notification visuelle à un joueur.
     */
    public static void send(@NotNull Player player, @NotNull NotificationType type, @NotNull String message) {
        send(player, type, message, null);
    }

    /**
     * Envoie une notification visuelle à un joueur avec sous-titre.
     */
    public static void send(@NotNull Player player, @NotNull NotificationType type,
                            @NotNull String message, @Nullable String subtitle) {
        try {
            var ref = player.getReference();
            if (ref == null || !ref.isValid()) return;

            var store = ref.getStore();
            var playerRef = store.getComponent(ref, PlayerRef.getComponentType());
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
        Message primaryMsg = Message.raw(message).color(type.getColor());
        Message secondaryMsg = subtitle != null ? Message.raw(subtitle) : Message.raw("");

        EventTitleUtil.showEventTitleToPlayer(
            playerRef,
            primaryMsg,
            secondaryMsg,
            false,
            null,
            type.getDuration(),
            FADE_IN,
            FADE_OUT
        );
    }
}
