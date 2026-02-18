package com.islandium.core.api.util;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.util.EventTitleUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Utilitaire pour afficher des titres au centre de l'écran.
 */
public final class TitleUtil {

    private TitleUtil() {}

    /**
     * Affiche un titre au centre de l'écran du joueur.
     *
     * @param player Joueur cible
     * @param title Titre principal
     * @param subtitle Sous-titre (peut être null)
     */
    public static void showTitle(@NotNull Player player, @NotNull String title, @Nullable String subtitle) {
        showTitle(player, title, subtitle, 0.3f, 1.5f, 0.3f);
    }

    /**
     * Affiche un titre au centre de l'écran du joueur avec durées personnalisées.
     *
     * @param player Joueur cible
     * @param title Titre principal
     * @param subtitle Sous-titre (peut être null)
     * @param fadeIn Durée du fade in en secondes
     * @param duration Durée d'affichage en secondes
     * @param fadeOut Durée du fade out en secondes
     */
    public static void showTitle(@NotNull Player player, @NotNull String title, @Nullable String subtitle,
                                  float fadeIn, float duration, float fadeOut) {
        try {
            var ref = player.getReference();
            if (ref == null || !ref.isValid()) return;

            var store = ref.getStore();
            var playerRef = store.getComponent(ref, PlayerRef.getComponentType());
            if (playerRef == null) return;

            Message primaryMsg = Message.raw(title);
            Message secondaryMsg = subtitle != null ? Message.raw(subtitle) : Message.raw("");

            EventTitleUtil.showEventTitleToPlayer(
                playerRef,
                primaryMsg,
                secondaryMsg,
                true, // isMajor - false pour un petit titre
                null,  // icon
                duration,
                fadeIn,
                fadeOut
            );
        } catch (Exception e) {
            // Ignorer silencieusement
        }
    }

    /**
     * Affiche un titre d'erreur (rouge) au joueur.
     */
    public static void showError(@NotNull Player player, @NotNull String message) {
        // Utiliser les codes couleur Hytale si supportés, sinon texte brut
        showTitle(player, "§c" + message, null, 0.2f, 1.0f, 0.2f);
    }

    /**
     * Affiche un titre de succès (vert) au joueur.
     */
    public static void showSuccess(@NotNull Player player, @NotNull String message) {
        showTitle(player, "§a" + message, null, 0.2f, 1.0f, 0.2f);
    }

    /**
     * Affiche un titre d'avertissement (jaune) au joueur.
     */
    public static void showWarning(@NotNull Player player, @NotNull String message) {
        showTitle(player, "§e" + message, null, 0.2f, 1.0f, 0.2f);
    }

    /**
     * Cache le titre actuellement affiché.
     */
    public static void hideTitle(@NotNull Player player) {
        try {
            var ref = player.getReference();
            if (ref == null || !ref.isValid()) return;

            var store = ref.getStore();
            var playerRef = store.getComponent(ref, PlayerRef.getComponentType());
            if (playerRef == null) return;

            EventTitleUtil.hideEventTitleFromPlayer(playerRef, 0.2f);
        } catch (Exception e) {
            // Ignorer silencieusement
        }
    }
}
