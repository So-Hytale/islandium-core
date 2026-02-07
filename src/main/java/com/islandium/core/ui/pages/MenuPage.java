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
 */
public class MenuPage extends InteractiveCustomUIPage<MenuPage.PageData> {

    private static final int COLUMNS = 3;
    private static final int CARD_WIDTH = 270;
    private static final int CARD_HEIGHT = 110;
    private static final int CARD_GAP = 12;

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
        cmd.clear("#CardGrid");

        List<IslandiumUIRegistry.Entry> entries = IslandiumUIRegistry.getInstance().getEntries();

        if (entries.isEmpty()) {
            cmd.appendInline("#CardGrid", "Label { Text: \"Aucun plugin enregistre.\"; Anchor: (Height: 40); Style: (FontSize: 14, TextColor: #808080, HorizontalAlignment: Center); }");
            return;
        }

        // Create rows of 3 cards
        int totalRows = (int) Math.ceil((double) entries.size() / COLUMNS);
        int entryIndex = 0;

        for (int row = 0; row < totalRows; row++) {
            String rowSelector = "#CardGrid[" + row + "]";

            // Row container
            int rowHeight = CARD_HEIGHT + (row < totalRows - 1 ? CARD_GAP : 0);
            cmd.appendInline("#CardGrid", "Group #Row { Anchor: (Height: " + rowHeight + "); LayoutMode: Left; }");

            for (int col = 0; col < COLUMNS && entryIndex < entries.size(); col++) {
                IslandiumUIRegistry.Entry entry = entries.get(entryIndex);
                String cardSelector = rowSelector + " #Row[" + col + "]";

                // Card with gap
                int leftMargin = col > 0 ? CARD_GAP : 0;
                String cardUi = buildCardUi(entry, leftMargin);
                cmd.appendInline(rowSelector + " #Row", cardUi);

                // Set dynamic text
                cmd.set(cardSelector + " #CardBtn #CardName.Text", entry.displayName());
                cmd.set(cardSelector + " #CardBtn #CardDesc.Text", entry.description());

                // Bind click event
                event.addEventBinding(CustomUIEventBindingType.Activating,
                        cardSelector + " #CardBtn",
                        EventData.of("OpenPlugin", entry.id()),
                        false);

                entryIndex++;
            }

            // Fill remaining columns with empty spacers
            for (int col = entries.size() - (row * COLUMNS); col < COLUMNS && row == totalRows - 1; col++) {
                int leftMargin = col > 0 ? CARD_GAP : 0;
                cmd.appendInline(rowSelector + " #Row", "Group { Anchor: (Width: " + CARD_WIDTH + ", Left: " + leftMargin + "); }");
            }
        }
    }

    private String buildCardUi(IslandiumUIRegistry.Entry entry, int leftMargin) {
        String accentColor = entry.accentColor();
        String leftAttr = leftMargin > 0 ? ", Left: " + leftMargin : "";

        return "Button #CardBtn { " +
                "Anchor: (Width: " + CARD_WIDTH + ", Height: " + CARD_HEIGHT + leftAttr + "); " +
                "Background: (Color: #151d28); " +
                "Style: ButtonStyle(Hovered: (Background: #1e2d3d)); " +
                "LayoutMode: Left; " +

                // Accent bar
                "Group { Anchor: (Width: 4); Background: (Color: " + accentColor + "); } " +

                // Content area
                "Group { FlexWeight: 1; LayoutMode: Top; Padding: (Full: 15); " +
                    "Label #CardName { Anchor: (Height: 28); Style: (FontSize: 16, TextColor: #ffffff, RenderBold: true, RenderUppercase: true, VerticalAlignment: Center); } " +
                    "Label #CardDesc { Anchor: (Height: 40); Style: (FontSize: 12, TextColor: #7c8b99, Wrap: true, VerticalAlignment: Top); } " +
                "} " +
            "}";
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
