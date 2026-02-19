package com.islandium.mixins.hooks;

import java.util.UUID;

/**
 * Hook pour le pickup d'items (Player.giveItem).
 * Retourne true si le pickup doit etre BLOQUE.
 *
 * DUPLIQUE depuis islandium-mixins.
 */
@FunctionalInterface
public interface ItemPickupHook {
    boolean shouldBlockPickup(UUID playerUuid, String itemId);
}
