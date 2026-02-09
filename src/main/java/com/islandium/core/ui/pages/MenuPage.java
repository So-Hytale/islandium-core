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
    private static final String DEFAULT_ICON = "Pages/Islandium/Icons/default.png";
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
                    String iconPath = entry.iconPath() != null ? entry.iconPath() : DEFAULT_ICON;

                    // Build card inline with icon baked in
                    String cardId = "Card_" + entry.id();
                    StringBuilder card = new StringBuilder();
                    card.append("Group { FlexWeight: 1; Padding: (Horizontal: 5); ");
                    card.append("  Button #").append(cardId).append(" { ");
                    card.append("    Style: ButtonStyle(Default: (Background: #151d28), Hovered: (Background: #1e2d3d)); ");
                    card.append("    Group { LayoutMode: Top; Padding: (Full: 12); ");
                    // Icon centered
                    card.append("      Group { Anchor: (Height: 88); LayoutMode: Left; ");
                    card.append("        Group { FlexWeight: 1; } ");
                    card.append("        Group { Anchor: (Width: 88, Height: 88); Background: PatchStyle(TexturePath: \"").append(iconPath).append("\"); } ");
                    card.append("        Group { FlexWeight: 1; } ");
                    card.append("      } ");
                    // Title
                    card.append("      Label { Anchor: (Height: 30, Top: 8); ");
                    card.append("        Text: \"").append(entry.displayName()).append("\"; ");
                    card.append("        Style: (FontSize: 15, TextColor: ").append(entry.accentColor()).append(", RenderBold: true, RenderUppercase: true, HorizontalAlignment: Center, VerticalAlignment: Center); ");
                    card.append("      } ");
                    // Description
                    card.append("      Label { Anchor: (Height: 20); ");
                    card.append("        Text: \"").append(entry.description()).append("\"; ");
                    card.append("        Style: (FontSize: 11, TextColor: #7c8b99, HorizontalAlignment: Center); ");
                    card.append("      } ");
                    card.append("    } ");
                    card.append("  } ");
                    card.append("} ");

                    cmd.appendInline(rowSelector, card.toString());

                    // Bind click event
                    event.addEventBinding(CustomUIEventBindingType.Activating,
                            "#" + cardId,
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
