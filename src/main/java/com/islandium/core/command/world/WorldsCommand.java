package com.islandium.core.command.world;

import com.islandium.core.IslandiumPlugin;
import com.islandium.core.command.base.IslandiumCommand;
import com.islandium.core.ui.pages.world.WorldManagerPage;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

/**
 * Commande /worlds - Ouvre l'interface de gestion des mondes.
 */
public class WorldsCommand extends IslandiumCommand {

    public WorldsCommand(@NotNull IslandiumPlugin plugin) {
        super(plugin, "worlds", "Ouvre l'interface de gestion des mondes");
        requirePermission("islandium.world.admin");
    }

    @Override
    public CompletableFuture<Void> execute(CommandContext ctx) {
        if (!isPlayer(ctx)) {
            sendRaw(ctx, "&cCette commande doit etre executee par un joueur.");
            return complete();
        }

        Player player = requirePlayer(ctx);

        var ref = player.getReference();
        if (ref == null || !ref.isValid()) {
            sendRaw(ctx, "&cErreur: impossible d'ouvrir l'interface.");
            return complete();
        }

        var store = ref.getStore();
        var world = store.getExternalData().getWorld();

        return CompletableFuture.runAsync(() -> {
            var playerRef = store.getComponent(ref, com.hypixel.hytale.server.core.universe.PlayerRef.getComponentType());

            if (playerRef == null) {
                sendRaw(ctx, "&cErreur: PlayerRef non trouve.");
                return;
            }

            WorldManagerPage page = new WorldManagerPage(playerRef, plugin);

            player.getPageManager().openCustomPage(ref, store, page);
        }, world);
    }
}
