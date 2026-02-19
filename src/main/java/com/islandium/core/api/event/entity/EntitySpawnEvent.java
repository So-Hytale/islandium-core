package com.islandium.core.api.event.entity;

import com.islandium.core.api.event.IslandiumEvent;
import org.jetbrains.annotations.NotNull;

/**
 * Declenche quand un mob/NPC spawn dans le monde.
 * Cancellable : annuler empeche le spawn.
 */
public class EntitySpawnEvent extends IslandiumEvent {

    private final String npcTypeId;
    private final double x, y, z;
    private final int environment;

    public EntitySpawnEvent(@NotNull String npcTypeId, double x, double y, double z, int environment) {
        this.npcTypeId = npcTypeId;
        this.x = x;
        this.y = y;
        this.z = z;
        this.environment = environment;
    }

    @NotNull
    public String getNpcTypeId() { return npcTypeId; }

    public double getX() { return x; }
    public double getY() { return y; }
    public double getZ() { return z; }
    public int getEnvironment() { return environment; }
}
