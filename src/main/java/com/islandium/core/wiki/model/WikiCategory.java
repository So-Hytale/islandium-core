package com.islandium.core.wiki.model;

/**
 * Categories for wiki entities.
 */
public enum WikiCategory {
    HOSTILE("Hostile", "#ff4444", "Creatures hostiles qui attaquent les joueurs"),
    PASSIVE("Passif", "#44ff44", "Creatures passives et animaux"),
    NEUTRAL("Neutre", "#ffff44", "Creatures neutres qui n'attaquent que si provoquees"),
    BOSS("Boss", "#ff8800", "Creatures puissantes et rares"),
    NPC("PNJ", "#4488ff", "Personnages non-joueurs"),
    ANIMAL("Animal", "#88ff88", "Animaux domestiques et sauvages"),
    UNKNOWN("Inconnu", "#888888", "Categorie inconnue");

    private final String displayName;
    private final String color;
    private final String description;

    WikiCategory(String displayName, String color, String description) {
        this.displayName = displayName;
        this.color = color;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getColor() {
        return color;
    }

    public String getDescription() {
        return description;
    }

    public static WikiCategory fromString(String value) {
        if (value == null) return UNKNOWN;

        String lower = value.toLowerCase();
        if (lower.contains("hostile") || lower.contains("enemy") || lower.contains("monster")) {
            return HOSTILE;
        } else if (lower.contains("passive") || lower.contains("friendly")) {
            return PASSIVE;
        } else if (lower.contains("neutral")) {
            return NEUTRAL;
        } else if (lower.contains("boss")) {
            return BOSS;
        } else if (lower.contains("npc") || lower.contains("villager") || lower.contains("merchant")) {
            return NPC;
        } else if (lower.contains("animal") || lower.contains("pet")) {
            return ANIMAL;
        }
        return UNKNOWN;
    }
}
