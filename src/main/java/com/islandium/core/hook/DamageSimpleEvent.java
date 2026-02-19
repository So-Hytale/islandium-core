package com.islandium.core.hook;

import com.islandium.core.api.event.IslandiumEvent;

import java.util.UUID;

/**
 * Event simplifie pour les degats, fire par le hook.
 * Contient uniquement les donnees brutes (UUID victime, cause, montant).
 */
public class DamageSimpleEvent extends IslandiumEvent {

    private final UUID victimUuid;
    private final String cause;
    private final float amount;

    public DamageSimpleEvent(UUID victimUuid, String cause, float amount) {
        this.victimUuid = victimUuid;
        this.cause = cause;
        this.amount = amount;
    }

    public UUID getVictimUuid() { return victimUuid; }
    public String getCause() { return cause; }
    public float getAmount() { return amount; }
}
