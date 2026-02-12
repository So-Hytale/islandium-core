package com.islandium.core.api.util;

import com.hypixel.hytale.protocol.packets.interface_.NotificationStyle;

/**
 * Types de notifications visuelles (toast) affichées aux joueurs.
 * Chaque type a une couleur, une icône (ItemStack) et un style natif Hytale.
 */
public enum NotificationType {

    SUCCESS("#55FF55", "Deco_Star_Yellow", NotificationStyle.Success),
    ERROR("#FF5555", "Deco_Cross_Red", NotificationStyle.Danger),
    INFO("#55FFFF", "Deco_Info_Blue", NotificationStyle.Default),
    WARNING("#FFFF55", "Deco_Warning_Yellow", NotificationStyle.Warning);

    private final String color;
    private String itemId;
    private final NotificationStyle style;

    NotificationType(String color, String itemId, NotificationStyle style) {
        this.color = color;
        this.itemId = itemId;
        this.style = style;
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

    public NotificationStyle getStyle() {
        return style;
    }
}
