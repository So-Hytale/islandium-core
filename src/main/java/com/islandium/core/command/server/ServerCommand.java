package com.islandium.core.command.server;

import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.islandium.core.IslandiumPlugin;
import com.islandium.core.command.base.IslandiumCommand;
import com.islandium.core.server.ServerData;
import com.islandium.core.service.server.ServerService;
import com.islandium.core.ui.pages.server.ServerSelectPage;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Commande pour changer de serveur ou voir le serveur actuel.
 * Usage: /server [nom] - Change de serveur ou affiche le serveur actuel
 */
public class ServerCommand extends IslandiumCommand {

    public ServerCommand(@NotNull IslandiumPlugin plugin) {
        super(plugin, "server", "Change de serveur ou affiche le serveur actuel");
        addAliases("srv", "hub", "lobby");
        requirePermission("islandium.server");
        setAllowsExtraArguments(true);
    }

    private ServerService serverService() {
        return plugin.getServiceManager().getServerService();
    }

    @Override
    public CompletableFuture<Void> execute(CommandContext ctx) {
        Player player = requirePlayer(ctx);

        // Parser l'argument manuellement
        String[] parts = ctx.getInputString().split("\\s+");

        // Si pas d'argument, ouvre l'UI de selection
        if (parts.length < 2) {
            return openSelectUI(player);
        }

        String targetServer = parts[1];

        // Alias pour lobby/hub
        if (targetServer.equalsIgnoreCase("hub")) {
            targetServer = "lobby";
        }

        ServerData serverData = serverService().getServer(targetServer);

        if (serverData == null) {
            return showAvailableServers(ctx, targetServer);
        }

        String currentServer = plugin.getConfigManager().getMainConfig().getServerName();
        if (currentServer.equalsIgnoreCase(targetServer)) {
            sendMessage(ctx, "server.already-connected", "server", serverData.getDisplayName());
            return complete();
        }

        // Transfert vers le serveur cible
        sendMessage(ctx, "server.connecting", "server", serverData.getDisplayName());
        player.getPlayerRef().referToServer(serverData.getHost(), serverData.getPort(), null);

        return complete();
    }

    private CompletableFuture<Void> openSelectUI(Player player) {
        var ref = player.getReference();
        if (ref == null || !ref.isValid()) {
            return complete();
        }

        var store = ref.getStore();
        var world = ((com.hypixel.hytale.server.core.universe.world.storage.EntityStore) store.getExternalData()).getWorld();

        return CompletableFuture.runAsync(() -> {
            var playerRef = store.getComponent(ref, PlayerRef.getComponentType());
            if (playerRef == null) return;

            ServerSelectPage page = new ServerSelectPage(playerRef, plugin);
            player.getPageManager().openCustomPage(ref, store, page);
        }, world);
    }

    private CompletableFuture<Void> showAvailableServers(CommandContext ctx, String attempted) {
        sendMessage(ctx, "server.not-found", "server", attempted);
        return complete();
    }

    @Override
    public CompletableFuture<List<String>> tabComplete(CommandContext ctx, String partial) {
        String[] parts = ctx.getInputString().split("\\s+");
        String current = parts.length >= 2 ? parts[1] : "";

        Map<String, ServerData> servers = serverService().getServers();

        List<String> suggestions = servers.keySet().stream()
            .filter(name -> name.toLowerCase().startsWith(current.toLowerCase()))
            .collect(Collectors.toList());

        return CompletableFuture.completedFuture(suggestions);
    }
}
