package com.islandium.core.api.event.command;

import com.islandium.core.api.event.IslandiumEvent;
import org.jetbrains.annotations.NotNull;

/**
 * Declenche avant l'execution d'une commande.
 * Cancellable : annuler bloque la commande.
 */
public class CommandPreProcessEvent extends IslandiumEvent {

    private final Object sender;
    private final String commandLine;

    public CommandPreProcessEvent(@NotNull Object sender, @NotNull String commandLine) {
        this.sender = sender;
        this.commandLine = commandLine;
    }

    @NotNull
    public Object getSender() { return sender; }

    @NotNull
    public String getCommandLine() { return commandLine; }

    /**
     * Retourne le nom de la commande (premier mot).
     */
    @NotNull
    public String getCommandName() {
        int space = commandLine.indexOf(' ');
        return space > 0 ? commandLine.substring(0, space) : commandLine;
    }
}
