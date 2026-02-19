package com.islandium.core.api.event.death;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageCause;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.islandium.core.api.event.IslandiumEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Declenche quand une entite meurt.
 * Cancellable : annuler empeche la mort.
 */
public class EntityDeathEvent extends IslandiumEvent {

    private final Ref<EntityStore> entity;
    private final DamageCause cause;
    private String deathMessage;
    private boolean dropItems;

    public EntityDeathEvent(@NotNull Ref<EntityStore> entity, @Nullable DamageCause cause,
                            @Nullable String deathMessage, boolean dropItems) {
        this.entity = entity;
        this.cause = cause;
        this.deathMessage = deathMessage;
        this.dropItems = dropItems;
    }

    @NotNull
    public Ref<EntityStore> getEntity() { return entity; }

    @Nullable
    public DamageCause getCause() { return cause; }

    @Nullable
    public String getDeathMessage() { return deathMessage; }

    public void setDeathMessage(@Nullable String deathMessage) { this.deathMessage = deathMessage; }

    public boolean isDropItems() { return dropItems; }

    public void setDropItems(boolean dropItems) { this.dropItems = dropItems; }
}
