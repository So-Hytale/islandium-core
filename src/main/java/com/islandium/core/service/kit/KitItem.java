package com.islandium.core.service.kit;

/**
 * Represente un item dans un kit.
 */
public class KitItem {

    public String itemId;
    public int quantity;

    public KitItem() {}

    public KitItem(String itemId, int quantity) {
        this.itemId = itemId;
        this.quantity = quantity;
    }
}
