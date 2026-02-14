package com.islandium.core.service.teleport;

import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.Ref;
import com.islandium.core.IslandiumPlugin;
import com.islandium.core.api.location.ServerLocation;
import com.islandium.core.api.player.IslandiumPlayer;
import com.islandium.core.api.util.ColorUtil;
import com.islandium.core.api.util.NotificationType;
import com.islandium.core.player.IslandiumPlayerImpl;
import com.islandium.core.service.back.BackService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.logging.Level;

/**
 * Service de teleportation avec warmup et annulation si mouvement.
 */
public class TeleportService {

    private final IslandiumPlugin plugin;
    private final ScheduledExecutorService scheduler;
    private final Map<UUID, TeleportTask> pendingTeleports = new ConcurrentHashMap<>();

    // Configuration
    private int warmupSeconds = 5;
    private double movementThreshold = 0.5; // Distance max avant annulation

    // Nombre max de retries pour appliquer la position apres cross-world
    private static final int MAX_POSITION_RETRIES = 5;
    private static final long RETRY_DELAY_MS = 200;

    public TeleportService(@NotNull IslandiumPlugin plugin) {
        this.plugin = plugin;
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "Islandium-Teleport");
            t.setDaemon(true);
            return t;
        });
    }

    /**
     * Configure le temps de warmup.
     */
    public void setWarmupSeconds(int seconds) {
        this.warmupSeconds = seconds;
    }

    /**
     * Configure la distance max de mouvement avant annulation.
     */
    public void setMovementThreshold(double threshold) {
        this.movementThreshold = threshold;
    }

    /**
     * Teleporte un joueur avec warmup.
     * Le warmup est ignore si le joueur est en mode creatif.
     *
     * @param player      Le joueur a teleporter
     * @param destination La destination
     * @param onComplete  Callback quand la teleportation est terminee (succes ou echec)
     */
    public void teleportWithWarmup(
            @NotNull IslandiumPlayer player,
            @NotNull ServerLocation destination,
            @Nullable Runnable onComplete
    ) {
        UUID uuid = player.getUniqueId();

        // Annuler toute teleportation en cours
        cancelPendingTeleport(uuid);

        // Obtenir la position initiale
        ServerLocation startLocation = player.getLocation();
        if (startLocation == null) {
            plugin.log(Level.WARNING, "Cannot teleport player " + uuid + ": no location");
            if (onComplete != null) onComplete.run();
            return;
        }

        // Verifier si le joueur est en mode creatif (bypass warmup)
        Player hytalePlayer = player.getHytalePlayer();
        if (hytalePlayer != null) {
            try {
                GameMode gameMode = hytalePlayer.getGameMode();
                if (gameMode == GameMode.Creative) {
                    doTeleport(player, destination);
                    player.sendNotification(NotificationType.SUCCESS,
                            ColorUtil.stripColors(plugin.getConfigManager().getMessages().getPrefixed("teleport.success")));
                    if (onComplete != null) onComplete.run();
                    return;
                }
            } catch (Exception e) {
                System.out.println("[ISLANDIUM-TP] Could not check game mode for " + player.getName() + ": " + e.getMessage());
            }
        } else {
            // Fallback: essayer de bypass le warmup via permission
            if (player.hasPermission("islandium.teleport.nowarmup")) {
                doTeleport(player, destination);
                player.sendNotification(NotificationType.SUCCESS,
                        ColorUtil.stripColors(plugin.getConfigManager().getMessages().getPrefixed("teleport.success")));
                if (onComplete != null) onComplete.run();
                return;
            }
        }

        // Bypass warmup dans le monde cellule
        ServerLocation playerLoc = player.getLocation();
        if (playerLoc != null && "cellule".equalsIgnoreCase(playerLoc.world())) {
            doTeleport(player, destination);
            player.sendNotification(NotificationType.SUCCESS,
                    ColorUtil.stripColors(plugin.getConfigManager().getMessages().getPrefixed("teleport.success")));
            if (onComplete != null) onComplete.run();
            return;
        }

        // Message de warmup
        player.sendNotification(NotificationType.INFO,
                ColorUtil.stripColors(plugin.getConfigManager().getMessages()
                        .getPrefixed("teleport.warmup", "seconds", String.valueOf(warmupSeconds))));

        // Creer la tache de teleportation
        TeleportTask task = new TeleportTask(player, startLocation, destination, onComplete);

        // Programmer la verification du mouvement toutes les 500ms
        ScheduledFuture<?> checkTask = scheduler.scheduleAtFixedRate(() -> {
            if (!checkMovement(task)) {
                cancelPendingTeleport(uuid);
            }
        }, 500, 500, TimeUnit.MILLISECONDS);

        task.setCheckTask(checkTask);

        // Programmer la teleportation apres le warmup
        ScheduledFuture<?> teleportTask = scheduler.schedule(() -> {
            executeTeleport(task);
        }, warmupSeconds, TimeUnit.SECONDS);

        task.setTeleportTask(teleportTask);

        pendingTeleports.put(uuid, task);
    }

    /**
     * Teleporte un joueur instantanement (sans warmup).
     */
    public void teleportInstant(@NotNull IslandiumPlayer player, @NotNull ServerLocation destination) {
        UUID uuid = player.getUniqueId();
        cancelPendingTeleport(uuid);
        doTeleport(player, destination);
    }

    /**
     * Verifie si le joueur a bouge.
     */
    private boolean checkMovement(TeleportTask task) {
        IslandiumPlayer player = task.getPlayer();
        if (!player.isOnline()) {
            return false;
        }

        ServerLocation current = player.getLocation();
        if (current == null) {
            return false;
        }

        ServerLocation start = task.getStartLocation();
        double distance = Math.sqrt(
                Math.pow(current.x() - start.x(), 2) +
                Math.pow(current.y() - start.y(), 2) +
                Math.pow(current.z() - start.z(), 2)
        );

        return distance <= movementThreshold;
    }

    /**
     * Execute la teleportation.
     */
    private void executeTeleport(TeleportTask task) {
        UUID uuid = task.getPlayer().getUniqueId();
        pendingTeleports.remove(uuid);

        // Arreter la verification du mouvement
        if (task.getCheckTask() != null) {
            task.getCheckTask().cancel(false);
        }

        if (!task.getPlayer().isOnline()) {
            if (task.getOnComplete() != null) {
                task.getOnComplete().run();
            }
            return;
        }

        // Faire la teleportation
        doTeleport(task.getPlayer(), task.getDestination());

        // Message de succes
        task.getPlayer().sendNotification(NotificationType.SUCCESS,
                ColorUtil.stripColors(plugin.getConfigManager().getMessages().getPrefixed("teleport.success")));

        if (task.getOnComplete() != null) {
            task.getOnComplete().run();
        }
    }

    /**
     * Effectue la teleportation reelle via l'API Hytale.
     * Gere le changement de monde si la destination est dans un monde different.
     * Execute sur le thread du monde pour garantir la thread-safety.
     */
    private void doTeleport(@NotNull IslandiumPlayer islandiumPlayer, @NotNull ServerLocation destination) {
        PlayerRef playerRef = getPlayerRef(islandiumPlayer);
        if (playerRef == null) {
            System.out.println("[ISLANDIUM-TP] doTeleport: playerRef is null for " + islandiumPlayer.getName());
            return;
        }

        try {
            // Sauvegarder la position actuelle pour /back
            ServerLocation currentLocation = islandiumPlayer.getLocation();
            if (currentLocation != null) {
                BackService backService = plugin.getBackService();
                if (backService != null) {
                    backService.saveLocation(islandiumPlayer.getUniqueId(), currentLocation);
                }
            }

            Universe universe = Universe.get();
            if (universe == null) {
                System.out.println("[ISLANDIUM-TP] Cannot teleport " + islandiumPlayer.getName() + ": Universe is null");
                return;
            }

            // Resoudre le monde de DESTINATION par son nom
            String destWorldName = destination.world();
            World destinationWorld = universe.getWorlds().get(destWorldName);
            if (destinationWorld == null) {
                // Fallback: chercher par UUID au cas ou destWorldName est un UUID
                destinationWorld = findWorldByUuidString(universe, destWorldName);
                if (destinationWorld == null) {
                    System.out.println("[ISLANDIUM-TP] Cannot teleport " + islandiumPlayer.getName() + ": destination world '" + destWorldName + "' not found. Available worlds: " + universe.getWorlds().keySet());
                    return;
                }
                System.out.println("[ISLANDIUM-TP] Resolved destination world by UUID fallback: " + destinationWorld.getName());
            }

            // Resoudre le monde ACTUEL du joueur
            World currentWorld = resolveCurrentWorld(universe, playerRef);

            if (currentWorld == null) {
                System.out.println("[ISLANDIUM-TP] Cannot teleport " + islandiumPlayer.getName() + ": current world not found for UUID " + playerRef.getWorldUuid() + ". Available worlds: " + universe.getWorlds().keySet());
                return;
            }

            // Verifier si on doit changer de monde
            boolean sameWorld = currentWorld == destinationWorld;

            if (sameWorld) {
                // Meme monde: teleportation simple aux coordonnees
                currentWorld.execute(() -> {
                    try {
                        applyTeleportPosition(playerRef, islandiumPlayer, destination);
                    } catch (Exception e) {
                        System.out.println("[ISLANDIUM-TP] Failed same-world teleport for " + islandiumPlayer.getName() + ": " + e.getMessage());
                        e.printStackTrace();
                    }
                });
            } else {
                // Monde different: retirer du monde actuel, ajouter au monde de destination, puis TP
                System.out.println("[ISLANDIUM-TP] Cross-world: " + currentWorld.getName() + " -> " + destinationWorld.getName() + " for " + islandiumPlayer.getName());
                final World destWorld = destinationWorld;
                currentWorld.execute(() -> {
                    try {
                        // Verifier que le joueur est toujours valide avant de le retirer
                        Ref<EntityStore> preRef = playerRef.getReference();
                        if (preRef == null || !preRef.isValid()) {
                            System.out.println("[ISLANDIUM-TP] Player ref invalid before removeFromStore for " + islandiumPlayer.getName());
                            return;
                        }

                        playerRef.removeFromStore();
                        destWorld.addPlayer(playerRef, null, Boolean.TRUE, Boolean.FALSE)
                            .thenRun(() -> {
                                // Verifier que le joueur est toujours en ligne
                                if (!islandiumPlayer.isOnline()) {
                                    System.out.println("[ISLANDIUM-TP] Player " + islandiumPlayer.getName() + " disconnected during cross-world teleport");
                                    return;
                                }
                                // Appliquer la position avec retry
                                applyPositionWithRetry(destWorld, playerRef, islandiumPlayer, destination, 0);
                            })
                            .exceptionally(ex -> {
                                System.out.println("[ISLANDIUM-TP] Failed to add " + islandiumPlayer.getName() + " to " + destWorld.getName() + ": " + ex.getMessage());
                                ex.printStackTrace();
                                return null;
                            });
                    } catch (Exception e) {
                        System.out.println("[ISLANDIUM-TP] Failed cross-world teleport for " + islandiumPlayer.getName() + ": " + e.getMessage());
                        e.printStackTrace();
                    }
                });
            }
        } catch (Exception e) {
            System.out.println("[ISLANDIUM-TP] Failed to teleport " + islandiumPlayer.getName() + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Applique la position apres un cross-world teleport avec retry.
     * Apres addPlayer(), le ref/transform peut ne pas etre immediatement pret.
     * On retry jusqu'a MAX_POSITION_RETRIES fois avec un delai.
     */
    private void applyPositionWithRetry(World destWorld, PlayerRef playerRef, IslandiumPlayer islandiumPlayer, ServerLocation destination, int attempt) {
        destWorld.execute(() -> {
            try {
                Ref<EntityStore> ref = playerRef.getReference();
                if (ref != null && ref.isValid()) {
                    Store<EntityStore> store = ref.getStore();
                    TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
                    if (transform != null) {
                        Vector3d targetPos = new Vector3d(destination.x(), destination.y(), destination.z());
                        Vector3f targetRotation = (destination.yaw() != 0f || destination.pitch() != 0f)
                            ? new Vector3f(destination.pitch(), destination.yaw(), 0f)
                            : transform.getRotation().clone();
                        Teleport teleport = new Teleport(targetPos, targetRotation);
                        store.addComponent(ref, Teleport.getComponentType(), teleport);
                        System.out.println("[ISLANDIUM-TP] SUCCESS: Teleported " + islandiumPlayer.getName() + " to " + destWorld.getName() + " " + destination.x() + "," + destination.y() + "," + destination.z() + (attempt > 0 ? " (retry " + attempt + ")" : ""));
                        return;
                    }
                }

                // Ref ou transform pas encore pret - retry si possible
                if (attempt < MAX_POSITION_RETRIES) {
                    System.out.println("[ISLANDIUM-TP] Ref/transform not ready for " + islandiumPlayer.getName() + " in " + destWorld.getName() + ", retry " + (attempt + 1) + "/" + MAX_POSITION_RETRIES);
                    scheduler.schedule(() -> {
                        if (islandiumPlayer.isOnline()) {
                            applyPositionWithRetry(destWorld, playerRef, islandiumPlayer, destination, attempt + 1);
                        }
                    }, RETRY_DELAY_MS, TimeUnit.MILLISECONDS);
                } else {
                    System.out.println("[ISLANDIUM-TP] FAILED: Could not apply position for " + islandiumPlayer.getName() + " after " + MAX_POSITION_RETRIES + " retries (ref=" + (ref != null) + ", valid=" + (ref != null && ref.isValid()) + ")");
                }
            } catch (Exception e) {
                System.out.println("[ISLANDIUM-TP] Error applying position for " + islandiumPlayer.getName() + ": " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    /**
     * Applique la position de teleportation au joueur via le composant Teleport.
     * Doit etre appele sur le thread du monde.
     */
    private void applyTeleportPosition(@NotNull PlayerRef playerRef, @NotNull IslandiumPlayer islandiumPlayer, @NotNull ServerLocation destination) {
        Ref<EntityStore> ref = playerRef.getReference();
        if (ref != null && ref.isValid()) {
            Store<EntityStore> store = ref.getStore();
            TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
            if (transform != null) {
                Vector3d targetPos = new Vector3d(destination.x(), destination.y(), destination.z());
                Vector3f targetRotation = (destination.yaw() != 0f || destination.pitch() != 0f)
                    ? new Vector3f(destination.pitch(), destination.yaw(), 0f)
                    : transform.getRotation().clone();
                Teleport teleport = new Teleport(targetPos, targetRotation);
                store.addComponent(ref, Teleport.getComponentType(), teleport);
                System.out.println("[ISLANDIUM-TP] SUCCESS: Teleported " + islandiumPlayer.getName() + " to " + destination.x() + "," + destination.y() + "," + destination.z());
                return;
            }
        }
        System.out.println("[ISLANDIUM-TP] Cannot teleport " + islandiumPlayer.getName() + ": invalid ref (" + (ref != null) + ") or transform");
    }

    /**
     * Resout le monde actuel du joueur a partir de son PlayerRef.
     * Cherche par UUID de WorldConfig, puis par nom dans la cle.
     */
    @Nullable
    private World resolveCurrentWorld(@NotNull Universe universe, @NotNull PlayerRef playerRef) {
        UUID currentWorldUuid = playerRef.getWorldUuid();
        if (currentWorldUuid == null) return null;

        // Methode 1: Comparer avec WorldConfig UUID
        for (World w : universe.getWorlds().values()) {
            try {
                UUID wUuid = w.getWorldConfig().getUuid();
                if (wUuid != null && wUuid.equals(currentWorldUuid)) {
                    return w;
                }
            } catch (Exception ignored) {}
        }

        // Methode 2: Chercher l'UUID dans le nom du monde (ex: "instance-basic-UUID")
        String uuidStr = currentWorldUuid.toString();
        for (var entry : universe.getWorlds().entrySet()) {
            if (entry.getKey().contains(uuidStr)) {
                return entry.getValue();
            }
        }

        return null;
    }

    /**
     * Cherche un monde par un UUID sous forme de string (fallback si le nom du monde est un UUID).
     */
    @Nullable
    private World findWorldByUuidString(@NotNull Universe universe, @NotNull String possibleUuid) {
        try {
            UUID uuid = UUID.fromString(possibleUuid);
            for (World w : universe.getWorlds().values()) {
                try {
                    UUID wUuid = w.getWorldConfig().getUuid();
                    if (wUuid != null && wUuid.equals(uuid)) {
                        return w;
                    }
                } catch (Exception ignored) {}
            }
        } catch (IllegalArgumentException ignored) {
            // Ce n'est pas un UUID, c'est normal
        }
        return null;
    }

    /**
     * Obtient le PlayerRef a partir d'un IslandiumPlayer.
     */
    @Nullable
    private PlayerRef getPlayerRef(@NotNull IslandiumPlayer player) {
        if (player instanceof IslandiumPlayerImpl impl) {
            Object raw = impl.getHytalePlayerRaw();
            if (raw instanceof PlayerRef ref) {
                return ref;
            } else if (raw instanceof Player p) {
                try {
                    return p.getPlayerRef();
                } catch (Exception e) {
                    System.out.println("[ISLANDIUM-TP] Failed to get PlayerRef from Player for " + player.getName() + ": " + e.getMessage());
                }
            }
        }
        // Fallback: essayer getHytalePlayer()
        Player hytalePlayer = player.getHytalePlayer();
        if (hytalePlayer != null) {
            try {
                return hytalePlayer.getPlayerRef();
            } catch (Exception e) {
                System.out.println("[ISLANDIUM-TP] Failed to get PlayerRef via fallback for " + player.getName() + ": " + e.getMessage());
            }
        }
        return null;
    }

    /**
     * Annule une teleportation en cours.
     */
    public void cancelPendingTeleport(@NotNull UUID uuid) {
        TeleportTask task = pendingTeleports.remove(uuid);
        if (task != null) {
            if (task.getCheckTask() != null) {
                task.getCheckTask().cancel(false);
            }
            if (task.getTeleportTask() != null) {
                task.getTeleportTask().cancel(false);
            }

            // Message d'annulation
            if (task.getPlayer().isOnline()) {
                task.getPlayer().sendNotification(NotificationType.WARNING,
                        ColorUtil.stripColors(plugin.getConfigManager().getMessages().getPrefixed("teleport.cancelled")));
            }
        }
    }

    /**
     * Verifie si un joueur a une teleportation en cours.
     */
    public boolean hasPendingTeleport(@NotNull UUID uuid) {
        return pendingTeleports.containsKey(uuid);
    }

    /**
     * Arrete le service.
     */
    public void shutdown() {
        // Annuler toutes les teleportations en cours
        for (UUID uuid : pendingTeleports.keySet()) {
            cancelPendingTeleport(uuid);
        }
        scheduler.shutdown();
    }

    /**
     * Classe interne pour stocker les infos d'une teleportation en attente.
     */
    private static class TeleportTask {
        private final IslandiumPlayer player;
        private final ServerLocation startLocation;
        private final ServerLocation destination;
        private final Runnable onComplete;
        private ScheduledFuture<?> checkTask;
        private ScheduledFuture<?> teleportTask;

        public TeleportTask(
                IslandiumPlayer player,
                ServerLocation startLocation,
                ServerLocation destination,
                Runnable onComplete
        ) {
            this.player = player;
            this.startLocation = startLocation;
            this.destination = destination;
            this.onComplete = onComplete;
        }

        public IslandiumPlayer getPlayer() { return player; }
        public ServerLocation getStartLocation() { return startLocation; }
        public ServerLocation getDestination() { return destination; }
        public Runnable getOnComplete() { return onComplete; }
        public ScheduledFuture<?> getCheckTask() { return checkTask; }
        public void setCheckTask(ScheduledFuture<?> checkTask) { this.checkTask = checkTask; }
        public ScheduledFuture<?> getTeleportTask() { return teleportTask; }
        public void setTeleportTask(ScheduledFuture<?> teleportTask) { this.teleportTask = teleportTask; }
    }
}
