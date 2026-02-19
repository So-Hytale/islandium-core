package com.islandium.core.hook;

import com.islandium.core.api.event.IslandiumEvent;

import java.util.UUID;

/**
 * Event simplifie pour le pickup d'items, fire par le hook.
 * Contient uniquement les donnees brutes (UUID, itemId)
 * car le mixin dans earlyplugins/ n'a pas acces aux classes islandium-core.
 */
public class ItemPickupSimpleEvent extends IslandiumEvent {

    private final UUID playerUuid;
    private final String itemId;

    public ItemPickupSimpleEvent(UUID playerUuid, String itemId) {
        this.playerUuid = playerUuid;
        this.itemId = itemId;
    }

    public UUID getPlayerUuid() { return playerUuid; }
    public String getItemId() { return itemId; }
}
