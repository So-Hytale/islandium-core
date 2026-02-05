package com.islandium.core.player;

import com.islandium.core.api.location.ServerLocation;
import com.islandium.core.api.player.IslandiumPlayer;
import com.islandium.core.api.player.PlayerState;
import com.islandium.core.api.util.ColorUtil;
import com.islandium.core.IslandiumPlugin;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.movement.MovementManager;
import com.hypixel.hytale.server.core.permissions.PermissionsModule;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Implémentation de IslandiumPlayer.
 * Utilise les APIs natives Hytale pour: vanish, god mode, fly, permissions.
 */
public class IslandiumPlayerImpl implements IslandiumPlayer {

    private final IslandiumPlugin plugin;
    private final PlayerData data;
    private Object hytalePlayer; // Player or PlayerRef

    // Transient states (custom Islandium states only - AFK, MUTED, BANNED)
    private final Set<PlayerState> states = EnumSet.noneOf(PlayerState.class);
    private UUID lastMessageSender = null;

    public IslandiumPlayerImpl(
            @NotNull IslandiumPlugin plugin,
            @NotNull PlayerData data,
            @Nullable Object hytalePlayer
    ) {
        this.plugin = plugin;
        this.data = data;
        this.hytalePlayer = hytalePlayer;
        // Note: Les états natifs (vanish, god, fly) sont gérés par Hytale
    }

    // === Basic Info ===

    @Override
    @NotNull
    public UUID getUniqueId() {
        return data.uuid();
    }

    @Override
    @NotNull
    public String getName() {
        return data.username();
    }

    @Override
    public boolean isOnline() {
        return hytalePlayer != null;
    }

    @Override
    public boolean isOnlineNetwork() {
        if (isOnline()) return true;
        return plugin.getRedisManager().sismember("ess:online", getUniqueId().toString())
                .join();
    }

    @Override
    @Nullable
    public String getCurrentServer() {
        if (isOnline()) {
            return plugin.getServerName();
        }
        return data.lastServer();
    }

    // === Custom States (Islandium) ===

    @Override
    public boolean hasState(@NotNull PlayerState state) {
        return states.contains(state);
    }

    @Override
    public void addState(@NotNull PlayerState state) {
        states.add(state);
    }

    @Override
    public void removeState(@NotNull PlayerState state) {
        states.remove(state);
    }

    // === Native States (Hytale) ===

    @Override
    public boolean isVanished() {
        if (!isOnline()) return false;
        if (hytalePlayer instanceof PlayerRef ref) {
            // Un joueur est "vanished" s'il est caché de tous les autres joueurs
            // Note: HiddenPlayersManager gère qui VOIT qui, pas qui EST vanish
            // Pour un vrai vanish global, on doit vérifier autrement
            // Ici on retourne false car le système natif ne stocke pas "vanished" globalement
            return false;
        }
        return false;
    }

    @Override
    public void setVanished(boolean vanished) {
        if (!isOnline()) return;
        // TODO: Implémenter le vanish via HiddenPlayersManager quand l'API sera mieux connue
        // Pour l'instant, cette fonctionnalité est désactivée
    }

    @Override
    public boolean isGodMode() {
        // TODO: Implémenter via Invulnerable component quand l'API sera mieux connue
        return false;
    }

    @Override
    public void setGodMode(boolean godMode) {
        // TODO: Implémenter via Invulnerable component quand l'API sera mieux connue
    }

    @Override
    public boolean canFly() {
        if (!isOnline()) return false;
        if (hytalePlayer instanceof PlayerRef ref) {
            MovementManager movementManager = ref.getComponent(MovementManager.getComponentType());
            if (movementManager != null && movementManager.getSettings() != null) {
                return movementManager.getSettings().canFly;
            }
        }
        return false;
    }

    @Override
    public void setCanFly(boolean canFly) {
        if (!isOnline()) return;
        if (hytalePlayer instanceof PlayerRef ref) {
            MovementManager movementManager = ref.getComponent(MovementManager.getComponentType());
            if (movementManager != null) {
                if (movementManager.getSettings() != null) {
                    movementManager.getSettings().canFly = canFly;
                }
                if (movementManager.getDefaultSettings() != null) {
                    movementManager.getDefaultSettings().canFly = canFly;
                }
                movementManager.update(ref.getPacketHandler());
            }
        }
    }

    // === Economy ===

    @Override
    @NotNull
    public BigDecimal getBalance() {
        return data.balance();
    }

    @Override
    public CompletableFuture<Void> setBalance(@NotNull BigDecimal balance) {
        data.setBalance(balance);
        return save();
    }

    @Override
    public CompletableFuture<Void> addBalance(@NotNull BigDecimal amount) {
        data.addBalance(amount);
        return save();
    }

    @Override
    public CompletableFuture<Boolean> removeBalance(@NotNull BigDecimal amount) {
        boolean success = data.removeBalance(amount);
        if (success) {
            return save().thenApply(v -> true);
        }
        return CompletableFuture.completedFuture(false);
    }

    @Override
    public boolean hasBalance(@NotNull BigDecimal amount) {
        return data.hasBalance(amount);
    }

    // === Location (Utilise le système natif Hytale) ===

    @Override
    @Nullable
    public ServerLocation getLocation() {
        if (!isOnline()) return getLastLocation();

        // PlayerRef - méthode préférée
        if (hytalePlayer instanceof PlayerRef ref) {
            var transform = ref.getTransform();
            if (transform == null) return getLastLocation();
            var pos = transform.getPosition();
            var rot = transform.getRotation();
            // Obtenir le monde - utiliser la clé du monde (pas l'UUID)
            String worldName = getWorldNameFromUuid(ref.getWorldUuid());
            return ServerLocation.of(
                plugin.getServerName(),
                worldName,
                pos.getX(), pos.getY(), pos.getZ(),
                rot.getY(), rot.getX() // yaw, pitch
            );
        }

        // Player - fallback
        if (hytalePlayer instanceof Player player) {
            var transform = player.getTransformComponent();
            if (transform == null) return getLastLocation();
            var pos = transform.getPosition();
            var rot = transform.getRotation();
            String world = player.getWorld() != null ? player.getWorld().getName() : "world";
            return ServerLocation.of(
                plugin.getServerName(),
                world,
                pos.getX(), pos.getY(), pos.getZ(),
                rot.getY(), rot.getX() // yaw, pitch
            );
        }
        return getLastLocation();
    }

    @Override
    @Nullable
    public ServerLocation getLastLocation() {
        // Utilise PlayerConfigData natif Hytale pour la dernière position
        if (hytalePlayer instanceof Player player) {
            var configData = player.getPlayerConfigData();
            if (configData != null) {
                var pos = configData.lastSavedPosition;
                var rot = configData.lastSavedRotation;
                String world = configData.getWorld();
                if (world != null) {
                    return ServerLocation.of(
                        data.lastServer() != null ? data.lastServer() : plugin.getServerName(),
                        world,
                        pos.getX(), pos.getY(), pos.getZ(),
                        rot.getY(), rot.getX() // yaw, pitch
                    );
                }
            }
        }
        return null;
    }

    @Override
    @NotNull
    public String getServer() {
        return isOnline() ? plugin.getServerName() : (data.lastServer() != null ? data.lastServer() : "unknown");
    }

    @Override
    public CompletableFuture<Boolean> teleport(@NotNull ServerLocation location) {
        if (!isOnline()) {
            return CompletableFuture.completedFuture(false);
        }

        // TODO: Implémenter la téléportation via l'API native Hytale quand elle sera mieux connue
        // Pour l'instant, cette fonctionnalité est désactivée
        return CompletableFuture.completedFuture(false);
    }

    // === Timestamps ===

    @Override
    public long getFirstLogin() {
        return data.firstLogin();
    }

    @Override
    public long getLastLogin() {
        return data.lastLogin();
    }

    // === Messaging ===

    @Override
    public void sendMessage(@NotNull String message) {
        if (!isOnline()) return;

        Message hytaleMessage = ColorUtil.parse(message);

        if (hytalePlayer instanceof Player player) {
            player.sendMessage(hytaleMessage);
        } else if (hytalePlayer instanceof PlayerRef ref) {
            ref.sendMessage(hytaleMessage);
        }
    }

    @Override
    public void sendMessage(@NotNull String message, Object... args) {
        sendMessage(String.format(message, args));
    }

    /**
     * Envoie un message Hytale directement.
     */
    @Override
    public void sendMessage(@NotNull Message message) {
        if (!isOnline()) return;

        if (hytalePlayer instanceof Player player) {
            player.sendMessage(message);
        } else if (hytalePlayer instanceof PlayerRef ref) {
            ref.sendMessage(message);
        }
    }

    @Override
    @Nullable
    public UUID getLastMessageSender() {
        return lastMessageSender;
    }

    @Override
    public void setLastMessageSender(@Nullable UUID uuid) {
        this.lastMessageSender = uuid;
    }

    // === Permissions (Utilise le système natif Hytale) ===

    @Override
    public boolean hasPermission(@NotNull String permission) {
        // Utilise uniquement le système de permissions natif Hytale
        return PermissionsModule.get().hasPermission(getUniqueId(), permission);
    }

    @Override
    public boolean isOp() {
        // Vérifie si le joueur est dans le groupe "OP" natif Hytale
        return PermissionsModule.get().getGroupsForUser(getUniqueId()).contains("OP");
    }

    // === Actions ===

    @Override
    public void heal() {
        // Note: L'API Hytale utilise un système ECS pour la santé
        // Cette fonctionnalité nécessite l'accès aux composants de santé
        // TODO: Implémenter quand l'API le permettra
    }

    @Override
    public void feed() {
        // Note: L'API Hytale utilise un système ECS pour la nourriture
        // Cette fonctionnalité nécessite l'accès aux composants de faim
        // TODO: Implémenter quand l'API le permettra
    }

    @Override
    public void setGameMode(@NotNull String gameMode) {
        // Note: setGameMode dans Hytale requiert Ref<EntityStore>, GameMode, ComponentAccessor
        // Cette API complexe nécessite plus de contexte
        // TODO: Implémenter avec l'API ECS appropriée
    }

    @Override
    public void kick(@NotNull String reason) {
        if (hytalePlayer instanceof Player player) {
            var playerRef = player.getPlayerRef();
            if (playerRef != null) {
                // Note: disconnect prend un String, on utilise stripColors pour le kick
                playerRef.getPacketHandler().disconnect(ColorUtil.stripColors(reason));
            }
        }
    }

    // === Data ===

    @Override
    public CompletableFuture<Void> save() {
        return plugin.getServiceManager().getPlayerRepository()
                .save(data)
                .thenApply(v -> null);
    }

    @Override
    public CompletableFuture<Void> reload() {
        return plugin.getServiceManager().getPlayerRepository()
                .findById(getUniqueId())
                .thenAccept(opt -> opt.ifPresent(newData -> {
                    data.setUsername(newData.username());
                    data.setBalance(newData.balance());
                    data.setLastServer(newData.lastServer());
                    // Note: vanish, godMode, location sont gérés nativement par Hytale
                }));
    }

    // === Internal ===

    @NotNull
    public PlayerData getData() {
        return data;
    }

    public void setHytalePlayer(@Nullable Object hytalePlayer) {
        this.hytalePlayer = hytalePlayer;
    }

    @Override
    @Nullable
    public Player getHytalePlayer() {
        if (hytalePlayer instanceof Player player) {
            return player;
        }
        return null;
    }

    /**
     * @return le joueur Hytale natif brut (Player ou PlayerRef), ou null si offline
     */
    @Nullable
    public Object getHytalePlayerRaw() {
        return hytalePlayer;
    }

    /**
     * Convertit un UUID de monde en nom de monde (clé dans Universe).
     * Le problème: PlayerRef.getWorldUuid() retourne l'UUID, mais on veut la clé (ex: "prison").
     */
    @NotNull
    private String getWorldNameFromUuid(@Nullable UUID worldUuid) {
        if (worldUuid == null) return "world";

        Universe universe = Universe.get();
        if (universe == null) return worldUuid.toString();

        // Chercher le monde par son UUID
        for (var entry : universe.getWorlds().entrySet()) {
            String key = entry.getKey();
            World w = entry.getValue();

            // Méthode 1: L'UUID est dans la clé (ex: "instance-basic-UUID")
            if (key.contains(worldUuid.toString())) {
                return key;
            }

            // Méthode 2: Comparer avec World.getUuid() si disponible
            try {
                UUID wUuid = w.getWorldConfig().getUuid();
                if (wUuid != null && wUuid.equals(worldUuid)) {
                    return key; // Retourner la clé (ex: "prison")
                }
            } catch (Exception ignored) {}
        }

        // Fallback: retourner l'UUID sous forme de string
        return worldUuid.toString();
    }
}
