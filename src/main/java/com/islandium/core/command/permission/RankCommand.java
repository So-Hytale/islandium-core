package com.islandium.core.command.permission;

import com.islandium.core.api.permission.PermissionService;
import com.islandium.core.api.permission.Rank;
import com.islandium.core.IslandiumPlugin;
import com.islandium.core.command.base.IslandiumCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.islandium.core.ui.pages.permission.RankManagerPage;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Commande /rank pour gerer les ranks.
 */
public class RankCommand extends IslandiumCommand {

    private final RequiredArg<String> actionArg;
    private final OptionalArg<String> arg1;
    private final OptionalArg<String> arg2;
    private final OptionalArg<String> arg3;

    public RankCommand(@NotNull IslandiumPlugin plugin) {
        super(plugin, "rank", "Gestion des ranks");
        requirePermission("islandium.rank.admin");

        actionArg = withRequiredArg("action", "Action (list, info, create, delete, setperm, unsetperm, setparent, setdefault, assign, unassign, gui)", ArgTypes.STRING);
        arg1 = withOptionalArg("arg1", "Argument 1", ArgTypes.STRING);
        arg2 = withOptionalArg("arg2", "Argument 2", ArgTypes.STRING);
        arg3 = withOptionalArg("arg3", "Argument 3", ArgTypes.STRING);
    }

    @Override
    public CompletableFuture<Void> execute(CommandContext ctx) {
        String action = ctx.get(actionArg).toLowerCase();
        PermissionService permService = plugin.getServiceManager().getPermissionService();

        return switch (action) {
            case "list" -> executeList(ctx, permService);
            case "info" -> executeInfo(ctx, permService);
            case "create" -> executeCreate(ctx, permService);
            case "delete" -> executeDelete(ctx, permService);
            case "setperm" -> executeSetPerm(ctx, permService);
            case "unsetperm" -> executeUnsetPerm(ctx, permService);
            case "setparent" -> executeSetParent(ctx, permService);
            case "setdefault" -> executeSetDefault(ctx, permService);
            case "setprefix" -> executeSetPrefix(ctx, permService);
            case "setcolor" -> executeSetColor(ctx, permService);
            case "setpriority" -> executeSetPriority(ctx, permService);
            case "assign" -> executeAssign(ctx, permService);
            case "unassign" -> executeUnassign(ctx, permService);
            case "gui" -> executeGui(ctx);
            default -> showHelp(ctx);
        };
    }

    private CompletableFuture<Void> showHelp(CommandContext ctx) {
        sendRaw(ctx, "&6=== Rank Command Help ===");
        sendRaw(ctx, "&e/rank list &7- Liste tous les ranks");
        sendRaw(ctx, "&e/rank info <rank> &7- Informations sur un rank");
        sendRaw(ctx, "&e/rank create <name> <displayName> &7- Cree un rank");
        sendRaw(ctx, "&e/rank delete <rank> &7- Supprime un rank");
        sendRaw(ctx, "&e/rank setperm <rank> <permission> &7- Ajoute une permission");
        sendRaw(ctx, "&e/rank unsetperm <rank> <permission> &7- Retire une permission");
        sendRaw(ctx, "&e/rank setparent <rank> <parent> &7- Definit l'heritage");
        sendRaw(ctx, "&e/rank setdefault <rank> &7- Definit le rank par defaut");
        sendRaw(ctx, "&e/rank setprefix <rank> <prefix> &7- Definit le prefix");
        sendRaw(ctx, "&e/rank setcolor <rank> <#color> &7- Definit la couleur");
        sendRaw(ctx, "&e/rank setpriority <rank> <priority> &7- Definit la priorite");
        sendRaw(ctx, "&e/rank assign <joueur> <rank> &7- Assigne un rank a un joueur");
        sendRaw(ctx, "&e/rank unassign <joueur> <rank> &7- Retire un rank d'un joueur");
        sendRaw(ctx, "&e/rank gui &7- Ouvre l'interface graphique");
        return complete();
    }

    private CompletableFuture<Void> executeList(CommandContext ctx, PermissionService permService) {
        return permService.getAllRanks().thenAccept(ranks -> {
            sendRaw(ctx, "&6=== Ranks (" + ranks.size() + ") ===");
            for (Rank rank : ranks) {
                String defaultMark = rank.isDefault() ? " &a[DEFAUT]" : "";
                String parent = rank.getParent() != null ? " &7(< " + rank.getParent().getName() + ")" : "";
                sendRaw(ctx, "&e- " + rank.getColor() + rank.getDisplayName() + " &7(" + rank.getName() +
                        ") prio:" + rank.getPriority() + " perms:" + rank.getDirectPermissions().size() + parent + defaultMark);
            }
        });
    }

    private CompletableFuture<Void> executeInfo(CommandContext ctx, PermissionService permService) {
        String rankName = ctx.get(arg1);
        if (rankName == null) {
            sendRaw(ctx, "&cUsage: /rank info <rank>");
            return complete();
        }

        return permService.getRank(rankName).thenAccept(opt -> {
            if (opt.isEmpty()) {
                sendRaw(ctx, "&cRank '" + rankName + "' non trouve.");
                return;
            }

            Rank rank = opt.get();
            sendRaw(ctx, "&6=== Rank: " + rank.getDisplayName() + " ===");
            sendRaw(ctx, "&eNom: &f" + rank.getName());
            sendRaw(ctx, "&eDisplay: &f" + rank.getDisplayName());
            sendRaw(ctx, "&ePrefix: &f" + (rank.getPrefix() != null ? rank.getPrefix() : "aucun"));
            sendRaw(ctx, "&eCouleur: " + rank.getColor() + rank.getColor());
            sendRaw(ctx, "&ePriorite: &f" + rank.getPriority());
            sendRaw(ctx, "&eParent: &f" + (rank.getParent() != null ? rank.getParent().getName() : "aucun"));
            sendRaw(ctx, "&eDefaut: &f" + (rank.isDefault() ? "oui" : "non"));
            sendRaw(ctx, "&ePermissions directes (" + rank.getDirectPermissions().size() + "):");
            for (String perm : rank.getDirectPermissions()) {
                sendRaw(ctx, "&7  - " + perm);
            }
        });
    }

    private CompletableFuture<Void> executeCreate(CommandContext ctx, PermissionService permService) {
        String name = ctx.get(arg1);
        String displayName = ctx.get(arg2);

        if (name == null || displayName == null) {
            sendRaw(ctx, "&cUsage: /rank create <name> <displayName>");
            return complete();
        }

        return permService.createRank(name, displayName, null, "#FFFFFF", 0)
                .thenAccept(rank -> sendRaw(ctx, "&aRank '" + rank.getDisplayName() + "' cree avec succes!"));
    }

    private CompletableFuture<Void> executeDelete(CommandContext ctx, PermissionService permService) {
        String rankName = ctx.get(arg1);
        if (rankName == null) {
            sendRaw(ctx, "&cUsage: /rank delete <rank>");
            return complete();
        }

        return permService.deleteRank(rankName).thenAccept(deleted -> {
            if (deleted) {
                sendRaw(ctx, "&aRank '" + rankName + "' supprime.");
            } else {
                sendRaw(ctx, "&cRank '" + rankName + "' non trouve.");
            }
        });
    }

    private CompletableFuture<Void> executeSetPerm(CommandContext ctx, PermissionService permService) {
        String rankName = ctx.get(arg1);
        String permission = ctx.get(arg2);

        if (rankName == null || permission == null) {
            sendRaw(ctx, "&cUsage: /rank setperm <rank> <permission>");
            return complete();
        }

        return permService.addRankPermission(rankName, permission)
                .thenRun(() -> sendRaw(ctx, "&aPermission '" + permission + "' ajoutee au rank '" + rankName + "'."));
    }

    private CompletableFuture<Void> executeUnsetPerm(CommandContext ctx, PermissionService permService) {
        String rankName = ctx.get(arg1);
        String permission = ctx.get(arg2);

        if (rankName == null || permission == null) {
            sendRaw(ctx, "&cUsage: /rank unsetperm <rank> <permission>");
            return complete();
        }

        return permService.removeRankPermission(rankName, permission)
                .thenRun(() -> sendRaw(ctx, "&aPermission '" + permission + "' retiree du rank '" + rankName + "'."));
    }

    private CompletableFuture<Void> executeSetParent(CommandContext ctx, PermissionService permService) {
        String rankName = ctx.get(arg1);
        String parentName = ctx.get(arg2);

        if (rankName == null) {
            sendRaw(ctx, "&cUsage: /rank setparent <rank> <parent>");
            return complete();
        }

        return permService.setRankParent(rankName, parentName)
                .thenRun(() -> {
                    if (parentName != null) {
                        sendRaw(ctx, "&aRank '" + rankName + "' herite maintenant de '" + parentName + "'.");
                    } else {
                        sendRaw(ctx, "&aHeritage retire du rank '" + rankName + "'.");
                    }
                });
    }

    private CompletableFuture<Void> executeSetDefault(CommandContext ctx, PermissionService permService) {
        String rankName = ctx.get(arg1);
        if (rankName == null) {
            sendRaw(ctx, "&cUsage: /rank setdefault <rank>");
            return complete();
        }

        return permService.setDefaultRank(rankName)
                .thenRun(() -> sendRaw(ctx, "&aRank '" + rankName + "' defini comme rank par defaut."));
    }

    private CompletableFuture<Void> executeSetPrefix(CommandContext ctx, PermissionService permService) {
        String rankName = ctx.get(arg1);
        String prefix = ctx.get(arg2);

        if (rankName == null) {
            sendRaw(ctx, "&cUsage: /rank setprefix <rank> <prefix>");
            return complete();
        }

        return permService.getRank(rankName).thenCompose(opt -> {
            if (opt.isEmpty()) {
                sendRaw(ctx, "&cRank '" + rankName + "' non trouve.");
                return complete();
            }

            Rank rank = opt.get();
            if (rank instanceof com.islandium.core.service.permission.RankImpl impl) {
                impl.setPrefix(prefix);
                return permService.updateRank(impl)
                        .thenRun(() -> sendRaw(ctx, "&aPrefix du rank '" + rankName + "' mis a jour."));
            }
            return complete();
        });
    }

    private CompletableFuture<Void> executeSetColor(CommandContext ctx, PermissionService permService) {
        String rankName = ctx.get(arg1);
        String color = ctx.get(arg2);

        if (rankName == null || color == null) {
            sendRaw(ctx, "&cUsage: /rank setcolor <rank> <#hexcolor>");
            return complete();
        }

        return permService.getRank(rankName).thenCompose(opt -> {
            if (opt.isEmpty()) {
                sendRaw(ctx, "&cRank '" + rankName + "' non trouve.");
                return complete();
            }

            Rank rank = opt.get();
            if (rank instanceof com.islandium.core.service.permission.RankImpl impl) {
                impl.setColor(color);
                return permService.updateRank(impl)
                        .thenRun(() -> sendRaw(ctx, "&aCouleur du rank '" + rankName + "' mise a jour."));
            }
            return complete();
        });
    }

    private CompletableFuture<Void> executeSetPriority(CommandContext ctx, PermissionService permService) {
        String rankName = ctx.get(arg1);
        String priorityStr = ctx.get(arg2);

        if (rankName == null || priorityStr == null) {
            sendRaw(ctx, "&cUsage: /rank setpriority <rank> <priority>");
            return complete();
        }

        int priority;
        try {
            priority = Integer.parseInt(priorityStr);
        } catch (NumberFormatException e) {
            sendRaw(ctx, "&cLa priorite doit etre un nombre.");
            return complete();
        }

        return permService.getRank(rankName).thenCompose(opt -> {
            if (opt.isEmpty()) {
                sendRaw(ctx, "&cRank '" + rankName + "' non trouve.");
                return complete();
            }

            Rank rank = opt.get();
            if (rank instanceof com.islandium.core.service.permission.RankImpl impl) {
                impl.setPriority(priority);
                return permService.updateRank(impl)
                        .thenRun(() -> sendRaw(ctx, "&aPriorite du rank '" + rankName + "' mise a jour."));
            }
            return complete();
        });
    }

    private CompletableFuture<Void> executeAssign(CommandContext ctx, PermissionService permService) {
        String playerName = ctx.get(arg1);
        String rankName = ctx.get(arg2);

        if (playerName == null || rankName == null) {
            sendRaw(ctx, "&cUsage: /rank assign <joueur> <rank>");
            return complete();
        }

        // Chercher le joueur par nom
        return plugin.getPlayerManager().getPlayerUUID(playerName).thenCompose(optUuid -> {
            if (optUuid.isEmpty()) {
                sendRaw(ctx, "&cJoueur '" + playerName + "' non trouve.");
                return complete();
            }

            UUID playerUuid = optUuid.get();
            UUID assignerUuid = isPlayer(ctx) ? requirePlayer(ctx).getUuid() : null;

            return permService.getRank(rankName).thenCompose(optRank -> {
                if (optRank.isEmpty()) {
                    sendRaw(ctx, "&cRank '" + rankName + "' non trouve.");
                    return complete();
                }

                return permService.addPlayerRank(playerUuid, rankName, null, assignerUuid)
                        .thenRun(() -> sendRaw(ctx, "&aRank '" + rankName + "' assigne a " + playerName + "."));
            });
        });
    }

    private CompletableFuture<Void> executeUnassign(CommandContext ctx, PermissionService permService) {
        String playerName = ctx.get(arg1);
        String rankName = ctx.get(arg2);

        if (playerName == null || rankName == null) {
            sendRaw(ctx, "&cUsage: /rank unassign <joueur> <rank>");
            return complete();
        }

        // Chercher le joueur par nom
        return plugin.getPlayerManager().getPlayerUUID(playerName).thenCompose(optUuid -> {
            if (optUuid.isEmpty()) {
                sendRaw(ctx, "&cJoueur '" + playerName + "' non trouve.");
                return complete();
            }

            UUID playerUuid = optUuid.get();

            return permService.removePlayerRank(playerUuid, rankName)
                    .thenRun(() -> sendRaw(ctx, "&aRank '" + rankName + "' retire de " + playerName + "."));
        });
    }

    private CompletableFuture<Void> executeGui(CommandContext ctx) {
        if (!isPlayer(ctx)) {
            sendRaw(ctx, "&cCette commande doit etre executee par un joueur.");
            return complete();
        }

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

        // Exécuter sur le WorldThread comme AdminUI le fait
        return java.util.concurrent.CompletableFuture.runAsync(() -> {
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
            sendRaw(ctx, "&aOuverture de l'interface de gestion des ranks...");
        }, world);
    }

    public CompletableFuture<List<String>> tabComplete(CommandContext ctx, String partial) {
        // Première completion: actions
        if (!ctx.provided(arg1)) {
            return CompletableFuture.completedFuture(
                    List.of("list", "info", "create", "delete", "setperm", "unsetperm",
                            "setparent", "setdefault", "setprefix", "setcolor", "setpriority", "gui")
                            .stream()
                            .filter(s -> s.toLowerCase().startsWith(partial.toLowerCase()))
                            .toList()
            );
        }

        String action = ctx.get(actionArg).toLowerCase();

        // Deuxième completion: noms de ranks pour la plupart des actions
        if (!ctx.provided(arg2) && List.of("info", "delete", "setperm", "unsetperm", "setparent",
                "setdefault", "setprefix", "setcolor", "setpriority").contains(action)) {
            return plugin.getServiceManager().getPermissionService()
                    .getAllRanks()
                    .thenApply(ranks -> ranks.stream()
                            .map(Rank::getName)
                            .filter(n -> n.toLowerCase().startsWith(partial.toLowerCase()))
                            .toList());
        }

        // Troisième completion: parent rank pour setparent
        if (ctx.provided(arg1) && !ctx.provided(arg3) && action.equals("setparent")) {
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
