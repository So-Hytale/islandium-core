package com.islandium.core.command.player;

import com.islandium.core.IslandiumPlugin;
import com.islandium.core.command.base.IslandiumCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import java.util.concurrent.CompletableFuture;

public class ReplyCommand extends IslandiumCommand {
    private final RequiredArg<String> messageArg;

    public ReplyCommand(IslandiumPlugin plugin) {
        super(plugin, "reply", "Répond au dernier message privé");
        addAliases("r");
        requirePermission("islandium.msg");
        messageArg = withRequiredArg("message", "Message à envoyer", ArgTypes.STRING);
    }

    @Override
    public CompletableFuture<Void> execute(CommandContext ctx) {
        var player = getIslandiumPlayer(ctx);
        String message = ctx.get(messageArg);

        var lastSender = player.getLastMessageSender();
        if (lastSender == null) {
            return error(ctx, "msg.no-reply");
        }

        return plugin.getPlayerManager().getPlayerName(lastSender).thenCompose(nameOpt -> {
            String targetName = nameOpt.orElse("Inconnu");

            return plugin.getServiceManager().getCrossServerMessenger()
                .sendPrivateMessage(player.getUniqueId(), lastSender, message)
                .thenAccept(sent -> {
                    if (sent) {
                        sendMessage(ctx, "msg.sent", "player", targetName, "message", message);
                    } else {
                        sendMessage(ctx, "msg.failed", "player", targetName);
                    }
                });
        });
    }
}
