package com.islandium.core.wiki.search;

import com.islandium.core.wiki.model.WikiCategory;
import com.islandium.core.wiki.model.WikiDrop;
import com.islandium.core.wiki.model.WikiEntity;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Search engine for the wiki system.
 * Provides filtering and search capabilities for entities and items.
 */
public class WikiSearchEngine {

    private final List<WikiEntity> entities = new ArrayList<>();
    private final Map<String, WikiEntity> entityById = new HashMap<>();
    private final Map<String, List<WikiEntity>> entitiesByItem = new HashMap<>();
    private final Map<WikiCategory, List<WikiEntity>> entitiesByCategory = new HashMap<>();
    private final Set<String> uniqueItemIds = new HashSet<>();

    /**
     * Index entities for fast searching.
     */
    public void indexEntities(List<WikiEntity> entityList) {
        // Clear previous data
        entities.clear();
        entityById.clear();
        entitiesByItem.clear();
        entitiesByCategory.clear();
        uniqueItemIds.clear();

        // Index entities
        for (WikiEntity entity : entityList) {
            entities.add(entity);
            entityById.put(entity.getId().toLowerCase(), entity);

            // Index by category
            entitiesByCategory.computeIfAbsent(entity.getCategory(), k -> new ArrayList<>()).add(entity);

            // Index by dropped items (reverse lookup)
            for (WikiDrop drop : entity.getDrops()) {
                String itemId = drop.getItemId().toLowerCase();
                entitiesByItem.computeIfAbsent(itemId, k -> new ArrayList<>()).add(entity);
                uniqueItemIds.add(itemId);
            }
        }

        // Sort entities by name
        entities.sort(Comparator.comparing(WikiEntity::getDisplayName, String.CASE_INSENSITIVE_ORDER));
    }

    // ========== Entity Search ==========

    /**
     * Get all indexed entities.
     */
    public List<WikiEntity> getAllEntities() {
        return new ArrayList<>(entities);
    }

    /**
     * Get entity by ID.
     */
    public Optional<WikiEntity> getEntityById(String id) {
        return Optional.ofNullable(entityById.get(id.toLowerCase()));
    }

    /**
     * Search entities by query (name/description match).
     */
    public List<WikiEntity> searchEntities(String query) {
        if (query == null || query.trim().isEmpty()) {
            return getAllEntities();
        }

        String lowerQuery = query.toLowerCase().trim();
        return entities.stream()
                .filter(entity -> entity.matchesSearch(lowerQuery))
                .collect(Collectors.toList());
    }

    /**
     * Search entities with category filter.
     */
    public List<WikiEntity> searchEntities(String query, WikiCategory category) {
        List<WikiEntity> results = searchEntities(query);
        if (category != null) {
            results = results.stream()
                    .filter(e -> e.getCategory() == category)
                    .collect(Collectors.toList());
        }
        return results;
    }

    /**
     * Search entities with multiple category filters.
     */
    public List<WikiEntity> searchEntities(String query, Set<WikiCategory> categories) {
        List<WikiEntity> results = searchEntities(query);
        if (categories != null && !categories.isEmpty()) {
            results = results.stream()
                    .filter(e -> categories.contains(e.getCategory()))
                    .collect(Collectors.toList());
        }
        return results;
    }

    /**
     * Filter entities by category.
     */
    public List<WikiEntity> filterByCategory(WikiCategory category) {
        if (category == null) {
            return getAllEntities();
        }
        return new ArrayList<>(entitiesByCategory.getOrDefault(category, Collections.emptyList()));
    }

    // ========== Item Search (Reverse Lookup) ==========

    /**
     * Find all entities that drop a specific item.
     */
    public List<WikiEntity> findEntitiesDroppingItem(String itemId) {
        if (itemId == null || itemId.trim().isEmpty()) {
            return Collections.emptyList();
        }
        return new ArrayList<>(entitiesByItem.getOrDefault(itemId.toLowerCase().trim(), Collections.emptyList()));
    }

    /**
     * Search items by name across all drops.
     */
    public List<WikiDrop> searchItems(String query) {
        if (query == null || query.trim().isEmpty()) {
            return getAllUniqueItems();
        }

        String lowerQuery = query.toLowerCase().trim();
        Set<String> seenItems = new HashSet<>();
        List<WikiDrop> results = new ArrayList<>();

        for (WikiEntity entity : entities) {
            for (WikiDrop drop : entity.getDrops()) {
                String itemId = drop.getItemId().toLowerCase();
                if (!seenItems.contains(itemId)) {
                    if (drop.getItemId().toLowerCase().contains(lowerQuery) ||
                        drop.getItemName().toLowerCase().contains(lowerQuery)) {
                        results.add(drop);
                        seenItems.add(itemId);
                    }
                }
            }
        }

        results.sort(Comparator.comparing(WikiDrop::getItemName, String.CASE_INSENSITIVE_ORDER));
        return results;
    }

    /**
     * Get all unique droppable items.
     */
    public List<WikiDrop> getAllUniqueItems() {
        Set<String> seenItems = new HashSet<>();
        List<WikiDrop> results = new ArrayList<>();

        for (WikiEntity entity : entities) {
            for (WikiDrop drop : entity.getDrops()) {
                String itemId = drop.getItemId().toLowerCase();
                if (!seenItems.contains(itemId)) {
                    results.add(drop);
                    seenItems.add(itemId);
                }
            }
        }

        results.sort(Comparator.comparing(WikiDrop::getItemName, String.CASE_INSENSITIVE_ORDER));
        return results;
    }

    // ========== Statistics ==========

    /**
     * Get total entity count.
     */
    public int getEntityCount() {
        return entities.size();
    }

    /**
     * Get unique item count.
     */
    public int getUniqueItemCount() {
        return uniqueItemIds.size();
    }

    /**
     * Get entity count by category.
     */
    public int getEntityCountByCategory(WikiCategory category) {
        return entitiesByCategory.getOrDefault(category, Collections.emptyList()).size();
    }

    /**
     * Get all categories with their entity counts.
     */
    public Map<WikiCategory, Integer> getCategoryCounts() {
        Map<WikiCategory, Integer> counts = new EnumMap<>(WikiCategory.class);
        for (WikiCategory category : WikiCategory.values()) {
            counts.put(category, getEntityCountByCategory(category));
        }
        return counts;
    }
}
