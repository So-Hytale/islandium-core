package com.islandium.core.ui.pages.permission;

import com.islandium.core.api.permission.PermissionService;
import com.islandium.core.api.permission.PlayerPermissions;
import com.islandium.core.api.permission.Rank;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.server.core.entity.entities.player.pages.BasicCustomUIPage;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import java.util.Set;
import java.util.UUID;

/**
 * Page de gestion des permissions d'un joueur.
 */
public class PlayerPermissionsPage extends BasicCustomUIPage {

    private final PermissionService permissionService;
    private final UUID targetUuid;
    private final String targetName;

    public PlayerPermissionsPage(
            PlayerRef playerRef,
            PermissionService permissionService,
            UUID targetUuid,
            String targetName
    ) {
        super(playerRef, CustomPageLifetime.CanDismiss);
        this.permissionService = permissionService;
        this.targetUuid = targetUuid;
        this.targetName = targetName;
    }

    @Override
    public void build(UICommandBuilder builder) {
        // Charger le fichier .ui externe
        builder.append("Pages/Islandium/PlayerPermissionsPage.ui");

        // Mettre à jour le nom du joueur
        builder.set("#PlayerName.Text", targetName);

        // Charger les donnees du joueur
        PlayerPermissions perms = permissionService.getPlayerPermissions(targetUuid).join();

        // Afficher les ranks
        Set<Rank> ranks = perms.getRanks();
        Rank primaryRank = perms.getPrimaryRank();

        if (ranks.isEmpty()) {
            builder.appendInline("#RanksList", "Label { Text: \"Aucun rank\"; Anchor: (Height: 25); Style: (FontSize: 12, TextColor: #808080, HorizontalAlignment: Center); }");
        } else {
            int rankIndex = 0;
            for (Rank rank : ranks) {
                String selector = "#RanksList[" + rankIndex + "]";
                String badge = rank.equals(primaryRank) ? " [Principal]" : "";
                builder.appendInline("#RanksList", "Label #RankEntry { Anchor: (Height: 22); Style: (FontSize: 12); }");
                builder.set(selector + " #RankEntry.Text", rank.getDisplayName() + badge);
                builder.set(selector + " #RankEntry.Style.TextColor", rank.getColor());
                rankIndex++;
            }
        }

        // Afficher les permissions personnelles
        Set<String> personalPerms = perms.getPersonalPermissions();
        if (personalPerms.isEmpty()) {
            builder.appendInline("#PermsList", "Label { Text: \"Aucune\"; Anchor: (Height: 25); Style: (FontSize: 12, TextColor: #808080, HorizontalAlignment: Center); }");
        } else {
            int permIndex = 0;
            for (String perm : personalPerms) {
                String selector = "#PermsList[" + permIndex + "]";
                builder.appendInline("#PermsList", "Label #PermEntry { Anchor: (Height: 20); Style: (FontSize: 11, TextColor: #bfcdd5); }");
                builder.set(selector + " #PermEntry.Text", perm);
                permIndex++;
            }
        }
    }
}
