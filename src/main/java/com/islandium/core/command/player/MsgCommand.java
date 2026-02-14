package com.islandium.core.command.player;

import com.islandium.core.IslandiumPlugin;
import com.islandium.core.command.base.IslandiumCommand;
import com.islandium.core.ui.pages.PrivateMessagePage;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import java.util.concurrent.CompletableFuture;

public class MsgCommand extends IslandiumCommand {

    public MsgCommand(IslandiumPlugin plugin) {
        super(plugin, "msg", "Envoie un message prive ou ouvre l'interface");
        addAliases("tell", "w", "whisper", "pm");
        requirePermission("islandium.msg");
        setAllowsExtraArguments(true);
    }

    @Override
    public CompletableFuture<Void> execute(CommandContext ctx) {
        if (!isPlayer(ctx)) {
            sendRaw(ctx, "&cCette commande doit etre executee par un joueur.");
            return complete();
        }

        var player = getIslandiumPlayer(ctx);
        String input = ctx.getInputString();
        String[] parts = input.split("\\s+");

        // /msg sans args -> ouvrir l'UI
        if (parts.length <= 1) {
            return openUI(ctx);
        }

        // /msg <joueur> <message> -> envoi direct
        if (parts.length < 3) {
            sendRaw(ctx, "&cUtilisation: /msg <joueur> <message> ou /msg pour ouvrir l'interface");
            return complete();
        }

        String targetName = parts[1];
        // Reconstruire le message a partir de parts[2+]
        StringBuilder sb = new StringBuilder();
        for (int i = 2; i < parts.length; i++) {
            if (i > 2) sb.append(" ");
            sb.append(parts[i]);
        }
        String message = sb.toString();

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

    private CompletableFuture<Void> openUI(CommandContext ctx) {
        Player hytalePlayer = requirePlayer(ctx);

        var ref = hytalePlayer.getReference();
        if (ref == null || !ref.isValid()) {
            sendRaw(ctx, "&cErreur: impossible d'ouvrir l'interface.");
            return complete();
        }

        var store = ref.getStore();
        var world = ((com.hypixel.hytale.server.core.universe.world.storage.EntityStore) store.getExternalData()).getWorld();

        return CompletableFuture.runAsync(() -> {
            var playerRef = store.getComponent(ref, PlayerRef.getComponentType());

            if (playerRef == null) {
                sendRaw(ctx, "&cErreur: PlayerRef non trouve.");
                return;
            }

            var islandiumPlayer = getIslandiumPlayer(ctx);
            PrivateMessagePage page = new PrivateMessagePage(playerRef, plugin, islandiumPlayer.getUniqueId());
            hytalePlayer.getPageManager().openCustomPage(ref, store, page);
        }, world);
    }
}
