package com.islandium.core.command.server;

import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.islandium.core.IslandiumPlugin;
import com.islandium.core.command.base.IslandiumCommand;
import com.islandium.core.server.ServerData;
import com.islandium.core.service.server.ServerService;
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

        // Si pas d'argument, affiche le serveur actuel
        if (parts.length < 2) {
            return showCurrentServer(ctx);
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

    private CompletableFuture<Void> showCurrentServer(CommandContext ctx) {
        String currentServerName = plugin.getConfigManager().getMainConfig().getServerName();

        // Cherche le displayName du serveur actuel
        ServerData current = serverService().getServer(currentServerName);
        String displayName = current != null ? current.getDisplayName() : currentServerName;

        sendMessage(ctx, "server.current", "server", displayName);
        showServerList(ctx);

        return complete();
    }

    private CompletableFuture<Void> showAvailableServers(CommandContext ctx, String attempted) {
        sendMessage(ctx, "server.not-found", "server", attempted);
        showServerList(ctx);
        return complete();
    }

    private void showServerList(CommandContext ctx) {
        Map<String, ServerData> servers = serverService().getServers();
        String currentServer = plugin.getConfigManager().getMainConfig().getServerName();

        if (servers.isEmpty()) {
            sendMessage(ctx, "server.no-servers");
            return;
        }

        sendMessage(ctx, "server.list-header");

        for (Map.Entry<String, ServerData> entry : servers.entrySet()) {
            String name = entry.getKey();
            ServerData info = entry.getValue();
            boolean isCurrent = name.equalsIgnoreCase(currentServer);

            if (isCurrent) {
                sendMessage(ctx, "server.list-entry-current",
                    "name", name,
                    "display", info.getDisplayName());
            } else {
                sendMessage(ctx, "server.list-entry",
                    "name", name,
                    "display", info.getDisplayName());
            }
        }
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
