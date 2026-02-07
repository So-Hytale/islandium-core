package com.islandium.core.database.repository;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.islandium.core.database.SQLExecutor;
import com.islandium.core.service.kit.KitDefinition;
import com.islandium.core.service.kit.KitItem;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Type;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Repository pour la persistence des kits en BDD.
 */
public class KitRepository {

    private static final Gson GSON = new GsonBuilder().create();
    private static final Type ITEMS_TYPE = new TypeToken<List<KitItem>>() {}.getType();

    private final SQLExecutor sql;

    public KitRepository(@NotNull SQLExecutor sql) {
        this.sql = sql;
    }

    /**
     * Charge tous les kits depuis la BDD.
     */
    public CompletableFuture<List<KitDefinition>> findAll() {
        return sql.queryList(
            "SELECT * FROM essentials_kits ORDER BY created_at ASC",
            this::mapKit
        );
    }

    /**
     * Charge un kit par son nom.
     */
    public CompletableFuture<Optional<KitDefinition>> findByName(@NotNull String name) {
        return sql.queryOne(
            "SELECT * FROM essentials_kits WHERE name = ?",
            this::mapKit,
            name
        );
    }

    /**
     * Sauvegarde un kit (insert ou update).
     */
    public CompletableFuture<Void> save(@NotNull KitDefinition kit) {
        String itemsJson = GSON.toJson(kit.items != null ? kit.items : new ArrayList<>());
        return sql.execute("""
            INSERT INTO essentials_kits (name, display_name, description, icon, color, cooldown_seconds, permission, give_on_first_join, items, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE
                display_name = VALUES(display_name),
                description = VALUES(description),
                icon = VALUES(icon),
                color = VALUES(color),
                cooldown_seconds = VALUES(cooldown_seconds),
                permission = VALUES(permission),
                give_on_first_join = VALUES(give_on_first_join),
                items = VALUES(items)
            """,
            kit.id,
            kit.displayName != null ? kit.displayName : kit.id,
            kit.description,
            kit.icon != null ? kit.icon : "minecraft:chest",
            kit.color != null ? kit.color : "#4fc3f7",
            kit.cooldownSeconds,
            kit.permission,
            kit.giveOnFirstJoin ? 1 : 0,
            itemsJson,
            System.currentTimeMillis()
        );
    }

    /**
     * Supprime un kit par son nom.
     */
    public CompletableFuture<Void> delete(@NotNull String name) {
        return sql.execute("DELETE FROM essentials_kits WHERE name = ?", name);
    }

    // === Cooldowns ===

    /**
     * Charge le timestamp du dernier claim d'un kit pour un joueur.
     */
    public CompletableFuture<Long> getLastClaim(@NotNull String playerUuid, @NotNull String kitName) {
        return sql.queryLong(
            "SELECT last_used FROM essentials_kit_cooldowns WHERE player_uuid = ? AND kit_name = ?",
            playerUuid, kitName
        );
    }

    /**
     * Charge tous les cooldowns d'un joueur.
     */
    public CompletableFuture<java.util.Map<String, Long>> getPlayerCooldowns(@NotNull String playerUuid) {
        return sql.queryList(
            "SELECT kit_name, last_used FROM essentials_kit_cooldowns WHERE player_uuid = ?",
            rs -> {
                try {
                    return new Object[]{rs.getString("kit_name"), rs.getLong("last_used")};
                } catch (Exception e) {
                    return null;
                }
            },
            playerUuid
        ).thenApply(list -> {
            java.util.Map<String, Long> map = new java.util.HashMap<>();
            for (Object[] row : list) {
                if (row != null) {
                    map.put((String) row[0], (Long) row[1]);
                }
            }
            return map;
        });
    }

    /**
     * Enregistre un claim de kit (insert ou update).
     */
    public CompletableFuture<Void> recordClaim(@NotNull String playerUuid, @NotNull String kitName) {
        return sql.execute("""
            INSERT INTO essentials_kit_cooldowns (player_uuid, kit_name, last_used)
            VALUES (?, ?, ?)
            ON DUPLICATE KEY UPDATE last_used = VALUES(last_used)
            """,
            playerUuid, kitName, System.currentTimeMillis()
        );
    }

    // === Mapper ===

    private KitDefinition mapKit(ResultSet rs) {
        try {
            KitDefinition kit = new KitDefinition();
            kit.id = rs.getString("name");
            kit.displayName = rs.getString("display_name");
            kit.description = rs.getString("description");
            kit.icon = rs.getString("icon");
            kit.color = rs.getString("color");
            kit.cooldownSeconds = rs.getInt("cooldown_seconds");
            kit.permission = rs.getString("permission");
            kit.giveOnFirstJoin = rs.getBoolean("give_on_first_join");

            String itemsJson = rs.getString("items");
            if (itemsJson != null && !itemsJson.isEmpty()) {
                kit.items = GSON.fromJson(itemsJson, ITEMS_TYPE);
            } else {
                kit.items = new ArrayList<>();
            }

            return kit;
        } catch (Exception e) {
            throw new RuntimeException("Failed to map kit from ResultSet", e);
        }
    }
}
