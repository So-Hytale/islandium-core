package com.islandium.core.api.messaging;

import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Représente un message privé.
 */
public record PrivateMessage(
        @NotNull UUID fromUuid,
        @NotNull String fromName,
        @NotNull UUID toUuid,
        @NotNull String message,
        long timestamp
) {

    /**
     * @return l'UUID de l'envoyeur
     */
    @NotNull
    public UUID getFromUuid() {
        return fromUuid;
    }

    /**
     * @return le nom de l'envoyeur
     */
    @NotNull
    public String getFromName() {
        return fromName;
    }

    /**
     * @return l'UUID du destinataire
     */
    @NotNull
    public UUID getToUuid() {
        return toUuid;
    }

    /**
     * @return le message
     */
    @NotNull
    public String getMessage() {
        return message;
    }

    /**
     * @return le timestamp
     */
    public long getTimestamp() {
        return timestamp;
    }
}
