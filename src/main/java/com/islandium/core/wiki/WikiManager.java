package com.islandium.core.wiki;

import com.hypixel.hytale.builtin.buildertools.utils.Material;
import com.hypixel.hytale.protocol.BlockMaterial;
import com.islandium.core.IslandiumPlugin;
import com.islandium.core.wiki.model.WikiCategory;
import com.islandium.core.wiki.model.WikiDrop;
import com.islandium.core.wiki.model.WikiEntity;
import com.islandium.core.wiki.search.WikiSearchEngine;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;

/**
 * Main manager for the Wiki system.
 * Singleton that provides access to wiki data and search functionality.
 * Uses lazy loading - data is only loaded when first accessed.
 */
public class WikiManager {

    private static WikiManager instance;

    private final IslandiumPlugin plugin;
    private final WikiDataLoader dataLoader;
    private final WikiSearchEngine searchEngine;
    private volatile boolean loaded = false;

    private WikiManager(IslandiumPlugin plugin) {
        this.plugin = plugin;
        this.dataLoader = new WikiDataLoader(plugin);
        this.searchEngine = new WikiSearchEngine();
    }

    /**
     * Initialize the WikiManager singleton.
     * Note: Data is loaded lazily when first accessed, not at init time.
     */
    public static void init(IslandiumPlugin plugin) {
        if (instance != null) {
            plugin.getLogger().at(Level.WARNING).log("WikiManager already initialized!");
            return;
        }
        instance = new WikiManager(plugin);
        // Don't load here - use lazy loading instead
        plugin.getLogger().at(Level.INFO).log("WikiManager initialized (lazy loading enabled)");
    }

    /**
     * Get the WikiManager instance.
     */
    public static WikiManager get() {
        if (instance == null) {
            throw new IllegalStateException("WikiManager not initialized! Call init() first.");
        }
        return instance;
    }

    /**
     * Check if WikiManager is available.
     */
    public static boolean isAvailable() {
        return instance != null;
    }

    /**
     * Ensure data is loaded. Called automatically before any data access.
     * This implements lazy loading - data is loaded on first access.
     */
    private synchronized void ensureLoaded() {
        if (!loaded) {
            plugin.getLogger().at(Level.INFO).log("WikiManager: Loading data on first access...");
            List<WikiEntity> entities = dataLoader.loadAll();
            searchEngine.indexEntities(entities);
            loaded = true;
            plugin.getLogger().at(Level.INFO).log("Wiki loaded with " + entities.size() + " entities");
        }
    }

    /**
     * Load or reload all wiki data.
     */
    public void load() {
        loaded = false;
        ensureLoaded();
    }

    /**
     * Reload wiki data.
     */
    public void reload() {
        loaded = false;
        ensureLoaded();
    }

    /**
     * Force reload wiki data (useful when assets change).
     */
    public void forceReload() {
        plugin.getLogger().at(Level.INFO).log("WikiManager: Force reloading data...");
        loaded = false;
        ensureLoaded();
    }

    // ========== Entity Access ==========

    /**
     * Get all entities.
     */
    public List<WikiEntity> getAllEntities() {
        ensureLoaded();
        return searchEngine.getAllEntities();
    }

    /**
     * Get entity by ID.
     */
    public Optional<WikiEntity> getEntity(String id) {
        ensureLoaded();
        return searchEngine.getEntityById(id);
    }

    /**
     * Get entities by category.
     */
    public List<WikiEntity> getEntitiesByCategory(WikiCategory category) {
        ensureLoaded();
        return searchEngine.filterByCategory(category);
    }

    // ========== Search ==========

    /**
     * Search entities by name/description.
     */
    public List<WikiEntity> searchEntities(String query) {
        ensureLoaded();
        return searchEngine.searchEntities(query);
    }

    /**
     * Search entities with filters.
     */
    public List<WikiEntity> searchEntities(String query, WikiCategory category) {
        ensureLoaded();
        return searchEngine.searchEntities(query, category);
    }

    /**
     * Search entities with multiple category filters.
     */
    public List<WikiEntity> searchEntities(String query, Set<WikiCategory> categories) {
        ensureLoaded();
        return searchEngine.searchEntities(query, categories);
    }

    /**
     * Find all entities that drop a specific item.
     */
    public List<WikiEntity> findEntitiesDroppingItem(String itemId) {
        ensureLoaded();
        return searchEngine.findEntitiesDroppingItem(itemId);
    }

    /**
     * Search items by name across all entities.
     */
    public List<WikiDrop> searchItems(String query) {
        ensureLoaded();
        return searchEngine.searchItems(query);
    }

    /**
     * Get all unique items that can be dropped.
     */
    public List<WikiDrop> getAllDroppableItems() {
        ensureLoaded();
        return searchEngine.getAllUniqueItems();
    }

    // ========== Statistics ==========

    /**
     * Get total entity count.
     */
    public int getEntityCount() {
        ensureLoaded();
        return searchEngine.getEntityCount();
    }

    /**
     * Get unique item count.
     */
    public int getUniqueItemCount() {
        ensureLoaded();
        return searchEngine.getUniqueItemCount();
    }

    /**
     * Get entity count by category.
     */
    public int getEntityCountByCategory(WikiCategory category) {
        ensureLoaded();
        return searchEngine.getEntityCountByCategory(category);
    }
}
