package com.islandium.core.command.permission;

import com.islandium.core.api.permission.PermissionService;
import com.islandium.core.api.permission.PlayerPermissions;
import com.islandium.core.api.permission.Rank;
import com.islandium.core.IslandiumPlugin;
import com.islandium.core.command.base.IslandiumCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.islandium.core.ui.pages.permission.PlayerPermissionsManagerPage;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Commande /perm pour gerer les permissions des joueurs.
 */
public class PermCommand extends IslandiumCommand {

    private final RequiredArg<String> actionArg;
    private final OptionalArg<String> playerArg;
    private final OptionalArg<String> valueArg;
    private final OptionalArg<String> durationArg;

    public PermCommand(@NotNull IslandiumPlugin plugin) {
        super(plugin, "perm", "Gestion des permissions joueurs");
        requirePermission("islandium.perm.admin");

        addAliases("perms", "permission", "permissions");

        actionArg = withRequiredArg("action", "Action (player, add, remove, addrank, removerank, check, gui)", ArgTypes.STRING);
        playerArg = withOptionalArg("player", "Nom du joueur", ArgTypes.STRING);
        valueArg = withOptionalArg("value", "Permission ou rank", ArgTypes.STRING);
        durationArg = withOptionalArg("duration", "Duree (ex: 1d, 7d, 30d)", ArgTypes.STRING);
    }

    @Override
    public CompletableFuture<Void> execute(CommandContext ctx) {
        String action = ctx.get(actionArg).toLowerCase();
        PermissionService permService = plugin.getServiceManager().getPermissionService();

        return switch (action) {
            case "player", "info" -> executePlayer(ctx, permService);
            case "add" -> executeAdd(ctx, permService);
            case "remove" -> executeRemove(ctx, permService);
            case "addrank" -> executeAddRank(ctx, permService);
            case "removerank" -> executeRemoveRank(ctx, permService);
            case "check" -> executeCheck(ctx, permService);
            case "gui" -> executeGui(ctx, permService);
            default -> showHelp(ctx);
        };
    }

    private CompletableFuture<Void> showHelp(CommandContext ctx) {
        sendRaw(ctx, "&6=== Perm Command Help ===");
        sendRaw(ctx, "&e/perm player <player> &7- Voir les permissions d'un joueur");
        sendRaw(ctx, "&e/perm add <player> <permission> [duree] &7- Ajouter une permission");
        sendRaw(ctx, "&e/perm remove <player> <permission> &7- Retirer une permission");
        sendRaw(ctx, "&e/perm addrank <player> <rank> [duree] &7- Ajouter un rank");
        sendRaw(ctx, "&e/perm removerank <player> <rank> &7- Retirer un rank");
        sendRaw(ctx, "&e/perm check <player> <permission> &7- Verifier une permission");
        sendRaw(ctx, "&e/perm gui [player] &7- Ouvrir l'interface graphique (joueur optionnel)");
        return complete();
    }

    private CompletableFuture<Void> executePlayer(CommandContext ctx, PermissionService permService) {
        String playerName = ctx.get(playerArg);
        if (playerName == null) {
            sendRaw(ctx, "&cUsage: /perm player <player>");
            return complete();
        }

        return getPlayerUUID(playerName).thenCompose(uuidOpt -> {
            if (uuidOpt == null) {
                sendRaw(ctx, "&cJoueur '" + playerName + "' non trouve.");
                return complete();
            }

            return permService.getPlayerPermissions(uuidOpt).thenAccept(perms -> {
                sendRaw(ctx, "&6=== Permissions de " + playerName + " ===");

                // Ranks
                sendRaw(ctx, "&eRanks:");
                if (perms.getRanks().isEmpty()) {
                    sendRaw(ctx, "&7  Aucun rank");
                } else {
                    for (Rank rank : perms.getRanks()) {
                        String primary = rank.equals(perms.getPrimaryRank()) ? " &a[PRINCIPAL]" : "";
                        sendRaw(ctx, "&7  - " + rank.getColor() + rank.getDisplayName() + primary);
                    }
                }

                // Permissions personnelles
                sendRaw(ctx, "&ePermissions personnelles:");
                if (perms.getPersonalPermissions().isEmpty()) {
                    sendRaw(ctx, "&7  Aucune");
                } else {
                    for (String perm : perms.getPersonalPermissions()) {
                        sendRaw(ctx, "&7  - " + perm);
                    }
                }

                // Total
                sendRaw(ctx, "&eTotal des permissions: &f" + perms.getAllPermissions().size());
            });
        });
    }

    private CompletableFuture<Void> executeAdd(CommandContext ctx, PermissionService permService) {
        String playerName = ctx.get(playerArg);
        String permission = ctx.get(valueArg);
        String duration = ctx.get(durationArg);

        if (playerName == null || permission == null) {
            sendRaw(ctx, "&cUsage: /perm add <player> <permission> [duree]");
            return complete();
        }

        Long expiresAt = parseDuration(duration);

        return getPlayerUUID(playerName).thenCompose(uuid -> {
            if (uuid == null) {
                sendRaw(ctx, "&cJoueur '" + playerName + "' non trouve.");
                return complete();
            }

            return permService.addPlayerPermission(uuid, permission, expiresAt)
                    .thenRun(() -> {
                        String msg = "&aPermission '" + permission + "' ajoutee a " + playerName;
                        if (expiresAt != null) {
                            msg += " (expire dans " + duration + ")";
                        }
                        sendRaw(ctx, msg + ".");
                        permService.invalidatePlayerCache(uuid);
                    });
        });
    }

    private CompletableFuture<Void> executeRemove(CommandContext ctx, PermissionService permService) {
        String playerName = ctx.get(playerArg);
        String permission = ctx.get(valueArg);

        if (playerName == null || permission == null) {
            sendRaw(ctx, "&cUsage: /perm remove <player> <permission>");
            return complete();
        }

        return getPlayerUUID(playerName).thenCompose(uuid -> {
            if (uuid == null) {
                sendRaw(ctx, "&cJoueur '" + playerName + "' non trouve.");
                return complete();
            }

            return permService.removePlayerPermission(uuid, permission)
                    .thenRun(() -> {
                        sendRaw(ctx, "&aPermission '" + permission + "' retiree de " + playerName + ".");
                        permService.invalidatePlayerCache(uuid);
                    });
        });
    }

    private CompletableFuture<Void> executeAddRank(CommandContext ctx, PermissionService permService) {
        String playerName = ctx.get(playerArg);
        String rankName = ctx.get(valueArg);
        String duration = ctx.get(durationArg);

        if (playerName == null || rankName == null) {
            sendRaw(ctx, "&cUsage: /perm addrank <player> <rank> [duree]");
            return complete();
        }

        Long expiresAt = parseDuration(duration);
        UUID assignedBy = isPlayer(ctx) ? requirePlayer(ctx).getUuid() : null;

        return getPlayerUUID(playerName).thenCompose(uuid -> {
            if (uuid == null) {
                sendRaw(ctx, "&cJoueur '" + playerName + "' non trouve.");
                return complete();
            }

            return permService.addPlayerRank(uuid, rankName, expiresAt, assignedBy)
                    .thenRun(() -> {
                        String msg = "&aRank '" + rankName + "' ajoute a " + playerName;
                        if (expiresAt != null) {
                            msg += " (expire dans " + duration + ")";
                        }
                        sendRaw(ctx, msg + ".");
                        permService.invalidatePlayerCache(uuid);
                    });
        });
    }

    private CompletableFuture<Void> executeRemoveRank(CommandContext ctx, PermissionService permService) {
        String playerName = ctx.get(playerArg);
        String rankName = ctx.get(valueArg);

        if (playerName == null || rankName == null) {
            sendRaw(ctx, "&cUsage: /perm removerank <player> <rank>");
            return complete();
        }

        return getPlayerUUID(playerName).thenCompose(uuid -> {
            if (uuid == null) {
                sendRaw(ctx, "&cJoueur '" + playerName + "' non trouve.");
                return complete();
            }

            return permService.removePlayerRank(uuid, rankName)
                    .thenRun(() -> {
                        sendRaw(ctx, "&aRank '" + rankName + "' retire de " + playerName + ".");
                        permService.invalidatePlayerCache(uuid);
                    });
        });
    }

    private CompletableFuture<Void> executeCheck(CommandContext ctx, PermissionService permService) {
        String playerName = ctx.get(playerArg);
        String permission = ctx.get(valueArg);

        if (playerName == null || permission == null) {
            sendRaw(ctx, "&cUsage: /perm check <player> <permission>");
            return complete();
        }

        return getPlayerUUID(playerName).thenCompose(uuid -> {
            if (uuid == null) {
                sendRaw(ctx, "&cJoueur '" + playerName + "' non trouve.");
                return complete();
            }

            return permService.hasPermission(uuid, permission)
                    .thenAccept(has -> {
                        if (has) {
                            sendRaw(ctx, "&a" + playerName + " a la permission '" + permission + "'.");
                        } else {
                            sendRaw(ctx, "&c" + playerName + " n'a pas la permission '" + permission + "'.");
                        }
                    });
        });
    }

    private CompletableFuture<Void> executeGui(CommandContext ctx, PermissionService permService) {
        if (!isPlayer(ctx)) {
            sendRaw(ctx, "&cCette commande doit etre executee par un joueur.");
            return complete();
        }

        String playerName = ctx.get(playerArg);
        Player player = requirePlayer(ctx);

        // Obtenir les références
        var ref = player.getReference();
        if (ref == null || !ref.isValid()) {
            sendRaw(ctx, "&cErreur: impossible d'ouvrir l'interface.");
            return complete();
        }

        var store = ref.getStore();

        // Obtenir le World pour exécuter sur le bon thread
        var world = ((com.hypixel.hytale.server.core.universe.world.storage.EntityStore) store.getExternalData()).getWorld();

        // Si aucun joueur specifie, ouvrir le GUI avec la recherche
        if (playerName == null || playerName.isBlank()) {
            return java.util.concurrent.CompletableFuture.runAsync(() -> {
                var playerRef = store.getComponent(ref, com.hypixel.hytale.server.core.universe.PlayerRef.getComponentType());

                if (playerRef == null) {
                    sendRaw(ctx, "&cErreur: PlayerRef non trouve.");
                    return;
                }

                PlayerPermissionsManagerPage page = new PlayerPermissionsManagerPage(
                        playerRef,
                        permService,
                        plugin
                );

                player.getPageManager().openCustomPage(ref, store, page);
                sendRaw(ctx, "&aOuverture du gestionnaire de permissions...");
            }, world);
        }

        // Sinon, chercher le joueur et ouvrir le GUI avec ce joueur pre-selectionne
        return getPlayerUUID(playerName).thenCompose(uuid -> {
            if (uuid == null) {
                sendRaw(ctx, "&cJoueur '" + playerName + "' non trouve.");
                return complete();
            }

            // Exécuter sur le WorldThread comme AdminUI le fait
            return java.util.concurrent.CompletableFuture.runAsync(() -> {
                var playerRef = store.getComponent(ref, com.hypixel.hytale.server.core.universe.PlayerRef.getComponentType());

                if (playerRef == null) {
                    sendRaw(ctx, "&cErreur: PlayerRef non trouve.");
                    return;
                }

                PlayerPermissionsManagerPage page = new PlayerPermissionsManagerPage(
                        playerRef,
                        permService,
                        plugin,
                        uuid,
                        playerName
                );

                player.getPageManager().openCustomPage(ref, store, page);
                sendRaw(ctx, "&aOuverture du gestionnaire de permissions pour " + playerName + "...");
            }, world);
        });
    }

    private CompletableFuture<UUID> getPlayerUUID(String playerName) {
        // D'abord chercher dans les joueurs online
        var online = plugin.getPlayerManager().getOnlinePlayer(playerName);
        if (online.isPresent()) {
            return CompletableFuture.completedFuture(online.get().getUniqueId());
        }

        // Sinon chercher dans la base de donnees
        return plugin.getServiceManager().getPlayerRepository()
                .findByUsername(playerName)
                .thenApply(opt -> opt.map(data -> data.uuid()).orElse(null));
    }

    private Long parseDuration(String duration) {
        if (duration == null || duration.isEmpty()) {
            return null;
        }

        try {
            long multiplier = 1000L; // millisecondes
            String number = duration.substring(0, duration.length() - 1);
            char unit = duration.charAt(duration.length() - 1);

            switch (Character.toLowerCase(unit)) {
                case 's' -> multiplier *= 1; // secondes
                case 'm' -> multiplier *= 60; // minutes
                case 'h' -> multiplier *= 3600; // heures
                case 'd' -> multiplier *= 86400; // jours
                case 'w' -> multiplier *= 604800; // semaines
                default -> {
                    return null;
                }
            }

            long durationMs = Long.parseLong(number) * multiplier;
            return System.currentTimeMillis() + durationMs;
        } catch (Exception e) {
            return null;
        }
    }

    public CompletableFuture<List<String>> tabComplete(CommandContext ctx, String partial) {
        // Première completion: actions
        if (!ctx.provided(playerArg)) {
            return CompletableFuture.completedFuture(
                    List.of("player", "add", "remove", "addrank", "removerank", "check", "gui")
                            .stream()
                            .filter(s -> s.toLowerCase().startsWith(partial.toLowerCase()))
                            .toList()
            );
        }

        String action = ctx.get(actionArg).toLowerCase();

        // Deuxième completion: noms de joueurs
        if (!ctx.provided(valueArg)) {
            return CompletableFuture.completedFuture(
                    plugin.getPlayerManager().getOnlinePlayersLocal().stream()
                            .map(p -> p.getName())
                            .filter(n -> n.toLowerCase().startsWith(partial.toLowerCase()))
                            .toList()
            );
        }

        // Troisième completion: noms de ranks pour addrank/removerank
        if (action.equals("addrank") || action.equals("removerank")) {
            return plugin.getServiceManager().getPermissionService()
                    .getAllRanks()
                    .thenApply(ranks -> ranks.stream()
                            .map(Rank::getName)
                            .filter(n -> n.toLowerCase().startsWith(partial.toLowerCase()))
                            .toList());
        }

        return CompletableFuture.completedFuture(List.of());
    }
}
