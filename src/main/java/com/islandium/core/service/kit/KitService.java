package com.islandium.core.service.kit;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.permissions.PermissionsModule;
import com.islandium.core.IslandiumPlugin;
import com.islandium.core.database.repository.KitRepository;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Service de gestion des kits avec persistance en BDD.
 */
public class KitService {

    private final IslandiumPlugin plugin;
    private final KitRepository repository;

    // Cache local des kits (rechargeable depuis BDD)
    private final List<KitDefinition> kitsCache = Collections.synchronizedList(new ArrayList<>());

    // Cache local des cooldowns par joueur (chargement lazy)
    private final Map<String, Map<String, Long>> cooldownsCache = new ConcurrentHashMap<>();

    public KitService(@NotNull IslandiumPlugin plugin, @NotNull KitRepository repository) {
        this.plugin = plugin;
        this.repository = repository;
    }

    /**
     * Charge tous les kits depuis la BDD dans le cache local.
     */
    public CompletableFuture<Void> loadKits() {
        return repository.findAll().thenAccept(kits -> {
            synchronized (kitsCache) {
                kitsCache.clear();
                kitsCache.addAll(kits);
            }
            plugin.log(Level.INFO, "Loaded " + kits.size() + " kits from database");
        });
    }

    // === Getters ===

    @NotNull
    public List<KitDefinition> getKits() {
        return Collections.unmodifiableList(new ArrayList<>(kitsCache));
    }

    @Nullable
    public KitDefinition getKit(@NotNull String kitId) {
        synchronized (kitsCache) {
            return kitsCache.stream()
                .filter(k -> k.id.equals(kitId))
                .findFirst()
                .orElse(null);
        }
    }

    // === CRUD ===

    /**
     * Ajoute un kit et le sauvegarde en BDD.
     */
    public CompletableFuture<Void> addKit(@NotNull KitDefinition kit) {
        synchronized (kitsCache) {
            kitsCache.add(kit);
        }
        return repository.save(kit);
    }

    /**
     * Met a jour un kit existant en BDD.
     */
    public CompletableFuture<Void> updateKit(@NotNull KitDefinition kit) {
        return repository.save(kit);
    }

    /**
     * Supprime un kit de la BDD et du cache.
     */
    public CompletableFuture<Void> removeKit(@NotNull String kitId) {
        synchronized (kitsCache) {
            kitsCache.removeIf(k -> k.id.equals(kitId));
        }
        return repository.delete(kitId);
    }

    // === Kit Claiming ===

    public enum ClaimResult {
        SUCCESS,
        KIT_NOT_FOUND,
        NO_PERMISSION,
        ON_COOLDOWN,
        ALREADY_CLAIMED,
        INVENTORY_FULL
    }

    /**
     * Tente de donner un kit a un joueur.
     */
    public ClaimResult claimKit(@NotNull Player player, @NotNull String kitId) {
        UUID uuid = getPlayerUuid(player);
        if (uuid == null) return ClaimResult.KIT_NOT_FOUND;

        KitDefinition kit = getKit(kitId);
        if (kit == null) return ClaimResult.KIT_NOT_FOUND;

        // Check permission
        if (!hasKitPermission(uuid, kit)) {
            return ClaimResult.NO_PERMISSION;
        }

        // Check cooldown
        long remaining = getRemainingCooldown(uuid, kitId);
        if (kit.cooldownSeconds == 0) {
            if (remaining == -2) return ClaimResult.ALREADY_CLAIMED;
        } else if (kit.cooldownSeconds > 0 && remaining > 0) {
            return ClaimResult.ON_COOLDOWN;
        }

        // Give items
        giveItems(player, kit);

        // Record cooldown en BDD (async)
        String uuidStr = uuid.toString();
        cooldownsCache.computeIfAbsent(uuidStr, k -> new ConcurrentHashMap<>());
        cooldownsCache.get(uuidStr).put(kitId, System.currentTimeMillis());
        repository.recordClaim(uuidStr, kitId).exceptionally(ex -> {
            plugin.log(Level.WARNING, "Failed to record kit claim in DB: " + ex.getMessage());
            return null;
        });

        return ClaimResult.SUCCESS;
    }

    /**
     * Donne les items d'un kit a un joueur.
     */
    private boolean giveItems(@NotNull Player player, @NotNull KitDefinition kit) {
        if (kit.items == null || kit.items.isEmpty()) return true;

        var inv = player.getInventory();
        if (inv == null) return false;

        var hotbar = inv.getHotbar();
        var storage = inv.getStorage();

        for (KitItem kitItem : kit.items) {
            if (kitItem.itemId == null || kitItem.quantity <= 0) continue;

            String itemId = kitItem.itemId;

            ItemStack stack = new ItemStack(itemId, kitItem.quantity);

            var tx = hotbar.addItemStack(stack);
            if (!tx.succeeded()) {
                tx = storage.addItemStack(stack);
                if (!tx.succeeded()) {
                    player.sendMessage(Message.raw("Inventaire plein! Certains items n'ont pas pu etre donnes."));
                    return false;
                }
            }
        }
        return true;
    }

    // === Cooldown Logic ===

    /**
     * Charge les cooldowns d'un joueur depuis la BDD vers le cache.
     */
    public CompletableFuture<Void> loadPlayerCooldowns(@NotNull UUID uuid) {
        String uuidStr = uuid.toString();
        return repository.getPlayerCooldowns(uuidStr).thenAccept(cooldowns -> {
            cooldownsCache.put(uuidStr, new ConcurrentHashMap<>(cooldowns));
        });
    }

    /**
     * Retourne le temps restant en secondes avant de pouvoir reclamer un kit.
     * 0 = disponible, -2 = deja reclame (usage unique), > 0 = secondes restantes
     */
    public long getRemainingCooldown(@NotNull UUID uuid, @NotNull String kitId) {
        KitDefinition kit = getKit(kitId);
        if (kit == null) return 0;

        if (kit.cooldownSeconds < 0) return 0; // Pas de cooldown

        String uuidStr = uuid.toString();
        Map<String, Long> playerCooldowns = cooldownsCache.get(uuidStr);
        if (playerCooldowns == null) return 0;

        Long lastClaim = playerCooldowns.get(kitId);
        if (lastClaim == null) return 0;

        if (kit.cooldownSeconds == 0) return -2; // Usage unique, deja utilise

        long elapsed = (System.currentTimeMillis() - lastClaim) / 1000;
        long remaining = kit.cooldownSeconds - elapsed;
        return Math.max(0, remaining);
    }

    // === First Join ===

    /**
     * Donne tous les kits first-join a un joueur.
     */
    public void giveFirstJoinKits(@NotNull Player player) {
        UUID uuid = getPlayerUuid(player);
        plugin.log(Level.INFO, "[FirstJoinKit] getPlayerUuid returned: " + uuid);
        if (uuid == null) return;

        String uuidStr = uuid.toString();
        Map<String, Long> playerCooldowns = cooldownsCache.get(uuidStr);

        List<KitDefinition> allKits = getKits();
        plugin.log(Level.INFO, "[FirstJoinKit] Total kits loaded: " + allKits.size());

        for (KitDefinition kit : allKits) {
            plugin.log(Level.INFO, "[FirstJoinKit] Kit '" + kit.id + "' giveOnFirstJoin=" + kit.giveOnFirstJoin
                + " items=" + (kit.items != null ? kit.items.size() : 0));

            if (!kit.giveOnFirstJoin) {
                plugin.log(Level.INFO, "[FirstJoinKit] SKIP '" + kit.id + "': giveOnFirstJoin=false");
                continue;
            }

            if (playerCooldowns != null && playerCooldowns.containsKey(kit.id)) {
                plugin.log(Level.INFO, "[FirstJoinKit] SKIP '" + kit.id + "': already claimed");
                continue;
            }

            if (!hasKitPermission(uuid, kit)) {
                plugin.log(Level.INFO, "[FirstJoinKit] SKIP '" + kit.id + "': no permission");
                continue;
            }

            // Give items
            boolean success = giveItems(player, kit);
            plugin.log(Level.INFO, "[FirstJoinKit] giveItems for '" + kit.id + "' returned: " + success);

            // Record claim
            cooldownsCache.computeIfAbsent(uuidStr, k -> new ConcurrentHashMap<>());
            cooldownsCache.get(uuidStr).put(kit.id, System.currentTimeMillis());
            repository.recordClaim(uuidStr, kit.id).exceptionally(ex -> {
                plugin.log(Level.WARNING, "Failed to record first-join kit claim: " + ex.getMessage());
                return null;
            });

            player.sendMessage(Message.raw("Kit " + kit.displayName + " recu!"));
            plugin.log(Level.INFO, "[FirstJoinKit] Gave kit '" + kit.id + "' to " + uuidStr);
        }
    }

    // === Permission Check ===

    public boolean hasKitPermission(@NotNull UUID uuid, @NotNull KitDefinition kit) {
        if (kit.permission == null || kit.permission.isEmpty()) return true;
        try {
            var perms = PermissionsModule.get();
            if (perms.getGroupsForUser(uuid).contains("OP")) return true;
            return perms.hasPermission(uuid, kit.permission) || perms.hasPermission(uuid, "*");
        } catch (Exception e) {
            return true;
        }
    }

    // === Formatting ===

    public static String formatCooldown(long seconds) {
        if (seconds <= 0) return "Disponible";

        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;

        StringBuilder sb = new StringBuilder();
        if (hours > 0) sb.append(hours).append("h ");
        if (minutes > 0) sb.append(minutes).append("m ");
        if (secs > 0 && hours == 0) sb.append(secs).append("s");
        return sb.toString().trim();
    }

    // === Utility ===

    @Nullable
    private UUID getPlayerUuid(@NotNull Player player) {
        try {
            var ref = player.getReference();
            if (ref == null || !ref.isValid()) return null;
            var store = ref.getStore();
            var playerRef = store.getComponent(ref, com.hypixel.hytale.server.core.universe.PlayerRef.getComponentType());
            return playerRef != null ? playerRef.getUuid() : null;
        } catch (Exception e) {
            return null;
        }
    }
}
