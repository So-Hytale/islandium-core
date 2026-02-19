package com.islandium.mixins.hooks;

/**
 * Hook pour l'interception de commandes.
 * Retourne true si la commande doit etre BLOQUEE.
 *
 * DUPLIQUE depuis islandium-mixins.
 */
@FunctionalInterface
public interface CommandHook {
    boolean shouldBlockCommand(Object sender, String commandLine);
}
