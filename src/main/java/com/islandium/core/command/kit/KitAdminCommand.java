package com.islandium.core.command.kit;

import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.islandium.core.IslandiumPlugin;
import com.islandium.core.api.util.ColorUtil;
import com.islandium.core.command.base.IslandiumCommand;
import com.islandium.core.ui.pages.kit.KitConfigPage;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

/**
 * Commande /kitadmin - Ouvre l'interface admin de config des kits.
 */
public class KitAdminCommand extends IslandiumCommand {

    public KitAdminCommand(@NotNull IslandiumPlugin plugin) {
        super(plugin, "kitadmin", "Configuration admin des kits");
        addAliases("kitconfig", "ka");
    }

    @Override
    public CompletableFuture<Void> execute(CommandContext ctx) {
        if (!isPlayer(ctx)) {
            sendRaw(ctx, "&cCette commande est reservee aux joueurs!");
            return complete();
        }

        if (!hasPermission(ctx, "essentials.kit.admin")) {
            sendRaw(ctx, "&cTu n'as pas la permission!");
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

            KitConfigPage page = new KitConfigPage(playerRef, plugin);
            player.getPageManager().openCustomPage(ref, store, page);
        }, world);
    }
}
