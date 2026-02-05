package com.islandium.core.ui.pages.permission;

import com.islandium.core.api.permission.PermissionService;
import com.islandium.core.api.permission.Rank;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.server.core.entity.entities.player.pages.BasicCustomUIPage;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

/**
 * Page d'edition d'un rank.
 */
public class RankEditorPage extends BasicCustomUIPage {

    private final PermissionService permissionService;
    private final Rank rank; // null pour creation

    public RankEditorPage(PlayerRef playerRef, PermissionService permissionService, @Nullable Rank rank) {
        super(playerRef, CustomPageLifetime.CanDismiss);
        this.permissionService = permissionService;
        this.rank = rank;
    }

    @Override
    public void build(UICommandBuilder builder) {
        String title = rank == null ? "Creer un Rank" : "Editer: " + rank.getDisplayName();
        boolean isEdit = rank != null;

        builder.appendInline("", """
            Group #RankEditorRoot {
                Anchor: (Fill: true);
                Background: (Color: #000000B0);

                Group #Container {
                    Anchor: (Width: 600, Height: 580, Horizontal: 0, Vertical: 0);
                    Background: (Color: #1a2030);
                    OutlineColor: #3d4f6f;
                    OutlineSize: 2;
                    LayoutMode: Top;
                    Padding: (Full: 20);

                    // Header
                    Label #Title {
                        Text: "%s";
                        Style: (
                            FontSize: 22,
                            TextColor: #FFD700,
                            RenderBold: true,
                            HorizontalAlignment: Center
                        );
                        Anchor: (Height: 40, Bottom: 20);
                    }

                    // Formulaire
                    Group #Form {
                        Anchor: (Height: 220);
                        LayoutMode: Top;
                        Padding: (Horizontal: 20);

                        // Nom
                        Group #NameRow {
                            Anchor: (Height: 45, Bottom: 10);
                            LayoutMode: Left;

                            Label {
                                Text: "Nom technique:";
                                Anchor: (Width: 150);
                                Style: (FontSize: 14, TextColor: #96a9be, VerticalAlignment: Center);
                            }

                            TextField #NameField {
                                FlexWeight: 1;
                                Anchor: (Height: 38);
                                Style: (FontSize: 14, TextColor: #FFFFFF);
                                %s
                            }
                        }

                        // Display Name
                        Group #DisplayRow {
                            Anchor: (Height: 45, Bottom: 10);
                            LayoutMode: Left;

                            Label {
                                Text: "Nom d'affichage:";
                                Anchor: (Width: 150);
                                Style: (FontSize: 14, TextColor: #96a9be, VerticalAlignment: Center);
                            }

                            TextField #DisplayField {
                                FlexWeight: 1;
                                Anchor: (Height: 38);
                                Style: (FontSize: 14, TextColor: #FFFFFF);
                            }
                        }

                        // Prefix
                        Group #PrefixRow {
                            Anchor: (Height: 45, Bottom: 10);
                            LayoutMode: Left;

                            Label {
                                Text: "Prefix:";
                                Anchor: (Width: 150);
                                Style: (FontSize: 14, TextColor: #96a9be, VerticalAlignment: Center);
                            }

                            TextField #PrefixField {
                                FlexWeight: 1;
                                Anchor: (Height: 38);
                                Style: (FontSize: 14, TextColor: #FFFFFF);
                            }
                        }

                        // Couleur et Priorite
                        Group #ColorPrioRow {
                            Anchor: (Height: 45, Bottom: 10);
                            LayoutMode: Left;

                            Label {
                                Text: "Couleur (#hex):";
                                Anchor: (Width: 150);
                                Style: (FontSize: 14, TextColor: #96a9be, VerticalAlignment: Center);
                            }

                            TextField #ColorField {
                                Anchor: (Width: 100, Height: 38, Right: 20);
                                Style: (FontSize: 14, TextColor: #FFFFFF);
                            }

                            Label {
                                Text: "Priorite:";
                                Anchor: (Width: 70);
                                Style: (FontSize: 14, TextColor: #96a9be, VerticalAlignment: Center);
                            }

                            NumberField #PriorityField {
                                FlexWeight: 1;
                                Anchor: (Height: 38);
                                Style: (FontSize: 14, TextColor: #FFFFFF);
                            }
                        }
                    }

                    // Separateur
                    Group #Sep1 {
                        Anchor: (Height: 2, Bottom: 15);
                        Background: (Color: #3d4f6f);
                    }

                    // Section Permissions
                    Label #PermTitle {
                        Text: "Permissions (%d)";
                        Style: (FontSize: 16, TextColor: #FFFFFF, RenderBold: true);
                        Anchor: (Height: 30);
                    }

                    Group #PermList {
                        FlexWeight: 1;
                        LayoutMode: TopScrolling;
                        Padding: (Right: 10);
                        Background: (Color: #151d28);
                    }

                    // Ajouter permission
                    Group #AddPermRow {
                        Anchor: (Height: 45, Top: 10);
                        LayoutMode: Left;

                        TextField #NewPermField {
                            FlexWeight: 1;
                            Anchor: (Height: 38, Right: 10);
                            Style: (FontSize: 14, TextColor: #FFFFFF);
                        }

                        TextButton #AddPermBtn {
                            Text: "+ Ajouter";
                            Anchor: (Width: 100, Height: 38);
                            Style: (
                                Default: (
                                    Background: (Color: #4CAF50),
                                    LabelStyle: (FontSize: 13, TextColor: #FFFFFF, HorizontalAlignment: Center, VerticalAlignment: Center)
                                ),
                                Hovered: (
                                    Background: (Color: #66BB6A),
                                    LabelStyle: (FontSize: 13, TextColor: #FFFFFF, HorizontalAlignment: Center, VerticalAlignment: Center)
                                )
                            );
                        }
                    }

                    // Separateur
                    Group #Sep2 {
                        Anchor: (Height: 2, Top: 15, Bottom: 15);
                        Background: (Color: #3d4f6f);
                    }

                    // Boutons action
                    Group #Actions {
                        Anchor: (Height: 45);
                        LayoutMode: Left;

                        TextButton #SaveBtn {
                            Text: "Sauvegarder";
                            Anchor: (Width: 130, Height: 40, Right: 10);
                            Style: (
                                Default: (
                                    Background: (Color: #2196F3),
                                    LabelStyle: (FontSize: 14, TextColor: #FFFFFF, RenderBold: true, HorizontalAlignment: Center, VerticalAlignment: Center)
                                ),
                                Hovered: (
                                    Background: (Color: #42A5F5),
                                    LabelStyle: (FontSize: 14, TextColor: #FFFFFF, RenderBold: true, HorizontalAlignment: Center, VerticalAlignment: Center)
                                )
                            );
                        }

                        TextButton #CancelBtn {
                            Text: "Annuler";
                            Anchor: (Width: 100, Height: 40);
                            Style: (
                                Default: (
                                    Background: (Color: #4a5a70),
                                    LabelStyle: (FontSize: 14, TextColor: #bfcdd5, HorizontalAlignment: Center, VerticalAlignment: Center)
                                ),
                                Hovered: (
                                    Background: (Color: #5a6a80),
                                    LabelStyle: (FontSize: 14, TextColor: #FFFFFF, HorizontalAlignment: Center, VerticalAlignment: Center)
                                )
                            );
                        }

                        Group { FlexWeight: 1; }
                    }
                }
            }
        """.formatted(
                title,
                isEdit ? "Enabled: false;" : "",
                isEdit ? rank.getDirectPermissions().size() : 0
        ));

        // Remplir les champs si edition
        if (isEdit) {
            builder.set("#NameField.Text", rank.getName());
            builder.set("#DisplayField.Text", rank.getDisplayName());
            if (rank.getPrefix() != null) {
                builder.set("#PrefixField.Text", rank.getPrefix());
            }
            builder.set("#ColorField.Text", rank.getColor());
            builder.set("#PriorityField.Value", rank.getPriority());

            // Afficher les permissions
            Set<String> perms = rank.getDirectPermissions();
            int idx = 0;
            for (String perm : perms) {
                addPermissionRow(builder, perm, idx++);
            }

            if (perms.isEmpty()) {
                builder.appendInline("#PermList", """
                    Label #EmptyPerms {
                        Text: "Aucune permission";
                        Anchor: (Height: 40);
                        Style: (FontSize: 13, TextColor: #96a9be, HorizontalAlignment: Center, VerticalAlignment: Center);
                    }
                """);
            }
        }
    }

    private void addPermissionRow(UICommandBuilder builder, String permission, int index) {
        builder.appendInline("#PermList", """
            Group #Perm_%d {
                Anchor: (Height: 35, Bottom: 5);
                LayoutMode: Left;
                Padding: (Horizontal: 10);

                Label {
                    Text: "%s";
                    FlexWeight: 1;
                    Style: (FontSize: 13, TextColor: #bfcdd5, VerticalAlignment: Center);
                }

                TextButton #RemovePerm_%d {
                    Text: "X";
                    Anchor: (Width: 28, Height: 28, Vertical: 0);
                    Style: (
                        Default: (
                            Background: (Color: #5a3030),
                            LabelStyle: (FontSize: 12, TextColor: #ff6b6b, RenderBold: true, HorizontalAlignment: Center, VerticalAlignment: Center)
                        ),
                        Hovered: (
                            Background: (Color: #8B0000),
                            LabelStyle: (FontSize: 12, TextColor: #FFFFFF, RenderBold: true, HorizontalAlignment: Center, VerticalAlignment: Center)
                        )
                    );
                }
            }
        """.formatted(index, permission, index));
    }
}
