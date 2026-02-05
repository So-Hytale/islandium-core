package com.islandium.core.api.economy;

/**
 * Types de transactions économiques.
 */
public enum TransactionType {

    /**
     * Dépôt (ajout d'argent).
     */
    DEPOSIT,

    /**
     * Retrait (retrait d'argent).
     */
    WITHDRAW,

    /**
     * Ajout administratif.
     */
    ADMIN_ADD,

    /**
     * Retrait administratif.
     */
    ADMIN_REMOVE,

    /**
     * Transfert (entre joueurs).
     */
    TRANSFER,

    /**
     * Récompense (système).
     */
    REWARD,

    /**
     * Pénalité (système).
     */
    PENALTY,

    /**
     * Achat (boutique).
     */
    PURCHASE,

    /**
     * Vente (boutique).
     */
    SALE
}
