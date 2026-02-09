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
 * Grille de cartes style Prison avec icones, titres et descriptions.
 */
public class MenuPage extends InteractiveCustomUIPage<MenuPage.PageData> {

    private static final String ROW_TEMPLATE = "Pages/Islandium/MenuRow.ui";
    private static final String CARD_TEMPLATE = "Pages/Islandium/MenuCard.ui";
    private static final int COLUMNS = 3;

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

        // Build the card grid
        buildCardGrid(cmd, event);
    }

    private void buildCardGrid(UICommandBuilder cmd, UIEventBuilder event) {
        List<IslandiumUIRegistry.Entry> entries = IslandiumUIRegistry.getInstance().getEntries();

        if (entries.isEmpty()) {
            return;
        }

        int totalRows = (int) Math.ceil((double) entries.size() / COLUMNS);

        for (int row = 0; row < totalRows; row++) {
            // Append a row container
            cmd.append("#CardGrid", ROW_TEMPLATE);
            String rowSelector = "#CardGrid[" + row + "]";

            // Fill row with cards (up to COLUMNS per row)
            for (int col = 0; col < COLUMNS; col++) {
                int entryIndex = row * COLUMNS + col;

                if (entryIndex < entries.size()) {
                    IslandiumUIRegistry.Entry entry = entries.get(entryIndex);

                    // Append card template into the row
                    cmd.append(rowSelector, CARD_TEMPLATE);
                    String cardSelector = rowSelector + "[" + col + "]";

                    // Set title text and accent color
                    cmd.set(cardSelector + " #CardName.Text", entry.displayName());
                    cmd.set(cardSelector + " #CardName.Style.TextColor", entry.accentColor());

                    // Set description
                    cmd.set(cardSelector + " #CardDesc.Text", entry.description());

                    // Set icon if provided
                    if (entry.iconPath() != null) {
                        cmd.set(cardSelector + " #CardIcon.Background.TexturePath", entry.iconPath());
                    }

                    // Bind click event
                    event.addEventBinding(CustomUIEventBindingType.Activating,
                            cardSelector + " #MenuCardBtn",
                            EventData.of("OpenPlugin", entry.id()),
                            false);
                } else {
                    // Empty spacer to keep grid alignment
                    cmd.appendInline(rowSelector, "Group { FlexWeight: 1; Padding: (Horizontal: 5); }");
                }
            }
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
