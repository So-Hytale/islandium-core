package com.islandium.core.mixin;

import com.hypixel.hytale.server.spawning.SpawningPlugin;
import com.islandium.core.api.event.IslandiumEventBus;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Mixin pour le systeme de spawn NPC.
 * Hook dans SpawningPlugin pour fire EntitySpawnEvent avant la creation d'entites.
 * Note: Le point d'injection exact sera determine apres analyse
 * de la chaine de spawn (LocalSpawnController -> NPCEntity creation).
 */
@Mixin(SpawningPlugin.class)
public abstract class SpawningPluginMixin {
    // Le hook sera ajoute sur la methode qui cree effectivement les NPCEntity.
    // SpawningPlugin est le singleton qui gere les spawns - on devra peut-etre
    // cibler une classe plus specifique comme les spawn systems internes.
}
