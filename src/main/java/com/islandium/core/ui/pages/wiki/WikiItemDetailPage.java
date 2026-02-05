package com.islandium.core.ui.pages.wiki;

import com.islandium.core.IslandiumPlugin;
import com.islandium.core.ui.NavBarHelper;
import com.islandium.core.wiki.WikiManager;
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
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * Detail page showing all entities that drop a specific item.
 * This is the "reverse lookup" feature.
 */
public class WikiItemDetailPage extends InteractiveCustomUIPage<WikiItemDetailPage.PageData> {

    private final IslandiumPlugin plugin;
    private final String itemId;
    private WikiDrop itemInfo;

    public WikiItemDetailPage(@Nonnull PlayerRef playerRef, IslandiumPlugin plugin, String itemId) {
        super(playerRef, CustomPageLifetime.CanDismiss, PageData.CODEC);
        this.plugin = plugin;
        this.itemId = itemId;
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder cmd, @Nonnull UIEventBuilder event, @Nonnull Store<EntityStore> store) {
        cmd.append("Pages/Islandium/Wiki/WikiItemDetailPage.ui");

        // Setup navigation bar
        NavBarHelper.setupBar(ref, cmd, event, store);

        // Back button
        event.addEventBinding(CustomUIEventBindingType.Activating, "#BackButton", EventData.of("Action", "back"), false);

        // Find item info from any entity that drops it
        List<WikiEntity> droppingEntities = WikiManager.get().findEntitiesDroppingItem(itemId);
        if (!droppingEntities.isEmpty()) {
            itemInfo = droppingEntities.get(0).getDropForItem(itemId);
        }

        // Set item information
        if (itemInfo != null) {
            cmd.set("#ItemName.Text", itemInfo.getItemName());
            cmd.set("#ItemName.Style.TextColor", itemInfo.getRarity().getColor());
            cmd.set("#ItemId.Text", itemId);

            // Try to set item icon (may fail if item ID is not valid)
            try {
                ItemStack itemStack = itemInfo.createItemStack(1);
                if (itemStack != null) {
                    cmd.setObject("#ItemIcon", itemStack);
                }
            } catch (Exception ignored) {
                // Item icon not available
            }
        } else {
            cmd.set("#ItemName.Text", formatItemName(itemId));
            cmd.set("#ItemId.Text", itemId);

            // Try to set item icon anyway (may fail)
            try {
                ItemStack itemStack = new ItemStack(itemId, 1);
                if (itemStack != null) {
                    cmd.setObject("#ItemIcon", itemStack);
                }
            } catch (Exception ignored) {
                // Item icon not available
            }
        }

        // Build entities list
        buildEntitiesList(cmd, event, droppingEntities);
    }

    private void buildEntitiesList(UICommandBuilder cmd, UIEventBuilder event, List<WikiEntity> entities) {
        cmd.clear("#EntitiesList");

        if (entities.isEmpty()) {
            cmd.set("#EntitiesList.Visible", false);
            cmd.set("#NoEntitiesMessage.Visible", true);
            cmd.set("#EntityCount.Text", "0 entites");
            return;
        }

        cmd.set("#EntitiesList.Visible", true);
        cmd.set("#NoEntitiesMessage.Visible", false);
        cmd.set("#EntityCount.Text", entities.size() + " entite" + (entities.size() > 1 ? "s" : ""));

        int index = 0;
        for (WikiEntity entity : entities) {
            WikiDrop drop = entity.getDropForItem(itemId);
            if (drop == null) continue;

            String rowId = "EntityRow" + index;
            String bgColor = index % 2 == 0 ? "#121a26" : "#151d28";
            String categoryColor = entity.getCategory().getColor();

            String rowUi = String.format(
                    "Button #%s { Anchor: (Height: 55, Bottom: 3); Background: (Color: %s); Padding: (Horizontal: 15, Vertical: 8); LayoutMode: Top; }",
                    rowId, bgColor
            );

            cmd.appendInline("#EntitiesList", rowUi);

            // Entity name row
            cmd.appendInline("#" + rowId,
                    "Group #NameRow { Anchor: (Height: 22); LayoutMode: Left; " +
                    "Label #EntityName { FlexWeight: 1; Style: (FontSize: 14, TextColor: #ffffff, RenderBold: true, VerticalAlignment: Center); } " +
                    "Label #EntityCat { Anchor: (Width: 100); Style: (FontSize: 11, VerticalAlignment: Center, HorizontalAlignment: End); } }"
            );
            cmd.set("#" + rowId + " #NameRow #EntityName.Text", entity.getDisplayName());
            cmd.set("#" + rowId + " #NameRow #EntityCat.Text", entity.getCategory().getDisplayName());
            cmd.set("#" + rowId + " #NameRow #EntityCat.Style.TextColor", categoryColor);

            // Drop info row
            cmd.appendInline("#" + rowId,
                    "Group #DropRow { Anchor: (Height: 18); LayoutMode: Left; " +
                    "Label #DropQty { Anchor: (Width: 100); Style: (FontSize: 11, TextColor: #aaaaaa, VerticalAlignment: Center); } " +
                    "Label #DropChance { Anchor: (Width: 80); Style: (FontSize: 11, TextColor: #aaaaaa, VerticalAlignment: Center); } " +
                    "Label #DropRarity { FlexWeight: 1; Style: (FontSize: 11, VerticalAlignment: Center, HorizontalAlignment: End); } }"
            );
            cmd.set("#" + rowId + " #DropRow #DropQty.Text", "Quantite: " + drop.getQuantityDisplay());
            cmd.set("#" + rowId + " #DropRow #DropChance.Text", "Chance: " + drop.getChanceDisplay());
            cmd.set("#" + rowId + " #DropRow #DropRarity.Text", drop.getRarity().getDisplayName());
            cmd.set("#" + rowId + " #DropRow #DropRarity.Style.TextColor", drop.getRarity().getColor());

            // Click to view entity
            event.addEventBinding(CustomUIEventBindingType.Activating, "#" + rowId,
                    EventData.of("SelectEntity", entity.getId()), false);

            index++;
        }
    }

    private String formatItemName(String id) {
        if (id == null) return "Unknown";
        String[] parts = id.split("_");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (!part.isEmpty()) {
                sb.append(Character.toUpperCase(part.charAt(0)))
                        .append(part.substring(1).toLowerCase())
                        .append(" ");
            }
        }
        return sb.toString().trim();
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

        // Handle back button
        if (data.action != null && data.action.equals("back")) {
            player.getPageManager().openCustomPage(ref, store,
                    new WikiMainPage(playerRef, plugin)
            );
            return;
        }

        // Handle entity selection - navigate to entity detail
        if (data.selectEntity != null) {
            WikiManager.get().getEntity(data.selectEntity).ifPresent(entity -> {
                player.getPageManager().openCustomPage(ref, store,
                        new WikiEntityDetailPage(playerRef, plugin, entity)
                );
            });
        }
    }

    public static class PageData {
        public static final BuilderCodec<PageData> CODEC = BuilderCodec.builder(PageData.class, PageData::new)
                .addField(new KeyedCodec<>("Action", Codec.STRING), (d, v) -> d.action = v, d -> d.action)
                .addField(new KeyedCodec<>("SelectEntity", Codec.STRING), (d, v) -> d.selectEntity = v, d -> d.selectEntity)
                .addField(new KeyedCodec<>("NavBar", Codec.STRING), (d, v) -> d.navBar = v, d -> d.navBar)
                .build();

        public String action;
        public String selectEntity;
        public String navBar;
    }
}
