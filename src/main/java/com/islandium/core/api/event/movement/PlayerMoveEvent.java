package com.islandium.core.api.event.movement;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.islandium.core.api.event.IslandiumEvent;
import org.jetbrains.annotations.NotNull;

/**
 * Declenche quand un joueur se deplace.
 * Cancellable : annuler revient a la position precedente.
 */
public class PlayerMoveEvent extends IslandiumEvent {

    private final Ref<EntityStore> player;
    private final double fromX, fromY, fromZ;
    private double toX, toY, toZ;

    public PlayerMoveEvent(@NotNull Ref<EntityStore> player,
                           double fromX, double fromY, double fromZ,
                           double toX, double toY, double toZ) {
        this.player = player;
        this.fromX = fromX;
        this.fromY = fromY;
        this.fromZ = fromZ;
        this.toX = toX;
        this.toY = toY;
        this.toZ = toZ;
    }

    @NotNull
    public Ref<EntityStore> getPlayer() { return player; }

    public double getFromX() { return fromX; }
    public double getFromY() { return fromY; }
    public double getFromZ() { return fromZ; }

    public double getToX() { return toX; }
    public double getToY() { return toY; }
    public double getToZ() { return toZ; }

    public void setToX(double toX) { this.toX = toX; }
    public void setToY(double toY) { this.toY = toY; }
    public void setToZ(double toZ) { this.toZ = toZ; }
}
