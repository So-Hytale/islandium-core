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
 * Service de téléportation avec warmup et annulation si mouvement.
 */
public class TeleportService {

    private final IslandiumPlugin plugin;
    private final ScheduledExecutorService scheduler;
    private final Map<UUID, TeleportTask> pendingTeleports = new ConcurrentHashMap<>();

    // Configuration
    private int warmupSeconds = 5;
    private double movementThreshold = 0.5; // Distance max avant annulation

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
     * Téléporte un joueur avec warmup.
     * Le warmup est ignoré si le joueur est en mode créatif.
     *
     * @param player      Le joueur à téléporter
     * @param destination La destination
     * @param onComplete  Callback quand la téléportation est terminée (succès ou échec)
     */
    public void teleportWithWarmup(
            @NotNull IslandiumPlayer player,
            @NotNull ServerLocation destination,
            @Nullable Runnable onComplete
    ) {
        UUID uuid = player.getUniqueId();

        // Annuler toute téléportation en cours
        cancelPendingTeleport(uuid);

        // Obtenir la position initiale
        ServerLocation startLocation = player.getLocation();
        if (startLocation == null) {
            plugin.log(Level.WARNING, "Cannot teleport player " + uuid + ": no location");
            if (onComplete != null) onComplete.run();
            return;
        }

        // Vérifier si le joueur est en mode créatif (bypass warmup)
        // Utiliser Player.getGameMode() car il est sur la classe Player
        Player hytalePlayer = player.getHytalePlayer();
        if (hytalePlayer != null) {
            try {
                GameMode gameMode = hytalePlayer.getGameMode();
                System.out.println("[ISLANDIUM-TP] Player " + player.getName() + " gamemode: " + gameMode);
                if (gameMode == GameMode.Creative) {
                    // Téléportation instantanée en créatif
                    System.out.println("[ISLANDIUM-TP] Creative mode detected, instant teleport for " + player.getName());
                    doTeleport(player, destination);
                    player.sendNotification(NotificationType.SUCCESS,
                            ColorUtil.stripColors(plugin.getConfigManager().getMessages().getPrefixed("teleport.success")));
                    if (onComplete != null) onComplete.run();
                    return;
                }
            } catch (Exception e) {
                System.out.println("[ISLANDIUM-TP] Could not check game mode: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            // Fallback: essayer de bypass le warmup via permission
            if (player.hasPermission("islandium.teleport.nowarmup")) {
                System.out.println("[ISLANDIUM-TP] Player " + player.getName() + " has nowarmup permission, instant teleport");
                doTeleport(player, destination);
                player.sendNotification(NotificationType.SUCCESS,
                        ColorUtil.stripColors(plugin.getConfigManager().getMessages().getPrefixed("teleport.success")));
                if (onComplete != null) onComplete.run();
                return;
            }
            System.out.println("[ISLANDIUM-TP] hytalePlayer is null for " + player.getName() + ", proceeding with warmup");
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

        // Créer la tâche de téléportation
        TeleportTask task = new TeleportTask(player, startLocation, destination, onComplete);

        // Programmer la vérification du mouvement toutes les 500ms
        ScheduledFuture<?> checkTask = scheduler.scheduleAtFixedRate(() -> {
            if (!checkMovement(task)) {
                cancelPendingTeleport(uuid);
            }
        }, 500, 500, TimeUnit.MILLISECONDS);

        task.setCheckTask(checkTask);

        // Programmer la téléportation après le warmup
        ScheduledFuture<?> teleportTask = scheduler.schedule(() -> {
            executeTeleport(task);
        }, warmupSeconds, TimeUnit.SECONDS);

        task.setTeleportTask(teleportTask);

        pendingTeleports.put(uuid, task);
    }

    /**
     * Téléporte un joueur instantanément (sans warmup).
     */
    public void teleportInstant(@NotNull IslandiumPlayer player, @NotNull ServerLocation destination) {
        UUID uuid = player.getUniqueId();
        cancelPendingTeleport(uuid);

        // Exécuter la téléportation directement
        doTeleport(player, destination);
    }

    /**
     * Vérifie si le joueur a bougé.
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
     * Exécute la téléportation.
     */
    private void executeTeleport(TeleportTask task) {
        UUID uuid = task.getPlayer().getUniqueId();
        pendingTeleports.remove(uuid);

        // Arrêter la vérification du mouvement
        if (task.getCheckTask() != null) {
            task.getCheckTask().cancel(false);
        }

        if (!task.getPlayer().isOnline()) {
            if (task.getOnComplete() != null) {
                task.getOnComplete().run();
            }
            return;
        }

        // Faire la téléportation
        doTeleport(task.getPlayer(), task.getDestination());

        // Message de succès
        task.getPlayer().sendNotification(NotificationType.SUCCESS,
                ColorUtil.stripColors(plugin.getConfigManager().getMessages().getPrefixed("teleport.success")));

        if (task.getOnComplete() != null) {
            task.getOnComplete().run();
        }
    }

    /**
     * Effectue la téléportation réelle via l'API Hytale.
     * Utilise le composant Teleport pour synchroniser correctement client/serveur.
     * Sauvegarde automatiquement la position précédente pour /back.
     * Gère le changement de monde si la destination est dans un monde différent.
     * Exécute sur le thread du monde pour garantir la thread-safety.
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
                System.out.println("[ISLANDIUM-TP] Cannot teleport player " + islandiumPlayer.getName() + ": Universe is null");
                return;
            }

            // Résoudre le monde de DESTINATION par son nom
            String destWorldName = destination.world();
            World destinationWorld = universe.getWorlds().get(destWorldName);
            if (destinationWorld == null) {
                System.out.println("[ISLANDIUM-TP] Cannot teleport player " + islandiumPlayer.getName() + ": destination world '" + destWorldName + "' not found");
                return;
            }

            // Résoudre le monde ACTUEL du joueur
            UUID currentWorldUuid = playerRef.getWorldUuid();
            World currentWorld = null;
            if (currentWorldUuid != null) {
                for (World w : universe.getWorlds().values()) {
                    try {
                        if (w.getWorldConfig().getUuid().equals(currentWorldUuid)) {
                            currentWorld = w;
                            break;
                        }
                    } catch (Exception ignored) {}
                }
            }

            if (currentWorld == null) {
                System.out.println("[ISLANDIUM-TP] Cannot teleport player " + islandiumPlayer.getName() + ": current world not found for UUID " + currentWorldUuid);
                return;
            }

            // Vérifier si on doit changer de monde
            boolean sameWorld = currentWorld == destinationWorld;
            System.out.println("[ISLANDIUM-TP] Player " + islandiumPlayer.getName() + " current world: " + currentWorld.getName() + ", destination world: " + destinationWorld.getName() + ", sameWorld: " + sameWorld);

            if (sameWorld) {
                // Même monde: téléportation simple aux coordonnées
                currentWorld.execute(() -> {
                    try {
                        applyTeleportPosition(playerRef, islandiumPlayer, destination);
                    } catch (Exception e) {
                        System.out.println("[ISLANDIUM-TP] Failed to teleport in world thread: " + e.getMessage());
                        e.printStackTrace();
                    }
                });
            } else {
                // Monde différent: retirer du monde actuel, ajouter au monde de destination, puis TP aux coordonnées
                System.out.println("[ISLANDIUM-TP] Cross-world teleport: " + currentWorld.getName() + " -> " + destinationWorld.getName());
                currentWorld.execute(() -> {
                    try {
                        playerRef.removeFromStore();
                        destinationWorld.addPlayer(playerRef, null, Boolean.TRUE, Boolean.FALSE)
                            .thenRun(() -> {
                                // Après ajout au nouveau monde, appliquer la position exacte
                                destinationWorld.execute(() -> {
                                    try {
                                        applyTeleportPosition(playerRef, islandiumPlayer, destination);
                                    } catch (Exception e) {
                                        System.out.println("[ISLANDIUM-TP] Failed to apply position after world change: " + e.getMessage());
                                        e.printStackTrace();
                                    }
                                });
                            })
                            .exceptionally(ex -> {
                                System.out.println("[ISLANDIUM-TP] Failed to add player to destination world: " + ex.getMessage());
                                ex.printStackTrace();
                                return null;
                            });
                    } catch (Exception e) {
                        System.out.println("[ISLANDIUM-TP] Failed cross-world teleport: " + e.getMessage());
                        e.printStackTrace();
                    }
                });
            }
        } catch (Exception e) {
            System.out.println("[ISLANDIUM-TP] Failed to teleport player: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Applique la position de téléportation au joueur via le composant Teleport.
     * Doit être appelé sur le thread du monde.
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
                System.out.println("[ISLANDIUM-TP] SUCCESS: Teleported " + islandiumPlayer.getName() + " to " + destination);
                return;
            }
        }
        System.out.println("[ISLANDIUM-TP] Cannot teleport player " + islandiumPlayer.getName() + ": invalid ref or transform");
    }

    /**
     * Obtient le PlayerRef à partir d'un IslandiumPlayer.
     */
    @Nullable
    private PlayerRef getPlayerRef(@NotNull IslandiumPlayer player) {
        if (player instanceof IslandiumPlayerImpl impl) {
            Object raw = impl.getHytalePlayerRaw();
            if (raw instanceof PlayerRef ref) {
                return ref;
            } else if (raw instanceof Player p) {
                return p.getPlayerRef();
            }
        }
        // Fallback: essayer getHytalePlayer()
        Player hytalePlayer = player.getHytalePlayer();
        if (hytalePlayer != null) {
            return hytalePlayer.getPlayerRef();
        }
        return null;
    }

    /**
     * Annule une téléportation en cours.
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
     * Vérifie si un joueur a une téléportation en cours.
     */
    public boolean hasPendingTeleport(@NotNull UUID uuid) {
        return pendingTeleports.containsKey(uuid);
    }

    /**
     * Arrête le service.
     */
    public void shutdown() {
        // Annuler toutes les téléportations en cours
        for (UUID uuid : pendingTeleports.keySet()) {
            cancelPendingTeleport(uuid);
        }
        scheduler.shutdown();
    }

    /**
     * Classe interne pour stocker les infos d'une téléportation en attente.
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
