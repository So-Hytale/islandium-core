package com.islandium.core.ui.pages.wiki;

import com.islandium.core.IslandiumPlugin;
import com.islandium.core.ui.NavBarHelper;
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

/**
 * Detail page for a single entity showing all its drops.
 */
public class WikiEntityDetailPage extends InteractiveCustomUIPage<WikiEntityDetailPage.PageData> {

    private final IslandiumPlugin plugin;
    private final WikiEntity entity;

    public WikiEntityDetailPage(@Nonnull PlayerRef playerRef, IslandiumPlugin plugin, WikiEntity entity) {
        super(playerRef, CustomPageLifetime.CanDismiss, PageData.CODEC);
        this.plugin = plugin;
        this.entity = entity;
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder cmd, @Nonnull UIEventBuilder event, @Nonnull Store<EntityStore> store) {
        cmd.append("Pages/Islandium/Wiki/WikiEntityDetailPage.ui");

        // Setup navigation bar
        NavBarHelper.setupBar(ref, cmd, event, store);

        // Back button
        event.addEventBinding(CustomUIEventBindingType.Activating, "#BackButton", EventData.of("Action", "back"), false);

        // Set entity information
        cmd.set("#EntityName.Text", entity.getDisplayName());
        cmd.set("#EntityCategory.Text", entity.getCategory().getDisplayName());
        cmd.set("#EntityCategory.Style.TextColor", entity.getCategory().getColor());
        cmd.set("#EntityHealth.Text", "HP: " + entity.getHealth());
        cmd.set("#EntityAttack.Text", "Attaque: " + (int) entity.getAttackDamage());
        cmd.set("#EntityDescription.Text", entity.getDescription().isEmpty() ? "Aucune description disponible." : entity.getDescription());

        // Build drops list
        buildDropsList(cmd, event);
    }

    private void buildDropsList(UICommandBuilder cmd, UIEventBuilder event) {
        cmd.clear("#DropsList");

        if (!entity.hasDrops()) {
            cmd.set("#DropsList.Visible", false);
            cmd.set("#NoDropsMessage.Visible", true);
            cmd.set("#DropCount.Text", "0 items");
            return;
        }

        cmd.set("#DropsList.Visible", true);
        cmd.set("#NoDropsMessage.Visible", false);
        cmd.set("#DropCount.Text", entity.getDropCount() + " item" + (entity.getDropCount() > 1 ? "s" : ""));

        int index = 0;
        for (WikiDrop drop : entity.getDrops()) {
            String rowId = "DropRow" + index;
            String bgColor = index % 2 == 0 ? "#121a26" : "#151d28";
            String rarityColor = drop.getRarity().getColor();

            String rowUi = String.format(
                    "Group #%s { Anchor: (Height: 48, Bottom: 2); Background: (Color: %s); Padding: (Horizontal: 10, Vertical: 4); LayoutMode: Left; }",
                    rowId, bgColor
            );

            cmd.appendInline("#DropsList", rowUi);

            // Item icon slot - only show if we can create a valid ItemStack
            ItemStack itemStack = drop.createItemStack(1);
            if (itemStack != null) {
                cmd.appendInline("#" + rowId,
                        "Group #ItemSlot { Anchor: (Width: 40, Height: 40); Background: (Color: #1a1a2e); Padding: (Full: 4); }"
                );
                try {
                    cmd.setObject("#" + rowId + " #ItemSlot", itemStack);
                } catch (Exception ignored) {
                    // Item icon failed, that's okay - we'll show without icon
                }
                // Spacer after icon
                cmd.appendInline("#" + rowId, "Group { Anchor: (Width: 10); }");
            }

            // Item name with rarity color
            cmd.appendInline("#" + rowId, String.format(
                    "Label #ItemName { Anchor: (Width: 200); Style: (FontSize: 14, TextColor: %s, RenderBold: true, VerticalAlignment: Center); }",
                    rarityColor
            ));
            cmd.set("#" + rowId + " #ItemName.Text", drop.getItemName());

            // Quantity
            cmd.appendInline("#" + rowId,
                    "Label #Quantity { Anchor: (Width: 70); Style: (FontSize: 13, TextColor: #ffffff, VerticalAlignment: Center, HorizontalAlignment: Center); }"
            );
            cmd.set("#" + rowId + " #Quantity.Text", drop.getQuantityDisplay());

            // Chance
            cmd.appendInline("#" + rowId,
                    "Label #Chance { Anchor: (Width: 70); Style: (FontSize: 13, TextColor: #aaaaaa, VerticalAlignment: Center, HorizontalAlignment: Center); }"
            );
            cmd.set("#" + rowId + " #Chance.Text", drop.getChanceDisplay());

            // Rarity badge
            String badgeBg = getRarityBadgeColor(drop.getRarity());
            cmd.appendInline("#" + rowId, String.format(
                    "Group #Badge { Anchor: (Width: 90, Height: 24); Background: (Color: %s); " +
                    "Label #RarityText { Style: (FontSize: 10, TextColor: #ffffff, HorizontalAlignment: Center, VerticalAlignment: Center, RenderBold: true); } }",
                    badgeBg
            ));
            cmd.set("#" + rowId + " #Badge #RarityText.Text", drop.getRarity().getDisplayName().toUpperCase());

            index++;
        }
    }

    private String getRarityBadgeColor(WikiDrop.DropRarity rarity) {
        return switch (rarity) {
            case COMMON -> "#3a3a3a";
            case UNCOMMON -> "#1a5a1a";
            case RARE -> "#1a3a6a";
            case EPIC -> "#4a1a6a";
            case LEGENDARY -> "#6a3a1a";
        };
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
    }

    public static class PageData {
        public static final BuilderCodec<PageData> CODEC = BuilderCodec.builder(PageData.class, PageData::new)
                .addField(new KeyedCodec<>("Action", Codec.STRING), (d, v) -> d.action = v, d -> d.action)
                .addField(new KeyedCodec<>("NavBar", Codec.STRING), (d, v) -> d.navBar = v, d -> d.navBar)
                .build();

        public String action;
        public String navBar;
    }
}
