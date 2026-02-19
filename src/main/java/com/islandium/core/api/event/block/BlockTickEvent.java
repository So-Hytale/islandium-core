package com.islandium.core.api.event.block;

import com.islandium.core.api.event.IslandiumEvent;

/**
 * Declenche quand un bloc est tick (croissance, changement d'etat, etc.).
 * Cancellable : annuler empeche le tick du bloc.
 */
public class BlockTickEvent extends IslandiumEvent {

    private final int blockX, blockY, blockZ;
    private final int blockType;

    public BlockTickEvent(int blockX, int blockY, int blockZ, int blockType) {
        this.blockX = blockX;
        this.blockY = blockY;
        this.blockZ = blockZ;
        this.blockType = blockType;
    }

    public int getBlockX() { return blockX; }
    public int getBlockY() { return blockY; }
    public int getBlockZ() { return blockZ; }
    public int getBlockType() { return blockType; }
}
