package com.islandium.core.mixin;

import com.hypixel.hytale.protocol.Packet;
import com.hypixel.hytale.protocol.ToClientPacket;
import com.hypixel.hytale.server.core.io.PacketHandler;
import com.islandium.core.api.event.IslandiumEventBus;
import com.islandium.core.api.event.network.PacketSendEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PacketHandler.class)
public abstract class PacketHandlerWriteMixin {

    @Inject(method = "write(Lcom/hypixel/hytale/protocol/ToClientPacket;)V",
            at = @At("HEAD"), cancellable = true)
    private void onWritePacket(ToClientPacket packet, CallbackInfo ci) {
        if (!IslandiumEventBus.isAvailable()) return;

        PacketSendEvent event = new PacketSendEvent(packet, ((Packet) packet).getId());
        IslandiumEventBus.get().fire(event);

        if (event.isCancelled()) {
            ci.cancel();
        }
    }
}
