package com.islandium.core.api.event.network;

import com.hypixel.hytale.protocol.ToServerPacket;
import com.islandium.core.api.event.IslandiumEvent;
import org.jetbrains.annotations.NotNull;

/**
 * Declenche quand un paquet client->serveur est recu.
 * Cancellable : annuler ignore le paquet.
 */
public class PacketReceiveEvent extends IslandiumEvent {

    private final ToServerPacket packet;
    private final int packetId;

    public PacketReceiveEvent(@NotNull ToServerPacket packet, int packetId) {
        this.packet = packet;
        this.packetId = packetId;
    }

    @NotNull
    public ToServerPacket getPacket() { return packet; }

    public int getPacketId() { return packetId; }
}
