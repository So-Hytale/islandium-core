package com.islandium.core.mixin;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.islandium.core.api.event.IslandiumEventBus;
import com.islandium.core.api.event.death.EntityDeathEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DeathComponent.class)
public abstract class DeathComponentMixin {

    @Shadow
    public abstract com.hypixel.hytale.server.core.modules.entity.damage.DamageCause getDeathCause();

    @Inject(method = "<init>(Lcom/hypixel/hytale/server/core/modules/entity/damage/Damage;)V",
            at = @At("RETURN"))
    private void onDeathComponentCreated(Damage damage, CallbackInfo ci) {
        // L'event sera fire depuis le systeme qui ajoute le composant
    }
}
