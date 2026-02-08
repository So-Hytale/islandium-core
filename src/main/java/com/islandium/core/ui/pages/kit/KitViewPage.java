package com.islandium.core.ui.pages.kit;

import com.islandium.core.IslandiumPlugin;
import com.islandium.core.service.kit.KitDefinition;
import com.islandium.core.service.kit.KitItem;
import com.islandium.core.service.kit.KitService;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * Page de test pour les icones d'items.
 * Copie exacte du pattern WikiEntityDetailPage.
 */
public class KitViewPage extends InteractiveCustomUIPage<KitViewPage.PageData> {

    private final IslandiumPlugin plugin;

    public KitViewPage(@Nonnull PlayerRef playerRef, IslandiumPlugin plugin) {
        super(playerRef, CustomPageLifetime.CanDismiss, PageData.CODEC);
        this.plugin = plugin;
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder cmd, @Nonnull UIEventBuilder event, @Nonnull Store<EntityStore> store) {
        cmd.append("Pages/Islandium/Kit/KitViewPage.ui");

        KitService kitService = plugin.getServiceManager().getKitService();
        List<KitDefinition> kits = kitService.getKits();

        int totalItems = 0;
        for (KitDefinition kit : kits) {
            if (kit.items != null) totalItems += kit.items.size();
        }
        cmd.set("#KitCount.Text", kits.size() + " kits, " + totalItems + " items total");

        int index = 0;
        for (KitDefinition kit : kits) {
            // Kit header row
            String kitRowId = "Kit" + index;
            cmd.appendInline("#ItemsList",
                "Group #" + kitRowId + " { Anchor: (Height: 30, Bottom: 2); Background: (Color: #1a2a3a); Padding: (Horizontal: 10); LayoutMode: Left; }");
            cmd.appendInline("#" + kitRowId,
                "Label #KitName { FlexWeight: 1; Style: (FontSize: 14, TextColor: #4fc3f7, RenderBold: true, VerticalAlignment: Center); }");
            cmd.set("#" + kitRowId + " #KitName.Text", kit.displayName != null ? kit.displayName : kit.id);

            // Items of this kit
            if (kit.items != null) {
                for (int i = 0; i < kit.items.size(); i++) {
                    KitItem item = kit.items.get(i);
                    String rowId = "R" + index + "x" + i;
                    String bgColor = i % 2 == 0 ? "#121a26" : "#151d28";

                    // Step 1: Create empty row (exact WikiEntityDetailPage pattern)
                    cmd.appendInline("#ItemsList",
                        String.format("Group #%s { Anchor: (Height: 48, Bottom: 2); Background: (Color: %s); Padding: (Horizontal: 10, Vertical: 4); LayoutMode: Left; }",
                            rowId, bgColor));

                    // Step 2: Item icon using ItemSlot (native element from HyTreasury pattern)
                    String slotId = "IS" + index + "x" + i;
                    try {
                        ItemStack itemStack = new ItemStack(item.itemId, item.quantity);
                        cmd.appendInline("#" + rowId,
                            "ItemSlot #" + slotId + " { Anchor: (Width: 40, Height: 40); ShowQualityBackground: false; ShowQuantity: false; }");
                        cmd.setObject("#" + rowId + " #" + slotId, itemStack);
                        cmd.appendInline("#" + rowId, "Group { Anchor: (Width: 10); }");
                    } catch (Exception | Error e) {
                        // Icon failed - add placeholder
                        cmd.appendInline("#" + rowId,
                            "Group { Anchor: (Width: 40, Height: 40); Background: (Color: #2a1a1a); }");
                        cmd.appendInline("#" + rowId, "Group { Anchor: (Width: 10); }");
                    }

                    // Step 3: Item name
                    cmd.appendInline("#" + rowId,
                        "Label #ItemName { FlexWeight: 1; Style: (FontSize: 13, TextColor: #ffffff, RenderBold: true, VerticalAlignment: Center); }");
                    cmd.set("#" + rowId + " #ItemName.Text", item.itemId);

                    // Step 4: Quantity
                    cmd.appendInline("#" + rowId,
                        "Label #Qty { Anchor: (Width: 70); Style: (FontSize: 13, TextColor: #66bb6a, VerticalAlignment: Center); }");
                    cmd.set("#" + rowId + " #Qty.Text", "x" + item.quantity);
                }
            }

            index++;
        }
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, @Nonnull PageData data) {
        super.handleDataEvent(ref, store, data);
    }

    public static class PageData {
        public static final BuilderCodec<PageData> CODEC = BuilderCodec.builder(PageData.class, PageData::new)
                .addField(new KeyedCodec<>("Action", Codec.STRING), (d, v) -> d.action = v, d -> d.action)
                .build();

        public String action;
    }
}
