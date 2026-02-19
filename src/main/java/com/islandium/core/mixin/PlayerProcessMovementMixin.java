package com.islandium.core.mixin;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.modules.entity.player.PlayerProcessMovementSystem;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.islandium.core.api.event.IslandiumEventBus;
import com.islandium.core.api.event.movement.PlayerMoveEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerProcessMovementSystem.class)
public abstract class PlayerProcessMovementMixin {

    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(float deltaTime, int entityIndex,
                        ArchetypeChunk<EntityStore> chunk,
                        Store<EntityStore> store,
                        CommandBuffer<EntityStore> commandBuffer,
                        CallbackInfo ci) {
        // Le PlayerMoveEvent sera fire ici une fois qu'on aura acces
        // aux positions before/after dans le tick.
        // Pour l'instant c'est un hook de base, l'event sera enrichi
        // avec les positions via @Local ou en wrappant le tick complet.
        if (!IslandiumEventBus.isAvailable()) return;
        // Note: Les positions exactes seront extraites via le TransformComponent
        // dans une future iteration plus precise du hook.
    }
}
