package com.islandium.core.service.back;

import com.islandium.core.IslandiumPlugin;
import com.islandium.core.api.location.ServerLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service pour stocker et restaurer les positions précédentes des joueurs.
 * Utilisé par la commande /back pour revenir à la dernière position avant téléportation.
 */
public class BackService {

    private final IslandiumPlugin plugin;
    private final Map<UUID, ServerLocation> previousLocations = new ConcurrentHashMap<>();

    public BackService(@NotNull IslandiumPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Sauvegarde la position précédente d'un joueur.
     * Appelé avant chaque téléportation.
     *
     * @param uuid     L'UUID du joueur
     * @param location La position à sauvegarder
     */
    public void saveLocation(@NotNull UUID uuid, @NotNull ServerLocation location) {
        previousLocations.put(uuid, location);
    }

    /**
     * Récupère la position précédente d'un joueur.
     *
     * @param uuid L'UUID du joueur
     * @return La position précédente ou null si aucune n'est enregistrée
     */
    @Nullable
    public ServerLocation getPreviousLocation(@NotNull UUID uuid) {
        return previousLocations.get(uuid);
    }

    /**
     * Vérifie si un joueur a une position précédente enregistrée.
     *
     * @param uuid L'UUID du joueur
     * @return true si une position est enregistrée
     */
    public boolean hasPreviousLocation(@NotNull UUID uuid) {
        return previousLocations.containsKey(uuid);
    }

    /**
     * Supprime la position précédente d'un joueur.
     * Appelé après un /back réussi ou quand le joueur se déconnecte.
     *
     * @param uuid L'UUID du joueur
     */
    public void clearLocation(@NotNull UUID uuid) {
        previousLocations.remove(uuid);
    }

    /**
     * Nettoie les données d'un joueur (déconnexion).
     *
     * @param uuid L'UUID du joueur
     */
    public void handlePlayerQuit(@NotNull UUID uuid) {
        // Optionnel: on peut garder la position pour permettre /back après reconnexion
        // Pour l'instant, on la supprime pour économiser la mémoire
        clearLocation(uuid);
    }
}
