package com.islandium.core.api.event.damage;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageCause;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.islandium.core.api.event.IslandiumEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Declenche quand une entite recoit des degats (avant application).
 * Cancellable : annuler empeche les degats.
 */
public class EntityDamageEvent extends IslandiumEvent {

    private final Ref<EntityStore> victim;
    private final Ref<EntityStore> attacker;
    private final DamageCause cause;
    private float amount;

    public EntityDamageEvent(@NotNull Ref<EntityStore> victim, @Nullable Ref<EntityStore> attacker,
                             @NotNull DamageCause cause, float amount) {
        this.victim = victim;
        this.attacker = attacker;
        this.cause = cause;
        this.amount = amount;
    }

    @NotNull
    public Ref<EntityStore> getVictim() { return victim; }

    @Nullable
    public Ref<EntityStore> getAttacker() { return attacker; }

    @NotNull
    public DamageCause getCause() { return cause; }

    public float getAmount() { return amount; }

    public void setAmount(float amount) { this.amount = amount; }
}
