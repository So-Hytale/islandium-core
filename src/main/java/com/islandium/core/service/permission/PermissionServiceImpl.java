package com.islandium.core.service.permission;

import com.islandium.core.api.permission.PermissionService;
import com.islandium.core.api.permission.PlayerPermissions;
import com.islandium.core.api.permission.Rank;
import com.islandium.core.IslandiumPlugin;
import com.islandium.core.database.SQLExecutor;
import com.islandium.core.database.repository.PlayerPermissionRepository;
import com.islandium.core.database.repository.PlayerRankRepository;
import com.islandium.core.database.repository.RankPermissionRepository;
import com.islandium.core.database.repository.RankRepository;
import com.hypixel.hytale.server.core.permissions.PermissionsModule;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.stream.Collectors;

/**
 * Implementation du service de permissions.
 * Stocke les permissions en BDD pour la synchronisation multi-serveur
 * et synchronise avec le système natif Hytale (PermissionsModule).
 */
public class PermissionServiceImpl implements PermissionService {

    private final IslandiumPlugin plugin;
    private final RankRepository rankRepository;
    private final RankPermissionRepository rankPermRepository;
    private final PlayerRankRepository playerRankRepository;
    private final PlayerPermissionRepository playerPermRepository;
    private final PermissionCache cache;
    private final IslandiumPermissionProvider nativeProvider;

    public PermissionServiceImpl(@NotNull IslandiumPlugin plugin, @NotNull SQLExecutor sql) {
        this.plugin = plugin;
        this.rankRepository = new RankRepository(sql);
        this.rankPermRepository = new RankPermissionRepository(sql);
        this.playerRankRepository = new PlayerRankRepository(sql);
        this.playerPermRepository = new PlayerPermissionRepository(sql);
        this.cache = new PermissionCache();
        this.nativeProvider = new IslandiumPermissionProvider();
    }

    /**
     * Initialise le service en chargeant tous les ranks.
     * Cree automatiquement le rank "player" par defaut s'il n'existe pas.
     * Enregistre le provider aupres du systeme natif Hytale.
     */
    public CompletableFuture<Void> initialize() {
        plugin.log(Level.INFO, "Loading permission system...");

        // Enregistrer notre provider aupres du systeme natif Hytale
        try {
            PermissionsModule.get().addProvider(nativeProvider);
            plugin.log(Level.INFO, "Registered IslandiumPermissionProvider with native Hytale system");
        } catch (Exception e) {
            plugin.log(Level.WARNING, "Failed to register permission provider: " + e.getMessage());
        }

        return rankRepository.findAll()
                .thenCompose(ranks -> {
                    // Charger les permissions pour chaque rank
                    List<CompletableFuture<Void>> futures = ranks.stream()
                            .map(rank -> rankPermRepository.findByRankId(rank.getId())
                                    .thenAccept(rank::setDirectPermissions))
                            .collect(Collectors.toList());

                    return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                            .thenCompose(v -> {
                                cache.loadRanks(ranks);
                                plugin.log(Level.INFO, "Loaded " + ranks.size() + " ranks");

                                // Synchroniser les permissions des groupes vers le systeme natif
                                syncAllGroupPermissionsToNative();

                                // Verifier si le rank "player" existe, sinon le creer
                                return ensureDefaultRankExists();
                            });
                });
    }

    /**
     * Synchronise les permissions de tous les groupes (ranks) vers le systeme natif.
     */
    private void syncAllGroupPermissionsToNative() {
        for (RankImpl rank : cache.getAllRanks()) {
            Set<String> allPerms = rank.getAllPermissions();
            nativeProvider.setGroupPermissions(rank.getName(), allPerms);
            plugin.log(Level.FINE, "Synced group " + rank.getName() + " with " + allPerms.size() + " permissions to native system");
        }
    }

    /**
     * S'assure qu'un rank par defaut "player" existe.
     * Si aucun rank n'existe ou si aucun rank par defaut n'est defini,
     * cree le rank "player" et le definit comme defaut.
     */
    private CompletableFuture<Void> ensureDefaultRankExists() {
        RankImpl playerRank = cache.getRankByName("player");

        if (playerRank == null) {
            // Le rank "player" n'existe pas, on le cree
            plugin.log(Level.INFO, "Creating default 'player' rank...");
            return createRank("player", "Player", null, "#AAAAAA", 0)
                    .thenCompose(rank -> {
                        plugin.log(Level.INFO, "Default 'player' rank created");
                        // Verifier s'il y a un rank par defaut
                        if (cache.getDefaultRank() == null) {
                            return setDefaultRank("player");
                        }
                        return CompletableFuture.completedFuture(null);
                    });
        } else {
            // Le rank "player" existe, verifier s'il y a un rank par defaut
            if (cache.getDefaultRank() == null) {
                plugin.log(Level.INFO, "No default rank set, setting 'player' as default");
                return setDefaultRank("player");
            }
        }

        return CompletableFuture.completedFuture(null);
    }

    // ========================================
    // Gestion des Ranks
    // ========================================

    @Override
    public CompletableFuture<Optional<Rank>> getRank(@NotNull String name) {
        RankImpl cached = cache.getRankByName(name);
        if (cached != null) {
            return CompletableFuture.completedFuture(Optional.of(cached));
        }
        return rankRepository.findByName(name).thenApply(opt -> opt.map(r -> r));
    }

    @Override
    public CompletableFuture<Optional<Rank>> getRankById(int id) {
        RankImpl cached = cache.getRankById(id);
        if (cached != null) {
            return CompletableFuture.completedFuture(Optional.of(cached));
        }
        return rankRepository.findById(id).thenApply(opt -> opt.map(r -> r));
    }

    @Override
    public CompletableFuture<List<Rank>> getAllRanks() {
        List<Rank> ranks = new ArrayList<>(cache.getAllRanks());
        return CompletableFuture.completedFuture(ranks);
    }

    @Override
    public CompletableFuture<Optional<Rank>> getDefaultRank() {
        RankImpl defaultRank = cache.getDefaultRank();
        if (defaultRank != null) {
            return CompletableFuture.completedFuture(Optional.of(defaultRank));
        }
        return rankRepository.findDefault().thenApply(opt -> opt.map(r -> r));
    }

    @Override
    public CompletableFuture<Rank> createRank(
            @NotNull String name,
            @NotNull String displayName,
            @Nullable String prefix,
            @Nullable String color,
            int priority
    ) {
        RankImpl rank = new RankImpl(name, displayName, prefix, color, priority);
        return rankRepository.save(rank)
                .thenApply(saved -> {
                    cache.putRank(saved);
                    plugin.log(Level.INFO, "Created rank: " + name);
                    return saved;
                });
    }

    @Override
    public CompletableFuture<Boolean> deleteRank(@NotNull String name) {
        return rankRepository.deleteByName(name)
                .thenApply(deleted -> {
                    if (deleted) {
                        cache.removeRank(name);
                        cache.invalidateRank(name);
                        plugin.log(Level.INFO, "Deleted rank: " + name);
                    }
                    return deleted;
                });
    }

    @Override
    public CompletableFuture<Rank> updateRank(@NotNull Rank rank) {
        if (!(rank instanceof RankImpl impl)) {
            throw new IllegalArgumentException("Rank must be instance of RankImpl");
        }
        return rankRepository.save(impl)
                .thenApply(saved -> {
                    cache.putRank(saved);
                    cache.invalidateRank(saved.getName());
                    return saved;
                });
    }

    @Override
    public CompletableFuture<Void> setRankParent(@NotNull String rankName, @Nullable String parentName) {
        return getRank(rankName).thenCompose(rankOpt -> {
            if (rankOpt.isEmpty()) {
                return CompletableFuture.completedFuture(null);
            }

            if (parentName == null) {
                return rankRepository.setParent(rankName, null)
                        .thenRun(() -> {
                            RankImpl rank = cache.getRankByName(rankName);
                            if (rank != null) {
                                rank.setParent(null);
                                rank.setParentId(null);
                            }
                            cache.invalidateRank(rankName);
                        });
            }

            return getRank(parentName).thenCompose(parentOpt -> {
                if (parentOpt.isEmpty()) {
                    return CompletableFuture.completedFuture(null);
                }

                RankImpl parent = (RankImpl) parentOpt.get();
                return rankRepository.setParent(rankName, parent.getId())
                        .thenRun(() -> {
                            RankImpl rank = cache.getRankByName(rankName);
                            if (rank != null) {
                                rank.setParent(parent);
                                rank.setParentId(parent.getId());
                            }
                            cache.invalidateRank(rankName);
                        });
            });
        });
    }

    @Override
    public CompletableFuture<Void> setDefaultRank(@NotNull String rankName) {
        return rankRepository.setDefault(rankName)
                .thenRun(() -> {
                    // Mettre a jour le cache
                    for (RankImpl rank : cache.getAllRanks()) {
                        rank.setDefault(rank.getName().equalsIgnoreCase(rankName));
                    }
                    plugin.log(Level.INFO, "Set default rank: " + rankName);
                });
    }

    // ========================================
    // Permissions de Rank
    // ========================================

    @Override
    public CompletableFuture<Void> addRankPermission(@NotNull String rankName, @NotNull String permission) {
        RankImpl rank = cache.getRankByName(rankName);
        if (rank == null) {
            return CompletableFuture.completedFuture(null);
        }

        return rankPermRepository.add(rank.getId(), permission)
                .thenRun(() -> {
                    rank.addDirectPermission(permission);
                    cache.invalidateRank(rankName);

                    // Synchroniser vers le systeme natif
                    nativeProvider.addGroupPermissions(rankName, Set.of(permission));
                    try {
                        PermissionsModule.get().addGroupPermission(rankName, Set.of(permission));
                    } catch (Exception e) {
                        plugin.log(Level.WARNING, "Failed to sync rank permission to native: " + e.getMessage());
                    }
                });
    }

    @Override
    public CompletableFuture<Void> removeRankPermission(@NotNull String rankName, @NotNull String permission) {
        RankImpl rank = cache.getRankByName(rankName);
        if (rank == null) {
            return CompletableFuture.completedFuture(null);
        }

        return rankPermRepository.remove(rank.getId(), permission)
                .thenRun(() -> {
                    rank.removeDirectPermission(permission);
                    cache.invalidateRank(rankName);

                    // Synchroniser vers le systeme natif
                    nativeProvider.removeGroupPermissions(rankName, Set.of(permission));
                    try {
                        PermissionsModule.get().removeGroupPermission(rankName, Set.of(permission));
                    } catch (Exception e) {
                        plugin.log(Level.WARNING, "Failed to sync rank permission removal to native: " + e.getMessage());
                    }
                });
    }

    @Override
    public CompletableFuture<Set<String>> getRankPermissions(@NotNull String rankName) {
        RankImpl rank = cache.getRankByName(rankName);
        if (rank != null) {
            return CompletableFuture.completedFuture(rank.getDirectPermissions());
        }
        return CompletableFuture.completedFuture(Set.of());
    }

    // ========================================
    // Gestion des Joueurs
    // ========================================

    @Override
    public CompletableFuture<PlayerPermissions> getPlayerPermissions(@NotNull UUID playerUuid) {
        // Verifier le cache
        PlayerPermissionsImpl cached = cache.getPlayerCache(playerUuid);
        if (cached != null) {
            return CompletableFuture.completedFuture(cached);
        }

        // Charger depuis la DB
        return playerRankRepository.findRankIdsByPlayer(playerUuid)
                .thenCompose(rankIds -> {
                    Set<Rank> ranks = cache.getRanksByIds(rankIds);

                    // Si aucun rank, assigner le rank par defaut
                    if (ranks.isEmpty()) {
                        RankImpl defaultRank = cache.getDefaultRank();
                        if (defaultRank != null) {
                            ranks.add(defaultRank);
                        }
                    }

                    return playerPermRepository.findByPlayer(playerUuid)
                            .thenApply(personalPerms -> {
                                PlayerPermissionsImpl perms = new PlayerPermissionsImpl(
                                        playerUuid,
                                        ranks,
                                        personalPerms
                                );
                                cache.cachePlayer(playerUuid, perms);
                                return perms;
                            });
                });
    }

    @Override
    public CompletableFuture<Void> addPlayerRank(
            @NotNull UUID playerUuid,
            @NotNull String rankName,
            @Nullable Long expiresAt,
            @Nullable UUID assignedBy
    ) {
        RankImpl rank = cache.getRankByName(rankName);
        if (rank == null) {
            return CompletableFuture.completedFuture(null);
        }

        return playerRankRepository.add(playerUuid, rank.getId(), expiresAt, assignedBy)
                .thenRun(() -> {
                    cache.invalidatePlayer(playerUuid);

                    // Synchroniser vers le systeme natif
                    nativeProvider.addUserToGroup(playerUuid, rankName);
                    try {
                        PermissionsModule.get().addUserToGroup(playerUuid, rankName);
                    } catch (Exception e) {
                        plugin.log(Level.WARNING, "Failed to sync player rank to native: " + e.getMessage());
                    }

                    plugin.log(Level.INFO, "Added rank " + rankName + " to player " + playerUuid);
                });
    }

    @Override
    public CompletableFuture<Void> removePlayerRank(@NotNull UUID playerUuid, @NotNull String rankName) {
        RankImpl rank = cache.getRankByName(rankName);
        if (rank == null) {
            return CompletableFuture.completedFuture(null);
        }

        return playerRankRepository.remove(playerUuid, rank.getId())
                .thenRun(() -> {
                    cache.invalidatePlayer(playerUuid);

                    // Synchroniser vers le systeme natif
                    nativeProvider.removeUserFromGroup(playerUuid, rankName);
                    try {
                        PermissionsModule.get().removeUserFromGroup(playerUuid, rankName);
                    } catch (Exception e) {
                        plugin.log(Level.WARNING, "Failed to sync player rank removal to native: " + e.getMessage());
                    }

                    plugin.log(Level.INFO, "Removed rank " + rankName + " from player " + playerUuid);
                });
    }

    @Override
    public CompletableFuture<Set<Rank>> getPlayerRanks(@NotNull UUID playerUuid) {
        return getPlayerPermissions(playerUuid)
                .thenApply(PlayerPermissions::getRanks);
    }

    @Override
    public CompletableFuture<Set<UUID>> getPlayersWithRank(@NotNull String rankName) {
        RankImpl rank = cache.getRankByName(rankName);
        if (rank == null) {
            return CompletableFuture.completedFuture(Set.of());
        }
        return playerRankRepository.findPlayersByRankId(rank.getId());
    }

    // ========================================
    // Permissions Personnelles
    // ========================================

    @Override
    public CompletableFuture<Void> addPlayerPermission(
            @NotNull UUID playerUuid,
            @NotNull String permission,
            @Nullable Long expiresAt
    ) {
        return playerPermRepository.add(playerUuid, permission, expiresAt)
                .thenRun(() -> {
                    cache.invalidatePlayer(playerUuid);

                    // Synchroniser vers le systeme natif
                    nativeProvider.addUserPermissions(playerUuid, Set.of(permission));
                    try {
                        PermissionsModule.get().addUserPermission(playerUuid, Set.of(permission));
                    } catch (Exception e) {
                        plugin.log(Level.WARNING, "Failed to sync player permission to native: " + e.getMessage());
                    }

                    plugin.log(Level.INFO, "Added permission " + permission + " to player " + playerUuid);
                });
    }

    @Override
    public CompletableFuture<Void> removePlayerPermission(@NotNull UUID playerUuid, @NotNull String permission) {
        return playerPermRepository.remove(playerUuid, permission)
                .thenRun(() -> {
                    cache.invalidatePlayer(playerUuid);

                    // Synchroniser vers le systeme natif
                    nativeProvider.removeUserPermissions(playerUuid, Set.of(permission));
                    try {
                        PermissionsModule.get().removeUserPermission(playerUuid, Set.of(permission));
                    } catch (Exception e) {
                        plugin.log(Level.WARNING, "Failed to sync player permission removal to native: " + e.getMessage());
                    }

                    plugin.log(Level.INFO, "Removed permission " + permission + " from player " + playerUuid);
                });
    }

    @Override
    public CompletableFuture<Set<String>> getPlayerPersonalPermissions(@NotNull UUID playerUuid) {
        return playerPermRepository.findByPlayer(playerUuid);
    }

    // ========================================
    // Verification
    // ========================================

    @Override
    public CompletableFuture<Boolean> hasPermission(@NotNull UUID playerUuid, @NotNull String permission) {
        return getPlayerPermissions(playerUuid)
                .thenApply(perms -> perms.hasPermission(permission));
    }

    // ========================================
    // Cache
    // ========================================

    @Override
    public void invalidatePlayerCache(@NotNull UUID playerUuid) {
        cache.invalidatePlayer(playerUuid);
    }

    @Override
    public void invalidateRankCache(@NotNull String rankName) {
        cache.invalidateRank(rankName);
    }

    @Override
    public CompletableFuture<Void> reloadAll() {
        cache.clear();
        return initialize();
    }

    /**
     * @return le cache interne (pour les tests)
     */
    public PermissionCache getCache() {
        return cache;
    }

    // ========================================
    // Synchronisation avec le système natif Hytale
    // ========================================

    /**
     * Synchronise les permissions d'un joueur avec le système natif Hytale.
     * Appelé quand un joueur se connecte pour charger ses permissions depuis la BDD.
     *
     * Cette methode:
     * 1. Charge les permissions du joueur depuis la BDD
     * 2. Les injecte dans notre provider natif
     * 3. Ajoute le joueur aux groupes correspondant a ses ranks
     *
     * @param playerUuid UUID du joueur
     * @return CompletableFuture qui se complete quand la sync est terminée
     */
    public CompletableFuture<Void> syncWithNativeSystem(@NotNull UUID playerUuid) {
        return getPlayerPermissions(playerUuid)
                .thenAccept(perms -> {
                    // 1. Synchroniser les permissions personnelles vers le systeme natif
                    Set<String> personalPerms = perms.getPersonalPermissions();
                    nativeProvider.setUserPermissions(playerUuid, personalPerms);

                    // 2. Synchroniser les groupes (ranks) vers le systeme natif
                    Set<String> groupNames = new HashSet<>();
                    for (Rank rank : perms.getRanks()) {
                        groupNames.add(rank.getName());
                    }
                    nativeProvider.setUserGroups(playerUuid, groupNames);

                    // 3. Appliquer aussi via l'API native directement pour etre sur
                    try {
                        PermissionsModule nativeModule = PermissionsModule.get();

                        // Ajouter les permissions personnelles
                        if (!personalPerms.isEmpty()) {
                            nativeModule.addUserPermission(playerUuid, personalPerms);
                        }

                        // Ajouter aux groupes
                        for (String groupName : groupNames) {
                            nativeModule.addUserToGroup(playerUuid, groupName);
                        }
                    } catch (Exception e) {
                        plugin.log(Level.WARNING, "Failed to sync permissions to native system for " + playerUuid + ": " + e.getMessage());
                    }

                    plugin.log(Level.INFO, "Synced permissions for player " + playerUuid +
                            " (" + personalPerms.size() + " personal perms, " + groupNames.size() + " groups)");
                });
    }

    /**
     * Nettoie le cache des permissions d'un joueur.
     * Appelé quand un joueur se déconnecte.
     *
     * @param playerUuid UUID du joueur
     */
    public void unsyncFromNativeSystem(@NotNull UUID playerUuid) {
        // Nettoyer le cache local
        cache.invalidatePlayer(playerUuid);

        // Nettoyer le provider natif
        nativeProvider.clearUserPermissions(playerUuid);

        plugin.log(Level.FINE, "Cleared permission cache for player " + playerUuid);
    }

    /**
     * @return le provider natif (pour les tests ou acces direct)
     */
    public IslandiumPermissionProvider getNativeProvider() {
        return nativeProvider;
    }
}
