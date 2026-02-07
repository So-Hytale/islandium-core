package com.islandium.core.ui.pages;

import com.islandium.core.IslandiumPlugin;
import com.islandium.core.ui.IslandiumUIRegistry;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * Page du menu principal - Hub central affichant les plugins enregistres.
 * Utilise cmd.append() avec un fichier MenuCard.ui template (comme NavBarHelper).
 */
public class MenuPage extends InteractiveCustomUIPage<MenuPage.PageData> {

    private static final String CARD_TEMPLATE = "Pages/Islandium/MenuCard.ui";

    private final IslandiumPlugin plugin;
    private final PlayerRef playerRef;

    public MenuPage(@Nonnull PlayerRef playerRef, IslandiumPlugin plugin) {
        super(playerRef, CustomPageLifetime.CanDismiss, PageData.CODEC);
        this.plugin = plugin;
        this.playerRef = playerRef;
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder cmd, @Nonnull UIEventBuilder event, @Nonnull Store<EntityStore> store) {
        cmd.append("Pages/Islandium/MenuPage.ui");

        // Close button
        event.addEventBinding(CustomUIEventBindingType.Activating, "#CloseButton", EventData.of("Action", "close"), false);

        // Build the card list
        buildCardList(cmd, event);
    }

    private void buildCardList(UICommandBuilder cmd, UIEventBuilder event) {
        cmd.clear("#CardGrid");

        List<IslandiumUIRegistry.Entry> entries = IslandiumUIRegistry.getInstance().getEntries();

        if (entries.isEmpty()) {
            return;
        }

        for (int i = 0; i < entries.size(); i++) {
            IslandiumUIRegistry.Entry entry = entries.get(i);
            String selector = "#CardGrid[" + i + "]";

            // Append card from .ui template file (like NavBarHelper does)
            cmd.append("#CardGrid", CARD_TEMPLATE);

            // Set text and accent color
            cmd.set(selector + " #CardName.Text", entry.displayName());
            cmd.set(selector + " #CardName.Style.TextColor", entry.accentColor());
            cmd.set(selector + " #CardDesc.Text", entry.description());

            // Bind click event
            event.addEventBinding(CustomUIEventBindingType.Activating,
                    selector + " #MenuCardBtn",
                    EventData.of("OpenPlugin", entry.id()),
                    false);
        }
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, @Nonnull PageData data) {
        super.handleDataEvent(ref, store, data);

        Player player = store.getComponent(ref, Player.getComponentType());

        // Close button
        if (data.action != null && data.action.equals("close")) {
            return;
        }

        // Open plugin page
        if (data.openPlugin != null) {
            IslandiumUIRegistry.Entry entry = IslandiumUIRegistry.getInstance().getEntry(data.openPlugin);
            if (entry != null) {
                InteractiveCustomUIPage<?> page = entry.guiSupplier().apply(playerRef);
                player.getPageManager().openCustomPage(ref, store, page);
            }
        }
    }

    public static class PageData {
        public static final BuilderCodec<PageData> CODEC = BuilderCodec.builder(PageData.class, PageData::new)
                .addField(new KeyedCodec<>("Action", Codec.STRING), (d, v) -> d.action = v, d -> d.action)
                .addField(new KeyedCodec<>("OpenPlugin", Codec.STRING), (d, v) -> d.openPlugin = v, d -> d.openPlugin)
                .build();

        public String action;
        public String openPlugin;
    }
}
