package com.islandium.core.ui.pages.wiki;

import com.islandium.core.IslandiumPlugin;
import com.islandium.core.ui.NavBarHelper;
import com.islandium.core.wiki.WikiManager;
import com.islandium.core.wiki.model.WikiCategory;
import com.islandium.core.wiki.model.WikiDrop;
import com.islandium.core.wiki.model.WikiEntity;
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
import java.util.*;

/**
 * Main wiki page with entity list and search functionality.
 */
public class WikiMainPage extends InteractiveCustomUIPage<WikiMainPage.PageData> {

    private final IslandiumPlugin plugin;
    private String searchQuery = "";
    private Set<WikiCategory> activeFilters = new HashSet<>();
    private boolean searchingItems = false; // false = entities, true = items

    public WikiMainPage(@Nonnull PlayerRef playerRef, IslandiumPlugin plugin) {
        super(playerRef, CustomPageLifetime.CanDismiss, PageData.CODEC);
        this.plugin = plugin;
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder cmd, @Nonnull UIEventBuilder event, @Nonnull Store<EntityStore> store) {
        cmd.append("Pages/Islandium/Wiki/WikiMainPage.ui");

        // Setup navigation bar
        NavBarHelper.setupBar(ref, cmd, event, store);

        // Setup events
        event.addEventBinding(CustomUIEventBindingType.ValueChanged, "#SearchField", EventData.of("@Search", "#SearchField.Value"), false);
        event.addEventBinding(CustomUIEventBindingType.Activating, "#SearchEntityBtn", EventData.of("Action", "search_entities"), false);
        event.addEventBinding(CustomUIEventBindingType.Activating, "#SearchItemBtn", EventData.of("Action", "search_items"), false);

        // Build initial content
        buildCategoryFilters(cmd, event);
        buildStatistics(cmd);
        buildResultsList(cmd, event);
    }

    private void buildCategoryFilters(UICommandBuilder cmd, UIEventBuilder event) {
        cmd.clear("#CategoryFilters");

        for (WikiCategory category : WikiCategory.values()) {
            if (category == WikiCategory.UNKNOWN) continue;

            int count = WikiManager.get().getEntityCountByCategory(category);
            if (count == 0) continue;

            String filterId = "Filter" + category.name();
            boolean isActive = activeFilters.isEmpty() || activeFilters.contains(category);
            String bgColor = isActive ? "#2a3f5f" : "#1a2332";
            String textColor = isActive ? category.getColor() : "#808080";

            String filterUi = String.format(
                    "Button #%s { Anchor: (Height: 28, Bottom: 3); Background: (Color: %s); Padding: (Horizontal: 10); " +
                    "Label #Lbl { Style: (FontSize: 12, TextColor: %s, VerticalAlignment: Center); } }",
                    filterId, bgColor, textColor
            );

            cmd.appendInline("#CategoryFilters", filterUi);
            cmd.set("#" + filterId + " #Lbl.Text", category.getDisplayName() + " (" + count + ")");

            event.addEventBinding(CustomUIEventBindingType.Activating, "#" + filterId,
                    EventData.of("ToggleFilter", category.name()), false);
        }
    }

    private void buildStatistics(UICommandBuilder cmd) {
        WikiManager wiki = WikiManager.get();
        cmd.set("#StatEntityCount.Text", String.valueOf(wiki.getEntityCount()));
        cmd.set("#StatItemCount.Text", String.valueOf(wiki.getUniqueItemCount()));
    }

    private void buildResultsList(UICommandBuilder cmd, UIEventBuilder event) {
        cmd.clear("#ResultsList");

        if (searchingItems) {
            buildItemResults(cmd, event);
        } else {
            buildEntityResults(cmd, event);
        }
    }

    private void buildEntityResults(UICommandBuilder cmd, UIEventBuilder event) {
        cmd.set("#ResultsTitle.Text", "ENTITES");

        // Update search mode buttons
        cmd.set("#SearchEntityBtn.Style.Default.Background", "#2d4a8b");
        cmd.set("#SearchItemBtn.Style.Default.Background", "#4a4a4a");

        List<WikiEntity> results;
        if (activeFilters.isEmpty()) {
            results = WikiManager.get().searchEntities(searchQuery);
        } else {
            results = WikiManager.get().searchEntities(searchQuery, activeFilters);
        }

        cmd.set("#ResultCount.Text", results.size() + " resultat" + (results.size() > 1 ? "s" : ""));

        if (results.isEmpty()) {
            cmd.set("#ResultsList.Visible", false);
            cmd.set("#NoResultsMessage.Visible", true);
            return;
        }

        cmd.set("#ResultsList.Visible", true);
        cmd.set("#NoResultsMessage.Visible", false);

        int index = 0;
        for (WikiEntity entity : results) {
            String rowId = "EntityRow" + index;
            String bgColor = index % 2 == 0 ? "#121a26" : "#151d28";
            String categoryColor = entity.getCategory().getColor();

            String rowUi = String.format(
                    "Button #%s { Anchor: (Height: 50, Bottom: 3); Background: (Color: %s); Padding: (Horizontal: 12, Vertical: 5); LayoutMode: Top; }",
                    rowId, bgColor
            );

            cmd.appendInline("#ResultsList", rowUi);

            // Entity name
            cmd.appendInline("#" + rowId, String.format(
                    "Label #Name { Anchor: (Height: 22); Style: (FontSize: 14, TextColor: #ffffff, RenderBold: true, VerticalAlignment: Center); }"
            ));
            cmd.set("#" + rowId + " #Name.Text", entity.getDisplayName());

            // Entity meta
            cmd.appendInline("#" + rowId, String.format(
                    "Group #Meta { Anchor: (Height: 18); LayoutMode: Left; " +
                    "Label #Cat { Anchor: (Width: 100); Style: (FontSize: 11, TextColor: %s, VerticalAlignment: Center); } " +
                    "Label #Drops { FlexWeight: 1; Style: (FontSize: 11, TextColor: #808080, VerticalAlignment: Center); } }",
                    categoryColor
            ));
            cmd.set("#" + rowId + " #Meta #Cat.Text", entity.getCategory().getDisplayName());
            cmd.set("#" + rowId + " #Meta #Drops.Text", entity.getDropCount() + " drop" + (entity.getDropCount() > 1 ? "s" : ""));

            event.addEventBinding(CustomUIEventBindingType.Activating, "#" + rowId,
                    EventData.of("SelectEntity", entity.getId()), false);

            index++;
        }
    }

    private void buildItemResults(UICommandBuilder cmd, UIEventBuilder event) {
        cmd.set("#ResultsTitle.Text", "ITEMS");

        // Update search mode buttons
        cmd.set("#SearchEntityBtn.Style.Default.Background", "#4a4a4a");
        cmd.set("#SearchItemBtn.Style.Default.Background", "#2d4a8b");

        List<WikiDrop> results = WikiManager.get().searchItems(searchQuery);

        cmd.set("#ResultCount.Text", results.size() + " resultat" + (results.size() > 1 ? "s" : ""));

        if (results.isEmpty()) {
            cmd.set("#ResultsList.Visible", false);
            cmd.set("#NoResultsMessage.Visible", true);
            return;
        }

        cmd.set("#ResultsList.Visible", true);
        cmd.set("#NoResultsMessage.Visible", false);

        int index = 0;
        for (WikiDrop item : results) {
            String rowId = "ItemRow" + index;
            String bgColor = index % 2 == 0 ? "#121a26" : "#151d28";
            String rarityColor = item.getRarity().getColor();

            // Count entities that drop this item
            int entityCount = WikiManager.get().findEntitiesDroppingItem(item.getItemId()).size();

            String rowUi = String.format(
                    "Button #%s { Anchor: (Height: 45, Bottom: 3); Background: (Color: %s); Padding: (Horizontal: 12, Vertical: 5); LayoutMode: Top; }",
                    rowId, bgColor
            );

            cmd.appendInline("#ResultsList", rowUi);

            // Item name
            cmd.appendInline("#" + rowId, String.format(
                    "Label #Name { Anchor: (Height: 22); Style: (FontSize: 14, TextColor: %s, RenderBold: true, VerticalAlignment: Center); }",
                    rarityColor
            ));
            cmd.set("#" + rowId + " #Name.Text", item.getItemName());

            // Item meta
            cmd.appendInline("#" + rowId,
                    "Label #Meta { Anchor: (Height: 16); Style: (FontSize: 11, TextColor: #808080, VerticalAlignment: Center); }"
            );
            cmd.set("#" + rowId + " #Meta.Text", "Obtenu sur " + entityCount + " entite" + (entityCount > 1 ? "s" : ""));

            event.addEventBinding(CustomUIEventBindingType.Activating, "#" + rowId,
                    EventData.of("SelectItem", item.getItemId()), false);

            index++;
        }
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, @Nonnull PageData data) {
        super.handleDataEvent(ref, store, data);

        // Handle navigation bar events
        if (NavBarHelper.handleData(ref, store, data.navBar, this::close)) {
            return;
        }

        PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
        Player player = store.getComponent(ref, Player.getComponentType());

        UICommandBuilder cmd = new UICommandBuilder();
        UIEventBuilder event = new UIEventBuilder();

        // Handle search input
        if (data.search != null) {
            searchQuery = data.search;
            buildResultsList(cmd, event);
            sendUpdate(cmd, event, false);
            return;
        }

        // Handle actions
        if (data.action != null) {
            switch (data.action) {
                case "search_entities" -> {
                    searchingItems = false;
                    buildResultsList(cmd, event);
                }
                case "search_items" -> {
                    searchingItems = true;
                    buildResultsList(cmd, event);
                }
            }
            sendUpdate(cmd, event, false);
            return;
        }

        // Handle category filter toggle
        if (data.toggleFilter != null) {
            WikiCategory category = WikiCategory.valueOf(data.toggleFilter);
            if (activeFilters.contains(category)) {
                activeFilters.remove(category);
            } else {
                activeFilters.add(category);
            }
            buildCategoryFilters(cmd, event);
            buildResultsList(cmd, event);
            sendUpdate(cmd, event, false);
            return;
        }

        // Handle entity selection - open detail page
        if (data.selectEntity != null) {
            WikiManager.get().getEntity(data.selectEntity).ifPresent(entity -> {
                player.getPageManager().openCustomPage(ref, store,
                        new WikiEntityDetailPage(playerRef, plugin, entity)
                );
            });
            return;
        }

        // Handle item selection - open item detail page
        if (data.selectItem != null) {
            player.getPageManager().openCustomPage(ref, store,
                    new WikiItemDetailPage(playerRef, plugin, data.selectItem)
            );
            return;
        }

        sendUpdate(cmd, event, false);
    }

    @Override
    protected void sendUpdate(UICommandBuilder cmd, UIEventBuilder event, boolean force) {
        super.sendUpdate(cmd, event, force);
    }

    public static class PageData {
        public static final BuilderCodec<PageData> CODEC = BuilderCodec.builder(PageData.class, PageData::new)
                .addField(new KeyedCodec<>("Action", Codec.STRING), (d, v) -> d.action = v, d -> d.action)
                .addField(new KeyedCodec<>("@Search", Codec.STRING), (d, v) -> d.search = v, d -> d.search)
                .addField(new KeyedCodec<>("ToggleFilter", Codec.STRING), (d, v) -> d.toggleFilter = v, d -> d.toggleFilter)
                .addField(new KeyedCodec<>("SelectEntity", Codec.STRING), (d, v) -> d.selectEntity = v, d -> d.selectEntity)
                .addField(new KeyedCodec<>("SelectItem", Codec.STRING), (d, v) -> d.selectItem = v, d -> d.selectItem)
                .addField(new KeyedCodec<>("NavBar", Codec.STRING), (d, v) -> d.navBar = v, d -> d.navBar)
                .build();

        public String action;
        public String search;
        public String toggleFilter;
        public String selectEntity;
        public String selectItem;
        public String navBar;
    }
}
