package com.islandium.mixins.hooks;

import java.util.UUID;

/**
 * Hook pour le harvest de bloc (touche F sur rubble).
 * Retourne true si le harvest doit etre BLOQUE.
 *
 * DUPLIQUE depuis islandium-mixins.
 */
@FunctionalInterface
public interface HarvestHook {
    boolean shouldBlockHarvest(UUID playerUuid, String blockTypeId, int x, int y, int z);
}
