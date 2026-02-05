package com.islandium.core.ui;

import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Registre des pages UI pour Essentials.
 * Inspiré du système AdminUIIndexRegistry.
 */
public class IslandiumUIRegistry {

    private static final IslandiumUIRegistry INSTANCE = new IslandiumUIRegistry();

    private final List<Entry> entries = new ArrayList<>();

    public static IslandiumUIRegistry getInstance() {
        return INSTANCE;
    }

    private IslandiumUIRegistry() {}

    public List<Entry> getEntries() {
        return entries;
    }

    public IslandiumUIRegistry register(Entry entry) {
        entries.add(entry);
        return this;
    }

    @Nullable
    public Entry getEntry(String id) {
        return entries.stream()
                .filter(e -> e.id().equals(id))
                .findFirst()
                .orElse(null);
    }

    /**
     * Entrée du registre UI.
     */
    public record Entry(
            @NotNull String id,
            @NotNull String displayName,
            @NotNull Function<PlayerRef, ? extends InteractiveCustomUIPage<?>> guiSupplier,
            boolean showsInNavBar,
            @NotNull String... commandShortcuts
    ) {}
}
