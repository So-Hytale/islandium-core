package com.islandium.core.service.spawn;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.islandium.core.IslandiumPlugin;
import com.islandium.core.api.location.ServerLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Level;

/**
 * Service de gestion du spawn serveur.
 */
public class SpawnService {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final IslandiumPlugin plugin;
    private final Path spawnFile;
    private SpawnData spawnData;

    public SpawnService(@NotNull IslandiumPlugin plugin) {
        this.plugin = plugin;
        this.spawnFile = plugin.getDataFolder().toPath().resolve("spawn.json");
    }

    /**
     * Charge les données du spawn depuis le fichier.
     */
    public void load() {
        try {
            if (Files.exists(spawnFile)) {
                String content = Files.readString(spawnFile);
                this.spawnData = GSON.fromJson(content, SpawnData.class);
                plugin.log(Level.INFO, "Spawn loaded: " + (spawnData.spawn != null ? spawnData.spawn : "not set"));
            } else {
                this.spawnData = new SpawnData();
                save();
                plugin.log(Level.INFO, "Created spawn.json (spawn not set)");
            }
        } catch (IOException e) {
            plugin.log(Level.WARNING, "Failed to load spawn: " + e.getMessage());
            this.spawnData = new SpawnData();
        }
    }

    /**
     * Sauvegarde les données du spawn.
     */
    public void save() {
        try {
            Files.writeString(spawnFile, GSON.toJson(spawnData));
        } catch (IOException e) {
            plugin.log(Level.WARNING, "Failed to save spawn: " + e.getMessage());
        }
    }

    /**
     * Définit le spawn.
     */
    public void setSpawn(@NotNull ServerLocation location) {
        spawnData.spawn = location.serialize();
        save();
        plugin.log(Level.INFO, "Spawn set to: " + location);
    }

    /**
     * Obtient le spawn actuel.
     */
    @Nullable
    public ServerLocation getSpawn() {
        if (spawnData.spawn == null || spawnData.spawn.isEmpty()) {
            return null;
        }
        return ServerLocation.deserialize(spawnData.spawn);
    }

    /**
     * Vérifie si le spawn est défini.
     */
    public boolean isSpawnSet() {
        return spawnData.spawn != null && !spawnData.spawn.isEmpty();
    }

    /**
     * Supprime le spawn.
     */
    public void clearSpawn() {
        spawnData.spawn = null;
        save();
        plugin.log(Level.INFO, "Spawn cleared");
    }

    /**
     * Classe interne pour la sérialisation JSON.
     */
    private static class SpawnData {
        String spawn;
    }
}
