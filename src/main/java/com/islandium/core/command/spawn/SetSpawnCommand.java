package com.islandium.core.command.spawn;

import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.islandium.core.IslandiumPlugin;
import com.islandium.core.api.location.ServerLocation;
import com.islandium.core.api.player.IslandiumPlayer;
import com.islandium.core.command.base.IslandiumCommand;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

/**
 * Commande pour définir le spawn du serveur.
 * Usage: /setspawn - Définit le spawn à la position actuelle
 */
public class SetSpawnCommand extends IslandiumCommand {

    public SetSpawnCommand(@NotNull IslandiumPlugin plugin) {
        super(plugin, "setspawn", "Définit le spawn du serveur");
        requirePermission("islandium.admin.setspawn");
    }

    @Override
    public CompletableFuture<Void> execute(CommandContext ctx) {
        Player player = requirePlayer(ctx);

        if (!hasPermission(ctx, "islandium.admin.setspawn")) {
            sendMessage(ctx, "no-permission");
            return complete();
        }

        IslandiumPlayer islandiumPlayer = getIslandiumPlayer(ctx);
        ServerLocation location = islandiumPlayer.getLocation();

        if (location == null) {
            sendMessage(ctx, "spawn.error");
            return complete();
        }

        plugin.getSpawnService().setSpawn(location);
        sendMessage(ctx, "spawn.set",
                "x", String.format("%.1f", location.x()),
                "y", String.format("%.1f", location.y()),
                "z", String.format("%.1f", location.z()),
                "world", location.world());

        return complete();
    }
}
