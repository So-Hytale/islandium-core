package com.islandium.core.api.event.block;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.islandium.core.api.event.IslandiumEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Fire quand un joueur appuie sur F pour harvest/ramasser un bloc (rubble, etc.).
 * Cancellable : annuler empêche la destruction du bloc et le drop de l'item.
 *
 * Fire depuis BlockHarvestMixin (intercepte BlockHarvestUtils.performPickupByInteraction).
 */
public class HarvestBlockEvent extends IslandiumEvent {

    private final Ref<EntityStore> playerRef;
    private final Player playerEntity;
    private final Vector3i blockPos;
    private final BlockType blockType;

    public HarvestBlockEvent(@NotNull Ref<EntityStore> playerRef,
                             @Nullable Player playerEntity,
                             @NotNull Vector3i blockPos,
                             @NotNull BlockType blockType) {
        this.playerRef = playerRef;
        this.playerEntity = playerEntity;
        this.blockPos = blockPos;
        this.blockType = blockType;
    }

    @NotNull
    public Ref<EntityStore> getPlayerRef() { return playerRef; }

    @Nullable
    public Player getPlayerEntity() { return playerEntity; }

    @NotNull
    public Vector3i getBlockPos() { return blockPos; }

    @NotNull
    public BlockType getBlockType() { return blockType; }
}
