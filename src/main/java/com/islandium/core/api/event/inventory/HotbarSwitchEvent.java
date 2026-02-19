package com.islandium.core.api.event.inventory;

import com.islandium.core.api.event.IslandiumEvent;

/**
 * Declenche quand un joueur change de slot actif dans la hotbar.
 * Cancellable : annuler empeche le changement.
 */
public class HotbarSwitchEvent extends IslandiumEvent {

    private final int inventorySectionId;
    private final int newSlot;

    public HotbarSwitchEvent(int inventorySectionId, int newSlot) {
        this.inventorySectionId = inventorySectionId;
        this.newSlot = newSlot;
    }

    public int getInventorySectionId() { return inventorySectionId; }
    public int getNewSlot() { return newSlot; }
}
