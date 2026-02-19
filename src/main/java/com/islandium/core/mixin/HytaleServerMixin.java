package com.islandium.core.mixin;

import com.hypixel.hytale.server.core.HytaleServer;
import com.islandium.core.api.event.IslandiumEventBus;
import com.islandium.core.api.event.server.ServerBootEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HytaleServer.class)
public abstract class HytaleServerMixin {

    @Inject(method = "boot", at = @At("TAIL"))
    private void onBootComplete(CallbackInfo ci) {
        if (!IslandiumEventBus.isAvailable()) return;

        IslandiumEventBus.get().fire(new ServerBootEvent());
    }
}
