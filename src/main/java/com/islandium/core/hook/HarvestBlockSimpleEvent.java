package com.islandium.core.hook;

import com.islandium.core.api.event.IslandiumEvent;

import java.util.UUID;

/**
 * Event simplifie pour le harvest de bloc, fire par le hook.
 * Contient uniquement les donnees brutes (UUID, blockId, position)
 * car le mixin dans earlyplugins/ n'a pas acces aux classes islandium-core.
 */
public class HarvestBlockSimpleEvent extends IslandiumEvent {

    private final UUID playerUuid;
    private final String blockTypeId;
    private final int x, y, z;

    public HarvestBlockSimpleEvent(UUID playerUuid, String blockTypeId, int x, int y, int z) {
        this.playerUuid = playerUuid;
        this.blockTypeId = blockTypeId;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public UUID getPlayerUuid() { return playerUuid; }
    public String getBlockTypeId() { return blockTypeId; }
    public int getX() { return x; }
    public int getY() { return y; }
    public int getZ() { return z; }
}
