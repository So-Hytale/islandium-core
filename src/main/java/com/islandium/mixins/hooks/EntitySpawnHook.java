package com.islandium.mixins.hooks;

/**
 * Hook pour intercepter le spawn d'entites (NPC/mobs).
 * Retourne true pour BLOQUER le spawn, false pour l'autoriser.
 *
 * DUPLIQUE depuis islandium-mixins (meme package) pour eviter
 * une dependance compile entre les deux JARs.
 */
@FunctionalInterface
public interface EntitySpawnHook {
    boolean shouldBlockSpawn(String npcTypeId, double x, double y, double z);
}
