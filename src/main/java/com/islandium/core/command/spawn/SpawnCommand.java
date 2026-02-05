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
 * Commande pour se téléporter au spawn.
 * Usage: /spawn - Téléporte au spawn avec warmup de 5s
 */
public class SpawnCommand extends IslandiumCommand {

    public SpawnCommand(@NotNull IslandiumPlugin plugin) {
        super(plugin, "spawn", "Téléporte au spawn");
        requirePermission("islandium.spawn");
    }

    @Override
    public CompletableFuture<Void> execute(CommandContext ctx) {
        Player player = requirePlayer(ctx);

        if (!hasPermission(ctx, "islandium.spawn")) {
            sendMessage(ctx, "no-permission");
            return complete();
        }

        // Vérifier si le spawn est défini
        ServerLocation spawn = plugin.getSpawnService().getSpawn();
        if (spawn == null) {
            sendMessage(ctx, "spawn.not-set");
            return complete();
        }

        IslandiumPlayer islandiumPlayer = getIslandiumPlayer(ctx);

        // Vérifier si une téléportation est déjà en cours
        if (plugin.getTeleportService().hasPendingTeleport(islandiumPlayer.getUniqueId())) {
            sendMessage(ctx, "teleport.already-pending");
            return complete();
        }

        // Téléporter avec warmup
        plugin.getTeleportService().teleportWithWarmup(
                islandiumPlayer,
                spawn,
                () -> {
                    // Callback après téléportation (succès ou échec)
                }
        );

        return complete();
    }
}
