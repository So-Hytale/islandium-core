package com.islandium.mixins.hooks;

import java.util.UUID;

/**
 * Hook pour les degats d'entite.
 * Retourne true si les degats doivent etre BLOQUES.
 *
 * DUPLIQUE depuis islandium-mixins.
 */
@FunctionalInterface
public interface DamageHook {
    boolean shouldBlockDamage(UUID victimUuid, String cause, float amount);
}
