package com.islandium.core.command.player;

import com.islandium.core.IslandiumPlugin;
import com.islandium.core.command.base.IslandiumCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import java.util.concurrent.CompletableFuture;

public class MsgCommand extends IslandiumCommand {
    private final RequiredArg<String> playerArg;
    private final RequiredArg<String> messageArg;

    public MsgCommand(IslandiumPlugin plugin) {
        super(plugin, "msg", "Envoie un message privé");
        addAliases("tell", "w", "whisper", "pm");
        requirePermission("islandium.msg");
        playerArg = withRequiredArg("player", "Joueur destinataire", ArgTypes.STRING);
        messageArg = withRequiredArg("message", "Message à envoyer", ArgTypes.STRING);
    }

    @Override
    public CompletableFuture<Void> execute(CommandContext ctx) {
        var player = getIslandiumPlayer(ctx);
        String targetName = ctx.get(playerArg);
        String message = ctx.get(messageArg);

        if (targetName.equalsIgnoreCase(player.getName())) {
            return error(ctx, "msg.self");
        }

        return plugin.getPlayerManager().getPlayerUUID(targetName).thenCompose(uuidOpt -> {
            if (uuidOpt.isEmpty()) return error(ctx, "player-not-found", "player", targetName);

            return plugin.getServiceManager().getCrossServerMessenger()
                .sendPrivateMessage(player.getUniqueId(), uuidOpt.get(), message)
                .thenAccept(sent -> {
                    if (sent) {
                        sendMessage(ctx, "msg.sent", "player", targetName, "message", message);
                        player.setLastMessageSender(uuidOpt.get());
                    } else {
                        sendMessage(ctx, "msg.failed", "player", targetName);
                    }
                });
        });
    }
}
