package com.islandium.core.mixin;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.modules.entity.player.PlayerItemEntityPickupSystem;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.islandium.core.api.event.IslandiumEventBus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerItemEntityPickupSystem.class)
public abstract class PlayerItemEntityPickupMixin {

    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(float deltaTime, int entityIndex,
                        ArchetypeChunk<EntityStore> chunk,
                        Store<EntityStore> store,
                        CommandBuffer<EntityStore> commandBuffer,
                        CallbackInfo ci) {
        // Hook point pour le systeme de pickup.
        // L'ItemPickupEvent sera fire quand le pickup est detecte.
        // Le vrai point d'injection est dans le lambda interne
        // qui appelle InteractivelyPickupItemEvent - on hook plutot
        // via l'event natif Hytale InteractivelyPickupItemEvent.
        if (!IslandiumEventBus.isAvailable()) return;
    }
}
