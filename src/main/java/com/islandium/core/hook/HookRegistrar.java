package com.islandium.core.hook;

import com.islandium.core.api.event.IslandiumEventBus;
import com.islandium.mixins.HookRegistry;
import com.islandium.mixins.hooks.*;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Enregistre les implementations de hook dans le HookRegistry.
 * Chaque hook delegue a IslandiumEventBus pour que les listeners
 * (ex: HarvestBlockBusListener dans islandium-regions) fonctionnent.
 *
 * Appele depuis IslandiumPlugin.setup() apres l'init de l'EventBus.
 *
 * NOTE: Seul le harvest hook est actif pour l'instant.
 * Les autres (@Inject) sont desactives car le TransformingClassLoader
 * de Hyxin ne peut pas resoudre CallbackInfo au runtime.
 * Ils seront reactives quand on les convertira en @Redirect.
 */
public final class HookRegistrar {

    private static final Logger LOGGER = Logger.getLogger("Essentials");

    public static void registerAll() {
        LOGGER.log(Level.INFO, "[Essentials] Registering mixin hooks...");

        // 1. Harvest hook (touche F sur rubble) — utilise @Redirect, pas de CallbackInfo
        HookRegistry.register(HookRegistry.HARVEST_HOOK, (HarvestHook) (playerUuid, blockTypeId, x, y, z) -> {
            if (!IslandiumEventBus.isAvailable()) return false;
            HarvestBlockSimpleEvent event = new HarvestBlockSimpleEvent(playerUuid, blockTypeId, x, y, z);
            IslandiumEventBus.get().fire(event);
            return event.isCancelled();
        });

        LOGGER.log(Level.INFO, "[Essentials] Harvest hook registered successfully!");
    }

    public static void unregisterAll() {
        HookRegistry.unregister(HookRegistry.HARVEST_HOOK);
        LOGGER.log(Level.INFO, "[Essentials] All mixin hooks unregistered.");
    }
}
