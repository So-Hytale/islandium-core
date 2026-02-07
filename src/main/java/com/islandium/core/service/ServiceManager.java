package com.islandium.core.service;

import com.islandium.core.api.economy.EconomyService;
import com.islandium.core.api.messaging.CrossServerMessenger;
import com.islandium.core.api.moderation.ModerationService;
import com.islandium.core.api.permission.PermissionService;
import com.islandium.core.IslandiumPlugin;
import com.islandium.core.database.repository.PlayerRepository;
import com.islandium.core.service.economy.EconomyServiceImpl;
import com.islandium.core.service.messaging.CrossServerMessengerImpl;
import com.islandium.core.service.moderation.ModerationServiceImpl;
import com.islandium.core.service.permission.PermissionServiceImpl;
import com.islandium.core.service.back.BackService;
import com.islandium.core.service.kit.KitService;
import com.islandium.core.service.spawn.SpawnService;
import com.islandium.core.service.teleport.TeleportService;
import com.islandium.core.database.repository.KitRepository;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

/**
 * Gestionnaire des services.
 */
public class ServiceManager {

    private final IslandiumPlugin plugin;

    // Repositories
    private PlayerRepository playerRepository;

    // Services
    private EconomyServiceImpl economyService;
    private ModerationServiceImpl moderationService;
    private CrossServerMessengerImpl messenger;
    private PermissionServiceImpl permissionService;
    private SpawnService spawnService;
    private TeleportService teleportService;
    private BackService backService;
    private KitService kitService;

    public ServiceManager(@NotNull IslandiumPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Initialise tous les services.
     * @return CompletableFuture pour l'initialisation async
     */
    public CompletableFuture<Void> initialize() {
        var sql = plugin.getDatabaseManager().getExecutor();

        // Initialize repositories
        this.playerRepository = new PlayerRepository(sql);

        // Initialize services
        this.economyService = new EconomyServiceImpl(plugin, playerRepository);
        this.moderationService = new ModerationServiceImpl(plugin);
        this.messenger = new CrossServerMessengerImpl(plugin);
        this.permissionService = new PermissionServiceImpl(plugin, sql);
        this.spawnService = new SpawnService(plugin);
        this.teleportService = new TeleportService(plugin);
        this.backService = new BackService(plugin);

        // Kit service
        var kitRepository = new KitRepository(sql);
        this.kitService = new KitService(plugin, kitRepository);

        // Load spawn data
        spawnService.load();

        // Configure teleport warmup from config
        teleportService.setWarmupSeconds(plugin.getConfigManager().getMainConfig().getTeleportWarmup());

        // Initialize permission service (loads ranks from DB)
        return permissionService.initialize()
            .thenCompose(v -> kitService.loadKits());
    }

    /**
     * Arrête tous les services.
     */
    public void shutdown() {
        if (teleportService != null) {
            teleportService.shutdown();
        }
    }

    // === Repositories ===

    @NotNull
    public PlayerRepository getPlayerRepository() {
        return playerRepository;
    }

    // === Services ===

    @NotNull
    public EconomyService getEconomyService() {
        return economyService;
    }

    @NotNull
    public ModerationService getModerationService() {
        return moderationService;
    }

    @NotNull
    public CrossServerMessenger getMessenger() {
        return messenger;
    }

    /**
     * Alias for getMessenger() for backwards compatibility.
     */
    @NotNull
    public CrossServerMessenger getCrossServerMessenger() {
        return messenger;
    }

    @NotNull
    public PermissionService getPermissionService() {
        return permissionService;
    }

    /**
     * Retourne l'implémentation du service de permissions.
     * Utilisé pour accéder aux méthodes spécifiques comme syncWithNativeSystem().
     */
    @NotNull
    public PermissionServiceImpl getPermissionServiceImpl() {
        return permissionService;
    }

    @NotNull
    public SpawnService getSpawnService() {
        return spawnService;
    }

    @NotNull
    public TeleportService getTeleportService() {
        return teleportService;
    }

    @NotNull
    public BackService getBackService() {
        return backService;
    }

    @NotNull
    public KitService getKitService() {
        return kitService;
    }
}
