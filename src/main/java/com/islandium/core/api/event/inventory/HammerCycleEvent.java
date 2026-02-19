package com.islandium.core.api.event.inventory;

import com.islandium.core.api.event.IslandiumEvent;
import org.jetbrains.annotations.NotNull;

/**
 * Declenche quand un joueur cycle le hammer (change de block set).
 * Cancellable : annuler empeche le cycle.
 */
public class HammerCycleEvent extends IslandiumEvent {

    private final String newItemId;

    public HammerCycleEvent(@NotNull String newItemId) {
        this.newItemId = newItemId;
    }

    @NotNull
    public String getNewItemId() { return newItemId; }
}
