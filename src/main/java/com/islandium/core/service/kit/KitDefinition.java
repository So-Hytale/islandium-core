package com.islandium.core.service.kit;

import java.util.ArrayList;
import java.util.List;

/**
 * Represente la definition d'un kit.
 */
public class KitDefinition {

    public String id;
    public String displayName;
    public String description;
    public String icon;
    public String color;
    public List<KitItem> items;
    public int cooldownSeconds; // 0=usage unique, -1=pas de cooldown, >0=cooldown en secondes
    public boolean giveOnFirstJoin;
    public String permission;

    public KitDefinition() {
        this.items = new ArrayList<>();
        this.cooldownSeconds = -1;
        this.color = "#4fc3f7";
        this.icon = "minecraft:chest";
    }
}
