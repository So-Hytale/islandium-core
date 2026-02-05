package com.islandium.core.config;

import com.islandium.core.IslandiumPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Level;

/**
 * Gestionnaire des fichiers de configuration.
 */
public class ConfigManager {

    private final IslandiumPlugin plugin;
    private final Path dataFolder;

    private MainConfig mainConfig;
    private MessagesConfig messagesConfig;

    public ConfigManager(@NotNull IslandiumPlugin plugin) {
        this.plugin = plugin;
        this.dataFolder = plugin.getDataFolder().toPath();
    }

    /**
     * Charge toutes les configurations.
     */
    public void load() {
        try {
            // Create data folder if needed
            if (!Files.exists(dataFolder)) {
                Files.createDirectories(dataFolder);
            }

            // Load configs
            this.mainConfig = new MainConfig(dataFolder.resolve("config.yml"));
            mainConfig.load();

            this.messagesConfig = new MessagesConfig(dataFolder.resolve("messages.yml"));
            messagesConfig.load();

            plugin.log(Level.INFO, "Configuration loaded successfully");

        } catch (IOException e) {
            plugin.log(Level.SEVERE, "Failed to load configuration: " + e.getMessage());
            throw new RuntimeException("Failed to load configuration", e);
        }
    }

    /**
     * Recharge toutes les configurations.
     */
    public void reload() {
        load();
    }

    @NotNull
    public MainConfig getMainConfig() {
        return mainConfig;
    }

    @NotNull
    public MessagesConfig getMessages() {
        return messagesConfig;
    }

    @NotNull
    public Path getDataFolder() {
        return dataFolder;
    }
}
