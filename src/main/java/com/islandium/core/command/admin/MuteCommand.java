package com.islandium.core.command.admin;

import com.islandium.core.api.moderation.PunishmentType;
import com.islandium.core.IslandiumPlugin;
import com.islandium.core.command.base.IslandiumCommand;
import com.islandium.core.util.text.TimeFormatter;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;

public class MuteCommand extends IslandiumCommand {
    private final RequiredArg<String> playerArg;
    private final OptionalArg<String> durationArg;
    private final OptionalArg<String> reasonArg;

    public MuteCommand(IslandiumPlugin plugin) {
        super(plugin, "mute", "Rend un joueur muet");
        requirePermission("islandium.mute");
        playerArg = withRequiredArg("player", "Joueur à mute", ArgTypes.STRING);
        durationArg = withOptionalArg("duration", "Durée (ex: 1h, 1d)", ArgTypes.STRING);
        reasonArg = withOptionalArg("reason", "Raison du mute", ArgTypes.STRING);
    }

    @Override
    public CompletableFuture<Void> execute(CommandContext ctx) {
        String targetName = ctx.get(playerArg);
        String durationStr = ctx.provided(durationArg) ? ctx.get(durationArg) : null;
        String reason = ctx.provided(reasonArg) ? ctx.get(reasonArg) : "Aucune raison spécifiée";
        var punisher = getIslandiumPlayer(ctx);

        Instant expiresAt = null;
        if (durationStr != null) {
            Duration duration = TimeFormatter.parseDuration(durationStr);
            if (duration == null) {
                return error(ctx, "invalid-duration", "duration", durationStr);
            }
            expiresAt = Instant.now().plus(duration);
        }

        final Instant finalExpiresAt = expiresAt;

        return plugin.getPlayerManager().getPlayerUUID(targetName).thenCompose(uuidOpt -> {
            if (uuidOpt.isEmpty()) return error(ctx, "player-not-found", "player", targetName);

            return plugin.getServiceManager().getModerationService()
                .punish(uuidOpt.get(), PunishmentType.MUTE, reason, punisher.getUniqueId(), finalExpiresAt)
                .thenAccept(punishment -> {
                    if (finalExpiresAt != null) {
                        String formattedDuration = TimeFormatter.formatDuration(Duration.between(Instant.now(), finalExpiresAt));
                        sendMessage(ctx, "mute.tempmuted", "player", targetName, "duration", formattedDuration, "reason", reason);
                    } else {
                        sendMessage(ctx, "mute.muted", "player", targetName, "reason", reason);
                    }
                });
        });
    }
}
