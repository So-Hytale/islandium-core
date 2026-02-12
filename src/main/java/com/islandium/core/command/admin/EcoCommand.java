package com.islandium.core.command.admin;

import com.islandium.core.IslandiumPlugin;
import com.islandium.core.api.util.NotificationType;
import com.islandium.core.command.base.IslandiumCommand;
import com.islandium.core.api.economy.EconomyService;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;

import java.math.BigDecimal;
import java.util.concurrent.CompletableFuture;

public class EcoCommand extends IslandiumCommand {
    private final RequiredArg<String> actionArg;
    private final RequiredArg<String> playerArg;
    private final RequiredArg<Double> amountArg;

    public EcoCommand(IslandiumPlugin plugin) {
        super(plugin, "eco", "Gestion de l'economie (admin)");
        requirePermission("islandium.eco.admin");
        actionArg = withRequiredArg("action", "give/take/set/reset", ArgTypes.STRING);
        playerArg = withRequiredArg("player", "Joueur cible", ArgTypes.STRING);
        amountArg = withRequiredArg("amount", "Montant (0 pour reset)", ArgTypes.DOUBLE);
    }

    @Override
    public CompletableFuture<Void> execute(CommandContext ctx) {
        if (!hasPermission(ctx, "islandium.eco.admin")) {
            return errorNotification(ctx, "no-permission");
        }

        String action = ctx.get(actionArg).toLowerCase();
        String targetName = ctx.get(playerArg);
        double rawAmount = ctx.get(amountArg);

        EconomyService economy = plugin.getServiceManager().getEconomyService();

        return plugin.getPlayerManager().getPlayer(targetName).thenCompose(targetOpt -> {
            if (targetOpt.isEmpty()) {
                return errorNotification(ctx, "player-not-found", "player", targetName);
            }

            var target = targetOpt.get();
            BigDecimal amount = BigDecimal.valueOf(rawAmount);

            switch (action) {
                case "give" -> {
                    if (rawAmount <= 0) return errorNotification(ctx, "economy.invalid-amount");
                    return economy.addBalance(target.getUniqueId(), amount, "Admin: " + ctx.sender().getDisplayName()).thenAccept(v -> {
                        String formatted = economy.format(amount);
                        sendNotificationKey(ctx, NotificationType.SUCCESS, "economy.eco.give", "amount", formatted, "player", targetName);
                    });
                }
                case "take" -> {
                    if (rawAmount <= 0) return errorNotification(ctx, "economy.invalid-amount");
                    return economy.removeBalance(target.getUniqueId(), amount, "Admin: " + ctx.sender().getDisplayName()).thenAccept(success -> {
                        if (success) {
                            String formatted = economy.format(amount);
                            sendNotificationKey(ctx, NotificationType.SUCCESS, "economy.eco.take", "amount", formatted, "player", targetName);
                        } else {
                            sendNotificationKey(ctx, NotificationType.ERROR, "economy.eco.not-enough", "player", targetName);
                        }
                    });
                }
                case "set" -> {
                    if (rawAmount < 0) return errorNotification(ctx, "economy.invalid-amount");
                    return economy.setBalance(target.getUniqueId(), amount).thenAccept(v -> {
                        String formatted = economy.format(amount);
                        sendNotificationKey(ctx, NotificationType.SUCCESS, "economy.eco.set", "amount", formatted, "player", targetName);
                    });
                }
                case "reset" -> {
                    BigDecimal startingBalance = economy.getStartingBalance();
                    return economy.setBalance(target.getUniqueId(), startingBalance).thenAccept(v -> {
                        String formatted = economy.format(startingBalance);
                        sendNotificationKey(ctx, NotificationType.SUCCESS, "economy.eco.reset", "player", targetName, "amount", formatted);
                    });
                }
                default -> {
                    return errorNotification(ctx, "economy.eco.usage");
                }
            }
        });
    }
}
