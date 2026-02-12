package com.islandium.core.command.server;

import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.islandium.core.IslandiumPlugin;
import com.islandium.core.command.base.IslandiumCommand;
import com.islandium.core.ui.pages.server.ServerManagerPage;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

/**
 * Commande admin pour gerer les serveurs via UI.
 * Usage: /serveradmin - Ouvre l'interface de gestion des serveurs
 */
public class ServerAdminCommand extends IslandiumCommand {

    public ServerAdminCommand(@NotNull IslandiumPlugin plugin) {
        super(plugin, "serveradmin", "Gestion des serveurs (admin)");
        addAliases("srvadmin");
        requirePermission("islandium.serveradmin");
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
            var playerRef = store.getComponent(ref, PlayerRef.getComponentType());

            if (playerRef == null) {
                sendRaw(ctx, "&cErreur: PlayerRef non trouve.");
                return;
            }

            ServerManagerPage page = new ServerManagerPage(playerRef, plugin);
            player.getPageManager().openCustomPage(ref, store, page);
            sendRaw(ctx, "&aOuverture de l'interface de gestion des serveurs...");
        }, world);
    }
}
