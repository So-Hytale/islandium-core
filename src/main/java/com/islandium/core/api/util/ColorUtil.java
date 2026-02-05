package com.islandium.core.api.util;

import com.hypixel.hytale.server.core.Message;
import org.jetbrains.annotations.NotNull;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utilitaires pour les couleurs et le formatage de texte.
 */
public final class ColorUtil {

    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");
    private static final Pattern COLOR_CODE_PATTERN = Pattern.compile("&([0-9a-fk-or])");
    private static final Pattern STRIP_PATTERN = Pattern.compile("&[0-9a-fk-or]|&#[A-Fa-f0-9]{6}");
    private static final Pattern SPLIT_PATTERN = Pattern.compile("(&#[A-Fa-f0-9]{6}|&[0-9a-fk-or])");

    private ColorUtil() {}

    /**
     * Parse un message avec codes couleur en Message Hytale.
     * Supporte &c, &a, etc. et &#RRGGBB pour les couleurs hex.
     */
    @NotNull
    public static Message parse(@NotNull String message) {
        if (message == null || message.isEmpty()) {
            return Message.raw("");
        }

        // Remplacer § par & pour compatibilité
        message = message.replace('§', '&');

        // Séparer le message par codes couleur
        String[] parts = SPLIT_PATTERN.split(message);
        Matcher matcher = SPLIT_PATTERN.matcher(message);

        java.util.List<String> codes = new java.util.ArrayList<>();
        while (matcher.find()) {
            codes.add(matcher.group());
        }

        // Si pas de codes couleur, retourner le message brut
        if (codes.isEmpty()) {
            return Message.raw(message);
        }

        // Construire le message avec couleurs
        Message[] messages = new Message[parts.length];
        String currentColor = "#FFFFFF"; // Blanc par défaut

        int codeIndex = 0;
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            if (!part.isEmpty()) {
                messages[i] = Message.raw(part).color(currentColor);
            } else {
                messages[i] = Message.raw("");
            }

            // Appliquer le prochain code couleur
            if (codeIndex < codes.size()) {
                currentColor = codeToHex(codes.get(codeIndex));
                codeIndex++;
            }
        }

        return Message.join(messages);
    }

    /**
     * Convertit un code couleur (&c, &#RRGGBB) en hex.
     */
    @NotNull
    public static String codeToHex(@NotNull String code) {
        if (code.startsWith("&#")) {
            return "#" + code.substring(2);
        }

        if (code.length() == 2 && code.charAt(0) == '&') {
            char c = Character.toLowerCase(code.charAt(1));
            return switch (c) {
                case '0' -> "#000000"; // Noir
                case '1' -> "#0000AA"; // Bleu foncé
                case '2' -> "#00AA00"; // Vert foncé
                case '3' -> "#00AAAA"; // Cyan foncé
                case '4' -> "#AA0000"; // Rouge foncé
                case '5' -> "#AA00AA"; // Violet
                case '6' -> "#FFAA00"; // Or
                case '7' -> "#AAAAAA"; // Gris
                case '8' -> "#555555"; // Gris foncé
                case '9' -> "#5555FF"; // Bleu
                case 'a' -> "#55FF55"; // Vert clair
                case 'b' -> "#55FFFF"; // Cyan
                case 'c' -> "#FF5555"; // Rouge
                case 'd' -> "#FF55FF"; // Rose
                case 'e' -> "#FFFF55"; // Jaune
                case 'f' -> "#FFFFFF"; // Blanc
                default -> "#FFFFFF";
            };
        }

        return "#FFFFFF";
    }

    /**
     * Traduit les codes couleur & en codes natifs (legacy, supprime les codes).
     */
    @NotNull
    public static String translateColorCodes(@NotNull String text) {
        return stripColors(text);
    }

    /**
     * Supprime tous les codes couleur d'un texte.
     */
    @NotNull
    public static String stripColors(@NotNull String text) {
        return STRIP_PATTERN.matcher(text).replaceAll("");
    }

    /**
     * Convertit un code hex en composants RGB.
     */
    public static int[] hexToRgb(@NotNull String hex) {
        hex = hex.replace("#", "");
        return new int[] {
                Integer.parseInt(hex.substring(0, 2), 16),
                Integer.parseInt(hex.substring(2, 4), 16),
                Integer.parseInt(hex.substring(4, 6), 16)
        };
    }
}
