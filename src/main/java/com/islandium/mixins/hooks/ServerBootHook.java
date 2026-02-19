package com.islandium.mixins.hooks;

/**
 * Hook appele quand le serveur a fini de boot.
 *
 * DUPLIQUE depuis islandium-mixins.
 */
@FunctionalInterface
public interface ServerBootHook {
    void onServerBooted();
}
