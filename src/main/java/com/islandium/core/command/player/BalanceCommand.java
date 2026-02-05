package com.islandium.core.command.player;

import com.islandium.core.IslandiumPlugin;
import com.islandium.core.command.base.IslandiumCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import java.util.concurrent.CompletableFuture;

public class BalanceCommand extends IslandiumCommand {
    private final OptionalArg<String> playerArg;

    public BalanceCommand(IslandiumPlugin plugin) {
        super(plugin, "balance", "Affiche ton solde");
        addAliases("bal", "money");
        requirePermission("islandium.balance");
        playerArg = withOptionalArg("player", "Joueur cible", ArgTypes.STRING);
    }

    @Override
    public CompletableFuture<Void> execute(CommandContext ctx) {
        String targetName = ctx.provided(playerArg) ? ctx.get(playerArg) : null;

        if (targetName != null) {
            if (!hasPermission(ctx, "islandium.balance.others")) {
                return error(ctx, "no-permission");
            }
            return plugin.getPlayerManager().getPlayer(targetName).thenAccept(targetOpt -> {
                if (targetOpt.isEmpty()) {
                    sendMessage(ctx, "player-not-found", "player", targetName);
                    return;
                }
                var target = targetOpt.get();
                String formattedBalance = plugin.getServiceManager().getEconomyService().format(target.getBalance());
                sendMessage(ctx, "balance.other", "player", targetName, "balance", formattedBalance);
            });
        }

        var player = getIslandiumPlayer(ctx);
        String formattedBalance = plugin.getServiceManager().getEconomyService().format(player.getBalance());
        sendMessage(ctx, "balance.self", "balance", formattedBalance);
        return CompletableFuture.completedFuture(null);
    }
}
