package com.islandium.core.listener.base;

import com.islandium.core.IslandiumPlugin;
import com.hypixel.hytale.event.EventRegistry;
import org.jetbrains.annotations.NotNull;

/**
 * Classe de base pour tous les listeners Essentials.
 */
public abstract class IslandiumListener {

    protected final IslandiumPlugin plugin;

    protected IslandiumListener(@NotNull IslandiumPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Enregistre ce listener auprès de l'EventRegistry.
     */
    public abstract void register(@NotNull EventRegistry registry);

    /**
     * Désenregistre ce listener.
     */
    public void unregister() {
        // Par défaut, ne fait rien - les listeners peuvent override si nécessaire
    }

    @NotNull
    protected IslandiumPlugin getPlugin() {
        return plugin;
    }
}
