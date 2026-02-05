package com.islandium.core.ui;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

/**
 * Helper pour la barre de navigation Essentials.
 * Inspiré du NavBarHelper d'AdminUI.
 */
public class NavBarHelper {

    /**
     * Configure la barre de navigation avec tous les boutons enregistrés.
     */
    public static void setupBar(Ref<EntityStore> ref, UICommandBuilder cmd, UIEventBuilder event, Store<EntityStore> store) {
        int index = 0;

        cmd.appendInline("#IslandiumTopNavigationBar #NavBarButtons", "Group #NavCards { LayoutMode: Left; }");

        for (IslandiumUIRegistry.Entry entry : IslandiumUIRegistry.getInstance().getEntries()) {
            if (!entry.showsInNavBar()) continue;

            cmd.append("#NavCards", "Pages/Islandium/Nav/Islandium_TopNavigationBarButton.ui");
            cmd.set("#NavCards[" + index + "] #NavActionButton.Text", entry.displayName());
            event.addEventBinding(
                    CustomUIEventBindingType.Activating,
                    "#NavCards[" + index + "] #NavActionButton",
                    EventData.of("NavBar", entry.id()),
                    false
            );
            index++;
        }
    }

    /**
     * Gère les événements de navigation.
     * @return true si un événement de navigation a été traité
     */
    public static boolean handleData(Ref<EntityStore> ref, Store<EntityStore> store, String navBarId, Runnable closeCurrentPage) {
        if (navBarId == null) return false;

        IslandiumUIRegistry.Entry entry = IslandiumUIRegistry.getInstance().getEntry(navBarId);
        if (entry == null) return false;

        PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
        Player player = store.getComponent(ref, Player.getComponentType());

        closeCurrentPage.run();
        player.getPageManager().openCustomPage(ref, store, entry.guiSupplier().apply(playerRef));

        return true;
    }
}
