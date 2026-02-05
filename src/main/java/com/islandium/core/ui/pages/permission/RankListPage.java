package com.islandium.core.ui.pages.permission;

import com.islandium.core.api.permission.PermissionService;
import com.islandium.core.api.permission.Rank;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.server.core.entity.entities.player.pages.BasicCustomUIPage;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import java.util.List;

/**
 * Page de liste des ranks avec interface graphique.
 */
public class RankListPage extends BasicCustomUIPage {

    private final PermissionService permissionService;

    public RankListPage(PlayerRef playerRef, PermissionService permissionService) {
        super(playerRef, CustomPageLifetime.CanDismiss);
        this.permissionService = permissionService;
    }

    @Override
    public void build(UICommandBuilder builder) {
        // Charger le fichier .ui externe
        builder.append("Pages/Islandium/RankListPage.ui");

        // Charger la liste des ranks
        List<Rank> ranks = permissionService.getAllRanks().join();

        if (ranks.isEmpty()) {
            builder.appendInline("#RankList", "Label { Text: \"Aucun rank configure\"; Anchor: (Height: 40); Style: (FontSize: 14, TextColor: #808080, HorizontalAlignment: Center, VerticalAlignment: Center); }");
        } else {
            int index = 0;
            for (Rank rank : ranks) {
                String selector = "#RankList[" + index + "]";
                String prefix = rank.getPrefix() != null ? rank.getPrefix() + " " : "";
                builder.appendInline("#RankList", "Group { Anchor: (Height: 35, Bottom: 5); LayoutMode: Left; Background: (Color: #151d28); Padding: (Horizontal: 10); Label #Name { FlexWeight: 1; Style: (FontSize: 14, VerticalAlignment: Center); } Label #Priority { Anchor: (Width: 80); Style: (FontSize: 11, TextColor: #808080, VerticalAlignment: Center); } }");
                builder.set(selector + " #Name.Text", prefix + rank.getDisplayName());
                builder.set(selector + " #Name.Style.TextColor", rank.getColor());
                builder.set(selector + " #Priority.Text", "Priorite: " + rank.getPriority());
                index++;
            }
        }
    }
}
