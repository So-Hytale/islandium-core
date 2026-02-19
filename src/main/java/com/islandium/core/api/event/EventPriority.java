package com.islandium.core.api.event;

/**
 * Priorite d'execution des handlers d'evenements.
 * LOWEST est execute en premier, MONITOR en dernier.
 */
public enum EventPriority {
    LOWEST,
    LOW,
    NORMAL,
    HIGH,
    HIGHEST,
    MONITOR
}
