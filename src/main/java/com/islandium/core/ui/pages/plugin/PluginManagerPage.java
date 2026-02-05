package com.islandium.core.ui.pages.plugin;

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
import com.hypixel.hytale.server.core.plugin.PluginBase;
import com.hypixel.hytale.server.core.plugin.PluginManager;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Page interactive de gestion des plugins.
 * Permet de voir tous les plugins charges avec recherche dynamique.
 */
public class PluginManagerPage extends InteractiveCustomUIPage<PluginManagerPage.PageData> {

    private final IslandiumPlugin plugin;
    private String searchFilter = "";
    private String selectedPluginId = null;
    private List<PluginInfo> allPlugins = new ArrayList<>();

    public PluginManagerPage(@Nonnull PlayerRef playerRef, IslandiumPlugin plugin) {
        super(playerRef, CustomPageLifetime.CanDismiss, PageData.CODEC);
        this.plugin = plugin;
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder cmd, @Nonnull UIEventBuilder event, @Nonnull Store<EntityStore> store) {
        cmd.append("Pages/Islandium/PluginManagerPage.ui");

        // Setup navigation bar
        NavBarHelper.setupBar(ref, cmd, event, store);

        // Events
        event.addEventBinding(CustomUIEventBindingType.Activating, "#BackButton", EventData.of("Action", "back"), false);
        event.addEventBinding(CustomUIEventBindingType.Activating, "#RefreshButton", EventData.of("Action", "refresh"), false);
        event.addEventBinding(CustomUIEventBindingType.ValueChanged, "#SearchField", EventData.of("@Search", "#SearchField.Value"), false);

        // Boutons du panel d'info
        event.addEventBinding(CustomUIEventBindingType.Activating, "#TogglePluginButton", EventData.of("Action", "toggle"), false);
        event.addEventBinding(CustomUIEventBindingType.Activating, "#ReloadPluginButton", EventData.of("Action", "reload"), false);

        // Charger les plugins
        loadPlugins();
        buildPluginList(cmd, event);
    }

    private void loadPlugins() {
        allPlugins.clear();

        // Recuperer les plugins via le plugin manager
        List<PluginBase> plugins = PluginManager.get().getPlugins();

        for (PluginBase p : plugins) {
            var manifest = p.getManifest();
            var identifier = p.getIdentifier();
            PluginInfo info = new PluginInfo(
                identifier.toString(),
                manifest.getName(),
                manifest.getGroup(),
                manifest.getVersion().toString(),
                manifest.getDescription() != null ? manifest.getDescription() : "",
                manifest.getMain(),
                p.isEnabled()
            );
            allPlugins.add(info);
        }

        // Trier par nom
        allPlugins.sort(Comparator.comparing(PluginInfo::name, String.CASE_INSENSITIVE_ORDER));
    }

    private List<PluginInfo> getFilteredPlugins() {
        if (searchFilter == null || searchFilter.isEmpty()) {
            return allPlugins;
        }

        String filter = searchFilter.toLowerCase();
        return allPlugins.stream()
            .filter(p -> p.name().toLowerCase().contains(filter)
                      || p.group().toLowerCase().contains(filter)
                      || p.description().toLowerCase().contains(filter))
            .collect(Collectors.toList());
    }

    private void buildPluginList(UICommandBuilder cmd, UIEventBuilder event) {
        cmd.clear("#PluginList");

        List<PluginInfo> filteredPlugins = getFilteredPlugins();

        // Mettre a jour le compteur
        cmd.set("#PluginCount.Text", filteredPlugins.size() + " plugin" + (filteredPlugins.size() > 1 ? "s" : ""));

        if (filteredPlugins.isEmpty()) {
            cmd.set("#PluginList.Visible", false);
            cmd.set("#NoPluginsMessage.Visible", true);
        } else {
            cmd.set("#PluginList.Visible", true);
            cmd.set("#NoPluginsMessage.Visible", false);

            int index = 0;
            for (PluginInfo p : filteredPlugins) {
                boolean isSelected = p.id().equals(selectedPluginId);
                String bgColor = isSelected ? "#2a3f5f" : (index % 2 == 0 ? "#121a26" : "#151d28");
                String statusColor = p.enabled() ? "#4a9f4a" : "#9f4a4a";
                String statusText = p.enabled() ? "Actif" : "Inactif";
                String rowId = "PluginRow" + index;

                // Structure simplifiee similaire a RankManagerPage
                String rowUi = "Button #" + rowId + " { Anchor: (Height: 32, Bottom: 2); Background: (Color: " + bgColor + "); Padding: (Horizontal: 8); Label #Lbl { Style: (FontSize: 12, VerticalAlignment: Center); } }";

                cmd.appendInline("#PluginList", rowUi);

                // Formater le texte avec toutes les infos sur une seule ligne
                String displayText = p.name() + " [" + p.group() + "] v" + p.version() + " - " + statusText;
                cmd.set("#" + rowId + " #Lbl.Text", displayText);
                cmd.set("#" + rowId + " #Lbl.Style.TextColor", statusColor);

                // Event de selection
                event.addEventBinding(CustomUIEventBindingType.Activating, "#" + rowId, EventData.of("SelectPlugin", p.id()), false);

                index++;
            }
        }
    }

    private void buildInfoPanel(UICommandBuilder cmd, PluginInfo p) {
        cmd.set("#PluginInfoPanel.Visible", true);
        cmd.set("#SelectedPluginName.Text", p.name());
        cmd.set("#InfoGroup.Text", p.group());
        cmd.set("#InfoVersion.Text", p.version());
        cmd.set("#InfoMain.Text", p.mainClass());
        cmd.set("#InfoDescription.Text", p.description().isEmpty() ? "Aucune description" : p.description());

        // Statut
        if (p.enabled()) {
            cmd.set("#StatusIndicator.Background.Color", "#2d5a2d");
            cmd.set("#StatusText.Text", "ACTIF");
            cmd.set("#TogglePluginButton.Text", "Desactiver");
        } else {
            cmd.set("#StatusIndicator.Background.Color", "#5a2d2d");
            cmd.set("#StatusText.Text", "INACTIF");
            cmd.set("#TogglePluginButton.Text", "Activer");
        }
    }

    @Override
    protected void sendUpdate(UICommandBuilder cmd, UIEventBuilder event, boolean force) {
        super.sendUpdate(cmd, event, force);
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

        // Gestion du champ de recherche
        if (data.search != null) {
            searchFilter = data.search;
            buildPluginList(cmd, event);
            sendUpdate(cmd, event, false);
            return;
        }

        // Actions
        if (data.action != null) {
            switch (data.action) {
                case "back" -> {
                    player.sendMessage(Message.raw("Fermez l'interface avec Echap"));
                    return;
                }
                case "refresh" -> {
                    loadPlugins();
                    selectedPluginId = null;
                    cmd.set("#PluginInfoPanel.Visible", false);
                    buildPluginList(cmd, event);
                    player.sendMessage(Message.raw("Plugins recharges."));
                }
                case "toggle" -> {
                    if (selectedPluginId != null) {
                        player.sendMessage(Message.raw("Le toggle de plugin n'est pas supporte par l'API Hytale actuelle."));
                        player.sendMessage(Message.raw("Utilisez la console du serveur pour gerer les plugins."));
                    }
                }
                case "reload" -> {
                    if (selectedPluginId != null) {
                        player.sendMessage(Message.raw("Le rechargement de plugin n'est pas supporte par l'API Hytale actuelle."));
                        player.sendMessage(Message.raw("Redemarrez le serveur pour appliquer les changements."));
                    }
                }
            }
        }

        // Selection d'un plugin
        if (data.selectPlugin != null) {
            selectedPluginId = data.selectPlugin;

            // Trouver le plugin
            Optional<PluginInfo> pluginOpt = allPlugins.stream()
                .filter(p -> p.id().equals(selectedPluginId))
                .findFirst();

            if (pluginOpt.isPresent()) {
                buildInfoPanel(cmd, pluginOpt.get());
            }

            // Rebuild list pour mettre a jour la selection
            buildPluginList(cmd, event);
        }

        sendUpdate(cmd, event, false);
    }

    // Data class for plugin info
    private record PluginInfo(
        String id,
        String name,
        String group,
        String version,
        String description,
        String mainClass,
        boolean enabled
    ) {}

    // Page data codec
    public static class PageData {
        public static final BuilderCodec<PageData> CODEC = BuilderCodec.builder(PageData.class, PageData::new)
            .addField(new KeyedCodec<>("Action", Codec.STRING), (d, v) -> d.action = v, d -> d.action)
            .addField(new KeyedCodec<>("SelectPlugin", Codec.STRING), (d, v) -> d.selectPlugin = v, d -> d.selectPlugin)
            .addField(new KeyedCodec<>("@Search", Codec.STRING), (d, v) -> d.search = v, d -> d.search)
            .addField(new KeyedCodec<>("NavBar", Codec.STRING), (d, v) -> d.navBar = v, d -> d.navBar)
            .build();

        public String action;
        public String selectPlugin;
        public String search;
        public String navBar;
    }
}
