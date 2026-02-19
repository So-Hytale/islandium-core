package com.islandium.mixins.hooks;

/**
 * Hook pour les paquets recus (ToServerPacket).
 * Retourne true si le paquet doit etre BLOQUE.
 *
 * DUPLIQUE depuis islandium-mixins.
 */
@FunctionalInterface
public interface PacketReceiveHook {
    boolean shouldBlockPacket(Object packet, int packetId);
}
