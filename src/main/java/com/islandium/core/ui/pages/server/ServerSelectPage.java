package com.islandium.core.ui.pages.server;

import com.islandium.core.IslandiumPlugin;
import com.islandium.core.server.ServerData;
import com.islandium.core.service.server.ServerService;
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
import java.util.Map;

/**
 * Page de selection de serveur pour les joueurs.
 * Affiche la liste des serveurs avec des boutons cliquables.
 */
public class ServerSelectPage extends InteractiveCustomUIPage<ServerSelectPage.PageData> {

    private final IslandiumPlugin plugin;
    private final ServerService serverService;
    private final String currentServerName;

    public ServerSelectPage(@Nonnull PlayerRef playerRef, @Nonnull IslandiumPlugin plugin) {
        super(playerRef, CustomPageLifetime.CanDismiss, PageData.CODEC);
        this.plugin = plugin;
        this.serverService = plugin.getServiceManager().getServerService();
        this.currentServerName = plugin.getConfigManager().getMainConfig().getServerName();
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder cmd, @Nonnull UIEventBuilder event, @Nonnull Store<EntityStore> store) {
        cmd.append("Pages/Islandium/ServerSelectPage.ui");

        event.addEventBinding(CustomUIEventBindingType.Activating, "#BackButton", EventData.of("Action", "back"), false);

        buildServerList(cmd, event);
    }

    private void buildServerList(UICommandBuilder cmd, UIEventBuilder event) {
        Map<String, ServerData> servers = serverService.getServers();

        if (servers.isEmpty()) {
            cmd.appendInline("#ServerList", "Label { Text: \"Aucun serveur disponible\"; Anchor: (Height: 40); Style: (FontSize: 14, TextColor: #808080); }");
            return;
        }

        int index = 0;
        for (Map.Entry<String, ServerData> entry : servers.entrySet()) {
            String name = entry.getKey();
            ServerData data = entry.getValue();
            boolean isCurrent = name.equalsIgnoreCase(currentServerName);
            String btnId = "Srv" + index;

            String displayText = isCurrent ? data.getDisplayName() + " (actuel)" : data.getDisplayName();
            String bgColor = isCurrent ? "#1a2a3a" : "#1e3a5f";
            String hoverColor = isCurrent ? "#1a2a3a" : "#2a4f7f";
            String pressColor = isCurrent ? "#1a2a3a" : "#153050";
            String textColor = isCurrent ? "#607080" : "#ffffff";

            cmd.appendInline("#ServerList",
                "TextButton #" + btnId + " { " +
                    "Anchor: (Height: 44, Bottom: 6); " +
                    "Text: \"" + displayText + "\"; " +
                    "Style: TextButtonStyle(" +
                        "Default: (Background: " + bgColor + ", LabelStyle: (FontSize: 15, TextColor: " + textColor + ", RenderBold: true))," +
                        "Hovered: (Background: " + hoverColor + ", LabelStyle: (FontSize: 15, TextColor: " + textColor + ", RenderBold: true))," +
                        "Pressed: (Background: " + pressColor + ", LabelStyle: (FontSize: 15, TextColor: " + textColor + ", RenderBold: true))," +
                    "); " +
                "}");

            if (!isCurrent) {
                event.addEventBinding(CustomUIEventBindingType.Activating, "#" + btnId, EventData.of("SelectServer", name), false);
            }

            index++;
        }
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, @Nonnull PageData data) {
        super.handleDataEvent(ref, store, data);

        Player player = store.getComponent(ref, Player.getComponentType());

        if (data.action != null) {
            if ("back".equals(data.action)) {
                return;
            }
        }

        if (data.selectServer != null) {
            ServerData server = serverService.getServer(data.selectServer);
            if (server != null) {
                player.getPlayerRef().referToServer(server.getHost(), server.getPort(), null);
            }
        }
    }

    // === Codec ===

    public static class PageData {
        public static final BuilderCodec<PageData> CODEC = BuilderCodec.builder(PageData.class, PageData::new)
            .addField(new KeyedCodec<>("Action", Codec.STRING), (d, v) -> d.action = v, d -> d.action)
            .addField(new KeyedCodec<>("SelectServer", Codec.STRING), (d, v) -> d.selectServer = v, d -> d.selectServer)
            .build();

        public String action;
        public String selectServer;
    }
}
