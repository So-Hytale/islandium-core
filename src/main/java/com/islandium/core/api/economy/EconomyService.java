package com.islandium.core.api.economy;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Service d'économie.
 */
public interface EconomyService {

    /**
     * Récupère le solde d'un joueur.
     *
     * @param playerUuid l'UUID du joueur
     * @return le solde
     */
    CompletableFuture<BigDecimal> getBalance(@NotNull UUID playerUuid);

    /**
     * Définit le solde d'un joueur.
     *
     * @param playerUuid l'UUID du joueur
     * @param amount le nouveau solde
     */
    CompletableFuture<Void> setBalance(@NotNull UUID playerUuid, @NotNull BigDecimal amount);

    /**
     * Ajoute au solde d'un joueur.
     *
     * @param playerUuid l'UUID du joueur
     * @param amount le montant à ajouter
     * @param description description de la transaction
     */
    CompletableFuture<Void> addBalance(
            @NotNull UUID playerUuid,
            @NotNull BigDecimal amount,
            @Nullable String description
    );

    /**
     * Retire du solde d'un joueur.
     *
     * @param playerUuid l'UUID du joueur
     * @param amount le montant à retirer
     * @param description description de la transaction
     * @return true si le joueur avait assez d'argent
     */
    CompletableFuture<Boolean> removeBalance(
            @NotNull UUID playerUuid,
            @NotNull BigDecimal amount,
            @Nullable String description
    );

    /**
     * Vérifie si un joueur a assez d'argent.
     *
     * @param playerUuid l'UUID du joueur
     * @param amount le montant à vérifier
     * @return true si le joueur a assez
     */
    CompletableFuture<Boolean> hasBalance(@NotNull UUID playerUuid, @NotNull BigDecimal amount);

    /**
     * Transfère de l'argent d'un joueur à un autre.
     *
     * @param from l'UUID de l'envoyeur
     * @param to l'UUID du destinataire
     * @param amount le montant
     * @param description description de la transaction
     * @return true si le transfert a réussi
     */
    CompletableFuture<Boolean> transfer(
            @NotNull UUID from,
            @NotNull UUID to,
            @NotNull BigDecimal amount,
            @Nullable String description
    );

    /**
     * Récupère le classement des joueurs les plus riches.
     *
     * @param limit le nombre de joueurs à récupérer
     * @return la liste des UUIDs triés par solde décroissant
     */
    CompletableFuture<List<UUID>> getTopPlayers(int limit);

    /**
     * Récupère les transactions d'un joueur.
     *
     * @param playerUuid l'UUID du joueur
     * @param limit le nombre de transactions à récupérer
     * @return la liste des transactions
     */
    CompletableFuture<List<Transaction>> getTransactions(@NotNull UUID playerUuid, int limit);

    /**
     * @return le solde de départ pour les nouveaux joueurs
     */
    @NotNull
    BigDecimal getStartingBalance();

    /**
     * @return le symbole de la devise
     */
    @NotNull
    String getCurrencySymbol();

    /**
     * @return le nom de la devise
     */
    @NotNull
    String getCurrencyName();

    /**
     * Formate un montant avec le symbole de devise.
     *
     * @param amount le montant
     * @return le montant formaté (ex: "$100.00")
     */
    @NotNull
    String format(@NotNull BigDecimal amount);
}
