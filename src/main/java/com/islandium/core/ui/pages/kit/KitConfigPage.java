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
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

/**
 * Page admin pour configurer les kits.
 */
public class KitConfigPage extends InteractiveCustomUIPage<KitConfigPage.PageData> {

    private final IslandiumPlugin plugin;
    private final PlayerRef playerRef;

    // State
    private boolean createMode = false;
    private boolean firstJoinToggle = false;
    private String editingKitId = null;
    private boolean addItemMode = false;
    private boolean editMode = false;
    private String editKitId = null;
    private boolean editFirstJoinToggle = false;

    public KitConfigPage(@Nonnull PlayerRef playerRef, IslandiumPlugin plugin) {
        super(playerRef, CustomPageLifetime.CanDismiss, PageData.CODEC);
        this.plugin = plugin;
        this.playerRef = playerRef;
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder cmd, @Nonnull UIEventBuilder event, @Nonnull Store<EntityStore> store) {
        cmd.append("Pages/Islandium/Kit/KitConfigPage.ui");

        // New kit button
        event.addEventBinding(CustomUIEventBindingType.Activating, "#NewKitBtn",
            EventData.of("Action", "showCreate"), false);

        // Create form events
        event.addEventBinding(CustomUIEventBindingType.Activating, "#ConfirmCreateBtn",
            EventData.of("Action", "confirmCreate")
                .append("@NewKitId", "#NewKitIdField.Value")
                .append("@NewKitName", "#NewKitNameField.Value")
                .append("@NewKitDesc", "#NewKitDescField.Value")
                .append("@NewKitColor", "#NewKitColorField.Value")
                .append("@NewKitCooldown", "#NewKitCooldownField.Value")
                .append("@NewKitPerm", "#NewKitPermField.Value"), false);

        event.addEventBinding(CustomUIEventBindingType.Activating, "#CancelCreateBtn",
            EventData.of("Action", "cancelCreate"), false);

        event.addEventBinding(CustomUIEventBindingType.Activating, "#ToggleFirstJoinBtn",
            EventData.of("Action", "toggleFirstJoin"), false);

        // Edit form events
        event.addEventBinding(CustomUIEventBindingType.Activating, "#ConfirmEditBtn",
            EventData.of("Action", "confirmEdit")
                .append("@EditName", "#EditKitNameField.Value")
                .append("@EditDesc", "#EditKitDescField.Value")
                .append("@EditColor", "#EditKitColorField.Value")
                .append("@EditCooldown", "#EditKitCooldownField.Value")
                .append("@EditPerm", "#EditKitPermField.Value"), false);

        event.addEventBinding(CustomUIEventBindingType.Activating, "#CancelEditBtn",
            EventData.of("Action", "cancelEdit"), false);

        event.addEventBinding(CustomUIEventBindingType.Activating, "#EditToggleFirstJoinBtn",
            EventData.of("Action", "editToggleFirstJoin"), false);

        // Add item form events
        event.addEventBinding(CustomUIEventBindingType.Activating, "#ConfirmAddItemBtn",
            EventData.of("Action", "confirmAddItem")
                .append("@NewItemId", "#NewItemIdField.Value")
                .append("@NewItemQty", "#NewItemQtyField.Value"), false);

        event.addEventBinding(CustomUIEventBindingType.Activating, "#CancelAddItemBtn",
            EventData.of("Action", "cancelAddItem"), false);

        updateStatus(cmd);
        buildKitList(cmd, event);
    }

    private void buildKitList(UICommandBuilder cmd, UIEventBuilder event) {
        cmd.clear("#KitList");

        KitService kitService = plugin.getServiceManager().getKitService();
        List<KitDefinition> kits = kitService.getKits();

        if (kits.isEmpty()) {
            cmd.appendInline("#KitList",
                "Label { Anchor: (Height: 30); Text: \"Aucun kit configure. Cliquez + NOUVEAU KIT.\"; " +
                "Style: (FontSize: 12, TextColor: #808080); }");
            return;
        }

        int index = 0;
        for (KitDefinition kit : kits) {
            boolean isEditing = kit.id.equals(editingKitId);
            String bgColor = isEditing ? "#1a2a3a" : (index % 2 == 0 ? "#111b27" : "#151d28");
            String nameColor = isEditing ? "#4fc3f7" : "#ffffff";
            String rowId = "KR" + index;

            String displayName = escapeUI(kit.displayName != null ? kit.displayName : kit.id);
            String desc = escapeUI(kit.description != null ? kit.description : "-");
            String cdText;
            if (kit.cooldownSeconds < 0) cdText = "Aucun";
            else if (kit.cooldownSeconds == 0) cdText = "Unique";
            else cdText = KitService.formatCooldown(kit.cooldownSeconds);
            String fjText = kit.giveOnFirstJoin ? "FJ" : "-";
            String fjColor = kit.giveOnFirstJoin ? "#66bb6a" : "#5a5a5a";

            // Kit row with ITEMS, EDIT, SUPPR buttons
            cmd.appendInline("#KitList",
                "Group #" + rowId + " { Anchor: (Height: 34); LayoutMode: Left; Padding: (Horizontal: 5); Background: (Color: " + bgColor + "); " +
                "  Label #" + rowId + "N { Anchor: (Width: 120); Text: \"" + displayName + "\"; Style: (FontSize: 12, TextColor: " + nameColor + ", VerticalAlignment: Center" + (isEditing ? ", RenderBold: true" : "") + "); } " +
                "  Label #" + rowId + "D { FlexWeight: 1; Text: \"" + desc + "\"; Style: (FontSize: 10, TextColor: #7c8b99, VerticalAlignment: Center); } " +
                "  Label #" + rowId + "C { Anchor: (Width: 60); Text: \"" + cdText + "\"; Style: (FontSize: 10, TextColor: #96a9be, VerticalAlignment: Center); } " +
                "  Label #" + rowId + "F { Anchor: (Width: 25); Text: \"" + fjText + "\"; Style: (FontSize: 10, TextColor: " + fjColor + ", RenderBold: true, VerticalAlignment: Center); } " +
                "  TextButton #" + rowId + "IB { Anchor: (Width: 50, Left: 3, Height: 26); Text: \"ITEMS\"; " +
                "    Style: TextButtonStyle(Default: (Background: #2d4a5a, LabelStyle: (FontSize: 10, TextColor: #ffffff, HorizontalAlignment: Center, VerticalAlignment: Center)), " +
                "    Hovered: (Background: #3d5a6a, LabelStyle: (FontSize: 10, TextColor: #ffffff, HorizontalAlignment: Center, VerticalAlignment: Center))); } " +
                "  TextButton #" + rowId + "EB { Anchor: (Width: 45, Left: 3, Height: 26); Text: \"EDIT\"; " +
                "    Style: TextButtonStyle(Default: (Background: #4a4a2d, LabelStyle: (FontSize: 10, TextColor: #ffffff, HorizontalAlignment: Center, VerticalAlignment: Center)), " +
                "    Hovered: (Background: #6a6a3d, LabelStyle: (FontSize: 10, TextColor: #ffffff, HorizontalAlignment: Center, VerticalAlignment: Center))); } " +
                "  TextButton #" + rowId + "DB { Anchor: (Width: 50, Left: 3, Height: 26); Text: \"SUPPR\"; " +
                "    Style: TextButtonStyle(Default: (Background: #5a2d2d, LabelStyle: (FontSize: 10, TextColor: #ffffff, HorizontalAlignment: Center, VerticalAlignment: Center)), " +
                "    Hovered: (Background: #7a3d3d, LabelStyle: (FontSize: 10, TextColor: #ffffff, HorizontalAlignment: Center, VerticalAlignment: Center))); } " +
                "}");

            event.addEventBinding(CustomUIEventBindingType.Activating, "#" + rowId + " #" + rowId + "IB",
                EventData.of("Action", "editKit").append("KitId", kit.id), false);
            event.addEventBinding(CustomUIEventBindingType.Activating, "#" + rowId + " #" + rowId + "EB",
                EventData.of("Action", "showEditKit").append("KitId", kit.id), false);
            event.addEventBinding(CustomUIEventBindingType.Activating, "#" + rowId + " #" + rowId + "DB",
                EventData.of("Action", "deleteKit").append("KitId", kit.id), false);

            // If this kit is being edited (items view), show its items below
            if (isEditing && kit.items != null) {
                for (int itemIdx = 0; itemIdx < kit.items.size(); itemIdx++) {
                    KitItem item = kit.items.get(itemIdx);
                    String ir = "IR" + index + "x" + itemIdx;
                    String itemName = formatBlockName(item.itemId);
                    String itemQty = "x" + item.quantity;

                    // Create a container, then append the .ui template into it
                    cmd.appendInline("#KitList", "Group #" + ir + " { }");
                    cmd.append("Pages/Islandium/Kit/KitItemRow.ui", "#" + ir);

                    // Set text values
                    cmd.set("#" + ir + " #RowItemName.TextSpans", Message.raw(itemName));
                    cmd.set("#" + ir + " #RowItemQty.TextSpans", Message.raw(itemQty));

                    // Set item icon
                    try {
                        ItemStack itemStack = new ItemStack(item.itemId, 1);
                        cmd.setObject("#" + ir + " #RowItemIcon", itemStack);
                    } catch (Exception ignored) {}

                    final int finalItemIdx = itemIdx;
                    event.addEventBinding(CustomUIEventBindingType.Activating, "#" + ir + " #RowRemoveBtn",
                        EventData.of("Action", "removeItem").append("KitId", kit.id).append("ItemIndex", String.valueOf(finalItemIdx)), false);
                }

                // Add item button row
                String ar = "AR" + index;
                cmd.appendInline("#KitList",
                    "Group #" + ar + " { Anchor: (Height: 28); LayoutMode: Left; Padding: (Left: 40, Right: 5); Background: (Color: #0d2520); " +
                    "  TextButton #" + ar + "B { Anchor: (Width: 120, Height: 24); Text: \"+ AJOUTER ITEM\"; " +
                    "    Style: TextButtonStyle(Default: (Background: #2d5a2d, LabelStyle: (FontSize: 10, TextColor: #ffffff, HorizontalAlignment: Center, VerticalAlignment: Center)), " +
                    "    Hovered: (Background: #3d7a3d, LabelStyle: (FontSize: 10, TextColor: #ffffff, HorizontalAlignment: Center, VerticalAlignment: Center))); } " +
                    "}");

                event.addEventBinding(CustomUIEventBindingType.Activating, "#" + ar + " #" + ar + "B",
                    EventData.of("Action", "showAddItem").append("KitId", kit.id), false);
            }

            index++;
        }
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, @Nonnull PageData data) {
        super.handleDataEvent(ref, store, data);

        Player player = store.getComponent(ref, Player.getComponentType());
        UICommandBuilder cmd = new UICommandBuilder();
        UIEventBuilder event = new UIEventBuilder();
        KitService kitService = plugin.getServiceManager().getKitService();

        if (data.action == null) return;

        switch (data.action) {
            case "showCreate" -> {
                createMode = true;
                firstJoinToggle = false;
                editMode = false;
                cmd.set("#CreateForm.Visible", true);
                cmd.set("#EditForm.Visible", false);
                cmd.set("#ToggleFirstJoinBtn.Text", "First Join: NON");
                sendUpdate(cmd, event, false);
                return;
            }
            case "cancelCreate" -> {
                createMode = false;
                cmd.set("#CreateForm.Visible", false);
                sendUpdate(cmd, event, false);
                return;
            }
            case "toggleFirstJoin" -> {
                firstJoinToggle = !firstJoinToggle;
                cmd.set("#ToggleFirstJoinBtn.Text", "First Join: " + (firstJoinToggle ? "OUI" : "NON"));
                sendUpdate(cmd, event, false);
                return;
            }
            case "confirmCreate" -> {
                if (data.newKitId == null || data.newKitId.trim().isEmpty()) {
                    player.sendMessage(Message.raw("L'ID du kit ne peut pas etre vide!"));
                    return;
                }
                String kitId = data.newKitId.trim().toLowerCase().replace(" ", "_");

                if (kitService.getKit(kitId) != null) {
                    player.sendMessage(Message.raw("Un kit avec l'ID '" + kitId + "' existe deja!"));
                    return;
                }

                KitDefinition newKit = new KitDefinition();
                newKit.id = kitId;
                newKit.displayName = (data.newKitName != null && !data.newKitName.trim().isEmpty()) ? data.newKitName.trim() : kitId;
                newKit.description = (data.newKitDesc != null && !data.newKitDesc.trim().isEmpty()) ? data.newKitDesc.trim() : "";
                newKit.icon = "chest";
                newKit.color = (data.newKitColor != null && !data.newKitColor.trim().isEmpty()) ? data.newKitColor.trim() : "#4fc3f7";
                newKit.items = new ArrayList<>();
                newKit.giveOnFirstJoin = firstJoinToggle;

                int cooldown = -1;
                if (data.newKitCooldown != null && !data.newKitCooldown.trim().isEmpty()) {
                    try {
                        cooldown = Integer.parseInt(data.newKitCooldown.trim());
                    } catch (NumberFormatException e) {
                        player.sendMessage(Message.raw("Cooldown invalide! Utilise un nombre entier."));
                        return;
                    }
                }
                newKit.cooldownSeconds = cooldown;
                newKit.permission = (data.newKitPerm != null && !data.newKitPerm.trim().isEmpty()) ? data.newKitPerm.trim() : null;

                kitService.addKit(newKit).exceptionally(ex -> {
                    String cause = ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage();
                    player.sendMessage(Message.raw("Erreur sauvegarde BDD: " + cause));
                    plugin.log(java.util.logging.Level.SEVERE, "Failed to save kit: " + cause, ex);
                    return null;
                });

                createMode = false;
                editingKitId = kitId;
                cmd.set("#CreateForm.Visible", false);
                player.sendMessage(Message.raw("Kit '" + newKit.displayName + "' cree! Ajoutez des items."));

                buildKitList(cmd, event);
                updateStatus(cmd);
                sendUpdate(cmd, event, false);
                return;
            }
            case "deleteKit" -> {
                if (data.kitId != null) {
                    KitDefinition kit = kitService.getKit(data.kitId);
                    String name = kit != null ? kit.displayName : data.kitId;

                    kitService.removeKit(data.kitId).exceptionally(ex -> {
                        player.sendMessage(Message.raw("Erreur suppression BDD: " + ex.getMessage()));
                        return null;
                    });

                    if (data.kitId.equals(editingKitId)) {
                        editingKitId = null;
                        cmd.set("#AddItemForm.Visible", false);
                    }
                    if (data.kitId.equals(editKitId)) {
                        editMode = false;
                        editKitId = null;
                        cmd.set("#EditForm.Visible", false);
                    }

                    player.sendMessage(Message.raw("Kit '" + name + "' supprime!"));
                    buildKitList(cmd, event);
                    updateStatus(cmd);
                    sendUpdate(cmd, event, false);
                }
                return;
            }
            case "editKit" -> {
                if (data.kitId != null) {
                    if (data.kitId.equals(editingKitId)) {
                        editingKitId = null;
                        addItemMode = false;
                        cmd.set("#AddItemForm.Visible", false);
                    } else {
                        editingKitId = data.kitId;
                        addItemMode = false;
                        cmd.set("#AddItemForm.Visible", false);
                    }
                    buildKitList(cmd, event);
                    sendUpdate(cmd, event, false);
                }
                return;
            }
            case "showEditKit" -> {
                if (data.kitId != null) {
                    KitDefinition kit = kitService.getKit(data.kitId);
                    if (kit == null) return;

                    editMode = true;
                    editKitId = data.kitId;
                    editFirstJoinToggle = kit.giveOnFirstJoin;
                    createMode = false;
                    cmd.set("#CreateForm.Visible", false);

                    cmd.set("#EditForm.Visible", true);
                    cmd.set("#EditFormTitle.Text", "EDITER: " + (kit.displayName != null ? kit.displayName : kit.id));
                    cmd.set("#EditKitNameField.Value", kit.displayName != null ? kit.displayName : "");
                    cmd.set("#EditKitDescField.Value", kit.description != null ? kit.description : "");
                    cmd.set("#EditKitColorField.Value", kit.color != null ? kit.color : "#4fc3f7");
                    cmd.set("#EditKitCooldownField.Value", String.valueOf(kit.cooldownSeconds));
                    cmd.set("#EditKitPermField.Value", kit.permission != null ? kit.permission : "");
                    cmd.set("#EditToggleFirstJoinBtn.Text", "First Join: " + (kit.giveOnFirstJoin ? "OUI" : "NON"));

                    sendUpdate(cmd, event, false);
                }
                return;
            }
            case "cancelEdit" -> {
                editMode = false;
                editKitId = null;
                cmd.set("#EditForm.Visible", false);
                sendUpdate(cmd, event, false);
                return;
            }
            case "editToggleFirstJoin" -> {
                editFirstJoinToggle = !editFirstJoinToggle;
                cmd.set("#EditToggleFirstJoinBtn.Text", "First Join: " + (editFirstJoinToggle ? "OUI" : "NON"));
                sendUpdate(cmd, event, false);
                return;
            }
            case "confirmEdit" -> {
                if (editKitId == null) return;
                KitDefinition kit = kitService.getKit(editKitId);
                if (kit == null) {
                    player.sendMessage(Message.raw("Kit introuvable!"));
                    return;
                }

                if (data.editName != null && !data.editName.trim().isEmpty()) {
                    kit.displayName = data.editName.trim();
                }
                if (data.editDesc != null) {
                    kit.description = data.editDesc.trim();
                }
                if (data.editColor != null && !data.editColor.trim().isEmpty()) {
                    kit.color = data.editColor.trim();
                }
                if (data.editCooldown != null && !data.editCooldown.trim().isEmpty()) {
                    try {
                        kit.cooldownSeconds = Integer.parseInt(data.editCooldown.trim());
                    } catch (NumberFormatException e) {
                        player.sendMessage(Message.raw("Cooldown invalide! Utilise un nombre entier."));
                        return;
                    }
                }
                kit.permission = (data.editPerm != null && !data.editPerm.trim().isEmpty()) ? data.editPerm.trim() : null;
                kit.giveOnFirstJoin = editFirstJoinToggle;

                kitService.updateKit(kit).exceptionally(ex -> {
                    player.sendMessage(Message.raw("Erreur sauvegarde BDD: " + ex.getMessage()));
                    return null;
                });

                player.sendMessage(Message.raw("Kit '" + kit.displayName + "' mis a jour!"));
                editMode = false;
                editKitId = null;
                cmd.set("#EditForm.Visible", false);
                buildKitList(cmd, event);
                sendUpdate(cmd, event, false);
                return;
            }
            case "showAddItem" -> {
                if (data.kitId != null) {
                    addItemMode = true;
                    editingKitId = data.kitId;
                    KitDefinition kit = kitService.getKit(data.kitId);
                    cmd.set("#AddItemForm.Visible", true);
                    cmd.set("#AddItemLabel.Text", "Kit: " + (kit != null ? kit.displayName : data.kitId));

                    // Auto-fill from main hand
                    var inv = player.getInventory();
                    if (inv != null) {
                        var mainHand = inv.getItemInHand();
                        if (mainHand != null && !mainHand.isEmpty()) {
                            String itemId = mainHand.getItemId();
                            cmd.set("#NewItemIdField.Value", itemId);
                            cmd.set("#NewItemQtyField.Value", String.valueOf(mainHand.getQuantity()));
                        }
                    }

                    sendUpdate(cmd, event, false);
                }
                return;
            }
            case "cancelAddItem" -> {
                addItemMode = false;
                cmd.set("#AddItemForm.Visible", false);
                sendUpdate(cmd, event, false);
                return;
            }
            case "confirmAddItem" -> {
                if (editingKitId == null || data.newItemId == null || data.newItemId.trim().isEmpty()) {
                    player.sendMessage(Message.raw("L'ID de l'item ne peut pas etre vide!"));
                    return;
                }

                String itemId = data.newItemId.trim();

                int qty = 1;
                if (data.newItemQty != null && !data.newItemQty.trim().isEmpty()) {
                    try {
                        qty = Integer.parseInt(data.newItemQty.trim());
                        if (qty <= 0) {
                            player.sendMessage(Message.raw("La quantite doit etre positive!"));
                            return;
                        }
                    } catch (NumberFormatException e) {
                        player.sendMessage(Message.raw("Quantite invalide!"));
                        return;
                    }
                }

                KitDefinition kit = kitService.getKit(editingKitId);
                if (kit != null) {
                    if (kit.items == null) kit.items = new ArrayList<>();
                    kit.items.add(new KitItem(itemId, qty));
                    kitService.updateKit(kit).exceptionally(ex -> {
                        player.sendMessage(Message.raw("Erreur sauvegarde BDD: " + ex.getMessage()));
                        return null;
                    });
                    player.sendMessage(Message.raw("Item " + formatBlockName(itemId) + " x" + qty + " ajoute au kit!"));
                }

                addItemMode = false;
                cmd.set("#AddItemForm.Visible", false);
                buildKitList(cmd, event);
                sendUpdate(cmd, event, false);
                return;
            }
            case "removeItem" -> {
                if (data.kitId != null && data.itemIndex != null) {
                    try {
                        int idx = Integer.parseInt(data.itemIndex);
                        KitDefinition kit = kitService.getKit(data.kitId);
                        if (kit != null && kit.items != null && idx >= 0 && idx < kit.items.size()) {
                            KitItem removed = kit.items.remove(idx);
                            kitService.updateKit(kit).exceptionally(ex -> {
                                player.sendMessage(Message.raw("Erreur sauvegarde BDD: " + ex.getMessage()));
                                return null;
                            });
                            player.sendMessage(Message.raw("Item " + formatBlockName(removed.itemId) + " retire du kit!"));
                        }
                    } catch (NumberFormatException ignored) {}

                    buildKitList(cmd, event);
                    sendUpdate(cmd, event, false);
                }
                return;
            }
        }
    }

    private void updateStatus(UICommandBuilder cmd) {
        int kitCount = plugin.getServiceManager().getKitService().getKits().size();
        cmd.set("#StatusLabel.Text", kitCount + " kit(s) configure(s)");
    }

    private String formatBlockName(String blockId) {
        if (blockId == null) return "???";
        String name = blockId;
        int colonIdx = name.indexOf(':');
        if (colonIdx >= 0) name = name.substring(colonIdx + 1);
        String[] parts = name.split("_");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) sb.append(" ");
            if (!parts[i].isEmpty()) {
                sb.append(Character.toUpperCase(parts[i].charAt(0)));
                if (parts[i].length() > 1) sb.append(parts[i].substring(1));
            }
        }
        return sb.toString();
    }

    /**
     * Echappe les caracteres speciaux pour les inserer dans un appendInline.
     */
    private String escapeUI(String text) {
        if (text == null) return "";
        return text.replace("\\", "\\\\").replace("\"", "'");
    }

    // =========================================
    // DATA CODEC
    // =========================================

    public static class PageData {
        public static final BuilderCodec<PageData> CODEC = BuilderCodec.builder(PageData.class, PageData::new)
                .addField(new KeyedCodec<>("Action", Codec.STRING), (d, v) -> d.action = v, d -> d.action)
                .addField(new KeyedCodec<>("KitId", Codec.STRING), (d, v) -> d.kitId = v, d -> d.kitId)
                .addField(new KeyedCodec<>("ItemIndex", Codec.STRING), (d, v) -> d.itemIndex = v, d -> d.itemIndex)
                .addField(new KeyedCodec<>("@NewKitId", Codec.STRING), (d, v) -> d.newKitId = v, d -> d.newKitId)
                .addField(new KeyedCodec<>("@NewKitName", Codec.STRING), (d, v) -> d.newKitName = v, d -> d.newKitName)
                .addField(new KeyedCodec<>("@NewKitDesc", Codec.STRING), (d, v) -> d.newKitDesc = v, d -> d.newKitDesc)
                .addField(new KeyedCodec<>("@NewKitColor", Codec.STRING), (d, v) -> d.newKitColor = v, d -> d.newKitColor)
                .addField(new KeyedCodec<>("@NewKitCooldown", Codec.STRING), (d, v) -> d.newKitCooldown = v, d -> d.newKitCooldown)
                .addField(new KeyedCodec<>("@NewKitPerm", Codec.STRING), (d, v) -> d.newKitPerm = v, d -> d.newKitPerm)
                .addField(new KeyedCodec<>("@NewItemId", Codec.STRING), (d, v) -> d.newItemId = v, d -> d.newItemId)
                .addField(new KeyedCodec<>("@NewItemQty", Codec.STRING), (d, v) -> d.newItemQty = v, d -> d.newItemQty)
                .addField(new KeyedCodec<>("@EditName", Codec.STRING), (d, v) -> d.editName = v, d -> d.editName)
                .addField(new KeyedCodec<>("@EditDesc", Codec.STRING), (d, v) -> d.editDesc = v, d -> d.editDesc)
                .addField(new KeyedCodec<>("@EditColor", Codec.STRING), (d, v) -> d.editColor = v, d -> d.editColor)
                .addField(new KeyedCodec<>("@EditCooldown", Codec.STRING), (d, v) -> d.editCooldown = v, d -> d.editCooldown)
                .addField(new KeyedCodec<>("@EditPerm", Codec.STRING), (d, v) -> d.editPerm = v, d -> d.editPerm)
                .build();

        public String action;
        public String kitId;
        public String itemIndex;
        public String newKitId;
        public String newKitName;
        public String newKitDesc;
        public String newKitColor;
        public String newKitCooldown;
        public String newKitPerm;
        public String newItemId;
        public String newItemQty;
        public String editName;
        public String editDesc;
        public String editColor;
        public String editCooldown;
        public String editPerm;
    }
}
