package com.islandium.mixins.hooks;

/**
 * Hook pour les paquets envoyes (ToClientPacket).
 * Retourne true si le paquet doit etre BLOQUE.
 *
 * DUPLIQUE depuis islandium-mixins.
 */
@FunctionalInterface
public interface PacketSendHook {
    boolean shouldBlockPacket(Object packet, int packetId);
}
