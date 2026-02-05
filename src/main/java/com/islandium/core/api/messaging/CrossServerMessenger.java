package com.islandium.core.api.messaging;

import org.jetbrains.annotations.NotNull;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Service de messagerie cross-server.
 */
public interface CrossServerMessenger {

    /**
     * Envoie un message privé à un joueur (cross-server).
     *
     * @param fromUuid l'UUID de l'envoyeur
     * @param toUuid l'UUID du destinataire
     * @param message le message
     * @return true si le message a été envoyé
     */
    CompletableFuture<Boolean> sendPrivateMessage(
            @NotNull UUID fromUuid,
            @NotNull UUID toUuid,
            @NotNull String message
    );

    /**
     * Envoie un broadcast à tous les serveurs.
     *
     * @param message le message
     */
    void broadcast(@NotNull String message);

    /**
     * Envoie un broadcast aux joueurs ayant une permission.
     *
     * @param message le message
     * @param permission la permission requise
     */
    void broadcast(@NotNull String message, @NotNull String permission);

    /**
     * Envoie un message au staff (joueurs avec essentials.staff).
     *
     * @param message le message
     */
    void sendStaffMessage(@NotNull String message);

    /**
     * Récupère l'UUID du dernier interlocuteur d'un joueur (pour /r).
     *
     * @param playerUuid l'UUID du joueur
     * @return l'UUID du dernier interlocuteur ou null
     */
    UUID getLastMessageSender(@NotNull UUID playerUuid);

    /**
     * Définit le dernier interlocuteur d'un joueur.
     *
     * @param playerUuid l'UUID du joueur
     * @param lastSenderUuid l'UUID du dernier interlocuteur
     */
    void setLastMessageSender(@NotNull UUID playerUuid, @NotNull UUID lastSenderUuid);
}
