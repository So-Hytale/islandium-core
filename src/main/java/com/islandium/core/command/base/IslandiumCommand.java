package com.islandium.core.command.base;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.islandium.core.api.player.IslandiumPlayer;
import com.islandium.core.IslandiumPlugin;
import com.islandium.core.config.MessagesConfig;
import com.islandium.core.api.util.ColorUtil;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.permissions.PermissionsModule;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Classe de base pour les commandes Essentials.
 */
public abstract class IslandiumCommand extends AbstractCommand {

    protected final IslandiumPlugin plugin;

    protected IslandiumCommand(@NotNull IslandiumPlugin plugin, @NotNull String name, @NotNull String description) {
        super(name, description);
        this.plugin = plugin;
    }

    /**
     * Récupère le service de messages.
     */
    protected MessagesConfig messages() {
        return plugin.getConfigManager().getMessages();
    }

    /**
     * Envoie un message au sender.
     */
    protected void sendMessage(@NotNull CommandContext ctx, @NotNull String key, Object... args) {
        ctx.sendMessage(messages().getMessagePrefixed(key, args));
    }

    /**
     * Envoie un message raw au sender.
     */
    protected void sendRaw(@NotNull CommandContext ctx, @NotNull String message) {
        ctx.sendMessage(ColorUtil.parse(message));
    }

    /**
     * Vérifie si le sender est un joueur.
     */
    protected boolean isPlayer(@NotNull CommandContext ctx) {
        return ctx.isPlayer();
    }

    /**
     * Récupère le joueur sender.
     *
     * @throws IllegalStateException si le sender n'est pas un joueur
     */
    @NotNull
    protected Player requirePlayer(@NotNull CommandContext ctx) {
        if (!ctx.isPlayer()) {
            sendMessage(ctx, "player-only");
            throw new IllegalStateException("Command requires player");
        }
        return ctx.senderAs(Player.class);
    }

    /**
     * Récupère l'IslandiumPlayer du sender.
     */
    @NotNull
        protected IslandiumPlayer getIslandiumPlayer(@NotNull CommandContext ctx) {
        Player player = requirePlayer(ctx);
        return plugin.getPlayerManager().getOnlinePlayer(player.getUuid())
            .orElseThrow(() -> new IllegalStateException("Player not found in manager"));
    }

    /**
     * Vérifie si le sender a une permission.
     * Utilise le système de permissions natif Hytale qui est synchronisé avec notre BDD.
     * Les opérateurs (groupe "OP") ont automatiquement toutes les permissions.
     */
    protected boolean hasPermission(@NotNull CommandContext ctx, @NotNull String permission) {
        UUID uuid = ctx.sender().getUuid();
        PermissionsModule perms = PermissionsModule.get();

        // Les OP ont toutes les permissions
        if (perms.getGroupsForUser(uuid).contains("OP")) {
            return true;
        }

        // Vérifier la permission via le système natif (synchronisé avec notre BDD)
        return perms.hasPermission(uuid, permission);
    }

    /**
     * Retourne un CompletableFuture vide (pour les commandes qui n'ont rien d'async).
     */
    protected CompletableFuture<Void> complete() {
        return CompletableFuture.completedFuture(null);
    }

    /**
     * Retourne un CompletableFuture d'erreur.
     */
    protected CompletableFuture<Void> error(@NotNull CommandContext ctx, @NotNull String messageKey, Object... args) {
        sendMessage(ctx, messageKey, args);
        return complete();
    }

    /**
     * Tab completion par defaut - retourne une liste vide.
     * Peut etre override par les sous-classes.
     */
    public CompletableFuture<List<String>> tabComplete(CommandContext ctx, String partial) {
        return CompletableFuture.completedFuture(List.of());
    }
}
