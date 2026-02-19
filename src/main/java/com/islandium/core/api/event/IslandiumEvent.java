package com.islandium.core.api.event;

/**
 * Classe de base pour tous les evenements custom Islandium.
 * Les evenements cancellables peuvent etre annules par les handlers.
 */
public abstract class IslandiumEvent {

    private boolean cancelled = false;

    public boolean isCancelled() {
        return cancelled;
    }

    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }
}
