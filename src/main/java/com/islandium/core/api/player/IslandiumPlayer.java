package com.islandium.core.api.player;

import com.hypixel.hytale.server.core.Message;
import com.islandium.core.api.location.ServerLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Représente un joueur avec les données Essentials.
 * Wrapper enrichi autour du joueur Hytale.
 */
public interface IslandiumPlayer {

    /**
     * @return l'UUID du joueur
     */
    @NotNull
    UUID getUniqueId();

    /**
     * @return le nom du joueur
     */
    @NotNull
    String getName();

    /**
     * @return true si le joueur est online sur ce serveur
     */
    boolean isOnline();

    /**
     * @return true si le joueur est online sur le réseau (n'importe quel serveur)
     */
    boolean isOnlineNetwork();

    /**
     * @return le serveur actuel du joueur ou null si offline
     */
    @Nullable
    String getCurrentServer();

    // === États Custom (Islandium) ===

    /**
     * Vérifie si le joueur a un état custom spécifique.
     * Note: Pour les états natifs, utiliser les méthodes dédiées (isVanished, isGodMode, canFly).
     */
    boolean hasState(@NotNull PlayerState state);

    /**
     * Ajoute un état custom au joueur.
     */
    void addState(@NotNull PlayerState state);

    /**
     * Retire un état custom au joueur.
     */
    void removeState(@NotNull PlayerState state);

    /**
     * @return true si le joueur est AFK
     */
    default boolean isAfk() {
        return hasState(PlayerState.AFK);
    }

    /**
     * Définit l'état AFK du joueur.
     */
    default void setAfk(boolean afk) {
        if (afk) addState(PlayerState.AFK);
        else removeState(PlayerState.AFK);
    }

    // === États Natifs Hytale ===

    /**
     * @return true si le joueur est vanish (utilise HiddenPlayersManager natif)
     */
    boolean isVanished();

    /**
     * Définit l'état vanish du joueur (utilise HiddenPlayersManager natif).
     * Cache/montre le joueur à tous les autres joueurs.
     */
    void setVanished(boolean vanished);

    /**
     * @return true si le joueur est en god mode (utilise Invulnerable component natif)
     */
    boolean isGodMode();

    /**
     * Définit l'état god mode du joueur (utilise Invulnerable component natif).
     */
    void setGodMode(boolean godMode);

    /**
     * @return true si le joueur peut voler (utilise MovementManager natif)
     */
    boolean canFly();

    /**
     * Définit si le joueur peut voler (utilise MovementManager natif).
     */
    void setCanFly(boolean canFly);

    // === Économie ===

    /**
     * @return le solde du joueur
     */
    @NotNull
    BigDecimal getBalance();

    /**
     * Définit le solde du joueur.
     */
    CompletableFuture<Void> setBalance(@NotNull BigDecimal balance);

    /**
     * Ajoute au solde du joueur.
     */
    CompletableFuture<Void> addBalance(@NotNull BigDecimal amount);

    /**
     * Retire du solde du joueur.
     *
     * @return true si le joueur avait assez d'argent
     */
    CompletableFuture<Boolean> removeBalance(@NotNull BigDecimal amount);

    /**
     * Vérifie si le joueur a assez d'argent.
     */
    boolean hasBalance(@NotNull BigDecimal amount);

    // === Location ===

    /**
     * @return la location actuelle du joueur
     */
    @Nullable
    ServerLocation getLocation();

    /**
     * @return la dernière location connue du joueur
     */
    @Nullable
    ServerLocation getLastLocation();

    /**
     * @return le serveur actuel du joueur
     */
    @NotNull
    String getServer();

    /**
     * Téléporte le joueur vers une location (cross-server si nécessaire).
     * @return CompletableFuture contenant true si la téléportation a réussi
     */
    CompletableFuture<Boolean> teleport(@NotNull ServerLocation location);

    // === Timestamps ===

    /**
     * @return le timestamp de première connexion
     */
    long getFirstLogin();

    /**
     * @return le timestamp de dernière connexion
     */
    long getLastLogin();

    // === Messaging ===

    /**
     * Envoie un message au joueur (cross-server si nécessaire).
     */
    void sendMessage(@NotNull String message);

    /**
     * Envoie un message formaté au joueur.
     */
    void sendMessage(@NotNull String message, Object... args);

    /**
     * Envoie un message Hytale au joueur.
     */
    void sendMessage(@NotNull Message message);

    // === Permissions ===

    /**
     * Vérifie si le joueur a une permission.
     */
    boolean hasPermission(@NotNull String permission);

    /**
     * Vérifie si le joueur est un opérateur (groupe "OP" natif Hytale).
     * Les opérateurs ont toutes les permissions.
     */
    boolean isOp();

    // === Actions ===

    /**
     * Soigne le joueur (vie et effets).
     */
    void heal();

    /**
     * Nourrit le joueur.
     */
    void feed();

    /**
     * Change le mode de jeu du joueur.
     */
    void setGameMode(@NotNull String gameMode);

    /**
     * Expulse le joueur du serveur.
     */
    void kick(@NotNull String reason);

    // === Messaging ===

    /**
     * @return l'UUID du dernier joueur ayant envoyé un message privé
     */
    @Nullable
    UUID getLastMessageSender();

    /**
     * Définit le dernier envoyeur de message privé (pour /reply).
     */
    void setLastMessageSender(@Nullable UUID uuid);

    // === Data ===

    /**
     * Force la sauvegarde des données du joueur.
     */
    CompletableFuture<Void> save();

    /**
     * Force le rechargement des données du joueur depuis la BDD.
     */
    CompletableFuture<Void> reload();

    // === Hytale Integration ===

    /**
     * @return le joueur Hytale natif (Player ou PlayerRef), ou null si offline
     */
    @Nullable
    com.hypixel.hytale.server.core.entity.entities.Player getHytalePlayer();
}
