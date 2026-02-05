package com.islandium.core.wiki.model;

import com.hypixel.hytale.server.core.inventory.ItemStack;

/**
 * Represents a drop from an entity in the wiki.
 */
public class WikiDrop {

    private final String itemId;
    private final String itemName;
    private final int minQuantity;
    private final int maxQuantity;
    private final double dropChance;
    private final DropRarity rarity;

    public WikiDrop(String itemId, String itemName, int minQuantity, int maxQuantity, double dropChance) {
        this.itemId = itemId;
        this.itemName = itemName;
        this.minQuantity = minQuantity;
        this.maxQuantity = maxQuantity;
        this.dropChance = dropChance;
        this.rarity = DropRarity.fromChance(dropChance);
    }

    public String getItemId() {
        return itemId;
    }

    public String getItemName() {
        return itemName;
    }

    public int getMinQuantity() {
        return minQuantity;
    }

    public int getMaxQuantity() {
        return maxQuantity;
    }

    public double getDropChance() {
        return dropChance;
    }

    public DropRarity getRarity() {
        return rarity;
    }

    /**
     * Create an ItemStack for display in UI.
     * Uses the item ID and max quantity for visual representation.
     * Returns null if the item ID is invalid or doesn't exist.
     */
    public ItemStack createItemStack() {
        if (itemId == null || itemId.isEmpty()) {
            return null;
        }
        try {
            return new ItemStack(itemId, maxQuantity > 0 ? maxQuantity : 1);
        } catch (Exception | Error e) {
            // Item ID not valid in this game version
            return null;
        }
    }

    /**
     * Create an ItemStack with specific quantity for display.
     * Returns null if the item ID is invalid or doesn't exist.
     */
    public ItemStack createItemStack(int quantity) {
        if (itemId == null || itemId.isEmpty()) {
            return null;
        }
        try {
            return new ItemStack(itemId, quantity);
        } catch (Exception | Error e) {
            // Item ID not valid in this game version
            return null;
        }
    }

    public String getQuantityDisplay() {
        if (minQuantity == maxQuantity) {
            return String.valueOf(minQuantity);
        }
        return minQuantity + "-" + maxQuantity;
    }

    public String getChanceDisplay() {
        if (dropChance >= 1.0) {
            return "100%";
        }
        double percent = dropChance * 100;
        if (percent < 1) {
            return String.format("%.2f%%", percent);
        }
        return String.format("%.0f%%", percent);
    }

    /**
     * Drop rarity based on chance.
     */
    public enum DropRarity {
        COMMON("Commun", "#ffffff"),
        UNCOMMON("Peu commun", "#1eff00"),
        RARE("Rare", "#0070dd"),
        EPIC("Epique", "#a335ee"),
        LEGENDARY("Legendaire", "#ff8000");

        private final String displayName;
        private final String color;

        DropRarity(String displayName, String color) {
            this.displayName = displayName;
            this.color = color;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getColor() {
            return color;
        }

        public static DropRarity fromChance(double chance) {
            if (chance >= 0.5) return COMMON;
            if (chance >= 0.2) return UNCOMMON;
            if (chance >= 0.05) return RARE;
            if (chance >= 0.01) return EPIC;
            return LEGENDARY;
        }
    }

    public static class Builder {
        private String itemId;
        private String itemName;
        private int minQuantity = 1;
        private int maxQuantity = 1;
        private double dropChance = 1.0;

        public Builder itemId(String itemId) {
            this.itemId = itemId;
            return this;
        }

        public Builder itemName(String itemName) {
            this.itemName = itemName;
            return this;
        }

        public Builder quantity(int min, int max) {
            this.minQuantity = min;
            this.maxQuantity = max;
            return this;
        }

        public Builder quantity(int amount) {
            this.minQuantity = amount;
            this.maxQuantity = amount;
            return this;
        }

        public Builder dropChance(double chance) {
            this.dropChance = chance;
            return this;
        }

        public WikiDrop build() {
            if (itemName == null || itemName.isEmpty()) {
                itemName = formatItemName(itemId);
            }
            return new WikiDrop(itemId, itemName, minQuantity, maxQuantity, dropChance);
        }

        private String formatItemName(String id) {
            if (id == null) return "Unknown";
            return id.replace("_", " ")
                    .substring(0, 1).toUpperCase() +
                    id.replace("_", " ").substring(1);
        }
    }
}
