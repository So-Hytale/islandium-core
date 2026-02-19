package com.islandium.mixins.hooks;

/**
 * Hook pour les changements d'inventaire (hotbar switch, hammer cycle).
 * Retourne true si l'action doit etre BLOQUEE.
 *
 * DUPLIQUE depuis islandium-mixins.
 */
@FunctionalInterface
public interface InventoryHook {
    boolean shouldBlockInventoryAction(String actionType, Object packet);
}
