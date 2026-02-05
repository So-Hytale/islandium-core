package com.islandium.core.command.wiki;

import com.islandium.core.IslandiumPlugin;
import com.islandium.core.command.base.IslandiumCommand;
import com.islandium.core.ui.pages.wiki.WikiEntityDetailPage;
import com.islandium.core.ui.pages.wiki.WikiItemDetailPage;
import com.islandium.core.ui.pages.wiki.WikiMainPage;
import com.islandium.core.wiki.WikiManager;
import com.islandium.core.wiki.model.WikiEntity;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Command to access the entity wiki.
 *
 * Usage:
 *   /wiki                    - Opens the main wiki page
 *   /wiki entity <name>      - Opens a specific entity's page
 *   /wiki item <name>        - Opens item search with entities that drop it
 *   /wiki search <query>     - Searches entities and items
 *   /wiki reload             - Reloads wiki data (admin only)
 */
public class WikiCommand extends IslandiumCommand {

    private final OptionalArg<String> actionArg;
    private final OptionalArg<String> argValue;

    public WikiCommand(@NotNull IslandiumPlugin plugin) {
        super(plugin, "wiki", "Ouvre le wiki des entites et drops");

        actionArg = withOptionalArg("action", "Action (entity, item, search, reload)", ArgTypes.STRING);
        argValue = withOptionalArg("value", "Nom ou recherche", ArgTypes.STRING);
    }

    @Override
    public CompletableFuture<Void> execute(CommandContext ctx) {
        String action = ctx.get(actionArg);

        // No action = open main page
        if (action == null || action.isEmpty()) {
            return executeMain(ctx);
        }

        String value = ctx.get(argValue);

        return switch (action.toLowerCase()) {
            case "entity" -> executeEntity(ctx, value);
            case "item" -> executeItem(ctx, value);
            case "search" -> executeSearch(ctx, value);
            case "reload" -> executeReload(ctx);
            default -> {
                // Try to treat action as entity name if no subcommand matched
                if (value == null) {
                    yield executeEntity(ctx, action);
                }
                yield showHelp(ctx);
            }
        };
    }

    private CompletableFuture<Void> showHelp(CommandContext ctx) {
        sendRaw(ctx, "&6=== Wiki Command Help ===");
        sendRaw(ctx, "&e/wiki &7- Ouvre la page principale du wiki");
        sendRaw(ctx, "&e/wiki entity <nom> &7- Ouvre la page d'une entite");
        sendRaw(ctx, "&e/wiki item <nom> &7- Recherche un item et ses sources");
        sendRaw(ctx, "&e/wiki search <texte> &7- Recherche globale");
        sendRaw(ctx, "&e/wiki reload &7- Recharge les donnees (admin)");
        return complete();
    }

    /**
     * Main command - opens wiki main page.
     */
    private CompletableFuture<Void> executeMain(CommandContext ctx) {
        if (!isPlayer(ctx)) {
            sendRaw(ctx, "&cCette commande doit etre executee par un joueur.");
            return complete();
        }

        Player player = requirePlayer(ctx);

        // Get references for page opening
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

            WikiMainPage page = new WikiMainPage(playerRef, plugin);
            player.getPageManager().openCustomPage(ref, store, page);
        }, world);
    }

    /**
     * Opens a specific entity's detail page.
     */
    private CompletableFuture<Void> executeEntity(CommandContext ctx, String name) {
        if (!isPlayer(ctx)) {
            sendRaw(ctx, "&cCette commande doit etre executee par un joueur.");
            return complete();
        }

        if (name == null || name.isEmpty()) {
            sendRaw(ctx, "&cUsage: /wiki entity <nom>");
            return complete();
        }

        Player player = requirePlayer(ctx);

        // Try to find entity by exact ID first
        Optional<WikiEntity> entity = WikiManager.get().getEntity(name);

        if (entity.isEmpty()) {
            // Try search by name
            List<WikiEntity> results = WikiManager.get().searchEntities(name);
            if (!results.isEmpty()) {
                entity = Optional.of(results.get(0));
            }
        }

        if (entity.isEmpty()) {
            sendRaw(ctx, "&cEntite introuvable: &e" + name);
            sendRaw(ctx, "&7Utilisez &f/wiki &7pour voir toutes les entites.");
            return complete();
        }

        final WikiEntity foundEntity = entity.get();

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

            WikiEntityDetailPage page = new WikiEntityDetailPage(playerRef, plugin, foundEntity);
            player.getPageManager().openCustomPage(ref, store, page);
        }, world);
    }

    /**
     * Opens item detail page showing entities that drop it.
     */
    private CompletableFuture<Void> executeItem(CommandContext ctx, String name) {
        if (!isPlayer(ctx)) {
            sendRaw(ctx, "&cCette commande doit etre executee par un joueur.");
            return complete();
        }

        if (name == null || name.isEmpty()) {
            sendRaw(ctx, "&cUsage: /wiki item <nom>");
            return complete();
        }

        Player player = requirePlayer(ctx);

        // Search for the item
        String itemId = name.toLowerCase().replace(" ", "_");
        List<WikiEntity> entities = WikiManager.get().findEntitiesDroppingItem(itemId);

        if (entities.isEmpty()) {
            // Try searching by item name
            var items = WikiManager.get().searchItems(name);
            if (!items.isEmpty()) {
                itemId = items.get(0).getItemId();
            }
        }

        final String finalItemId = itemId;

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

            WikiItemDetailPage page = new WikiItemDetailPage(playerRef, plugin, finalItemId);
            player.getPageManager().openCustomPage(ref, store, page);
        }, world);
    }

    /**
     * Search and open main page with query pre-filled.
     */
    private CompletableFuture<Void> executeSearch(CommandContext ctx, String query) {
        if (!isPlayer(ctx)) {
            sendRaw(ctx, "&cCette commande doit etre executee par un joueur.");
            return complete();
        }

        if (query == null || query.isEmpty()) {
            sendRaw(ctx, "&cUsage: /wiki search <texte>");
            return complete();
        }

        // Show matching counts
        List<WikiEntity> entities = WikiManager.get().searchEntities(query);
        var items = WikiManager.get().searchItems(query);

        sendRaw(ctx, "&6Resultats pour '&e" + query + "&6':");
        sendRaw(ctx, "&7- &f" + entities.size() + " &7entites");
        sendRaw(ctx, "&7- &f" + items.size() + " &7items");

        // Open main page
        return executeMain(ctx);
    }

    /**
     * Reload wiki data (admin only).
     */
    private CompletableFuture<Void> executeReload(CommandContext ctx) {
        if (!hasPermission(ctx, "islandium.wiki.admin")) {
            sendRaw(ctx, "&cVous n'avez pas la permission.");
            return complete();
        }

        WikiManager.get().reload();
        sendRaw(ctx, "&aWiki recharge avec succes!");
        sendRaw(ctx, "&7- &f" + WikiManager.get().getEntityCount() + " &7entites");
        sendRaw(ctx, "&7- &f" + WikiManager.get().getUniqueItemCount() + " &7items uniques");

        return complete();
    }

    @Override
    public CompletableFuture<List<String>> tabComplete(CommandContext ctx, String partial) {
        if (!ctx.provided(actionArg)) {
            return CompletableFuture.completedFuture(
                    List.of("entity", "item", "search", "reload")
                            .stream()
                            .filter(s -> s.toLowerCase().startsWith(partial.toLowerCase()))
                            .toList()
            );
        }

        String action = ctx.get(actionArg);
        if (action == null) return CompletableFuture.completedFuture(List.of());

        // Entity name completion
        if (action.equalsIgnoreCase("entity") && !ctx.provided(argValue)) {
            return CompletableFuture.completedFuture(
                    WikiManager.get().getAllEntities().stream()
                            .map(WikiEntity::getId)
                            .filter(id -> id.toLowerCase().startsWith(partial.toLowerCase()))
                            .limit(20)
                            .toList()
            );
        }

        // Item name completion
        if (action.equalsIgnoreCase("item") && !ctx.provided(argValue)) {
            return CompletableFuture.completedFuture(
                    WikiManager.get().getAllDroppableItems().stream()
                            .map(drop -> drop.getItemId())
                            .filter(id -> id.toLowerCase().startsWith(partial.toLowerCase()))
                            .limit(20)
                            .toList()
            );
        }

        return CompletableFuture.completedFuture(List.of());
    }
}
