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
import com.hypixel.hytale.server.core.Message;
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
 * Page interactive de gestion des serveurs (admin).
 */
public class ServerManagerPage extends InteractiveCustomUIPage<ServerManagerPage.PageData> {

    private final IslandiumPlugin plugin;
    private final ServerService serverService;

    private String selectedServer = null;
    private boolean createMode = false;

    // Valeurs des champs (mises a jour via ValueChanged)
    private String formHost = "";
    private String formPort = "25565";
    private String formDisplayName = "";
    private String formNewName = "";
    private String formNewHost = "";
    private String formNewPort = "25565";
    private String formNewDisplayName = "";

    public ServerManagerPage(@Nonnull PlayerRef playerRef, @Nonnull IslandiumPlugin plugin) {
        super(playerRef, CustomPageLifetime.CanDismiss, PageData.CODEC);
        this.plugin = plugin;
        this.serverService = plugin.getServiceManager().getServerService();
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder cmd, @Nonnull UIEventBuilder event, @Nonnull Store<EntityStore> store) {
        cmd.append("Pages/Islandium/ServerManagerPage.ui");

        // Boutons
        event.addEventBinding(CustomUIEventBindingType.Activating, "#BackButton", EventData.of("Action", "back"), false);
        event.addEventBinding(CustomUIEventBindingType.Activating, "#CreateButton", EventData.of("Action", "showCreate"), false);
        event.addEventBinding(CustomUIEventBindingType.Activating, "#SaveButton", EventData.of("Action", "save"), false);
        event.addEventBinding(CustomUIEventBindingType.Activating, "#DeleteButton", EventData.of("Action", "delete"), false);
        event.addEventBinding(CustomUIEventBindingType.Activating, "#ConfirmCreateButton", EventData.of("Action", "confirmCreate"), false);
        event.addEventBinding(CustomUIEventBindingType.Activating, "#CancelCreateButton", EventData.of("Action", "cancelCreate"), false);

        // Champs edition
        event.addEventBinding(CustomUIEventBindingType.ValueChanged, "#HostField", EventData.of("@Host", "#HostField.Value"), false);
        event.addEventBinding(CustomUIEventBindingType.ValueChanged, "#PortField", EventData.of("@Port", "#PortField.Value"), false);
        event.addEventBinding(CustomUIEventBindingType.ValueChanged, "#DisplayNameField", EventData.of("@DisplayName", "#DisplayNameField.Value"), false);

        // Champs creation
        event.addEventBinding(CustomUIEventBindingType.ValueChanged, "#NewNameField", EventData.of("@NewName", "#NewNameField.Value"), false);
        event.addEventBinding(CustomUIEventBindingType.ValueChanged, "#NewHostField", EventData.of("@NewHost", "#NewHostField.Value"), false);
        event.addEventBinding(CustomUIEventBindingType.ValueChanged, "#NewPortField", EventData.of("@NewPort", "#NewPortField.Value"), false);
        event.addEventBinding(CustomUIEventBindingType.ValueChanged, "#NewDisplayNameField", EventData.of("@NewDisplayName", "#NewDisplayNameField.Value"), false);

        buildServerList(cmd, event);
    }

    private void buildServerList(UICommandBuilder cmd, UIEventBuilder event) {
        cmd.clear("#ServerList");

        Map<String, ServerData> servers = serverService.getServers();
        String currentServerName = plugin.getConfigManager().getMainConfig().getServerName();

        if (servers.isEmpty()) {
            cmd.appendInline("#ServerList", "Label #EmptyLabel { Text: \"Aucun serveur\"; Anchor: (Height: 30); Style: (FontSize: 12, TextColor: #808080, HorizontalAlignment: Center); }");
        } else {
            int index = 0;
            for (Map.Entry<String, ServerData> entry : servers.entrySet()) {
                String name = entry.getKey();
                ServerData data = entry.getValue();
                boolean isSelected = name.equals(selectedServer);
                boolean isCurrent = name.equalsIgnoreCase(currentServerName);
                String bgColor = isSelected ? "#2a3f5f" : "#151d28";
                String textColor = isCurrent ? "#4fc3f7" : "#bfcdd5";
                String btnId = "SrvBtn" + index;

                String displayText = isCurrent ? data.getDisplayName() + " (actuel)" : data.getDisplayName();

                cmd.appendInline("#ServerList", "Button #" + btnId + " { Anchor: (Height: 32, Bottom: 3); Background: (Color: " + bgColor + "); Padding: (Horizontal: 8); Label #Lbl { Style: (FontSize: 13, VerticalAlignment: Center); } }");
                cmd.set("#" + btnId + " #Lbl.Text", displayText);
                cmd.set("#" + btnId + " #Lbl.Style.TextColor", textColor);

                event.addEventBinding(CustomUIEventBindingType.Activating, "#" + btnId, EventData.of("SelectServer", name), false);
                index++;
            }
        }
    }

    private void buildEditor(UICommandBuilder cmd, ServerData data) {
        cmd.set("#EditorTitle.Text", "Edition: " + data.getDisplayName());
        cmd.set("#EditorContent.Visible", true);
        cmd.set("#CreateContent.Visible", false);
        cmd.set("#NoServerSelected.Visible", false);

        cmd.set("#HostField.Value", data.getHost());
        cmd.set("#PortField.Value", String.valueOf(data.getPort()));
        cmd.set("#DisplayNameField.Value", data.getDisplayName());

        formHost = data.getHost();
        formPort = String.valueOf(data.getPort());
        formDisplayName = data.getDisplayName();
    }

    private void showCreateMode(UICommandBuilder cmd) {
        createMode = true;
        cmd.set("#EditorTitle.Text", "Nouveau Serveur");
        cmd.set("#EditorContent.Visible", false);
        cmd.set("#CreateContent.Visible", true);
        cmd.set("#NoServerSelected.Visible", false);

        cmd.set("#NewNameField.Value", "");
        cmd.set("#NewHostField.Value", "");
        cmd.set("#NewPortField.Value", "25565");
        cmd.set("#NewDisplayNameField.Value", "");

        formNewName = "";
        formNewHost = "";
        formNewPort = "25565";
        formNewDisplayName = "";
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, @Nonnull PageData data) {
        super.handleDataEvent(ref, store, data);

        Player player = store.getComponent(ref, Player.getComponentType());

        UICommandBuilder cmd = new UICommandBuilder();
        UIEventBuilder event = new UIEventBuilder();

        // Stocker les valeurs des champs
        if (data.host != null) formHost = data.host;
        if (data.port != null) formPort = data.port;
        if (data.displayName != null) formDisplayName = data.displayName;
        if (data.newName != null) formNewName = data.newName;
        if (data.newHost != null) formNewHost = data.newHost;
        if (data.newPort != null) formNewPort = data.newPort;
        if (data.newDisplayName != null) formNewDisplayName = data.newDisplayName;

        // Actions
        if (data.action != null) {
            switch (data.action) {
                case "back" -> {
                    player.sendMessage(Message.raw("Fermez l'interface avec Echap"));
                    return;
                }
                case "showCreate" -> {
                    selectedServer = null;
                    showCreateMode(cmd);
                    buildServerList(cmd, event);
                    sendUpdate(cmd, event, false);
                    return;
                }
                case "cancelCreate" -> {
                    createMode = false;
                    cmd.set("#CreateContent.Visible", false);
                    cmd.set("#EditorTitle.Text", "Selectionnez un serveur");
                    cmd.set("#NoServerSelected.Visible", true);
                    sendUpdate(cmd, event, false);
                    return;
                }
                case "confirmCreate" -> {
                    if (formNewName.isBlank() || formNewHost.isBlank()) {
                        player.sendMessage(Message.raw("Veuillez remplir le nom et le host."));
                        return;
                    }

                    if (serverService.exists(formNewName)) {
                        player.sendMessage(Message.raw("Le serveur '" + formNewName + "' existe deja!"));
                        return;
                    }

                    int port = parsePort(formNewPort);
                    if (port < 1 || port > 65535) {
                        player.sendMessage(Message.raw("Port invalide (1-65535)!"));
                        return;
                    }

                    String displayName = formNewDisplayName.isBlank() ? formNewName : formNewDisplayName;
                    serverService.addServer(formNewName, formNewHost, port, displayName).join();
                    player.sendMessage(Message.raw("Serveur '" + displayName + "' cree!"));

                    createMode = false;
                    selectedServer = formNewName.toLowerCase();
                    buildServerList(cmd, event);

                    ServerData created = serverService.getServer(selectedServer);
                    if (created != null) {
                        buildEditor(cmd, created);
                    }

                    sendUpdate(cmd, event, false);
                    return;
                }
                case "save" -> {
                    if (selectedServer != null) {
                        ServerData existing = serverService.getServer(selectedServer);
                        if (existing != null) {
                            int port = parsePort(formPort);
                            if (port < 1 || port > 65535) {
                                player.sendMessage(Message.raw("Port invalide (1-65535)!"));
                                return;
                            }

                            existing.setHost(!formHost.isBlank() ? formHost : existing.getHost());
                            existing.setPort(port);
                            existing.setDisplayName(!formDisplayName.isBlank() ? formDisplayName : existing.getDisplayName());

                            serverService.updateServer(existing).join();
                            player.sendMessage(Message.raw("Serveur '" + existing.getDisplayName() + "' sauvegarde!"));

                            buildServerList(cmd, event);
                            sendUpdate(cmd, event, false);
                        }
                    }
                    return;
                }
                case "delete" -> {
                    if (selectedServer != null) {
                        ServerData existing = serverService.getServer(selectedServer);
                        if (existing != null) {
                            serverService.removeServer(selectedServer).join();
                            player.sendMessage(Message.raw("Serveur '" + existing.getDisplayName() + "' supprime!"));

                            selectedServer = null;
                            cmd.set("#EditorContent.Visible", false);
                            cmd.set("#EditorTitle.Text", "Selectionnez un serveur");
                            cmd.set("#NoServerSelected.Visible", true);
                            buildServerList(cmd, event);
                            sendUpdate(cmd, event, false);
                        }
                    }
                    return;
                }
            }
        }

        // Selection d'un serveur
        if (data.selectServer != null) {
            selectedServer = data.selectServer;
            createMode = false;
            ServerData server = serverService.getServer(selectedServer);
            if (server != null) {
                buildServerList(cmd, event);
                buildEditor(cmd, server);
                sendUpdate(cmd, event, false);
            }
        }
    }

    private int parsePort(String portStr) {
        try {
            return Integer.parseInt(portStr);
        } catch (NumberFormatException e) {
            return 25565;
        }
    }

    // === Codec ===

    public static class PageData {
        public static final BuilderCodec<PageData> CODEC = BuilderCodec.builder(PageData.class, PageData::new)
            .addField(new KeyedCodec<>("Action", Codec.STRING), (d, v) -> d.action = v, d -> d.action)
            .addField(new KeyedCodec<>("SelectServer", Codec.STRING), (d, v) -> d.selectServer = v, d -> d.selectServer)
            .addField(new KeyedCodec<>("@Host", Codec.STRING), (d, v) -> d.host = v, d -> d.host)
            .addField(new KeyedCodec<>("@Port", Codec.STRING), (d, v) -> d.port = v, d -> d.port)
            .addField(new KeyedCodec<>("@DisplayName", Codec.STRING), (d, v) -> d.displayName = v, d -> d.displayName)
            .addField(new KeyedCodec<>("@NewName", Codec.STRING), (d, v) -> d.newName = v, d -> d.newName)
            .addField(new KeyedCodec<>("@NewHost", Codec.STRING), (d, v) -> d.newHost = v, d -> d.newHost)
            .addField(new KeyedCodec<>("@NewPort", Codec.STRING), (d, v) -> d.newPort = v, d -> d.newPort)
            .addField(new KeyedCodec<>("@NewDisplayName", Codec.STRING), (d, v) -> d.newDisplayName = v, d -> d.newDisplayName)
            .build();

        public String action;
        public String selectServer;
        public String host;
        public String port;
        public String displayName;
        public String newName;
        public String newHost;
        public String newPort;
        public String newDisplayName;
    }
}
