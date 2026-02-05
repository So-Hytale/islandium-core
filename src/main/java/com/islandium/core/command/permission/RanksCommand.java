package com.islandium.core.command.permission;

import com.islandium.core.IslandiumPlugin;
import com.islandium.core.command.base.IslandiumCommand;
import com.islandium.core.ui.pages.permission.RankManagerPage;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

/**
 * Commande /ranks - Raccourci pour ouvrir l'interface de gestion des ranks.
 */
public class RanksCommand extends IslandiumCommand {

    public RanksCommand(@NotNull IslandiumPlugin plugin) {
        super(plugin, "ranks", "Ouvre l'interface de gestion des ranks");
        requirePermission("islandium.rank.admin");
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
        var world = ((com.hypixel.hytale.server.core.universe.world.storage.EntityStore) store.getExternalData()).getWorld();

        return CompletableFuture.runAsync(() -> {
            var playerRef = store.getComponent(ref, com.hypixel.hytale.server.core.universe.PlayerRef.getComponentType());

            if (playerRef == null) {
                sendRaw(ctx, "&cErreur: PlayerRef non trouve.");
                return;
            }

            RankManagerPage page = new RankManagerPage(
                    playerRef,
                    plugin.getServiceManager().getPermissionService(),
                    plugin
            );

            player.getPageManager().openCustomPage(ref, store, page);
        }, world);
    }
}
