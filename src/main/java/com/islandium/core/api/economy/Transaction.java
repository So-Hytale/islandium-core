package com.islandium.core.api.economy;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Représente une transaction économique.
 */
public record Transaction(
        long id,
        @Nullable UUID fromUuid,
        @Nullable UUID toUuid,
        @NotNull BigDecimal amount,
        @NotNull TransactionType type,
        @Nullable String description,
        long createdAt
) {

    /**
     * @return l'ID de la transaction
     */
    public long getId() {
        return id;
    }

    /**
     * @return l'UUID de l'envoyeur (null si système)
     */
    @Nullable
    public UUID getFromUuid() {
        return fromUuid;
    }

    /**
     * @return l'UUID du destinataire (null si système)
     */
    @Nullable
    public UUID getToUuid() {
        return toUuid;
    }

    /**
     * @return le montant
     */
    @NotNull
    public BigDecimal getAmount() {
        return amount;
    }

    /**
     * @return le type de transaction
     */
    @NotNull
    public TransactionType getType() {
        return type;
    }

    /**
     * @return la description
     */
    @Nullable
    public String getDescription() {
        return description;
    }

    /**
     * @return le timestamp de création
     */
    public long getCreatedAt() {
        return createdAt;
    }
}
