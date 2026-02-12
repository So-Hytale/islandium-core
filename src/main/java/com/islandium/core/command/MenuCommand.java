package com.islandium.core.command;

import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.islandium.core.IslandiumPlugin;
import com.islandium.core.command.base.IslandiumCommand;
import com.islandium.core.ui.pages.MenuPage;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

/**
 * Commande /menu - Ouvre le menu principal (hub des plugins).
 * Accessible a tous les joueurs.
 */
public class MenuCommand extends IslandiumCommand {

    public MenuCommand(@NotNull IslandiumPlugin plugin) {
        super(plugin, "menu", "Ouvre le menu principal");
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

        String worldName = world.getName();

        return CompletableFuture.runAsync(() -> {
            var playerRef = store.getComponent(ref, PlayerRef.getComponentType());

            if (playerRef == null) {
                sendRaw(ctx, "&cErreur: PlayerRef non trouve.");
                return;
            }

            MenuPage page = new MenuPage(playerRef, plugin, worldName);
            player.getPageManager().openCustomPage(ref, store, page);
        }, world);
    }
}
