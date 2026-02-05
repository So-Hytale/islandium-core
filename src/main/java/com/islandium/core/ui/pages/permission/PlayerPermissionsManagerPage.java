package com.islandium.core.ui.pages.permission;

import com.islandium.core.api.permission.PermissionService;
import com.islandium.core.api.permission.PlayerPermissions;
import com.islandium.core.api.permission.Rank;
import com.islandium.core.IslandiumPlugin;
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

import javax.annotation.Nonnull;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Page interactive de gestion des permissions d'un joueur.
 * Permet de rechercher un joueur et de gerer ses permissions personnelles.
 */
public class PlayerPermissionsManagerPage extends InteractiveCustomUIPage<PlayerPermissionsManagerPage.PageData> {

    private final PermissionService permissionService;
    private final IslandiumPlugin plugin;

    // Joueur cible
    private UUID targetUuid = null;
    private String targetName = null;
    private boolean isTargetOnline = false;

    // Donnees du joueur
    private Set<String> personalPermissions = new HashSet<>();

    // Valeurs des champs
    private String formPlayerSearch = "";
    private String formAddPerm = "";

    public PlayerPermissionsManagerPage(@Nonnull PlayerRef playerRef, PermissionService permissionService, IslandiumPlugin plugin) {
        super(playerRef, CustomPageLifetime.CanDismiss, PageData.CODEC);
        this.permissionService = permissionService;
        this.plugin = plugin;
    }

    public PlayerPermissionsManagerPage(@Nonnull PlayerRef playerRef, PermissionService permissionService, IslandiumPlugin plugin, UUID targetUuid, String targetName) {
        this(playerRef, permissionService, plugin);
        this.targetUuid = targetUuid;
        this.targetName = targetName;
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder cmd, @Nonnull UIEventBuilder event, @Nonnull Store<EntityStore> store) {
        cmd.append("Pages/Islandium/PlayerPermissionsManagerPage.ui");

        // Setup navigation bar
        NavBarHelper.setupBar(ref, cmd, event, store);

        // Bouton retour
        event.addEventBinding(CustomUIEventBindingType.Activating, "#BackButton", EventData.of("Action", "back"), false);

        // Recherche de joueur
        event.addEventBinding(CustomUIEventBindingType.ValueChanged, "#PlayerSearchField", EventData.of("@PlayerSearch", "#PlayerSearchField.Value"), false);
        event.addEventBinding(CustomUIEventBindingType.Activating, "#SearchButton", EventData.of("Action", "search"), false);

        // Gestion des permissions
        event.addEventBinding(CustomUIEventBindingType.ValueChanged, "#AddPermField", EventData.of("@AddPerm", "#AddPermField.Value"), false);
        event.addEventBinding(CustomUIEventBindingType.Activating, "#AddPermButton", EventData.of("Action", "addPerm"), false);

        // Si un joueur cible est deja defini, charger ses donnees
        if (targetUuid != null && targetName != null) {
            cmd.set("#PlayerSearchField.Value", targetName);
            loadPlayerData(cmd, event);
        } else {
            // Sinon, afficher la liste des joueurs connectes
            buildOnlinePlayersList(cmd, event);
        }
    }

    private void loadPlayerData(UICommandBuilder cmd, UIEventBuilder event) {
        if (targetUuid == null) return;

        // Charger les permissions du joueur
        PlayerPermissions perms = permissionService.getPlayerPermissions(targetUuid).join();
        personalPermissions = new HashSet<>(perms.getPersonalPermissions());

        // Verifier si le joueur est en ligne
        isTargetOnline = plugin.getPlayerManager().getOnlinePlayer(targetName).isPresent();

        // Afficher le panel principal
        cmd.set("#MainPanel.Visible", true);
        cmd.set("#OnlinePlayersPanel.Visible", false);
        cmd.set("#SearchStatus.Text", "");

        // Infos joueur
        cmd.set("#PlayerNameLabel.Text", targetName);
        cmd.set("#PlayerUuidLabel.Text", targetUuid.toString());
        cmd.set("#PlayerStatusLabel.Text", isTargetOnline ? "En ligne" : "Hors ligne");
        cmd.set("#PlayerStatusLabel.Style.TextColor", isTargetOnline ? "#4ade80" : "#808080");

        // Construire la liste des permissions
        buildPersonalPermsList(cmd, event);
    }

    private void buildPersonalPermsList(UICommandBuilder cmd, UIEventBuilder event) {
        cmd.clear("#PersonalPermsList");

        if (personalPermissions.isEmpty()) {
            cmd.appendInline("#PersonalPermsList", "Label { Text: \"Aucune permission personnelle\"; Anchor: (Height: 30); Style: (FontSize: 12, TextColor: #808080, HorizontalAlignment: Center, VerticalAlignment: Center); }");
        } else {
            int index = 0;
            List<String> sortedPerms = personalPermissions.stream().sorted().collect(Collectors.toList());
            for (String perm : sortedPerms) {
                String grpId = "PermGrp" + index;

                cmd.appendInline("#PersonalPermsList",
                        "Group #" + grpId + " { Anchor: (Height: 28, Bottom: 4); LayoutMode: Left; Background: (Color: #151d28); Padding: (Left: 10, Right: 6); }");

                cmd.appendInline("#" + grpId,
                        "Label #Lbl { FlexWeight: 1; Style: (FontSize: 12, TextColor: #bfcdd5, VerticalAlignment: Center); }");

                cmd.appendInline("#" + grpId,
                        "Button #Rm { Anchor: (Width: 24, Height: 24); Background: (Color: #8b0000); Label { Text: \"X\"; Style: (FontSize: 11, TextColor: #ffffff, HorizontalAlignment: Center, VerticalAlignment: Center); } }");

                cmd.set("#" + grpId + " #Lbl.Text", perm);

                event.addEventBinding(CustomUIEventBindingType.Activating, "#" + grpId + " #Rm", EventData.of("RemovePerm", perm), false);
                index++;
            }
        }
    }

    private void buildOnlinePlayersList(UICommandBuilder cmd, UIEventBuilder event) {
        cmd.clear("#OnlinePlayersList");

        var onlinePlayers = plugin.getPlayerManager().getOnlinePlayersLocal();
        int count = onlinePlayers.size();

        cmd.set("#OnlinePlayersCount.Text", count + " joueur" + (count > 1 ? "s" : "") + " en ligne");

        if (onlinePlayers.isEmpty()) {
            cmd.appendInline("#OnlinePlayersList", "Label { Text: \"Aucun joueur connecte\"; Anchor: (Height: 40); Style: (FontSize: 12, TextColor: #808080, HorizontalAlignment: Center, VerticalAlignment: Center); }");
        } else {
            int index = 0;
            for (var onlinePlayer : onlinePlayers) {
                String cardId = "PlayerCard" + index;
                String playerName = onlinePlayer.getName();
                UUID playerUuid = onlinePlayer.getUniqueId();

                // Obtenir le rank principal du joueur pour afficher sa couleur
                String rankColor = "#ffffff";
                String rankName = "Joueur";
                try {
                    var perms = permissionService.getPlayerPermissions(playerUuid).join();
                    var primary = perms.getPrimaryRank();
                    if (primary != null) {
                        rankColor = primary.getColor();
                        rankName = primary.getDisplayName();
                    }
                } catch (Exception ignored) {}

                // Carte de joueur cliquable
                cmd.appendInline("#OnlinePlayersList",
                        "Button #" + cardId + " { Anchor: (Height: 50, Bottom: 6); Background: (Color: #151d28); Padding: (Horizontal: 12, Vertical: 6); LayoutMode: Top; }");

                cmd.appendInline("#" + cardId, "Label #Name { Anchor: (Height: 20); Style: (FontSize: 13, TextColor: " + rankColor + ", RenderBold: true, HorizontalAlignment: Center, VerticalAlignment: Center); }");
                cmd.appendInline("#" + cardId, "Label #Rank { Anchor: (Height: 16); Style: (FontSize: 10, TextColor: #808080, HorizontalAlignment: Center, VerticalAlignment: Center); }");

                cmd.set("#" + cardId + " #Name.Text", playerName);
                cmd.set("#" + cardId + " #Rank.Text", rankName);

                // Event pour selectionner ce joueur
                event.addEventBinding(CustomUIEventBindingType.Activating, "#" + cardId,
                        EventData.of("SelectPlayer", playerUuid.toString() + ":" + playerName), false);

                index++;
            }
        }
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, @Nonnull PageData data) {
        super.handleDataEvent(ref, store, data);

        // Handle navigation bar events
        if (NavBarHelper.handleData(ref, store, data.navBar, this::close)) {
            return;
        }

        Player player = store.getComponent(ref, Player.getComponentType());

        UICommandBuilder cmd = new UICommandBuilder();
        UIEventBuilder event = new UIEventBuilder();

        // Stocker les valeurs des champs
        if (data.playerSearch != null) formPlayerSearch = data.playerSearch;
        if (data.addPerm != null) formAddPerm = data.addPerm;

        // Gestion des actions
        if (data.action != null) {
            switch (data.action) {
                case "back" -> {
                    player.sendMessage(Message.raw("Fermez l'interface avec Echap"));
                    return;
                }
                case "search" -> {
                    if (!formPlayerSearch.isBlank()) {
                        searchPlayer(formPlayerSearch, cmd, event, player);
                    } else {
                        cmd.set("#SearchStatus.Text", "Veuillez entrer un pseudo");
                        sendUpdate(cmd, event, false);
                    }
                    return;
                }
                case "addPerm" -> {
                    if (targetUuid != null && !formAddPerm.isBlank()) {
                        addPermToPlayer(formAddPerm, cmd, event, player);
                    }
                    return;
                }
            }
        }

        // Suppression d'une permission
        if (data.removePerm != null && targetUuid != null) {
            removePermFromPlayer(data.removePerm, cmd, event, player);
            return;
        }

        // Selection d'un joueur depuis la liste des connectes
        if (data.selectPlayer != null) {
            String[] parts = data.selectPlayer.split(":", 2);
            if (parts.length == 2) {
                try {
                    targetUuid = UUID.fromString(parts[0]);
                    targetName = parts[1];
                    cmd.set("#PlayerSearchField.Value", targetName);
                    loadPlayerData(cmd, event);
                    sendUpdate(cmd, event, false);
                } catch (IllegalArgumentException ignored) {}
            }
            return;
        }
    }

    private void searchPlayer(String searchName, UICommandBuilder cmd, UIEventBuilder event, Player admin) {
        // Chercher d'abord dans les joueurs en ligne
        var online = plugin.getPlayerManager().getOnlinePlayer(searchName);
        if (online.isPresent()) {
            targetUuid = online.get().getUniqueId();
            targetName = online.get().getName();
            loadPlayerData(cmd, event);
            sendUpdate(cmd, event, false);
            return;
        }

        // Sinon chercher dans la base de donnees
        plugin.getServiceManager().getPlayerRepository()
                .findByUsername(searchName)
                .thenAccept(opt -> {
                    UICommandBuilder cmd2 = new UICommandBuilder();
                    UIEventBuilder event2 = new UIEventBuilder();

                    if (opt.isPresent()) {
                        var playerData = opt.get();
                        targetUuid = playerData.uuid();
                        targetName = playerData.username();
                        loadPlayerData(cmd2, event2);
                    } else {
                        cmd2.set("#SearchStatus.Text", "Joueur '" + searchName + "' non trouve");
                        cmd2.set("#MainPanel.Visible", false);
                        cmd2.set("#OnlinePlayersPanel.Visible", true);
                        targetUuid = null;
                        targetName = null;
                        buildOnlinePlayersList(cmd2, event2);
                    }
                    sendUpdate(cmd2, event2, false);
                });
    }

    private void addPermToPlayer(String permission, UICommandBuilder cmd, UIEventBuilder event, Player admin) {
        permissionService.addPlayerPermission(targetUuid, permission, null).thenRun(() -> {
            admin.sendMessage(Message.raw("Permission '" + permission + "' ajoutee a " + targetName + "!"));
            permissionService.invalidatePlayerCache(targetUuid);

            UICommandBuilder cmd2 = new UICommandBuilder();
            UIEventBuilder event2 = new UIEventBuilder();
            loadPlayerData(cmd2, event2);
            cmd2.set("#AddPermField.Value", "");
            sendUpdate(cmd2, event2, false);
        });

        formAddPerm = "";
    }

    private void removePermFromPlayer(String permission, UICommandBuilder cmd, UIEventBuilder event, Player admin) {
        permissionService.removePlayerPermission(targetUuid, permission).thenRun(() -> {
            admin.sendMessage(Message.raw("Permission '" + permission + "' retiree de " + targetName + "!"));
            permissionService.invalidatePlayerCache(targetUuid);

            UICommandBuilder cmd2 = new UICommandBuilder();
            UIEventBuilder event2 = new UIEventBuilder();
            loadPlayerData(cmd2, event2);
            sendUpdate(cmd2, event2, false);
        });
    }

    public static class PageData {
        public static final BuilderCodec<PageData> CODEC = BuilderCodec.builder(PageData.class, PageData::new)
                .addField(new KeyedCodec<>("Action", Codec.STRING), (d, v) -> d.action = v, d -> d.action)
                .addField(new KeyedCodec<>("NavBar", Codec.STRING), (d, v) -> d.navBar = v, d -> d.navBar)
                .addField(new KeyedCodec<>("RemovePerm", Codec.STRING), (d, v) -> d.removePerm = v, d -> d.removePerm)
                .addField(new KeyedCodec<>("SelectPlayer", Codec.STRING), (d, v) -> d.selectPlayer = v, d -> d.selectPlayer)
                .addField(new KeyedCodec<>("@PlayerSearch", Codec.STRING), (d, v) -> d.playerSearch = v, d -> d.playerSearch)
                .addField(new KeyedCodec<>("@AddPerm", Codec.STRING), (d, v) -> d.addPerm = v, d -> d.addPerm)
                .build();

        public String action;
        public String navBar;
        public String removePerm;
        public String selectPlayer;
        public String playerSearch;
        public String addPerm;
    }
}
