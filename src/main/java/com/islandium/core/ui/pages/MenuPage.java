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
 * Utilise une structure plate : chaque carte est un enfant direct du container.
 */
public class MenuPage extends InteractiveCustomUIPage<MenuPage.PageData> {

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
            cmd.appendInline("#CardGrid", "Label { Text: \"Aucun plugin enregistre.\"; Anchor: (Height: 40); Style: (FontSize: 14, TextColor: #808080, HorizontalAlignment: Center); }");
            return;
        }

        // Each entry is a direct child of #CardGrid (TopScrolling layout)
        // We use index-based selectors: #CardGrid[0], #CardGrid[1], etc.
        int index = 0;
        for (IslandiumUIRegistry.Entry entry : entries) {
            String selector = "#CardGrid[" + index + "]";
            String accentColor = entry.accentColor();

            // Card: Button with accent bar + name + description
            cmd.appendInline("#CardGrid",
                    "Button #CardBtn { " +
                        "Anchor: (Height: 90, Bottom: 10); " +
                        "Background: (Color: #151d28); " +
                        "Padding: (Left: 0); " +
                        "LayoutMode: Left; " +
                        "Group #Accent { Anchor: (Width: 4); Background: (Color: " + accentColor + "); } " +
                        "Group #Info { FlexWeight: 1; LayoutMode: Top; Padding: (Full: 15); " +
                            "Label #CardName { Anchor: (Height: 28); Style: (FontSize: 16, TextColor: #ffffff, RenderBold: true, RenderUppercase: true, VerticalAlignment: Center); } " +
                            "Label #CardDesc { Anchor: (Height: 30); Style: (FontSize: 12, TextColor: #7c8b99, VerticalAlignment: Top); } " +
                        "} " +
                    "}");

            // Set text
            cmd.set(selector + " #CardBtn #CardName.Text", entry.displayName());
            cmd.set(selector + " #CardBtn #CardDesc.Text", entry.description());

            // Bind click
            event.addEventBinding(CustomUIEventBindingType.Activating,
                    selector + " #CardBtn",
                    EventData.of("OpenPlugin", entry.id()),
                    false);

            index++;
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
