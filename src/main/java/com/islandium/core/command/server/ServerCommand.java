package com.islandium.core.command.server;

import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.islandium.core.IslandiumPlugin;
import com.islandium.core.command.base.IslandiumCommand;
import com.islandium.core.config.MainConfig;
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

    @Override
    public CompletableFuture<Void> execute(CommandContext ctx) {
        Player player = requirePlayer(ctx);

        // Parser l'argument manuellement
        String[] parts = ctx.getInputString().split("\\s+");
        // parts[0] = "server", parts[1] = nom du serveur (optionnel)

        // Si pas d'argument, affiche le serveur actuel
        if (parts.length < 2) {
            return showCurrentServer(ctx);
        }

        String targetServer = parts[1];

        // Alias pour lobby/hub
        if (targetServer.equalsIgnoreCase("hub")) {
            targetServer = "lobby";
        }

        MainConfig config = plugin.getConfigManager().getMainConfig();
        MainConfig.ServerInfo serverInfo = config.getServer(targetServer);

        if (serverInfo == null) {
            return showAvailableServers(ctx, targetServer);
        }

        String currentServer = config.getServerName();
        if (currentServer.equalsIgnoreCase(targetServer)) {
            sendMessage(ctx, "server.already-connected", "server", serverInfo.displayName);
            return complete();
        }

        // Transfert vers le serveur cible
        sendMessage(ctx, "server.connecting", "server", serverInfo.displayName);
        player.getPlayerRef().referToServer(serverInfo.host, serverInfo.port, null);

        return complete();
    }

    private CompletableFuture<Void> showCurrentServer(CommandContext ctx) {
        MainConfig config = plugin.getConfigManager().getMainConfig();
        String currentServerName = config.getServerName();

        // Cherche le displayName du serveur actuel
        String displayName = config.getServers().entrySet().stream()
            .filter(e -> e.getKey().equalsIgnoreCase(currentServerName))
            .map(e -> e.getValue().displayName)
            .findFirst()
            .orElse(currentServerName);

        sendMessage(ctx, "server.current", "server", displayName);

        // Affiche aussi la liste des serveurs disponibles
        showServerList(ctx);

        return complete();
    }

    private CompletableFuture<Void> showAvailableServers(CommandContext ctx, String attempted) {
        sendMessage(ctx, "server.not-found", "server", attempted);
        showServerList(ctx);
        return complete();
    }

    private void showServerList(CommandContext ctx) {
        Map<String, MainConfig.ServerInfo> servers = plugin.getConfigManager().getMainConfig().getServers();
        String currentServer = plugin.getConfigManager().getMainConfig().getServerName();

        if (servers.isEmpty()) {
            sendMessage(ctx, "server.no-servers");
            return;
        }

        sendMessage(ctx, "server.list-header");

        for (Map.Entry<String, MainConfig.ServerInfo> entry : servers.entrySet()) {
            String name = entry.getKey();
            MainConfig.ServerInfo info = entry.getValue();
            boolean isCurrent = name.equalsIgnoreCase(currentServer);

            if (isCurrent) {
                sendMessage(ctx, "server.list-entry-current",
                    "name", name,
                    "display", info.displayName);
            } else {
                sendMessage(ctx, "server.list-entry",
                    "name", name,
                    "display", info.displayName);
            }
        }
    }

    @Override
    public CompletableFuture<List<String>> tabComplete(CommandContext ctx, String partial) {
        String[] parts = ctx.getInputString().split("\\s+");
        String current = parts.length >= 2 ? parts[1] : "";

        Map<String, MainConfig.ServerInfo> servers = plugin.getConfigManager().getMainConfig().getServers();

        List<String> suggestions = servers.keySet().stream()
            .filter(name -> name.toLowerCase().startsWith(current.toLowerCase()))
            .collect(Collectors.toList());

        return CompletableFuture.completedFuture(suggestions);
    }
}
