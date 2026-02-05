package com.islandium.core.api.player;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Fournit l'accès aux IslandiumPlayer.
 */
public interface PlayerProvider {

    /**
     * Récupère un joueur par son UUID (cache ou BDD).
     *
     * @param uuid l'UUID du joueur
     * @return le joueur ou empty si non trouvé
     */
    CompletableFuture<Optional<IslandiumPlayer>> getPlayer(@NotNull UUID uuid);

    /**
     * Récupère un joueur par son nom (cache ou BDD).
     *
     * @param name le nom du joueur
     * @return le joueur ou empty si non trouvé
     */
    CompletableFuture<Optional<IslandiumPlayer>> getPlayer(@NotNull String name);

    /**
     * Récupère un joueur online sur ce serveur.
     *
     * @param uuid l'UUID du joueur
     * @return le joueur ou empty si non online sur ce serveur
     */
    @NotNull
    Optional<IslandiumPlayer> getOnlinePlayer(@NotNull UUID uuid);

    /**
     * Récupère un joueur online sur ce serveur par son nom.
     *
     * @param name le nom du joueur
     * @return le joueur ou empty si non online sur ce serveur
     */
    @NotNull
    Optional<IslandiumPlayer> getOnlinePlayer(@NotNull String name);

    /**
     * @return tous les joueurs online sur ce serveur (local)
     */
    @NotNull
    Collection<IslandiumPlayer> getOnlinePlayersLocal();

    /**
     * @return tous les joueurs online (async pour support cross-server)
     */
    @NotNull
    CompletableFuture<Collection<IslandiumPlayer>> getOnlinePlayers();

    /**
     * @return le nombre de joueurs online sur ce serveur
     */
    int getOnlineCount();

    /**
     * @return le nombre total de joueurs online sur le réseau
     */
    CompletableFuture<Integer> getNetworkOnlineCount();

    /**
     * Vérifie si un joueur est online sur le réseau.
     *
     * @param uuid l'UUID du joueur
     * @return true si le joueur est online quelque part
     */
    CompletableFuture<Boolean> isOnlineNetwork(@NotNull UUID uuid);

    /**
     * Récupère le serveur sur lequel un joueur est connecté.
     *
     * @param uuid l'UUID du joueur
     * @return le nom du serveur ou empty si offline
     */
    CompletableFuture<Optional<String>> getPlayerServer(@NotNull UUID uuid);

    /**
     * Récupère ou crée un joueur (pour les nouveaux joueurs).
     *
     * @param uuid l'UUID du joueur
     * @param name le nom du joueur
     * @return le joueur existant ou nouvellement créé
     */
    CompletableFuture<IslandiumPlayer> getOrCreate(@NotNull UUID uuid, @NotNull String name);
}
