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
 * Commande pour retourner à la position précédente avant téléportation.
 * Usage: /back - Téléporte à la dernière position avant une téléportation
 */
public class BackCommand extends IslandiumCommand {

    public BackCommand(@NotNull IslandiumPlugin plugin) {
        super(plugin, "back", "Retourne à la position précédente");
        requirePermission("islandium.back");
    }

    @Override
    public CompletableFuture<Void> execute(CommandContext ctx) {
        Player player = requirePlayer(ctx);

        if (!hasPermission(ctx, "islandium.back")) {
            sendMessage(ctx, "no-permission");
            return complete();
        }

        IslandiumPlayer islandiumPlayer = getIslandiumPlayer(ctx);

        // Vérifier si une téléportation est déjà en cours
        if (plugin.getTeleportService().hasPendingTeleport(islandiumPlayer.getUniqueId())) {
            sendMessage(ctx, "teleport.already-pending");
            return complete();
        }

        // Vérifier si une position précédente existe
        ServerLocation previousLocation = plugin.getBackService().getPreviousLocation(islandiumPlayer.getUniqueId());
        if (previousLocation == null) {
            sendMessage(ctx, "back.no-location");
            return complete();
        }

        // Téléporter avec warmup vers la position précédente
        plugin.getTeleportService().teleportWithWarmup(
                islandiumPlayer,
                previousLocation,
                () -> {
                    // Après la téléportation, on ne supprime pas la position
                    // pour permettre de faire /back plusieurs fois si besoin
                }
        );

        return complete();
    }
}
