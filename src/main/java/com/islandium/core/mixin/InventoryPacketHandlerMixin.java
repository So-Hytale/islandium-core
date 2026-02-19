package com.islandium.core.mixin;

import com.hypixel.hytale.protocol.packets.inventory.SetActiveSlot;
import com.hypixel.hytale.protocol.packets.inventory.SwitchHotbarBlockSet;
import com.hypixel.hytale.server.core.io.handlers.game.InventoryPacketHandler;
import com.islandium.core.api.event.IslandiumEventBus;
import com.islandium.core.api.event.inventory.HammerCycleEvent;
import com.islandium.core.api.event.inventory.HotbarSwitchEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InventoryPacketHandler.class)
public abstract class InventoryPacketHandlerMixin {

    @Inject(method = "handle(Lcom/hypixel/hytale/protocol/packets/inventory/SetActiveSlot;)V",
            at = @At("HEAD"), cancellable = true)
    private void onSetActiveSlot(SetActiveSlot packet, CallbackInfo ci) {
        if (!IslandiumEventBus.isAvailable()) return;

        HotbarSwitchEvent event = new HotbarSwitchEvent(
                packet.inventorySectionId,
                packet.activeSlot
        );
        IslandiumEventBus.get().fire(event);

        if (event.isCancelled()) {
            ci.cancel();
        }
    }

    @Inject(method = "handle(Lcom/hypixel/hytale/protocol/packets/inventory/SwitchHotbarBlockSet;)V",
            at = @At("HEAD"), cancellable = true)
    private void onSwitchHotbarBlockSet(SwitchHotbarBlockSet packet, CallbackInfo ci) {
        if (!IslandiumEventBus.isAvailable()) return;

        HammerCycleEvent event = new HammerCycleEvent(packet.itemId);
        IslandiumEventBus.get().fire(event);

        if (event.isCancelled()) {
            ci.cancel();
        }
    }
}
