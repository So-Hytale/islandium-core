package com.islandium.core.listener;

import com.islandium.core.IslandiumPlugin;
import com.islandium.core.listener.base.IslandiumListener;
import com.hypixel.hytale.event.EventRegistry;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

/**
 * Gestionnaire des listeners.
 */
public class ListenerManager {

    private final IslandiumPlugin plugin;
    private final List<IslandiumListener> listeners = new ArrayList<>();

    public ListenerManager(@NotNull IslandiumPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Enregistre tous les listeners.
     */
    public void registerAll() {
        EventRegistry eventRegistry = plugin.getEventRegistry();

        register(new PlayerJoinListener(plugin), eventRegistry);
        register(new PlayerQuitListener(plugin), eventRegistry);
        register(new PlayerChatListener(plugin), eventRegistry);
        register(new PlayerMoveListener(plugin), eventRegistry);

        plugin.log(Level.INFO, "Registered " + listeners.size() + " listeners");
    }

    private void register(@NotNull IslandiumListener listener, @NotNull EventRegistry registry) {
        listener.register(registry);
        listeners.add(listener);
    }

    /**
     * Désenregistre tous les listeners.
     */
    public void unregisterAll() {
        listeners.forEach(IslandiumListener::unregister);
        listeners.clear();
    }

    @NotNull
    public List<IslandiumListener> getListeners() {
        return listeners;
    }
}
