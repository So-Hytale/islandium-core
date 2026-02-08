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
import java.util.ArrayList;
import java.util.List;

/**
 * Page de test pour les icones d'items.
 * Tout est pre-defini dans le .ui, Java ne fait que set les valeurs.
 */
public class KitViewPage extends InteractiveCustomUIPage<KitViewPage.PageData> {

    private static final int MAX_ROWS = 10;
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

        // Flatten all items from all kits
        List<FlatItem> allItems = new ArrayList<>();
        for (KitDefinition kit : kits) {
            if (kit.items != null) {
                for (KitItem item : kit.items) {
                    allItems.add(new FlatItem(kit.displayName != null ? kit.displayName : kit.id, item.itemId, item.quantity));
                }
            }
        }

        cmd.set("#KitCount.Text", kits.size() + " kits, " + allItems.size() + " items");

        // Fill the pre-defined rows (max 10)
        int count = Math.min(allItems.size(), MAX_ROWS);
        for (int i = 0; i < count; i++) {
            FlatItem fi = allItems.get(i);

            // Show the row
            cmd.set("#Row" + i + ".Visible", true);

            // Set labels
            cmd.set("#Row" + i + " #Name" + i + ".Text", fi.kitName + " - " + fi.itemId);
            cmd.set("#Row" + i + " #Qty" + i + ".Text", "x" + fi.quantity);

            // Set item icon on the pre-defined ItemSlot
            try {
                ItemStack itemStack = new ItemStack(fi.itemId, fi.quantity);
                cmd.setObject("#Row" + i + " #Slot" + i, itemStack);
            } catch (Exception | Error ignored) {
                // Item not found
            }
        }
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, @Nonnull PageData data) {
        super.handleDataEvent(ref, store, data);
    }

    private static class FlatItem {
        final String kitName;
        final String itemId;
        final int quantity;

        FlatItem(String kitName, String itemId, int quantity) {
            this.kitName = kitName;
            this.itemId = itemId;
            this.quantity = quantity;
        }
    }

    public static class PageData {
        public static final BuilderCodec<PageData> CODEC = BuilderCodec.builder(PageData.class, PageData::new)
                .addField(new KeyedCodec<>("Action", Codec.STRING), (d, v) -> d.action = v, d -> d.action)
                .build();

        public String action;
    }
}
