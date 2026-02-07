package com.islandium.core.command;

import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.islandium.core.IslandiumPlugin;
import com.islandium.core.command.admin.*;
import com.islandium.core.command.permission.PermCommand;
import com.islandium.core.command.permission.RankCommand;
import com.islandium.core.command.permission.RanksCommand;
import com.islandium.core.command.player.*;
import com.islandium.core.command.server.ServerCommand;
import com.islandium.core.command.spawn.BackCommand;
import com.islandium.core.command.spawn.SetSpawnCommand;
import com.islandium.core.command.spawn.SpawnCommand;
import com.islandium.core.command.world.WorldsCommand;
import com.islandium.core.command.kit.KitCommand;
import com.islandium.core.command.kit.KitAdminCommand;
import com.islandium.core.command.wiki.WikiCommand;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

/**
 * Gestionnaire des commandes.
 */
public class CommandManager {

    private final IslandiumPlugin plugin;
    private final List<AbstractCommand> commands = new ArrayList<>();

    public CommandManager(@NotNull IslandiumPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Enregistre toutes les commandes.
     */
    public void registerAll() {
        // Admin commands
        register(new MuteCommand(plugin));
        register(new EcoCommand(plugin));

        // Player commands
        register(new BalanceCommand(plugin));
        register(new PayCommand(plugin));
        register(new MsgCommand(plugin));
        register(new ReplyCommand(plugin));
        register(new AfkCommand(plugin));

        // Permission commands
        register(new RankCommand(plugin));
        register(new RanksCommand(plugin));
        register(new PermCommand(plugin));

        // World commands
        register(new WorldsCommand(plugin));

        // Server commands
        register(new ServerCommand(plugin));

        // Spawn commands
        register(new SpawnCommand(plugin));
        register(new SetSpawnCommand(plugin));
        register(new BackCommand(plugin));

        // Plugin management
        register(new PluginsCommand(plugin));

        // Kit commands
        register(new KitCommand(plugin));
        register(new KitAdminCommand(plugin));

        // Wiki command
        register(new WikiCommand(plugin));

        // Menu command
        register(new MenuCommand(plugin));

        plugin.log(Level.INFO, "Registered " + commands.size() + " commands");
    }

    private void register(@NotNull AbstractCommand command) {
        plugin.getCommandRegistry().registerCommand(command);
        commands.add(command);
    }

    @NotNull
    public List<AbstractCommand> getCommands() {
        return commands;
    }
}
