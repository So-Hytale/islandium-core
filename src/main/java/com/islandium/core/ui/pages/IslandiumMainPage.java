package com.islandium.core.ui.pages;

import com.islandium.core.api.permission.PermissionService;
import com.islandium.core.api.permission.Rank;
import com.islandium.core.IslandiumPlugin;
import com.islandium.core.service.permission.RankImpl;
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
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Page principale d'administration Essentials avec barre de navigation.
 */
public class IslandiumMainPage extends InteractiveCustomUIPage<IslandiumMainPage.PageData> {

    private final IslandiumPlugin plugin;
    private final PermissionService permissionService;
    private String currentPage = "home";

    // Pour le sous-module Ranks
    private String selectedRankId = null;
    private boolean createMode = false;
    private Set<String> currentPermissions = new HashSet<>();

    public IslandiumMainPage(@Nonnull PlayerRef playerRef, IslandiumPlugin plugin) {
        super(playerRef, CustomPageLifetime.CanDismiss, PageData.CODEC);
        this.plugin = plugin;
        this.permissionService = plugin.getServiceManager().getPermissionService();
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder cmd, @Nonnull UIEventBuilder event, @Nonnull Store<EntityStore> store) {
        cmd.append("Pages/Islandium/IslandiumMainPage.ui");

        // Ajouter les boutons de navigation
        buildNavigationButtons(cmd, event);

        // Event fermer
        event.addEventBinding(CustomUIEventBindingType.Activating, "#CloseButton", EventData.of("Action", "close"), false);

        // Charger les stats pour la page d'accueil
        loadHomeStats(cmd);
    }

    private void buildNavigationButtons(UICommandBuilder cmd, UIEventBuilder event) {
        cmd.clear("#NavButtons");

        // Boutons de navigation
        String[] pages = {"home", "ranks", "players"};
        String[] labels = {"Accueil", "Ranks", "Joueurs"};

        int index = 0;
        for (int i = 0; i < pages.length; i++) {
            String page = pages[i];
            String label = labels[i];
            String selector = "#NavButtons[" + index + "]";
            boolean isSelected = page.equals(currentPage);

            if (isSelected) {
                cmd.appendInline("#NavButtons", "TextButton #NavBtn { Anchor: (Height: 40); Padding: (Horizontal: 20); Background: #1a2836; Style: TextButtonStyle(Default: (LabelStyle: (FontSize: 14, TextColor: #ffd700, VerticalAlignment: Center, RenderUppercase: true, RenderBold: true)), Hovered: (Background: #1a2836, LabelStyle: (FontSize: 14, TextColor: #ffd700, VerticalAlignment: Center, RenderUppercase: true, RenderBold: true))); }");
            } else {
                cmd.appendInline("#NavButtons", "TextButton #NavBtn { Anchor: (Height: 40); Padding: (Horizontal: 20); Style: TextButtonStyle(Default: (LabelStyle: (FontSize: 14, TextColor: #7c8b99, VerticalAlignment: Center, RenderUppercase: true, RenderBold: true)), Hovered: (Background: #1a2836, LabelStyle: (FontSize: 14, TextColor: #a0b0c0, VerticalAlignment: Center, RenderUppercase: true, RenderBold: true))); }");
            }
            cmd.set(selector + " #NavBtn.Text", label);
            event.addEventBinding(CustomUIEventBindingType.Activating, selector + " #NavBtn", EventData.of("NavTo", page), false);
            index++;
        }
    }

    private void loadHomeStats(UICommandBuilder cmd) {
        // Compter les ranks
        int rankCount = permissionService.getAllRanks().join().size();
        cmd.set("#RankCount.Text", String.valueOf(rankCount));

        // Compter les joueurs en ligne
        int playerCount = 0;
        try {
            playerCount = plugin.getPlayerManager().getOnlinePlayers().join().size();
        } catch (Exception ignored) {}
        cmd.set("#PlayerCount.Text", String.valueOf(playerCount));
    }

    private void showPage(UICommandBuilder cmd, UIEventBuilder event, String page) {
        currentPage = page;

        // Mettre a jour la navigation
        buildNavigationButtons(cmd, event);

        if (page.equals("home")) {
            cmd.set("#HomePage.Visible", true);
            cmd.set("#SubPageContent.Visible", false);
            loadHomeStats(cmd);
        } else {
            cmd.set("#HomePage.Visible", false);
            cmd.set("#SubPageContent.Visible", true);
            cmd.clear("#SubPageContent");

            switch (page) {
                case "ranks" -> buildRanksPage(cmd, event);
                case "players" -> buildPlayersPage(cmd, event);
            }
        }
    }

    // =========================================
    // PAGE RANKS
    // =========================================

    private void buildRanksPage(UICommandBuilder cmd, UIEventBuilder event) {
        cmd.appendInline("#SubPageContent", "Group { FlexWeight: 1; LayoutMode: Left; Group #RankLeftPanel { Anchor: (Width: 250); LayoutMode: Top; Padding: (Right: 10); Label { Text: \"Ranks\"; Anchor: (Height: 30); Style: (FontSize: 16, TextColor: #ffd700, RenderBold: true); } Group #RankList { FlexWeight: 1; LayoutMode: TopScrolling; Background: (Color: #0a0f17); Padding: (Full: 5); } Button #CreateRankBtn { Anchor: (Height: 36, Top: 10); Background: (Color: #2a5f2a); Padding: (Horizontal: 15); Label { Text: \"+ Nouveau Rank\"; Style: (FontSize: 13, TextColor: #ffffff, HorizontalAlignment: Center, VerticalAlignment: Center); } } } Group { Anchor: (Width: 2); Background: (Color: #4a4a6a); } Group #RankRightPanel { FlexWeight: 1; LayoutMode: Top; Padding: (Left: 15); Label #RankEditorTitle { Text: \"Selectionnez un rank\"; Anchor: (Height: 35); Style: (FontSize: 18, TextColor: #ffd700, RenderBold: true); } Group #RankEditorContent { FlexWeight: 1; LayoutMode: Top; Visible: false; } Group #RankCreateContent { FlexWeight: 1; LayoutMode: Top; Visible: false; } } }");

        // Event creation
        event.addEventBinding(CustomUIEventBindingType.Activating, "#CreateRankBtn", EventData.of("Action", "showRankCreate"), false);

        buildRankList(cmd, event);

        if (selectedRankId != null && !createMode) {
            var rank = permissionService.getRank(selectedRankId).join().orElse(null);
            if (rank != null) {
                buildRankEditor(cmd, event, rank);
            }
        } else if (createMode) {
            buildRankCreateForm(cmd, event);
        }
    }

    private void buildRankList(UICommandBuilder cmd, UIEventBuilder event) {
        cmd.clear("#RankList");

        List<Rank> ranks = permissionService.getAllRanks().join();

        if (ranks.isEmpty()) {
            cmd.appendInline("#RankList", "Label { Text: \"Aucun rank\"; Anchor: (Height: 30); Style: (FontSize: 12, TextColor: #808080, HorizontalAlignment: Center); }");
        } else {
            int index = 0;
            for (Rank rank : ranks) {
                String selector = "#RankList[" + index + "]";
                boolean isSelected = rank.getName().equals(selectedRankId);
                String bgColor = isSelected ? "#2a3f5f" : "#151d28";

                cmd.appendInline("#RankList", "Button #RankBtn { Anchor: (Height: 32, Bottom: 3); Background: (Color: " + bgColor + "); Padding: (Horizontal: 8); Label #RankName { Style: (FontSize: 13, VerticalAlignment: Center); } }");
                cmd.set(selector + " #RankBtn #RankName.Text", rank.getDisplayName());
                cmd.set(selector + " #RankBtn #RankName.Style.TextColor", rank.getColor());

                event.addEventBinding(CustomUIEventBindingType.Activating, selector + " #RankBtn", EventData.of("SelectRank", rank.getName()), false);
                index++;
            }
        }
    }

    private void buildRankEditor(UICommandBuilder cmd, UIEventBuilder event, Rank rank) {
        cmd.set("#RankEditorTitle.Text", "Edition: " + rank.getDisplayName());
        cmd.set("#RankEditorContent.Visible", true);
        cmd.set("#RankCreateContent.Visible", false);
        cmd.clear("#RankEditorContent");

        // Formulaire d'edition
        cmd.appendInline("#RankEditorContent", "Group { FlexWeight: 1; LayoutMode: Top; Label { Text: \"Nom d'affichage:\"; Anchor: (Height: 25); Style: (FontSize: 13, TextColor: #96a9be); } TextField #DisplayNameField { Anchor: (Height: 32); } Label { Text: \"Prefix (chat):\"; Anchor: (Height: 25, Top: 10); Style: (FontSize: 13, TextColor: #96a9be); } TextField #PrefixField { Anchor: (Height: 32); } Label { Text: \"Couleur (hex):\"; Anchor: (Height: 25, Top: 10); Style: (FontSize: 13, TextColor: #96a9be); } TextField #ColorField { Anchor: (Height: 32); PlaceholderText: \"#ffffff\"; } Label { Text: \"Priorite:\"; Anchor: (Height: 25, Top: 10); Style: (FontSize: 13, TextColor: #96a9be); } TextField #PriorityField { Anchor: (Height: 32); } Label { Text: \"Permissions:\"; Anchor: (Height: 25, Top: 15); Style: (FontSize: 13, TextColor: #96a9be); } Group #PermissionsList { Anchor: (Height: 100); LayoutMode: TopScrolling; Background: (Color: #0a0f17); Padding: (Full: 5); } Group { Anchor: (Height: 36, Top: 5); LayoutMode: Left; TextField #NewPermField { FlexWeight: 1; Anchor: (Height: 32); PlaceholderText: \"essentials.command.example\"; } Button #AddPermBtn { Anchor: (Width: 80, Left: 10, Height: 32); Background: (Color: #2a5f2a); Label { Text: \"Ajouter\"; Style: (FontSize: 12, TextColor: #ffffff, HorizontalAlignment: Center, VerticalAlignment: Center); } } } Group { Anchor: (Height: 44, Top: 20); LayoutMode: Left; Button #SaveRankBtn { Anchor: (Width: 120, Height: 40); Background: (Color: #2a5f2a); Label { Text: \"Sauvegarder\"; Style: (FontSize: 13, TextColor: #ffffff, HorizontalAlignment: Center, VerticalAlignment: Center); } } Button #DeleteRankBtn { Anchor: (Width: 120, Left: 15, Height: 40); Background: (Color: #5f2a2a); Label { Text: \"Supprimer\"; Style: (FontSize: 13, TextColor: #ffffff, HorizontalAlignment: Center, VerticalAlignment: Center); } } } }");

        // Remplir les champs
        cmd.set("#DisplayNameField.Value", rank.getDisplayName());
        cmd.set("#PrefixField.Value", rank.getPrefix() != null ? rank.getPrefix() : "");
        cmd.set("#ColorField.Value", rank.getColor());
        cmd.set("#PriorityField.Value", String.valueOf(rank.getPriority()));

        // Charger les permissions
        currentPermissions = new HashSet<>(rank.getDirectPermissions());
        buildPermissionsList(cmd, event);

        // Events
        event.addEventBinding(CustomUIEventBindingType.ValueChanged, "#DisplayNameField", EventData.of("@DisplayName", "#DisplayNameField.Value"), false);
        event.addEventBinding(CustomUIEventBindingType.ValueChanged, "#PrefixField", EventData.of("@Prefix", "#PrefixField.Value"), false);
        event.addEventBinding(CustomUIEventBindingType.ValueChanged, "#ColorField", EventData.of("@Color", "#ColorField.Value"), false);
        event.addEventBinding(CustomUIEventBindingType.ValueChanged, "#PriorityField", EventData.of("@Priority", "#PriorityField.Value"), false);
        event.addEventBinding(CustomUIEventBindingType.ValueChanged, "#NewPermField", EventData.of("@NewPerm", "#NewPermField.Value"), false);
        event.addEventBinding(CustomUIEventBindingType.Activating, "#AddPermBtn", EventData.of("Action", "addPerm"), false);
        event.addEventBinding(CustomUIEventBindingType.Activating, "#SaveRankBtn", EventData.of("Action", "saveRank"), false);
        event.addEventBinding(CustomUIEventBindingType.Activating, "#DeleteRankBtn", EventData.of("Action", "deleteRank"), false);
    }

    private void buildPermissionsList(UICommandBuilder cmd, UIEventBuilder event) {
        cmd.clear("#PermissionsList");

        if (currentPermissions.isEmpty()) {
            cmd.appendInline("#PermissionsList", "Label { Text: \"Aucune permission\"; Anchor: (Height: 25); Style: (FontSize: 11, TextColor: #808080, HorizontalAlignment: Center); }");
        } else {
            int index = 0;
            for (String perm : currentPermissions) {
                String selector = "#PermissionsList[" + index + "]";
                cmd.appendInline("#PermissionsList", "Group { Anchor: (Height: 22, Bottom: 2); LayoutMode: Left; Label #PermName { FlexWeight: 1; Style: (FontSize: 11, TextColor: #bfcdd5, VerticalAlignment: Center); } Button #RemoveBtn { Anchor: (Width: 20, Height: 20); Background: (Color: #8b0000); Label { Text: \"X\"; Style: (FontSize: 10, TextColor: #ffffff, HorizontalAlignment: Center, VerticalAlignment: Center); } } }");
                cmd.set(selector + " #PermName.Text", perm);
                event.addEventBinding(CustomUIEventBindingType.Activating, selector + " #RemoveBtn", EventData.of("RemovePerm", perm), false);
                index++;
            }
        }
    }

    private void buildRankCreateForm(UICommandBuilder cmd, UIEventBuilder event) {
        cmd.set("#RankEditorTitle.Text", "Nouveau Rank");
        cmd.set("#RankEditorContent.Visible", false);
        cmd.set("#RankCreateContent.Visible", true);
        cmd.clear("#RankCreateContent");

        cmd.appendInline("#RankCreateContent", "Group { FlexWeight: 1; LayoutMode: Top; Label { Text: \"Identifiant unique:\"; Anchor: (Height: 25); Style: (FontSize: 13, TextColor: #96a9be); } TextField #NewRankIdField { Anchor: (Height: 32); PlaceholderText: \"mon_rank\"; } Label { Text: \"Nom d'affichage:\"; Anchor: (Height: 25, Top: 10); Style: (FontSize: 13, TextColor: #96a9be); } TextField #NewRankNameField { Anchor: (Height: 32); PlaceholderText: \"Mon Rank\"; } Label { Text: \"Couleur (hex):\"; Anchor: (Height: 25, Top: 10); Style: (FontSize: 13, TextColor: #96a9be); } TextField #NewRankColorField { Anchor: (Height: 32); PlaceholderText: \"#ffffff\"; } Label { Text: \"Priorite:\"; Anchor: (Height: 25, Top: 10); Style: (FontSize: 13, TextColor: #96a9be); } TextField #NewRankPriorityField { Anchor: (Height: 32); PlaceholderText: \"0\"; } Group { Anchor: (Height: 44, Top: 20); LayoutMode: Left; Button #ConfirmCreateBtn { Anchor: (Width: 120, Height: 40); Background: (Color: #2a5f2a); Label { Text: \"Creer\"; Style: (FontSize: 13, TextColor: #ffffff, HorizontalAlignment: Center, VerticalAlignment: Center); } } Button #CancelCreateBtn { Anchor: (Width: 120, Left: 15, Height: 40); Background: (Color: #3a3a4a); Label { Text: \"Annuler\"; Style: (FontSize: 13, TextColor: #ffffff, HorizontalAlignment: Center, VerticalAlignment: Center); } } } }");

        // Events
        event.addEventBinding(CustomUIEventBindingType.ValueChanged, "#NewRankIdField", EventData.of("@NewRankId", "#NewRankIdField.Value"), false);
        event.addEventBinding(CustomUIEventBindingType.ValueChanged, "#NewRankNameField", EventData.of("@NewRankName", "#NewRankNameField.Value"), false);
        event.addEventBinding(CustomUIEventBindingType.ValueChanged, "#NewRankColorField", EventData.of("@NewRankColor", "#NewRankColorField.Value"), false);
        event.addEventBinding(CustomUIEventBindingType.ValueChanged, "#NewRankPriorityField", EventData.of("@NewRankPriority", "#NewRankPriorityField.Value"), false);
        event.addEventBinding(CustomUIEventBindingType.Activating, "#ConfirmCreateBtn", EventData.of("Action", "confirmRankCreate"), false);
        event.addEventBinding(CustomUIEventBindingType.Activating, "#CancelCreateBtn", EventData.of("Action", "cancelRankCreate"), false);
    }

    // =========================================
    // PAGE PLAYERS (placeholder)
    // =========================================

    private void buildPlayersPage(UICommandBuilder cmd, UIEventBuilder event) {
        cmd.appendInline("#SubPageContent", "Group { FlexWeight: 1; LayoutMode: Top; Label { Text: \"Gestion des Joueurs\"; Anchor: (Height: 40); Style: (FontSize: 20, TextColor: #ff69b4, RenderBold: true); } Label { Text: \"Cette fonctionnalite sera disponible prochainement.\"; Anchor: (Height: 30); Style: (FontSize: 14, TextColor: #808080); } }");
    }

    // =========================================
    // GESTION DES EVENEMENTS
    // =========================================

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, @Nonnull PageData data) {
        super.handleDataEvent(ref, store, data);

        PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
        Player player = store.getComponent(ref, Player.getComponentType());

        UICommandBuilder cmd = new UICommandBuilder();
        UIEventBuilder event = new UIEventBuilder();

        // Navigation
        if (data.navTo != null) {
            showPage(cmd, event, data.navTo);
            sendUpdate(cmd, event, false);
            return;
        }

        // Actions
        if (data.action != null) {
            switch (data.action) {
                case "close" -> {
                    player.sendMessage(Message.raw("Fermez l'interface avec Echap"));
                    return;
                }
                case "showRankCreate" -> {
                    createMode = true;
                    selectedRankId = null;
                    showPage(cmd, event, "ranks");
                    sendUpdate(cmd, event, false);
                    return;
                }
                case "cancelRankCreate" -> {
                    createMode = false;
                    showPage(cmd, event, "ranks");
                    sendUpdate(cmd, event, false);
                    return;
                }
                case "confirmRankCreate" -> {
                    if (data.newRankId != null && !data.newRankId.isBlank() && data.newRankName != null && !data.newRankName.isBlank()) {
                        String color = data.newRankColor != null && !data.newRankColor.isBlank() ? data.newRankColor : "#ffffff";
                        int priority = 0;
                        try {
                            if (data.newRankPriority != null) priority = Integer.parseInt(data.newRankPriority);
                        } catch (NumberFormatException ignored) {}

                        permissionService.createRank(data.newRankId, data.newRankName, null, color, priority).join();
                        player.sendMessage(Message.raw("Rank '" + data.newRankName + "' cree avec succes!"));

                        createMode = false;
                        selectedRankId = data.newRankId;
                        showPage(cmd, event, "ranks");
                        sendUpdate(cmd, event, false);
                    } else {
                        player.sendMessage(Message.raw("Veuillez remplir l'ID et le nom du rank."));
                    }
                    return;
                }
                case "saveRank" -> {
                    if (selectedRankId != null) {
                        Rank rank = permissionService.getRank(selectedRankId).join().orElse(null);
                        if (rank instanceof RankImpl rankImpl) {
                            String displayName = data.displayName != null ? data.displayName : rank.getDisplayName();
                            String prefix = data.prefix;
                            String color = data.color != null && !data.color.isBlank() ? data.color : rank.getColor();
                            int priority = rank.getPriority();
                            try {
                                if (data.priority != null) priority = Integer.parseInt(data.priority);
                            } catch (NumberFormatException ignored) {}

                            rankImpl.setDisplayName(displayName);
                            rankImpl.setPrefix(prefix);
                            rankImpl.setColor(color);
                            rankImpl.setPriority(priority);
                            rankImpl.setDirectPermissions(currentPermissions);

                            permissionService.updateRank(rankImpl).join();
                            player.sendMessage(Message.raw("Rank '" + displayName + "' sauvegarde!"));

                            showPage(cmd, event, "ranks");
                            sendUpdate(cmd, event, false);
                        }
                    }
                    return;
                }
                case "deleteRank" -> {
                    if (selectedRankId != null) {
                        Rank rank = permissionService.getRank(selectedRankId).join().orElse(null);
                        if (rank != null) {
                            permissionService.deleteRank(selectedRankId).join();
                            player.sendMessage(Message.raw("Rank '" + rank.getDisplayName() + "' supprime!"));

                            selectedRankId = null;
                            showPage(cmd, event, "ranks");
                            sendUpdate(cmd, event, false);
                        }
                    }
                    return;
                }
                case "addPerm" -> {
                    if (data.newPerm != null && !data.newPerm.isBlank()) {
                        currentPermissions.add(data.newPerm);
                        buildPermissionsList(cmd, event);
                        cmd.set("#NewPermField.Value", "");
                        sendUpdate(cmd, event, false);
                    }
                    return;
                }
            }
        }

        // Selection d'un rank
        if (data.selectRank != null) {
            selectedRankId = data.selectRank;
            createMode = false;
            showPage(cmd, event, "ranks");
            sendUpdate(cmd, event, false);
            return;
        }

        // Suppression d'une permission
        if (data.removePerm != null) {
            currentPermissions.remove(data.removePerm);
            buildPermissionsList(cmd, event);
            sendUpdate(cmd, event, false);
        }
    }

    public static class PageData {
        public static final BuilderCodec<PageData> CODEC = BuilderCodec.builder(PageData.class, PageData::new)
                .addField(new KeyedCodec<>("Action", Codec.STRING), (d, v) -> d.action = v, d -> d.action)
                .addField(new KeyedCodec<>("NavTo", Codec.STRING), (d, v) -> d.navTo = v, d -> d.navTo)
                .addField(new KeyedCodec<>("SelectRank", Codec.STRING), (d, v) -> d.selectRank = v, d -> d.selectRank)
                .addField(new KeyedCodec<>("RemovePerm", Codec.STRING), (d, v) -> d.removePerm = v, d -> d.removePerm)
                .addField(new KeyedCodec<>("@DisplayName", Codec.STRING), (d, v) -> d.displayName = v, d -> d.displayName)
                .addField(new KeyedCodec<>("@Prefix", Codec.STRING), (d, v) -> d.prefix = v, d -> d.prefix)
                .addField(new KeyedCodec<>("@Color", Codec.STRING), (d, v) -> d.color = v, d -> d.color)
                .addField(new KeyedCodec<>("@Priority", Codec.STRING), (d, v) -> d.priority = v, d -> d.priority)
                .addField(new KeyedCodec<>("@NewPerm", Codec.STRING), (d, v) -> d.newPerm = v, d -> d.newPerm)
                .addField(new KeyedCodec<>("@NewRankId", Codec.STRING), (d, v) -> d.newRankId = v, d -> d.newRankId)
                .addField(new KeyedCodec<>("@NewRankName", Codec.STRING), (d, v) -> d.newRankName = v, d -> d.newRankName)
                .addField(new KeyedCodec<>("@NewRankColor", Codec.STRING), (d, v) -> d.newRankColor = v, d -> d.newRankColor)
                .addField(new KeyedCodec<>("@NewRankPriority", Codec.STRING), (d, v) -> d.newRankPriority = v, d -> d.newRankPriority)
                .build();

        public String action;
        public String navTo;
        public String selectRank;
        public String removePerm;
        public String displayName;
        public String prefix;
        public String color;
        public String priority;
        public String newPerm;
        public String newRankId;
        public String newRankName;
        public String newRankColor;
        public String newRankPriority;
    }
}
