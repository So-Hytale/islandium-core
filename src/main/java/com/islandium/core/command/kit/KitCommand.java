package com.islandium.core.command.kit;

import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.islandium.core.IslandiumPlugin;
import com.islandium.core.api.util.ColorUtil;
import com.islandium.core.api.util.NotificationType;
import com.islandium.core.api.util.NotificationUtil;
import com.islandium.core.command.base.IslandiumCommand;
import com.islandium.core.ui.pages.kit.KitPage;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

/**
 * Commande /kit - Ouvre l'interface des kits.
 */
public class KitCommand extends IslandiumCommand {

    public KitCommand(@NotNull IslandiumPlugin plugin) {
        super(plugin, "kit", "Ouvre le menu des kits");
        addAliases("kits");
    }

    @Override
    public CompletableFuture<Void> execute(CommandContext ctx) {
        if (!isPlayer(ctx)) {
            sendNotification(ctx, NotificationType.ERROR, "Cette commande est reservee aux joueurs!");
            return complete();
        }

        if (!hasPermission(ctx, "essentials.kit")) {
            sendNotification(ctx, NotificationType.ERROR, "Tu n'as pas la permission!");
            return complete();
        }

        Player player = requirePlayer(ctx);

        var ref = player.getReference();
        if (ref == null || !ref.isValid()) {
            sendNotification(ctx, NotificationType.ERROR, "Impossible d'ouvrir l'interface.");
            return complete();
        }

        var store = ref.getStore();
        var world = ((com.hypixel.hytale.server.core.universe.world.storage.EntityStore) store.getExternalData()).getWorld();

        return CompletableFuture.runAsync(() -> {
            var playerRef = store.getComponent(ref, PlayerRef.getComponentType());
            if (playerRef == null) {
                NotificationUtil.send(ctx, NotificationType.ERROR, "PlayerRef non trouve.");
                return;
            }

            KitPage page = new KitPage(playerRef, plugin);
            player.getPageManager().openCustomPage(ref, store, page);
        }, world);
    }
}
