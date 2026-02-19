package com.islandium.core.api.event.network;

import com.hypixel.hytale.protocol.ToClientPacket;
import com.islandium.core.api.event.IslandiumEvent;
import org.jetbrains.annotations.NotNull;

/**
 * Declenche quand un paquet serveur->client est envoye.
 * Cancellable : annuler empeche l'envoi.
 */
public class PacketSendEvent extends IslandiumEvent {

    private final ToClientPacket packet;
    private final int packetId;

    public PacketSendEvent(@NotNull ToClientPacket packet, int packetId) {
        this.packet = packet;
        this.packetId = packetId;
    }

    @NotNull
    public ToClientPacket getPacket() { return packet; }

    public int getPacketId() { return packetId; }
}
