package com.islandium.core.api.util;

/**
 * Types de notifications visuelles (toast) affichées aux joueurs.
 * Chaque type a une couleur, une icône (ItemStack) et une durée configurable.
 */
public enum NotificationType {

    SUCCESS("#55FF55", "Deco_Star_Yellow", 1.5f),
    ERROR("#FF5555", "Deco_Cross_Red", 1.5f),
    INFO("#55FFFF", "Deco_Info_Blue", 1.5f),
    WARNING("#FFFF55", "Deco_Warning_Yellow", 1.5f);

    private final String color;
    private String itemId;
    private float duration;

    NotificationType(String color, String itemId, float duration) {
        this.color = color;
        this.itemId = itemId;
        this.duration = duration;
    }

    public String getColor() {
        return color;
    }

    public String getItemId() {
        return itemId;
    }

    public void setItemId(String itemId) {
        this.itemId = itemId;
    }

    public float getDuration() {
        return duration;
    }

    public void setDuration(float duration) {
        this.duration = duration;
    }
}
