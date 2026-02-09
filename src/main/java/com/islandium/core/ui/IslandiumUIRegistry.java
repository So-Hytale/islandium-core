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
 * Les plugins peuvent enregistrer des Entry pour apparaitre dans le menu principal (./menu).
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
     * Entree du registre UI.
     *
     * @param id             Identifiant unique
     * @param displayName    Nom affiche dans le menu
     * @param description    Description courte affichee sous le nom
     * @param accentColor    Couleur d'accent hex (ex: "#ffd700")
     * @param iconPath       Chemin vers un template .ui de carte custom (ex: "Pages/Islandium/MenuCardPrison.ui"), null pour carte par defaut
     * @param guiSupplier    Factory pour creer la page UI
     * @param showsInNavBar  Si visible dans la barre de navigation
     * @param commandShortcuts Alias de commandes
     */
    public record Entry(
            @NotNull String id,
            @NotNull String displayName,
            @NotNull String description,
            @NotNull String accentColor,
            @Nullable String iconPath,
            @NotNull Function<PlayerRef, ? extends InteractiveCustomUIPage<?>> guiSupplier,
            boolean showsInNavBar,
            @NotNull String... commandShortcuts
    ) {
        /**
         * Constructeur sans iconPath (retrocompatibilite).
         */
        public Entry(
                @NotNull String id,
                @NotNull String displayName,
                @NotNull String description,
                @NotNull String accentColor,
                @NotNull Function<PlayerRef, ? extends InteractiveCustomUIPage<?>> guiSupplier,
                boolean showsInNavBar,
                @NotNull String... commandShortcuts
        ) {
            this(id, displayName, description, accentColor, null, guiSupplier, showsInNavBar, commandShortcuts);
        }
    }
}
