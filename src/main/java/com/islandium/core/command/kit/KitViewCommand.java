package com.islandium.core.command.kit;

import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.islandium.core.IslandiumPlugin;
import com.islandium.core.api.util.ColorUtil;
import com.islandium.core.command.base.IslandiumCommand;
import com.islandium.core.ui.pages.kit.KitViewPage;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

/**
 * Commande /kitview - Test page pour les icones d'items.
 */
public class KitViewCommand extends IslandiumCommand {

    public KitViewCommand(@NotNull IslandiumPlugin plugin) {
        super(plugin, "kitview", "Test affichage icones kits");
    }

    @Override
    public CompletableFuture<Void> execute(CommandContext ctx) {
        if (!isPlayer(ctx)) {
            sendRaw(ctx, "&cCette commande est reservee aux joueurs!");
            return complete();
        }

        Player player = requirePlayer(ctx);
        var ref = player.getReference();
        if (ref == null || !ref.isValid()) {
            sendRaw(ctx, "&cErreur: impossible d'ouvrir l'interface.");
            return complete();
        }

        var store = ref.getStore();
        var world = ((com.hypixel.hytale.server.core.universe.world.storage.EntityStore) store.getExternalData()).getWorld();

        return CompletableFuture.runAsync(() -> {
            var playerRef = store.getComponent(ref, PlayerRef.getComponentType());
            if (playerRef == null) {
                ctx.sendMessage(ColorUtil.parse("&cErreur: PlayerRef non trouve."));
                return;
            }

            KitViewPage page = new KitViewPage(playerRef, plugin);
            player.getPageManager().openCustomPage(ref, store, page);
        }, world);
    }
}
