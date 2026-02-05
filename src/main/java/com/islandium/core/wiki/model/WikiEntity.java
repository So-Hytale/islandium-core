package com.islandium.core.wiki.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents an entity in the wiki system.
 */
public class WikiEntity {

    private final String id;
    private final String displayName;
    private final WikiCategory category;
    private final String description;
    private final List<WikiDrop> drops;
    private final String modelId;
    private final int health;
    private final double attackDamage;

    private WikiEntity(Builder builder) {
        this.id = builder.id;
        this.displayName = builder.displayName;
        this.category = builder.category;
        this.description = builder.description;
        this.drops = Collections.unmodifiableList(new ArrayList<>(builder.drops));
        this.modelId = builder.modelId;
        this.health = builder.health;
        this.attackDamage = builder.attackDamage;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public WikiCategory getCategory() {
        return category;
    }

    public String getDescription() {
        return description;
    }

    public List<WikiDrop> getDrops() {
        return drops;
    }

    public String getModelId() {
        return modelId;
    }

    public int getHealth() {
        return health;
    }

    public double getAttackDamage() {
        return attackDamage;
    }

    public int getDropCount() {
        return drops.size();
    }

    public boolean hasDrops() {
        return !drops.isEmpty();
    }

    /**
     * Check if this entity drops a specific item.
     */
    public boolean dropsItem(String itemId) {
        return drops.stream().anyMatch(drop -> drop.getItemId().equalsIgnoreCase(itemId));
    }

    /**
     * Get drop info for a specific item.
     */
    public WikiDrop getDropForItem(String itemId) {
        return drops.stream()
                .filter(drop -> drop.getItemId().equalsIgnoreCase(itemId))
                .findFirst()
                .orElse(null);
    }

    /**
     * Check if entity matches search query.
     */
    public boolean matchesSearch(String query) {
        if (query == null || query.isEmpty()) return true;
        String lowerQuery = query.toLowerCase();
        return id.toLowerCase().contains(lowerQuery) ||
               displayName.toLowerCase().contains(lowerQuery) ||
               (description != null && description.toLowerCase().contains(lowerQuery));
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String id;
        private String displayName;
        private WikiCategory category = WikiCategory.UNKNOWN;
        private String description = "";
        private List<WikiDrop> drops = new ArrayList<>();
        private String modelId;
        private int health = 20;
        private double attackDamage = 0;

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder displayName(String displayName) {
            this.displayName = displayName;
            return this;
        }

        public Builder category(WikiCategory category) {
            this.category = category;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder drops(List<WikiDrop> drops) {
            this.drops = new ArrayList<>(drops);
            return this;
        }

        public Builder addDrop(WikiDrop drop) {
            this.drops.add(drop);
            return this;
        }

        public Builder modelId(String modelId) {
            this.modelId = modelId;
            return this;
        }

        public Builder health(int health) {
            this.health = health;
            return this;
        }

        public Builder attackDamage(double attackDamage) {
            this.attackDamage = attackDamage;
            return this;
        }

        public WikiEntity build() {
            if (id == null || id.isEmpty()) {
                throw new IllegalArgumentException("Entity id cannot be null or empty");
            }
            if (displayName == null || displayName.isEmpty()) {
                displayName = formatDisplayName(id);
            }
            return new WikiEntity(this);
        }

        private String formatDisplayName(String id) {
            if (id == null) return "Unknown";
            String[] parts = id.split("_");
            StringBuilder sb = new StringBuilder();
            for (String part : parts) {
                if (!part.isEmpty()) {
                    sb.append(Character.toUpperCase(part.charAt(0)))
                      .append(part.substring(1).toLowerCase())
                      .append(" ");
                }
            }
            return sb.toString().trim();
        }
    }
}
