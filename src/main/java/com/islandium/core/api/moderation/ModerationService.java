package com.islandium.core.api.moderation;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Service de modération (ban, mute, kick).
 */
public interface ModerationService {

    // === Ban ===

    /**
     * Bannit un joueur de façon permanente.
     *
     * @param playerUuid l'UUID du joueur
     * @param reason la raison
     * @param punisherUuid l'UUID du punisseur (null si console)
     * @return la punition créée
     */
    CompletableFuture<Punishment> ban(
            @NotNull UUID playerUuid,
            @Nullable String reason,
            @Nullable UUID punisherUuid
    );

    /**
     * Bannit un joueur temporairement.
     *
     * @param playerUuid l'UUID du joueur
     * @param durationMs la durée en millisecondes
     * @param reason la raison
     * @param punisherUuid l'UUID du punisseur (null si console)
     * @return la punition créée
     */
    CompletableFuture<Punishment> tempBan(
            @NotNull UUID playerUuid,
            long durationMs,
            @Nullable String reason,
            @Nullable UUID punisherUuid
    );

    /**
     * Débannit un joueur.
     *
     * @param playerUuid l'UUID du joueur
     * @param unbannerUuid l'UUID de celui qui débannit (null si console)
     * @return true si le joueur était banni
     */
    CompletableFuture<Boolean> unban(@NotNull UUID playerUuid, @Nullable UUID unbannerUuid);

    /**
     * Vérifie si un joueur est banni.
     *
     * @param playerUuid l'UUID du joueur
     * @return true si banni
     */
    CompletableFuture<Boolean> isBanned(@NotNull UUID playerUuid);

    /**
     * Récupère le ban actif d'un joueur.
     *
     * @param playerUuid l'UUID du joueur
     * @return le ban actif ou empty
     */
    CompletableFuture<Optional<Punishment>> getActiveBan(@NotNull UUID playerUuid);

    // === Mute ===

    /**
     * Mute un joueur de façon permanente.
     *
     * @param playerUuid l'UUID du joueur
     * @param reason la raison
     * @param punisherUuid l'UUID du punisseur (null si console)
     * @return la punition créée
     */
    CompletableFuture<Punishment> mute(
            @NotNull UUID playerUuid,
            @Nullable String reason,
            @Nullable UUID punisherUuid
    );

    /**
     * Mute un joueur temporairement.
     *
     * @param playerUuid l'UUID du joueur
     * @param durationMs la durée en millisecondes
     * @param reason la raison
     * @param punisherUuid l'UUID du punisseur (null si console)
     * @return la punition créée
     */
    CompletableFuture<Punishment> tempMute(
            @NotNull UUID playerUuid,
            long durationMs,
            @Nullable String reason,
            @Nullable UUID punisherUuid
    );

    /**
     * Unmute un joueur.
     *
     * @param playerUuid l'UUID du joueur
     * @param unmuterUuid l'UUID de celui qui unmute (null si console)
     * @return true si le joueur était muté
     */
    CompletableFuture<Boolean> unmute(@NotNull UUID playerUuid, @Nullable UUID unmuterUuid);

    /**
     * Vérifie si un joueur est muté.
     *
     * @param playerUuid l'UUID du joueur
     * @return true si muté
     */
    CompletableFuture<Boolean> isMuted(@NotNull UUID playerUuid);

    /**
     * Récupère le mute actif d'un joueur.
     *
     * @param playerUuid l'UUID du joueur
     * @return le mute actif ou empty
     */
    CompletableFuture<Optional<Punishment>> getActiveMute(@NotNull UUID playerUuid);

    // === Kick ===

    /**
     * Kick un joueur du serveur.
     *
     * @param playerUuid l'UUID du joueur
     * @param reason la raison
     * @param kickerUuid l'UUID du kickeur (null si console)
     * @return true si le joueur a été kické
     */
    CompletableFuture<Boolean> kick(
            @NotNull UUID playerUuid,
            @Nullable String reason,
            @Nullable UUID kickerUuid
    );

    // === Génériques ===

    /**
     * Applique une punition générique.
     *
     * @param playerUuid l'UUID du joueur
     * @param type le type de punition
     * @param reason la raison
     * @param punisherUuid l'UUID du punisseur
     * @param expiresAt la date d'expiration (null pour permanent)
     * @return la punition créée
     */
    CompletableFuture<Punishment> punish(
            @NotNull UUID playerUuid,
            @NotNull PunishmentType type,
            @Nullable String reason,
            @Nullable UUID punisherUuid,
            @Nullable java.time.Instant expiresAt
    );

    /**
     * Révoque une punition.
     *
     * @param playerUuid l'UUID du joueur
     * @param type le type de punition
     * @param revokerUuid l'UUID de celui qui révoque
     * @return true si une punition a été révoquée
     */
    CompletableFuture<Boolean> revokePunishment(
            @NotNull UUID playerUuid,
            @NotNull PunishmentType type,
            @Nullable UUID revokerUuid
    );

    /**
     * Récupère la punition active d'un type spécifique.
     *
     * @param playerUuid l'UUID du joueur
     * @param type le type de punition
     * @return la punition active ou empty
     */
    CompletableFuture<Optional<Punishment>> getActivePunishment(
            @NotNull UUID playerUuid,
            @NotNull PunishmentType type
    );

    // === Historique ===

    /**
     * Récupère l'historique des punitions d'un joueur.
     *
     * @param playerUuid l'UUID du joueur
     * @param limit le nombre max de punitions
     * @return la liste des punitions
     */
    CompletableFuture<List<Punishment>> getPunishmentHistory(@NotNull UUID playerUuid, int limit);

    /**
     * Récupère les punitions actives d'un joueur.
     *
     * @param playerUuid l'UUID du joueur
     * @return la liste des punitions actives
     */
    CompletableFuture<List<Punishment>> getActivePunishments(@NotNull UUID playerUuid);
}
