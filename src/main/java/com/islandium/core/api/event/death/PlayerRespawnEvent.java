package com.islandium.core.api.event.death;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.islandium.core.api.event.IslandiumEvent;
import org.jetbrains.annotations.NotNull;

/**
 * Declenche quand un joueur respawn apres sa mort.
 * Non cancellable.
 */
public class PlayerRespawnEvent extends IslandiumEvent {

    private final Ref<EntityStore> player;

    public PlayerRespawnEvent(@NotNull Ref<EntityStore> player) {
        this.player = player;
    }

    @NotNull
    public Ref<EntityStore> getPlayer() { return player; }
}
