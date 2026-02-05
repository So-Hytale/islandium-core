package com.islandium.core.command.player;

import com.islandium.core.api.player.PlayerState;
import com.islandium.core.IslandiumPlugin;
import com.islandium.core.command.base.IslandiumCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import java.util.concurrent.CompletableFuture;

public class AfkCommand extends IslandiumCommand {
    public AfkCommand(IslandiumPlugin plugin) {
        super(plugin, "afk", "Active/désactive le mode AFK");
        requirePermission("islandium.afk");
    }

    @Override
    public CompletableFuture<Void> execute(CommandContext ctx) {
        var player = getIslandiumPlayer(ctx);
        boolean isAfk = player.hasState(PlayerState.AFK);

        if (isAfk) {
            player.removeState(PlayerState.AFK);
            sendMessage(ctx, "afk.disabled");
        } else {
            player.addState(PlayerState.AFK);
            sendMessage(ctx, "afk.enabled");
        }

        return CompletableFuture.completedFuture(null);
    }
}
