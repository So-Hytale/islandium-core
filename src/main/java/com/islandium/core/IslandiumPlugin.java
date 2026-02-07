package com.islandium.core;

import com.hypixel.hytale.server.core.universe.world.World;
import com.islandium.core.api.IslandiumAPI;
import com.islandium.core.command.CommandManager;
import com.islandium.core.config.ConfigManager;
import com.islandium.core.database.DatabaseManager;
import com.islandium.core.listener.ListenerManager;
import com.islandium.core.player.PlayerManager;
import com.islandium.core.redis.RedisManager;
import com.islandium.core.service.ServiceManager;
import com.islandium.core.service.back.BackService;
import com.islandium.core.service.spawn.SpawnService;
import com.islandium.core.service.teleport.TeleportService;
import com.islandium.core.ui.IslandiumUIRegistry;
import com.islandium.core.ui.pages.permission.PlayerPermissionsManagerPage;
import com.islandium.core.ui.pages.permission.RankManagerPage;
import com.islandium.core.ui.pages.world.WorldManagerPage;
import com.islandium.core.ui.pages.plugin.PluginManagerPage;
import com.islandium.core.ui.pages.wiki.WikiMainPage;
import com.islandium.core.wiki.WikiManager;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

/**
 * Plugin principal Essentials pour Hytale.
 */
public class IslandiumPlugin extends JavaPlugin {

    private static volatile IslandiumPlugin instance;
    private static volatile boolean initialized = false;

    private ConfigManager configManager;
    private DatabaseManager databaseManager;
    private RedisManager redisManager;
    private PlayerManager playerManager;
    private ServiceManager serviceManager;
    private CommandManager commandManager;
    private ListenerManager listenerManager;

    public IslandiumPlugin(JavaPluginInit init) {
        super(init);
    }

    @Override
    protected void setup() {
        instance = this;
        log(Level.INFO, "Initializing Essentials...");
        try {
            // 1. Configuration
            log(Level.INFO, "Loading configuration...");
            this.configManager = new ConfigManager(this);
            configManager.load();

            // 2. Database
            log(Level.INFO, "Connecting to database...");
            this.databaseManager = new DatabaseManager(this, configManager.getMainConfig());
            databaseManager.connect().join();
            databaseManager.runMigrations().join();

            // 3. Redis
            log(Level.INFO, "Connecting to Redis...");
            this.redisManager = new RedisManager(this, configManager.getMainConfig());
            redisManager.connect().join();

            // 4. Services
            log(Level.INFO, "Initializing services...");
            this.serviceManager = new ServiceManager(this);
            serviceManager.initialize();

            // 5. Player Manager
            log(Level.INFO, "Initializing player manager...");
            this.playerManager = new PlayerManager(this);

            // 6. Initialize API
            log(Level.INFO, "Registering API...");
            initializeAPI();

            // 7. Wiki Manager
            log(Level.INFO, "Initializing Wiki...");
            WikiManager.init(this);

            // 8. UI Registry
            log(Level.INFO, "Registering UI pages...");
            registerUIPages();

            // 9. Commands
            log(Level.INFO, "Registering commands...");
            this.commandManager = new CommandManager(this);
            commandManager.registerAll();

            // 10. Listeners
            log(Level.INFO, "Registering listeners...");
            this.listenerManager = new ListenerManager(this);
            listenerManager.registerAll();

            initialized = true;
            log(Level.INFO, "Islandium initialized successfully!");

        } catch (Exception e) {
            log(Level.SEVERE, "Failed to initialize Essentials: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // TODO: Trouver comment hook le shutdown du serveur
    public void teardown() {
        log(Level.INFO, "Shutting down Essentials...");

        try {
            // Shutdown API
            IslandiumAPI.shutdown();

            // Save all player data
            if (playerManager != null) {
                playerManager.saveAll().join();
            }

            // Close Redis
            if (redisManager != null) {
                redisManager.disconnect();
            }

            // Shutdown services
            if (serviceManager != null) {
                serviceManager.shutdown();
            }

            // Close Database
            if (databaseManager != null) {
                databaseManager.disconnect();
            }

            log(Level.INFO, "Islandium shut down successfully!");

        } catch (Exception e) {
            log(Level.SEVERE, "Error during shutdown: " + e.getMessage());
            e.printStackTrace();
        }

        initialized = false;
        instance = null;
    }

    private void registerUIPages() {
        IslandiumUIRegistry.getInstance()
                .register(new IslandiumUIRegistry.Entry(
                        "ranks",
                        "RANKS",
                        "Gestion des rangs et permissions",
                        "#ffd700",
                        playerRef -> new RankManagerPage(playerRef, serviceManager.getPermissionService(), this),
                        true,
                        "ranks"
                ))
                .register(new IslandiumUIRegistry.Entry(
                        "permissions",
                        "PERMISSIONS",
                        "Gestion des permissions joueurs",
                        "#ff6b6b",
                        playerRef -> new PlayerPermissionsManagerPage(playerRef, serviceManager.getPermissionService(), this),
                        true,
                        "perms", "perm"
                ))
                .register(new IslandiumUIRegistry.Entry(
                        "worlds",
                        "MONDES",
                        "Gestion des mondes",
                        "#4ecdc4",
                        playerRef -> new WorldManagerPage(playerRef, this),
                        true,
                        "worlds", "world"
                ))
                .register(new IslandiumUIRegistry.Entry(
                        "plugins",
                        "PLUGINS",
                        "Gestion des plugins",
                        "#a8e6cf",
                        playerRef -> new PluginManagerPage(playerRef, this),
                        true,
                        "plugins", "pl"
                ))
                .register(new IslandiumUIRegistry.Entry(
                        "wiki",
                        "WIKI",
                        "Documentation et informations",
                        "#82b1ff",
                        playerRef -> new WikiMainPage(playerRef, this),
                        true,
                        "wiki", "drops", "entities"
                ));
    }

    private void initializeAPI() {
        IslandiumAPI api = IslandiumAPI.builder()
                .playerProvider(playerManager)
                .economyService(serviceManager.getEconomyService())
                .moderationService(serviceManager.getModerationService())
                .messenger(serviceManager.getMessenger())
                .build();

        IslandiumAPI.init(api);
    }

    // === Getters ===

    @NotNull
    public static IslandiumPlugin get() {
        if (instance == null || !initialized) {
            throw new IllegalStateException("IslandiumPlugin is not yet initialized!");
        }
        return instance;
    }

    /**
     * Vérifie si le plugin est complètement initialisé.
     */
    public static boolean isInitialized() {
        return instance != null && initialized;
    }

    @NotNull
    public File getDataFolder() {
        File folder = new File("mods/essentials");
        if (!folder.exists()) {
            folder.mkdirs();
        }
        return folder;
    }

    @NotNull
    public ConfigManager getConfigManager() {
        return configManager;
    }

    @NotNull
    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    @NotNull
    public RedisManager getRedisManager() {
        return redisManager;
    }

    @NotNull
    public PlayerManager getPlayerManager() {
        return playerManager;
    }

    @NotNull
    public ServiceManager getServiceManager() {
        return serviceManager;
    }

    @NotNull
    public CommandManager getCommandManager() {
        return commandManager;
    }

    @NotNull
    public String getServerName() {
        return configManager.getMainConfig().getServerName();
    }

    @NotNull
    public com.islandium.core.config.MessagesConfig getMessages() {
        return configManager.getMessages();
    }

    @NotNull
    public SpawnService getSpawnService() {
        return serviceManager.getSpawnService();
    }

    @NotNull
    public TeleportService getTeleportService() {
        return serviceManager.getTeleportService();
    }

    @NotNull
    public BackService getBackService() {
        return serviceManager.getBackService();
    }

    // === Logging ===

    private static final java.util.logging.Logger LOGGER = java.util.logging.Logger.getLogger("Essentials");

    public void log(Level level, String message) {
        LOGGER.log(level, "[Essentials] " + message);
    }

    public void log(Level level, String message, Object... args) {
        LOGGER.log(level, "[Essentials] " + String.format(message.replace("{}", "%s"), args));
    }

    // === Async ===

    public CompletableFuture<Void> runAsync(Runnable runnable) {
        return CompletableFuture.runAsync(runnable);
    }

    public <T> CompletableFuture<T> supplyAsync(java.util.function.Supplier<T> supplier) {
        return CompletableFuture.supplyAsync(supplier);
    }
}
