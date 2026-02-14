package com.islandium.core.ui.pages;

import com.islandium.core.IslandiumPlugin;
import com.islandium.core.api.player.IslandiumPlayer;
import com.islandium.core.api.permission.PermissionService;
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
import java.util.UUID;

public class PrivateMessagePage extends InteractiveCustomUIPage<PrivateMessagePage.PageData> {

    private final IslandiumPlugin plugin;
    private final PlayerRef playerRef;
    private final UUID senderUuid;

    // Joueur cible selectionne
    private UUID targetUuid = null;
    private String targetName = null;

    // Valeur du champ message
    private String formMessage = "";

    public PrivateMessagePage(@Nonnull PlayerRef playerRef, IslandiumPlugin plugin, UUID senderUuid) {
        super(playerRef, CustomPageLifetime.CanDismiss, PageData.CODEC);
        this.plugin = plugin;
        this.playerRef = playerRef;
        this.senderUuid = senderUuid;
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder cmd, @Nonnull UIEventBuilder event, @Nonnull Store<EntityStore> store) {
        cmd.append("Pages/Islandium/PrivateMessagePage.ui");

        // Close button
        event.addEventBinding(CustomUIEventBindingType.Activating, "#CloseButton", EventData.of("Action", "close"), false);

        // Back button
        event.addEventBinding(CustomUIEventBindingType.Activating, "#BackButton", EventData.of("Action", "back"), false);

        // Message field value tracking
        event.addEventBinding(CustomUIEventBindingType.ValueChanged, "#MessageField", EventData.of("@MessageText", "#MessageField.Value"), false);

        // Send button
        event.addEventBinding(CustomUIEventBindingType.Activating, "#SendButton", EventData.of("Action", "send"), false);

        // Build initial player list
        if (targetUuid != null && targetName != null) {
            showConversation(cmd);
        } else {
            buildOnlinePlayersList(cmd, event);
        }
    }

    private void buildOnlinePlayersList(UICommandBuilder cmd, UIEventBuilder event) {
        cmd.clear("#PlayerList");

        var onlinePlayers = plugin.getPlayerManager().getOnlinePlayersLocal();
        // Exclure le joueur lui-meme
        long count = onlinePlayers.stream().filter(p -> !p.getUniqueId().equals(senderUuid)).count();

        cmd.set("#OnlineCount.Text", count + " joueur" + (count > 1 ? "s" : "") + " en ligne");

        if (count == 0) {
            cmd.appendInline("#PlayerList", "Label { Text: \"Aucun autre joueur connecte\"; Anchor: (Height: 40); Style: (FontSize: 12, TextColor: #808080, HorizontalAlignment: Center, VerticalAlignment: Center); }");
        } else {
            int index = 0;
            PermissionService permService = plugin.getServiceManager().getPermissionService();

            for (var onlinePlayer : onlinePlayers) {
                if (onlinePlayer.getUniqueId().equals(senderUuid)) continue;

                String cardId = "PCard" + index;
                String playerName = onlinePlayer.getName();
                UUID playerUuid = onlinePlayer.getUniqueId();

                // Obtenir le rang pour la couleur
                String rankColor = "#ffffff";
                String rankName = "Joueur";
                try {
                    var perms = permService.getPlayerPermissions(playerUuid).join();
                    var primary = perms.getPrimaryRank();
                    if (primary != null) {
                        rankColor = primary.getColor();
                        rankName = primary.getDisplayName();
                    }
                } catch (Exception ignored) {}

                cmd.appendInline("#PlayerList",
                        "Button #" + cardId + " { Anchor: (Height: 46, Bottom: 5); Background: (Color: #151d28); Padding: (Horizontal: 12, Vertical: 5); LayoutMode: Top; }");

                cmd.appendInline("#" + cardId, "Label #Name { Anchor: (Height: 20); Style: (FontSize: 13, TextColor: " + rankColor + ", RenderBold: true, HorizontalAlignment: Center, VerticalAlignment: Center); }");
                cmd.appendInline("#" + cardId, "Label #Rank { Anchor: (Height: 16); Style: (FontSize: 10, TextColor: #808080, HorizontalAlignment: Center, VerticalAlignment: Center); }");

                cmd.set("#" + cardId + " #Name.Text", playerName);
                cmd.set("#" + cardId + " #Rank.Text", rankName);

                event.addEventBinding(CustomUIEventBindingType.Activating, "#" + cardId,
                        EventData.of("SelectPlayer", playerUuid.toString() + ":" + playerName), false);

                index++;
            }
        }
    }

    private void showConversation(UICommandBuilder cmd) {
        cmd.set("#PlayerListPanel.Visible", false);
        cmd.set("#ConversationPanel.Visible", true);
        cmd.set("#BackButton.Visible", true);
        cmd.set("#TargetName.Text", targetName);
        cmd.set("#StatusLabel.Text", "");
    }

    private void showPlayerList(UICommandBuilder cmd) {
        cmd.set("#PlayerListPanel.Visible", true);
        cmd.set("#ConversationPanel.Visible", false);
        cmd.set("#BackButton.Visible", false);
        targetUuid = null;
        targetName = null;
        formMessage = "";
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, @Nonnull PageData data) {
        super.handleDataEvent(ref, store, data);

        Player player = store.getComponent(ref, Player.getComponentType());

        UICommandBuilder cmd = new UICommandBuilder();
        UIEventBuilder event = new UIEventBuilder();

        // Track message field value
        if (data.messageText != null) formMessage = data.messageText;

        // Handle actions
        if (data.action != null) {
            switch (data.action) {
                case "close" -> {
                    return;
                }
                case "back" -> {
                    showPlayerList(cmd);
                    buildOnlinePlayersList(cmd, event);
                    sendUpdate(cmd, event, false);
                    return;
                }
                case "send" -> {
                    if (targetUuid != null && !formMessage.isBlank()) {
                        sendPrivateMessage(cmd, event, player);
                    } else if (formMessage.isBlank()) {
                        cmd.set("#StatusLabel.Text", "Veuillez entrer un message");
                        cmd.set("#StatusLabel.Style.TextColor", "#ff6b6b");
                        sendUpdate(cmd, event, false);
                    }
                    return;
                }
            }
        }

        // Handle player selection
        if (data.selectPlayer != null) {
            String[] parts = data.selectPlayer.split(":", 2);
            if (parts.length == 2) {
                try {
                    targetUuid = UUID.fromString(parts[0]);
                    targetName = parts[1];
                    formMessage = "";
                    showConversation(cmd);
                    sendUpdate(cmd, event, false);
                } catch (IllegalArgumentException ignored) {}
            }
            return;
        }
    }

    private void sendPrivateMessage(UICommandBuilder cmd, UIEventBuilder event, Player player) {
        String message = formMessage.trim();
        UUID to = targetUuid;
        String toName = targetName;

        plugin.getServiceManager().getCrossServerMessenger()
                .sendPrivateMessage(senderUuid, to, message)
                .thenAccept(sent -> {
                    UICommandBuilder cmd2 = new UICommandBuilder();
                    UIEventBuilder event2 = new UIEventBuilder();

                    if (sent) {
                        cmd2.set("#StatusLabel.Text", "Message envoye a " + toName + " !");
                        cmd2.set("#StatusLabel.Style.TextColor", "#4ade80");
                        cmd2.set("#MessageField.Value", "");
                        formMessage = "";

                        // Mettre a jour le last message sender pour /r
                        var senderOpt = plugin.getPlayerManager().getOnlinePlayer(senderUuid);
                        senderOpt.ifPresent(p -> p.setLastMessageSender(to));
                    } else {
                        cmd2.set("#StatusLabel.Text", toName + " n'est plus en ligne");
                        cmd2.set("#StatusLabel.Style.TextColor", "#ff6b6b");
                    }

                    sendUpdate(cmd2, event2, false);
                });
    }

    public static class PageData {
        public static final BuilderCodec<PageData> CODEC = BuilderCodec.builder(PageData.class, PageData::new)
                .addField(new KeyedCodec<>("Action", Codec.STRING), (d, v) -> d.action = v, d -> d.action)
                .addField(new KeyedCodec<>("SelectPlayer", Codec.STRING), (d, v) -> d.selectPlayer = v, d -> d.selectPlayer)
                .addField(new KeyedCodec<>("@MessageText", Codec.STRING), (d, v) -> d.messageText = v, d -> d.messageText)
                .build();

        public String action;
        public String selectPlayer;
        public String messageText;
    }
}
