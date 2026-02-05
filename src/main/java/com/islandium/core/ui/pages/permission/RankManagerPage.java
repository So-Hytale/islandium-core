package com.islandium.core.ui.pages.permission;

import com.islandium.core.api.permission.PermissionService;
import com.islandium.core.api.permission.Rank;
import com.islandium.core.service.permission.RankImpl;
import com.islandium.core.ui.NavBarHelper;
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

import com.islandium.core.IslandiumPlugin;

import javax.annotation.Nonnull;
import java.util.*;

/**
 * Page interactive de gestion des ranks.
 */
public class RankManagerPage extends InteractiveCustomUIPage<RankManagerPage.PageData> {

    private final PermissionService permissionService;
    private final IslandiumPlugin plugin;
    private String selectedRankId = null;
    private boolean createMode = false;
    private Set<String> currentPermissions = new HashSet<>();
    private Set<UUID> currentMembers = new HashSet<>();
    private Map<UUID, String> memberNames = new HashMap<>();

    // Valeurs des champs stockées côté serveur (mises à jour via ValueChanged)
    private String formDisplayName = "";
    private String formPrefix = "";
    private String formColor = "#ffffff";
    private String formPriority = "0";
    private String formNewPerm = "";
    private String formNewRankId = "";
    private String formNewRankName = "";
    private String formNewRankColor = "#ffffff";
    private String formNewRankPriority = "0";
    private String formMemberSearch = "";

    public RankManagerPage(@Nonnull PlayerRef playerRef, PermissionService permissionService, IslandiumPlugin plugin) {
        super(playerRef, CustomPageLifetime.CanDismiss, PageData.CODEC);
        this.permissionService = permissionService;
        this.plugin = plugin;
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder cmd, @Nonnull UIEventBuilder event, @Nonnull Store<EntityStore> store) {
        cmd.append("Pages/Islandium/RankManagerPage.ui");

        // Setup navigation bar
        NavBarHelper.setupBar(ref, cmd, event, store);

        // Events globaux - Boutons
        event.addEventBinding(CustomUIEventBindingType.Activating, "#BackButton", EventData.of("Action", "back"), false);
        event.addEventBinding(CustomUIEventBindingType.Activating, "#CreateRankButton", EventData.of("Action", "showCreate"), false);
        event.addEventBinding(CustomUIEventBindingType.Activating, "#DeleteButton", EventData.of("Action", "delete"), false);
        event.addEventBinding(CustomUIEventBindingType.Activating, "#SetDefaultButton", EventData.of("Action", "setDefault"), false);
        event.addEventBinding(CustomUIEventBindingType.Activating, "#CancelCreateButton", EventData.of("Action", "cancelCreate"), false);
        event.addEventBinding(CustomUIEventBindingType.Activating, "#ConfirmCreateButton", EventData.of("Action", "confirmCreate"), false);
        event.addEventBinding(CustomUIEventBindingType.Activating, "#SaveButton", EventData.of("Action", "save"), false);
        event.addEventBinding(CustomUIEventBindingType.Activating, "#AddPermButton", EventData.of("Action", "addPerm"), false);

        // Champs edition
        event.addEventBinding(CustomUIEventBindingType.ValueChanged, "#DisplayNameField", EventData.of("@DisplayName", "#DisplayNameField.Value"), false);
        event.addEventBinding(CustomUIEventBindingType.ValueChanged, "#PrefixField", EventData.of("@Prefix", "#PrefixField.Value"), false);
        event.addEventBinding(CustomUIEventBindingType.ValueChanged, "#ColorPicker", EventData.of("@Color", "#ColorPicker.Value"), false);
        event.addEventBinding(CustomUIEventBindingType.ValueChanged, "#PriorityField", EventData.of("@Priority", "#PriorityField.Value"), false);
        event.addEventBinding(CustomUIEventBindingType.ValueChanged, "#NewPermField", EventData.of("@NewPerm", "#NewPermField.Value"), false);

        // Champs creation
        event.addEventBinding(CustomUIEventBindingType.ValueChanged, "#NewRankIdField", EventData.of("@NewRankId", "#NewRankIdField.Value"), false);
        event.addEventBinding(CustomUIEventBindingType.ValueChanged, "#NewRankNameField", EventData.of("@NewRankName", "#NewRankNameField.Value"), false);
        event.addEventBinding(CustomUIEventBindingType.ValueChanged, "#NewRankColorField", EventData.of("@NewRankColor", "#NewRankColorField.Value"), false);
        event.addEventBinding(CustomUIEventBindingType.ValueChanged, "#NewRankPriorityField", EventData.of("@NewRankPriority", "#NewRankPriorityField.Value"), false);

        // Champ recherche membres
        event.addEventBinding(CustomUIEventBindingType.ValueChanged, "#MemberSearchField", EventData.of("@MemberSearch", "#MemberSearchField.Value"), false);
        event.addEventBinding(CustomUIEventBindingType.Activating, "#AddMemberButton", EventData.of("Action", "addMember"), false);

        buildRankList(cmd, event);
    }

    private void buildRankList(UICommandBuilder cmd, UIEventBuilder event) {
        cmd.clear("#RankList");

        List<Rank> ranks = permissionService.getAllRanks().join();

        if (ranks.isEmpty()) {
            cmd.appendInline("#RankList", "Label #EmptyLabel { Text: \"Aucun rank\"; Anchor: (Height: 30); Style: (FontSize: 12, TextColor: #808080, HorizontalAlignment: Center); }");
        } else {
            int index = 0;
            for (Rank rank : ranks) {
                boolean isSelected = rank.getName().equals(selectedRankId);
                String bgColor = isSelected ? "#2a3f5f" : "#151d28";
                String btnId = "RankBtn" + index;

                // Afficher un indicateur pour le rank par defaut
                String displayText = rank.isDefault() ? "[*] " + rank.getDisplayName() : rank.getDisplayName();

                cmd.appendInline("#RankList", "Button #" + btnId + " { Anchor: (Height: 32, Bottom: 3); Background: (Color: " + bgColor + "); Padding: (Horizontal: 8); Label #Lbl { Style: (FontSize: 13, VerticalAlignment: Center); } }");
                cmd.set("#" + btnId + " #Lbl.Text", displayText);
                cmd.set("#" + btnId + " #Lbl.Style.TextColor", rank.getColor());

                event.addEventBinding(CustomUIEventBindingType.Activating, "#" + btnId, EventData.of("SelectRank", rank.getName()), false);
                index++;
            }
        }
    }

    private void buildEditor(UICommandBuilder cmd, UIEventBuilder event, Rank rank) {
        cmd.set("#EditorTitle.Text", "Edition: " + rank.getDisplayName());
        cmd.set("#EditorContent.Visible", true);
        cmd.set("#CreateContent.Visible", false);
        cmd.set("#NoRankSelected.Visible", false);

        // Sections permissions et membres
        cmd.set("#PermissionsSection.Visible", true);
        cmd.set("#NoRankSelectedPerm.Visible", false);
        cmd.set("#MembersSection.Visible", true);
        cmd.set("#NoRankSelectedMember.Visible", false);

        cmd.set("#DisplayNameField.Value", rank.getDisplayName());
        cmd.set("#PrefixField.Value", rank.getPrefix() != null ? rank.getPrefix() : "");
        cmd.set("#ColorPicker.Value", rank.getColor());
        cmd.set("#PriorityField.Value", String.valueOf(rank.getPriority()));

        // Initialiser les variables de formulaire avec les valeurs du rank
        formDisplayName = rank.getDisplayName();
        formPrefix = rank.getPrefix() != null ? rank.getPrefix() : "";
        formColor = rank.getColor();
        formPriority = String.valueOf(rank.getPriority());

        // Charger les permissions
        currentPermissions = new HashSet<>(rank.getDirectPermissions());
        buildPermissionsList(cmd, event);

        // Charger les membres du rank
        loadRankMembers(rank.getName());
        buildMembersList(cmd, event);
    }

    private void loadRankMembers(String rankName) {
        currentMembers.clear();
        memberNames.clear();

        // Charger les joueurs qui ont ce rank
        try {
            Set<UUID> players = permissionService.getPlayersWithRank(rankName).join();
            currentMembers.addAll(players);

            // Charger les noms des joueurs
            for (UUID uuid : players) {
                plugin.getPlayerManager().getPlayerName(uuid).thenAccept(opt -> opt.ifPresent(name -> memberNames.put(uuid, name)));
            }
        } catch (Exception e) {
            plugin.log(java.util.logging.Level.WARNING, "Failed to load rank members: " + e.getMessage());
        }
    }

    private void buildMembersList(UICommandBuilder cmd, UIEventBuilder event) {
        cmd.clear("#MembersList");

        if (currentMembers.isEmpty()) {
            cmd.appendInline("#MembersList", "Label #EmptyMembers { Text: \"Aucun membre\"; Anchor: (Height: 25); Style: (FontSize: 11, TextColor: #808080, HorizontalAlignment: Center); }");
        } else {
            int index = 0;
            for (UUID memberId : currentMembers) {
                String name = memberNames.getOrDefault(memberId, memberId.toString().substring(0, 8));
                String grpId = "MemberGrp" + index;
                cmd.appendInline("#MembersList", "Group #" + grpId + " { Anchor: (Height: 22, Bottom: 2); LayoutMode: Left; Label #Lbl { FlexWeight: 1; Style: (FontSize: 11, TextColor: #bfcdd5, VerticalAlignment: Center); } Button #Rm { Anchor: (Width: 20, Height: 20); Background: (Color: #8b0000); Label { Text: \"X\"; Style: (FontSize: 10, TextColor: #ffffff, HorizontalAlignment: Center, VerticalAlignment: Center); } } }");
                cmd.set("#" + grpId + " #Lbl.Text", name);
                event.addEventBinding(CustomUIEventBindingType.Activating, "#" + grpId + " #Rm", EventData.of("RemoveMember", memberId.toString()), false);
                index++;
            }
        }
    }

    private void buildPermissionsList(UICommandBuilder cmd, UIEventBuilder event) {
        cmd.clear("#PermissionsList");

        if (currentPermissions.isEmpty()) {
            cmd.appendInline("#PermissionsList", "Label #EmptyPerm { Text: \"Aucune permission\"; Anchor: (Height: 25); Style: (FontSize: 11, TextColor: #808080, HorizontalAlignment: Center); }");
        } else {
            int index = 0;
            for (String perm : currentPermissions) {
                String grpId = "PermGrp" + index;
                cmd.appendInline("#PermissionsList", "Group #" + grpId + " { Anchor: (Height: 22, Bottom: 2); LayoutMode: Left; Label #Lbl { FlexWeight: 1; Style: (FontSize: 11, TextColor: #bfcdd5, VerticalAlignment: Center); } Button #Rm { Anchor: (Width: 20, Height: 20); Background: (Color: #8b0000); Label { Text: \"X\"; Style: (FontSize: 10, TextColor: #ffffff, HorizontalAlignment: Center, VerticalAlignment: Center); } } }");
                cmd.set("#" + grpId + " #Lbl.Text", perm);
                event.addEventBinding(CustomUIEventBindingType.Activating, "#" + grpId + " #Rm", EventData.of("RemovePerm", perm), false);
                index++;
            }
        }
    }

    private void showCreateMode(UICommandBuilder cmd) {
        createMode = true;
        cmd.set("#EditorTitle.Text", "Nouveau Rank");
        cmd.set("#EditorContent.Visible", false);
        cmd.set("#CreateContent.Visible", true);
        cmd.set("#NoRankSelected.Visible", false);

        // Cacher les sections perms/membres
        cmd.set("#PermissionsSection.Visible", false);
        cmd.set("#NoRankSelectedPerm.Visible", true);
        cmd.set("#MembersSection.Visible", false);
        cmd.set("#NoRankSelectedMember.Visible", true);

        cmd.set("#NewRankIdField.Value", "");
        cmd.set("#NewRankNameField.Value", "");
        cmd.set("#NewRankColorField.Value", "#ffffff");
        cmd.set("#NewRankPriorityField.Value", "0");

        // Reset form values pour la creation
        formNewRankId = "";
        formNewRankName = "";
        formNewRankColor = "#ffffff";
        formNewRankPriority = "0";
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

        // Stocker les valeurs des champs reçues via ValueChanged
        if (data.displayName != null) formDisplayName = data.displayName;
        if (data.prefix != null) formPrefix = data.prefix;
        if (data.color != null) formColor = data.color;
        if (data.priority != null) formPriority = data.priority;
        if (data.newPerm != null) formNewPerm = data.newPerm;
        if (data.newRankId != null) formNewRankId = data.newRankId;
        if (data.newRankName != null) formNewRankName = data.newRankName;
        if (data.newRankColor != null) formNewRankColor = data.newRankColor;
        if (data.newRankPriority != null) formNewRankPriority = data.newRankPriority;
        if (data.memberSearch != null) formMemberSearch = data.memberSearch;

        // Gestion des actions
        if (data.action != null) {
            switch (data.action) {
                case "back" -> {
                    // La page se ferme via CanDismiss (Echap) - envoyer un message
                    player.sendMessage(Message.raw("Fermez l'interface avec Echap"));
                    return;
                }
                case "showCreate" -> {
                    showCreateMode(cmd);
                    sendUpdate(cmd, event, false);
                    return;
                }
                case "cancelCreate" -> {
                    createMode = false;
                    cmd.set("#CreateContent.Visible", false);
                    cmd.set("#EditorTitle.Text", "Selectionnez un rank");
                    cmd.set("#NoRankSelected.Visible", true);
                    sendUpdate(cmd, event, false);
                    return;
                }
                case "confirmCreate" -> {
                    if (!formNewRankId.isBlank() && !formNewRankName.isBlank()) {
                        String color = !formNewRankColor.isBlank() ? formNewRankColor : "#ffffff";
                        int priority = 0;
                        try {
                            priority = Integer.parseInt(formNewRankPriority);
                        } catch (NumberFormatException ignored) {}

                        permissionService.createRank(formNewRankId, formNewRankName, null, color, priority).join();
                        player.sendMessage(Message.raw("Rank '" + formNewRankName + "' cree avec succes!"));

                        createMode = false;
                        selectedRankId = formNewRankId;
                        buildRankList(cmd, event);

                        Rank newRank = permissionService.getRank(formNewRankId).join().orElse(null);
                        if (newRank != null) {
                            buildEditor(cmd, event, newRank);
                        }

                        // Reset form values
                        formNewRankId = "";
                        formNewRankName = "";
                        formNewRankColor = "#ffffff";
                        formNewRankPriority = "0";

                        sendUpdate(cmd, event, false);
                    } else {
                        player.sendMessage(Message.raw("Veuillez remplir l'ID et le nom du rank."));
                    }
                    return;
                }
                case "save" -> {
                    if (selectedRankId != null) {
                        Rank rank = permissionService.getRank(selectedRankId).join().orElse(null);
                        if (rank instanceof RankImpl rankImpl) {
                            String displayName = !formDisplayName.isBlank() ? formDisplayName : rank.getDisplayName();
                            String prefix = formPrefix;
                            String color = !formColor.isBlank() ? formColor : rank.getColor();
                            int priority = rank.getPriority();
                            try {
                                if (!formPriority.isBlank()) priority = Integer.parseInt(formPriority);
                            } catch (NumberFormatException ignored) {}

                            rankImpl.setDisplayName(displayName);
                            rankImpl.setPrefix(prefix);
                            rankImpl.setColor(color);
                            rankImpl.setPriority(priority);
                            rankImpl.setDirectPermissions(currentPermissions);

                            permissionService.updateRank(rankImpl).join();
                            player.sendMessage(Message.raw("Rank '" + displayName + "' sauvegarde!"));

                            buildRankList(cmd, event);
                            sendUpdate(cmd, event, false);
                        }
                    }
                    return;
                }
                case "delete" -> {
                    if (selectedRankId != null) {
                        Rank rank = permissionService.getRank(selectedRankId).join().orElse(null);
                        if (rank != null) {
                            permissionService.deleteRank(selectedRankId).join();
                            player.sendMessage(Message.raw("Rank '" + rank.getDisplayName() + "' supprime!"));

                            selectedRankId = null;
                            cmd.set("#EditorContent.Visible", false);
                            cmd.set("#EditorTitle.Text", "Selectionnez un rank");
                            cmd.set("#NoRankSelected.Visible", true);
                            // Cacher les sections perms/membres
                            cmd.set("#PermissionsSection.Visible", false);
                            cmd.set("#NoRankSelectedPerm.Visible", true);
                            cmd.set("#MembersSection.Visible", false);
                            cmd.set("#NoRankSelectedMember.Visible", true);
                            buildRankList(cmd, event);
                            sendUpdate(cmd, event, false);
                        }
                    }
                    return;
                }
                case "setDefault" -> {
                    if (selectedRankId != null) {
                        Rank rank = permissionService.getRank(selectedRankId).join().orElse(null);
                        if (rank != null) {
                            permissionService.setDefaultRank(selectedRankId).join();
                            player.sendMessage(Message.raw("Rank '" + rank.getDisplayName() + "' defini comme rank par defaut!"));
                            buildRankList(cmd, event);
                            sendUpdate(cmd, event, false);
                        }
                    }
                    return;
                }
                case "addPerm" -> {
                    if (!formNewPerm.isBlank()) {
                        currentPermissions.add(formNewPerm);
                        buildPermissionsList(cmd, event);
                        cmd.set("#NewPermField.Value", "");
                        formNewPerm = "";
                        sendUpdate(cmd, event, false);
                    }
                    return;
                }
                case "addMember" -> {
                    if (!formMemberSearch.isBlank() && selectedRankId != null) {
                        String searchName = formMemberSearch;
                        String rankId = selectedRankId;
                        UUID assignerUuid = player.getUuid();
                        Player thePlayer = player;
                        // Rechercher le joueur par pseudo
                        plugin.getPlayerManager().getPlayerUUID(searchName).thenAccept(optUuid -> {
                            if (optUuid.isPresent()) {
                                UUID targetUuid = optUuid.get();
                                permissionService.addPlayerRank(targetUuid, rankId, null, assignerUuid).join();
                                currentMembers.add(targetUuid);
                                memberNames.put(targetUuid, searchName);

                                UICommandBuilder cmd2 = new UICommandBuilder();
                                UIEventBuilder event2 = new UIEventBuilder();
                                buildMembersList(cmd2, event2);
                                cmd2.set("#MemberSearchField.Value", "");
                                thePlayer.sendMessage(Message.raw("Joueur '" + searchName + "' ajoute au rank!"));
                                sendUpdate(cmd2, event2, false);
                            } else {
                                thePlayer.sendMessage(Message.raw("Joueur '" + searchName + "' non trouve."));
                            }
                        });
                        formMemberSearch = "";
                    }
                    return;
                }
            }
        }

        // Selection d'un rank
        if (data.selectRank != null) {
            selectedRankId = data.selectRank;
            Rank rank = permissionService.getRank(selectedRankId).join().orElse(null);
            if (rank != null) {
                buildRankList(cmd, event);
                buildEditor(cmd, event, rank);
                sendUpdate(cmd, event, false);
            }
            return;
        }

        // Suppression d'une permission
        if (data.removePerm != null) {
            currentPermissions.remove(data.removePerm);
            buildPermissionsList(cmd, event);
            sendUpdate(cmd, event, false);
            return;
        }

        // Suppression d'un membre
        if (data.removeMember != null && selectedRankId != null) {
            try {
                UUID memberUuid = UUID.fromString(data.removeMember);
                permissionService.removePlayerRank(memberUuid, selectedRankId).join();
                currentMembers.remove(memberUuid);
                memberNames.remove(memberUuid);
                buildMembersList(cmd, event);
                player.sendMessage(Message.raw("Membre retire du rank!"));
                sendUpdate(cmd, event, false);
            } catch (IllegalArgumentException ignored) {}
            return;
        }
    }

    public static class PageData {
        public static final BuilderCodec<PageData> CODEC = BuilderCodec.builder(PageData.class, PageData::new)
                .addField(new KeyedCodec<>("Action", Codec.STRING), (d, v) -> d.action = v, d -> d.action)
                .addField(new KeyedCodec<>("NavBar", Codec.STRING), (d, v) -> d.navBar = v, d -> d.navBar)
                .addField(new KeyedCodec<>("SelectRank", Codec.STRING), (d, v) -> d.selectRank = v, d -> d.selectRank)
                .addField(new KeyedCodec<>("RemovePerm", Codec.STRING), (d, v) -> d.removePerm = v, d -> d.removePerm)
                .addField(new KeyedCodec<>("RemoveMember", Codec.STRING), (d, v) -> d.removeMember = v, d -> d.removeMember)
                .addField(new KeyedCodec<>("@DisplayName", Codec.STRING), (d, v) -> d.displayName = v, d -> d.displayName)
                .addField(new KeyedCodec<>("@Prefix", Codec.STRING), (d, v) -> d.prefix = v, d -> d.prefix)
                .addField(new KeyedCodec<>("@Color", Codec.STRING), (d, v) -> d.color = v, d -> d.color)
                .addField(new KeyedCodec<>("@Priority", Codec.STRING), (d, v) -> d.priority = v, d -> d.priority)
                .addField(new KeyedCodec<>("@NewPerm", Codec.STRING), (d, v) -> d.newPerm = v, d -> d.newPerm)
                .addField(new KeyedCodec<>("@NewRankId", Codec.STRING), (d, v) -> d.newRankId = v, d -> d.newRankId)
                .addField(new KeyedCodec<>("@NewRankName", Codec.STRING), (d, v) -> d.newRankName = v, d -> d.newRankName)
                .addField(new KeyedCodec<>("@NewRankColor", Codec.STRING), (d, v) -> d.newRankColor = v, d -> d.newRankColor)
                .addField(new KeyedCodec<>("@NewRankPriority", Codec.STRING), (d, v) -> d.newRankPriority = v, d -> d.newRankPriority)
                .addField(new KeyedCodec<>("@MemberSearch", Codec.STRING), (d, v) -> d.memberSearch = v, d -> d.memberSearch)
                .build();

        public String action;
        public String navBar;
        public String selectRank;
        public String removePerm;
        public String removeMember;
        public String displayName;
        public String prefix;
        public String color;
        public String priority;
        public String newPerm;
        public String newRankId;
        public String newRankName;
        public String newRankColor;
        public String newRankPriority;
        public String memberSearch;
    }
}
