package com.islandium.core.database.repository;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Interface générique pour les repositories.
 *
 * @param <T>  le type d'entité
 * @param <ID> le type d'identifiant
 */
public interface Repository<T, ID> {

    /**
     * Trouve une entité par son identifiant.
     */
    CompletableFuture<Optional<T>> findById(@NotNull ID id);

    /**
     * Récupère toutes les entités.
     */
    CompletableFuture<List<T>> findAll();

    /**
     * Sauvegarde une entité (insert ou update).
     */
    CompletableFuture<T> save(@NotNull T entity);

    /**
     * Supprime une entité par son identifiant.
     *
     * @return true si l'entité a été supprimée
     */
    CompletableFuture<Boolean> deleteById(@NotNull ID id);

    /**
     * Vérifie si une entité existe.
     */
    CompletableFuture<Boolean> existsById(@NotNull ID id);

    /**
     * Compte le nombre total d'entités.
     */
    CompletableFuture<Long> count();
}
