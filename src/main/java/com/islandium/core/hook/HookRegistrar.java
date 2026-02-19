package com.islandium.core.hook;

import com.islandium.core.api.event.IslandiumEventBus;
import com.islandium.core.api.event.server.ServerBootEvent;
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
 */
public final class HookRegistrar {

    private static final Logger LOGGER = Logger.getLogger("Essentials");

    public static void registerAll() {
        LOGGER.log(Level.INFO, "[Essentials] Registering mixin hooks...");

        // 1. Harvest hook (touche F sur rubble)
        HookRegistry.register(HookRegistry.HARVEST_HOOK, (HarvestHook) (playerUuid, blockTypeId, x, y, z) -> {
            if (!IslandiumEventBus.isAvailable()) return false;
            HarvestBlockSimpleEvent event = new HarvestBlockSimpleEvent(playerUuid, blockTypeId, x, y, z);
            IslandiumEventBus.get().fire(event);
            return event.isCancelled();
        });

        // 2. Item pickup hook
        HookRegistry.register(HookRegistry.ITEM_PICKUP_HOOK, (ItemPickupHook) (playerUuid, itemId) -> {
            if (!IslandiumEventBus.isAvailable()) return false;
            ItemPickupSimpleEvent event = new ItemPickupSimpleEvent(playerUuid, itemId);
            IslandiumEventBus.get().fire(event);
            return event.isCancelled();
        });

        // 3. Damage hook
        HookRegistry.register(HookRegistry.DAMAGE_HOOK, (DamageHook) (victimUuid, cause, amount) -> {
            if (!IslandiumEventBus.isAvailable()) return false;
            DamageSimpleEvent event = new DamageSimpleEvent(victimUuid, cause, amount);
            IslandiumEventBus.get().fire(event);
            return event.isCancelled();
        });

        // 4. Server boot hook
        HookRegistry.register(HookRegistry.SERVER_BOOT_HOOK, (ServerBootHook) () -> {
            if (!IslandiumEventBus.isAvailable()) return;
            IslandiumEventBus.get().fire(new ServerBootEvent());
        });

        LOGGER.log(Level.INFO, "[Essentials] All mixin hooks registered successfully!");
    }

    public static void unregisterAll() {
        HookRegistry.unregister(HookRegistry.HARVEST_HOOK);
        HookRegistry.unregister(HookRegistry.ITEM_PICKUP_HOOK);
        HookRegistry.unregister(HookRegistry.DAMAGE_HOOK);
        HookRegistry.unregister(HookRegistry.SERVER_BOOT_HOOK);
        LOGGER.log(Level.INFO, "[Essentials] All mixin hooks unregistered.");
    }
}
