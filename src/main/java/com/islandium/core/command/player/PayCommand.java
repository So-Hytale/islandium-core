package com.islandium.core.command.player;

import com.islandium.core.IslandiumPlugin;
import com.islandium.core.command.base.IslandiumCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import java.math.BigDecimal;
import java.util.concurrent.CompletableFuture;

public class PayCommand extends IslandiumCommand {
    private final RequiredArg<String> playerArg;
    private final RequiredArg<Double> amountArg;

    public PayCommand(IslandiumPlugin plugin) {
        super(plugin, "pay", "Envoie de l'argent à un joueur");
        requirePermission("islandium.pay");
        playerArg = withRequiredArg("player", "Joueur destinataire", ArgTypes.STRING);
        amountArg = withRequiredArg("amount", "Montant à envoyer", ArgTypes.DOUBLE);
    }

    @Override
    public CompletableFuture<Void> execute(CommandContext ctx) {
        var player = getIslandiumPlayer(ctx);
        String targetName = ctx.get(playerArg);
        double amount = ctx.get(amountArg);

        if (amount <= 0) {
            return error(ctx, "economy.invalid-amount");
        }

        if (targetName.equalsIgnoreCase(player.getName())) {
            return error(ctx, "economy.pay-self");
        }

        BigDecimal transferAmount = BigDecimal.valueOf(amount);

        if (player.getBalance().compareTo(transferAmount) < 0) {
            return error(ctx, "economy.insufficient-funds");
        }

        return plugin.getPlayerManager().getPlayer(targetName).thenCompose(targetOpt -> {
            if (targetOpt.isEmpty()) return error(ctx, "player-not-found", "player", targetName);

            var target = targetOpt.get();
            return plugin.getServiceManager().getEconomyService()
                .transfer(player.getUniqueId(), target.getUniqueId(), transferAmount, "Paiement de " + player.getName())
                .thenAccept(success -> {
                    if (success) {
                        String formattedAmount = plugin.getServiceManager().getEconomyService().format(transferAmount);
                        sendMessage(ctx, "economy.paid", "player", targetName, "amount", formattedAmount);
                    } else {
                        sendMessage(ctx, "economy.transfer-failed");
                    }
                });
        });
    }
}
