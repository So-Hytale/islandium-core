package com.islandium.core.mixin;

import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageSystems;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.islandium.core.api.event.IslandiumEventBus;
import com.islandium.core.api.event.damage.EntityDamageEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DamageSystems.class)
public abstract class DamageSystemsMixin {

    @Inject(method = "executeDamage(Lcom/hypixel/hytale/component/Ref;Lcom/hypixel/hytale/component/ComponentAccessor;Lcom/hypixel/hytale/server/core/modules/entity/damage/Damage;)V",
            at = @At("HEAD"), cancellable = true)
    private static void onExecuteDamage(Ref<EntityStore> victim, ComponentAccessor<EntityStore> accessor,
                                        Damage damage, CallbackInfo ci) {
        if (!IslandiumEventBus.isAvailable()) return;

        EntityDamageEvent event = new EntityDamageEvent(
                victim,
                null, // Source doesn't expose entity ref directly
                damage.getCause(),
                damage.getAmount()
        );

        IslandiumEventBus.get().fire(event);

        if (event.isCancelled()) {
            ci.cancel();
        }
    }
}
