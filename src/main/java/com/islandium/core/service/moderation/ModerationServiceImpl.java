package com.islandium.core.service.moderation;

import com.islandium.core.api.moderation.ModerationService;
import com.islandium.core.api.moderation.Punishment;
import com.islandium.core.api.moderation.PunishmentType;
import com.islandium.core.IslandiumPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Implémentation du service de modération.
 */
public class ModerationServiceImpl implements ModerationService {

    private final IslandiumPlugin plugin;

    public ModerationServiceImpl(@NotNull IslandiumPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public CompletableFuture<Punishment> ban(@NotNull UUID playerUuid, @Nullable String reason, @Nullable UUID punisherUuid) {
        return createPunishment(playerUuid, PunishmentType.BAN, reason, punisherUuid, null);
    }

    @Override
    public CompletableFuture<Punishment> tempBan(@NotNull UUID playerUuid, long durationMs, @Nullable String reason, @Nullable UUID punisherUuid) {
        return createPunishment(playerUuid, PunishmentType.BAN, reason, punisherUuid, System.currentTimeMillis() + durationMs);
    }

    @Override
    public CompletableFuture<Boolean> unban(@NotNull UUID playerUuid, @Nullable UUID unbannerUuid) {
        return revokePunishment(playerUuid, PunishmentType.BAN, unbannerUuid);
    }

    @Override
    public CompletableFuture<Boolean> isBanned(@NotNull UUID playerUuid) {
        return getActiveBan(playerUuid).thenApply(Optional::isPresent);
    }

    @Override
    public CompletableFuture<Optional<Punishment>> getActiveBan(@NotNull UUID playerUuid) {
        return getActivePunishment(playerUuid, PunishmentType.BAN);
    }

    @Override
    public CompletableFuture<Punishment> mute(@NotNull UUID playerUuid, @Nullable String reason, @Nullable UUID punisherUuid) {
        return createPunishment(playerUuid, PunishmentType.MUTE, reason, punisherUuid, null);
    }

    @Override
    public CompletableFuture<Punishment> tempMute(@NotNull UUID playerUuid, long durationMs, @Nullable String reason, @Nullable UUID punisherUuid) {
        return createPunishment(playerUuid, PunishmentType.MUTE, reason, punisherUuid, System.currentTimeMillis() + durationMs);
    }

    @Override
    public CompletableFuture<Boolean> unmute(@NotNull UUID playerUuid, @Nullable UUID unmuterUuid) {
        return revokePunishment(playerUuid, PunishmentType.MUTE, unmuterUuid);
    }

    @Override
    public CompletableFuture<Boolean> isMuted(@NotNull UUID playerUuid) {
        return getActiveMute(playerUuid).thenApply(Optional::isPresent);
    }

    @Override
    public CompletableFuture<Optional<Punishment>> getActiveMute(@NotNull UUID playerUuid) {
        return getActivePunishment(playerUuid, PunishmentType.MUTE);
    }

    @Override
    public CompletableFuture<Boolean> kick(@NotNull UUID playerUuid, @Nullable String reason, @Nullable UUID kickerUuid) {
        // Log the kick
        createPunishment(playerUuid, PunishmentType.KICK, reason, kickerUuid, null);

        // Actually kick the player
        var playerOpt = plugin.getPlayerManager().getOnlinePlayer(playerUuid);
        if (playerOpt.isEmpty()) {
            return CompletableFuture.completedFuture(false);
        }

        // Kick the player
        playerOpt.get().kick(reason != null ? reason : "Kicked by an administrator");
        return CompletableFuture.completedFuture(true);
    }

    @Override
    public CompletableFuture<List<Punishment>> getPunishmentHistory(@NotNull UUID playerUuid, int limit) {
        return plugin.getDatabaseManager().getExecutor().queryList("""
            SELECT * FROM essentials_punishments
            WHERE player_uuid = ?
            ORDER BY created_at DESC
            LIMIT ?
        """, this::mapPunishment, playerUuid.toString(), limit);
    }

    @Override
    public CompletableFuture<List<Punishment>> getActivePunishments(@NotNull UUID playerUuid) {
        return plugin.getDatabaseManager().getExecutor().queryList("""
            SELECT * FROM essentials_punishments
            WHERE player_uuid = ?
              AND revoked = 0
              AND (expires_at IS NULL OR expires_at > ?)
        """, this::mapPunishment, playerUuid.toString(), System.currentTimeMillis());
    }

    @Override
    public CompletableFuture<Punishment> punish(
            @NotNull UUID playerUuid,
            @NotNull PunishmentType type,
            @Nullable String reason,
            @Nullable UUID punisherUuid,
            @Nullable Instant expiresAt
    ) {
        Long expiresAtMs = expiresAt != null ? expiresAt.toEpochMilli() : null;
        return createPunishment(playerUuid, type, reason, punisherUuid, expiresAtMs);
    }

    @Override
    public CompletableFuture<Boolean> revokePunishment(
            @NotNull UUID playerUuid,
            @NotNull PunishmentType type,
            @Nullable UUID revokerUuid
    ) {
        return doRevokePunishment(playerUuid, type, revokerUuid);
    }

    @Override
    public CompletableFuture<Optional<Punishment>> getActivePunishment(
            @NotNull UUID playerUuid,
            @NotNull PunishmentType type
    ) {
        return doGetActivePunishment(playerUuid, type);
    }

    private CompletableFuture<Punishment> createPunishment(
            @NotNull UUID playerUuid,
            @NotNull PunishmentType type,
            @Nullable String reason,
            @Nullable UUID punisherUuid,
            @Nullable Long expiresAt
    ) {
        long now = System.currentTimeMillis();

        return plugin.getDatabaseManager().getExecutor().executeAndGetId("""
            INSERT INTO essentials_punishments (player_uuid, type, reason, punisher_uuid, created_at, expires_at)
            VALUES (?, ?, ?, ?, ?, ?)
        """,
                playerUuid.toString(),
                type.name(),
                reason,
                punisherUuid != null ? punisherUuid.toString() : null,
                now,
                expiresAt
        ).thenApply(id -> new PunishmentImpl(
                id.intValue(),
                playerUuid,
                type,
                reason,
                punisherUuid,
                now,
                expiresAt,
                false,
                null,
                null
        ));
    }

    private CompletableFuture<Optional<Punishment>> doGetActivePunishment(@NotNull UUID playerUuid, @NotNull PunishmentType type) {
        return plugin.getDatabaseManager().getExecutor().queryOne("""
            SELECT * FROM essentials_punishments
            WHERE player_uuid = ?
              AND type = ?
              AND revoked = 0
              AND (expires_at IS NULL OR expires_at > ?)
            ORDER BY created_at DESC
            LIMIT 1
        """, this::mapPunishment, playerUuid.toString(), type.name(), System.currentTimeMillis());
    }

    private CompletableFuture<Boolean> doRevokePunishment(@NotNull UUID playerUuid, @NotNull PunishmentType type, @Nullable UUID revokerUuid) {
        return plugin.getDatabaseManager().getExecutor().execute("""
            UPDATE essentials_punishments
            SET revoked = 1, revoked_by = ?, revoked_at = ?
            WHERE player_uuid = ? AND type = ? AND revoked = 0
        """,
                revokerUuid != null ? revokerUuid.toString() : null,
                System.currentTimeMillis(),
                playerUuid.toString(),
                type.name()
        ).thenApply(v -> true).exceptionally(e -> false);
    }

    private Punishment mapPunishment(java.sql.ResultSet rs) {
        try {
            String punisherStr = rs.getString("punisher_uuid");
            String revokedByStr = rs.getString("revoked_by");
            long expiresAt = rs.getLong("expires_at");
            long revokedAt = rs.getLong("revoked_at");

            return new PunishmentImpl(
                    rs.getInt("id"),
                    UUID.fromString(rs.getString("player_uuid")),
                    PunishmentType.valueOf(rs.getString("type")),
                    rs.getString("reason"),
                    punisherStr != null ? UUID.fromString(punisherStr) : null,
                    rs.getLong("created_at"),
                    expiresAt > 0 ? expiresAt : null,
                    rs.getBoolean("revoked"),
                    revokedByStr != null ? UUID.fromString(revokedByStr) : null,
                    revokedAt > 0 ? revokedAt : null
            );
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // Punishment implementation
    private record PunishmentImpl(
            int id,
            @NotNull UUID playerUuid,
            @NotNull PunishmentType type,
            @Nullable String reason,
            @Nullable UUID punisherUuid,
            long createdAt,
            @Nullable Long expiresAt,
            boolean revoked,
            @Nullable UUID revokedBy,
            @Nullable Long revokedAt
    ) implements Punishment {
        @Override public int getId() { return id; }
        @Override @NotNull public UUID getPlayerUuid() { return playerUuid; }
        @Override @NotNull public PunishmentType getType() { return type; }
        @Override @Nullable public String getReason() { return reason; }
        @Override @Nullable public UUID getPunisherUuid() { return punisherUuid; }
        @Override public long getCreatedAt() { return createdAt; }
        @Override @Nullable public Long getExpiresAt() { return expiresAt; }
        @Override public boolean isRevoked() { return revoked; }
        @Override @Nullable public UUID getRevokedBy() { return revokedBy; }
        @Override @Nullable public Long getRevokedAt() { return revokedAt; }
    }
}
