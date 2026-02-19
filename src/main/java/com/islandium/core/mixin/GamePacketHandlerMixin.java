package com.islandium.core.mixin;

import com.hypixel.hytale.protocol.Packet;
import com.hypixel.hytale.protocol.ToClientPacket;
import com.hypixel.hytale.protocol.ToServerPacket;
import com.hypixel.hytale.server.core.io.handlers.GenericPacketHandler;
import com.islandium.core.api.event.IslandiumEventBus;
import com.islandium.core.api.event.network.PacketReceiveEvent;
import com.islandium.core.api.event.network.PacketSendEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GenericPacketHandler.class)
public abstract class GamePacketHandlerMixin {

    @Inject(method = "accept", at = @At("HEAD"), cancellable = true)
    private void onAcceptPacket(ToServerPacket packet, CallbackInfo ci) {
        if (!IslandiumEventBus.isAvailable()) return;

        PacketReceiveEvent event = new PacketReceiveEvent(packet, ((Packet) packet).getId());
        IslandiumEventBus.get().fire(event);

        if (event.isCancelled()) {
            ci.cancel();
        }
    }
}
