package com.islandium.core.ui.pages.world;

import com.islandium.core.IslandiumPlugin;
import com.islandium.core.ui.NavBarHelper;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.WorldConfig;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import com.islandium.core.world.FlatWorldGenerator;

import javax.annotation.Nonnull;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.logging.Level;

/**
 * Page interactive de gestion des mondes.
 * Permet de visualiser, configurer et gerer les mondes du serveur.
 */
public class WorldManagerPage extends InteractiveCustomUIPage<WorldManagerPage.PageData> {

    private final IslandiumPlugin plugin;
    private String selectedWorldName = null;
    private boolean createMode = false;
    private String activeTab = "info"; // "info" ou "players"

    // Valeurs des champs stockees cote serveur
    private String formWeather = "";
    private String formGameMode = "";
    private String formTime = "";
    private String formDayCount = "";
    private String formDayDuration = "";
    private String formNightDuration = "";
    private String formTimeDilation = "";
    private String formNewWorldName = "";
    private String formNewWorldSeed = "";
    private String formNewWorldGen = "";

    // Toggles pour edition
    private boolean formPvp = false;
    private boolean formFallDamage = true;
    private boolean formNpcSpawn = true;
    private boolean formTimePaused = false;
    private boolean formWeatherLocked = false;
    private boolean formBlockTick = true;
    private boolean formSaveChunks = true;

    // Toggles pour creation (pre-parametres)
    private boolean newPvp = true;
    private boolean newFallDamage = true;
    private boolean newNpcSpawn = true;
    private boolean newBlockTick = true;
    private boolean newSaveChunks = true;

    // Type de monde pour creation: "normal", "void", "island", "flat"
    private String newWorldType = "normal";

    // Preset flat selectionne: "grass", "stone", "sand", "snow"
    private String flatPreset = "grass";

    public WorldManagerPage(@Nonnull PlayerRef playerRef, IslandiumPlugin plugin) {
        super(playerRef, CustomPageLifetime.CanDismiss, PageData.CODEC);
        this.plugin = plugin;
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder cmd, @Nonnull UIEventBuilder event, @Nonnull Store<EntityStore> store) {
        cmd.append("Pages/Islandium/WorldManagerPage.ui");

        // Setup navigation bar
        NavBarHelper.setupBar(ref, cmd, event, store);

        // Events globaux - Boutons
        event.addEventBinding(CustomUIEventBindingType.Activating, "#BackButton", EventData.of("Action", "back"), false);
        event.addEventBinding(CustomUIEventBindingType.Activating, "#CreateWorldButton", EventData.of("Action", "showCreate"), false);
        event.addEventBinding(CustomUIEventBindingType.Activating, "#CancelCreateButton", EventData.of("Action", "cancelCreate"), false);
        event.addEventBinding(CustomUIEventBindingType.Activating, "#ConfirmCreateButton", EventData.of("Action", "confirmCreate"), false);
        event.addEventBinding(CustomUIEventBindingType.Activating, "#ApplySettingsButton", EventData.of("Action", "applySettings"), false);
        event.addEventBinding(CustomUIEventBindingType.Activating, "#TeleportButton", EventData.of("Action", "teleport"), false);
        event.addEventBinding(CustomUIEventBindingType.Activating, "#SaveWorldButton", EventData.of("Action", "saveWorld"), false);
        event.addEventBinding(CustomUIEventBindingType.Activating, "#SetSpawnButton", EventData.of("Action", "setSpawn"), false);
        event.addEventBinding(CustomUIEventBindingType.Activating, "#RefreshButton", EventData.of("Action", "refresh"), false);
        event.addEventBinding(CustomUIEventBindingType.Activating, "#DeleteWorldButton", EventData.of("Action", "deleteWorld"), false);
        event.addEventBinding(CustomUIEventBindingType.Activating, "#TpAllHereButton", EventData.of("Action", "tpAllHere"), false);

        // Tabs pour basculer entre Info et Joueurs
        event.addEventBinding(CustomUIEventBindingType.Activating, "#TabInfo", EventData.of("Tab", "info"), false);
        event.addEventBinding(CustomUIEventBindingType.Activating, "#TabPlayers", EventData.of("Tab", "players"), false);

        // Actions sur les joueurs
        event.addEventBinding(CustomUIEventBindingType.Activating, "#KickAllButton", EventData.of("Action", "kickAll"), false);
        event.addEventBinding(CustomUIEventBindingType.Activating, "#TpAllToMeButton", EventData.of("Action", "tpAllToMe"), false);

        // Champs de texte
        event.addEventBinding(CustomUIEventBindingType.ValueChanged, "#TimeField", EventData.of("@Time", "#TimeField.Value"), false);
        event.addEventBinding(CustomUIEventBindingType.ValueChanged, "#DayCountField", EventData.of("@DayCount", "#DayCountField.Value"), false);
        event.addEventBinding(CustomUIEventBindingType.ValueChanged, "#DayDurationField", EventData.of("@DayDuration", "#DayDurationField.Value"), false);
        event.addEventBinding(CustomUIEventBindingType.ValueChanged, "#NightDurationField", EventData.of("@NightDuration", "#NightDurationField.Value"), false);
        event.addEventBinding(CustomUIEventBindingType.ValueChanged, "#TimeDilationField", EventData.of("@TimeDilation", "#TimeDilationField.Value"), false);
        event.addEventBinding(CustomUIEventBindingType.ValueChanged, "#GameModeField", EventData.of("@GameMode", "#GameModeField.Value"), false);

        // Boutons de meteo
        event.addEventBinding(CustomUIEventBindingType.Activating, "#WeatherClear", EventData.of("Weather", "Clear"), false);
        event.addEventBinding(CustomUIEventBindingType.Activating, "#WeatherCloudy", EventData.of("Weather", "Cloudy"), false);
        event.addEventBinding(CustomUIEventBindingType.Activating, "#WeatherRainLight", EventData.of("Weather", "RainLight"), false);
        event.addEventBinding(CustomUIEventBindingType.Activating, "#WeatherRain", EventData.of("Weather", "Rain"), false);
        event.addEventBinding(CustomUIEventBindingType.Activating, "#WeatherStorm", EventData.of("Weather", "Storm"), false);
        event.addEventBinding(CustomUIEventBindingType.Activating, "#WeatherSnow", EventData.of("Weather", "Snow"), false);
        event.addEventBinding(CustomUIEventBindingType.ValueChanged, "#NewWorldNameField", EventData.of("@NewWorldName", "#NewWorldNameField.Value"), false);
        event.addEventBinding(CustomUIEventBindingType.ValueChanged, "#NewWorldSeedField", EventData.of("@NewWorldSeed", "#NewWorldSeedField.Value"), false);
        event.addEventBinding(CustomUIEventBindingType.ValueChanged, "#NewWorldGenField", EventData.of("@NewWorldGen", "#NewWorldGenField.Value"), false);

        // Checkboxes edition (utilisant Activating car ce sont des Buttons)
        event.addEventBinding(CustomUIEventBindingType.Activating, "#PvpCheckBox", EventData.of("Toggle", "pvp"), false);
        event.addEventBinding(CustomUIEventBindingType.Activating, "#FallDamageCheckBox", EventData.of("Toggle", "fallDamage"), false);
        event.addEventBinding(CustomUIEventBindingType.Activating, "#NpcSpawnCheckBox", EventData.of("Toggle", "npcSpawn"), false);
        event.addEventBinding(CustomUIEventBindingType.Activating, "#TimePausedCheckBox", EventData.of("Toggle", "timePaused"), false);
        event.addEventBinding(CustomUIEventBindingType.Activating, "#WeatherLockedCheckBox", EventData.of("Toggle", "weatherLocked"), false);
        event.addEventBinding(CustomUIEventBindingType.Activating, "#BlockTickCheckBox", EventData.of("Toggle", "blockTick"), false);
        event.addEventBinding(CustomUIEventBindingType.Activating, "#SaveChunksCheckBox", EventData.of("Toggle", "saveChunks"), false);

        // Checkboxes creation (pre-parametres)
        event.addEventBinding(CustomUIEventBindingType.Activating, "#NewPvpCheckBox", EventData.of("ToggleNew", "pvp"), false);
        event.addEventBinding(CustomUIEventBindingType.Activating, "#NewFallDamageCheckBox", EventData.of("ToggleNew", "fallDamage"), false);
        event.addEventBinding(CustomUIEventBindingType.Activating, "#NewNpcSpawnCheckBox", EventData.of("ToggleNew", "npcSpawn"), false);
        event.addEventBinding(CustomUIEventBindingType.Activating, "#NewBlockTickCheckBox", EventData.of("ToggleNew", "blockTick"), false);
        event.addEventBinding(CustomUIEventBindingType.Activating, "#NewSaveChunksCheckBox", EventData.of("ToggleNew", "saveChunks"), false);

        // Type de monde
        event.addEventBinding(CustomUIEventBindingType.Activating, "#WorldTypeNormal", EventData.of("WorldType", "normal"), false);
        event.addEventBinding(CustomUIEventBindingType.Activating, "#WorldTypeVoid", EventData.of("WorldType", "void"), false);
        event.addEventBinding(CustomUIEventBindingType.Activating, "#WorldTypeIsland", EventData.of("WorldType", "island"), false);
        event.addEventBinding(CustomUIEventBindingType.Activating, "#WorldTypeFlat", EventData.of("WorldType", "flat"), false);

        // Presets flat
        event.addEventBinding(CustomUIEventBindingType.Activating, "#FlatPresetGrass", EventData.of("FlatPreset", "grass"), false);
        event.addEventBinding(CustomUIEventBindingType.Activating, "#FlatPresetStone", EventData.of("FlatPreset", "stone"), false);
        event.addEventBinding(CustomUIEventBindingType.Activating, "#FlatPresetSand", EventData.of("FlatPreset", "sand"), false);
        event.addEventBinding(CustomUIEventBindingType.Activating, "#FlatPresetSnow", EventData.of("FlatPreset", "snow"), false);

        buildWorldList(cmd, event);
    }

    private void buildWorldList(UICommandBuilder cmd, UIEventBuilder event) {
        cmd.clear("#WorldList");

        Map<String, World> worlds = Universe.get().getWorlds();

        if (worlds.isEmpty()) {
            cmd.appendInline("#WorldList", "Label #EmptyLabel { Text: \"Aucun monde\"; Anchor: (Height: 30); Style: (FontSize: 12, TextColor: #808080, HorizontalAlignment: Center); }");
        } else {
            int index = 0;
            List<String> sortedNames = new ArrayList<>(worlds.keySet());
            Collections.sort(sortedNames);

            for (String worldName : sortedNames) {
                World world = worlds.get(worldName);
                boolean isSelected = worldName.equals(selectedWorldName);
                String bgColor = isSelected ? "#2a3f5f" : "#151d28";
                String btnId = "WorldBtn" + index;

                // Compter les joueurs
                int playerCount = world.getPlayerCount();
                String displayText = worldName + " (" + playerCount + ")";

                cmd.appendInline("#WorldList", "Button #" + btnId + " { Anchor: (Height: 36, Bottom: 3); Background: (Color: " + bgColor + "); Padding: (Horizontal: 8); Label #Lbl { Style: (FontSize: 12, VerticalAlignment: Center); } }");
                cmd.set("#" + btnId + " #Lbl.Text", displayText);
                cmd.set("#" + btnId + " #Lbl.Style.TextColor", isSelected ? "#ffffff" : "#bfcdd5");

                event.addEventBinding(CustomUIEventBindingType.Activating, "#" + btnId, EventData.of("SelectWorld", worldName), false);
                index++;
            }
        }
    }

    private void updateCheckboxDisplay(UICommandBuilder cmd, String checkId, boolean value) {
        cmd.set(checkId + ".Text", value ? "X" : "");
    }

    private void updateWorldTypeDisplay(UICommandBuilder cmd, String selectedType) {
        // Normal
        cmd.set("#WorldTypeNormal.Background.Color", selectedType.equals("normal") ? "#2d5a2d" : "#1a2332");
        cmd.set("#WorldTypeNormalLbl.Style.TextColor", selectedType.equals("normal") ? "#ffffff" : "#bfcdd5");
        // Void
        cmd.set("#WorldTypeVoid.Background.Color", selectedType.equals("void") ? "#2d5a2d" : "#1a2332");
        cmd.set("#WorldTypeVoidLbl.Style.TextColor", selectedType.equals("void") ? "#ffffff" : "#bfcdd5");
        // Island
        cmd.set("#WorldTypeIsland.Background.Color", selectedType.equals("island") ? "#2d5a2d" : "#1a2332");
        cmd.set("#WorldTypeIslandLbl.Style.TextColor", selectedType.equals("island") ? "#ffffff" : "#bfcdd5");
        // Flat
        cmd.set("#WorldTypeFlat.Background.Color", selectedType.equals("flat") ? "#2d5a2d" : "#1a2332");
        cmd.set("#WorldTypeFlatLbl.Style.TextColor", selectedType.equals("flat") ? "#ffffff" : "#bfcdd5");

        // Afficher/masquer les presets flat
        cmd.set("#FlatPresetsSection.Visible", selectedType.equals("flat"));
    }

    private void updateFlatPresetDisplay(UICommandBuilder cmd, String selectedPreset) {
        // Grass
        cmd.set("#FlatPresetGrass.Background.Color", selectedPreset.equals("grass") ? "#4a7a2d" : "#1a2332");
        cmd.set("#FlatPresetGrassLbl.Style.TextColor", selectedPreset.equals("grass") ? "#ffffff" : "#bfcdd5");
        // Stone
        cmd.set("#FlatPresetStone.Background.Color", selectedPreset.equals("stone") ? "#5a5a5a" : "#1a2332");
        cmd.set("#FlatPresetStoneLbl.Style.TextColor", selectedPreset.equals("stone") ? "#ffffff" : "#bfcdd5");
        // Sand
        cmd.set("#FlatPresetSand.Background.Color", selectedPreset.equals("sand") ? "#8a7a4a" : "#1a2332");
        cmd.set("#FlatPresetSandLbl.Style.TextColor", selectedPreset.equals("sand") ? "#ffffff" : "#bfcdd5");
        // Snow
        cmd.set("#FlatPresetSnow.Background.Color", selectedPreset.equals("snow") ? "#6a8aaa" : "#1a2332");
        cmd.set("#FlatPresetSnowLbl.Style.TextColor", selectedPreset.equals("snow") ? "#ffffff" : "#bfcdd5");
    }

    private String getFlatPresetDisplayName(String preset) {
        return switch (preset) {
            case "grass" -> "Herbe";
            case "stone" -> "Pierre";
            case "sand" -> "Sable";
            case "snow" -> "Neige";
            default -> preset;
        };
    }

    private void updateWeatherDisplay(UICommandBuilder cmd, String selectedWeather) {
        String weather = selectedWeather != null ? selectedWeather.toLowerCase() : "";
        // Clear
        cmd.set("#WeatherClear.Background.Color", weather.equals("clear") ? "#4a9f4a" : "#1a2332");
        cmd.set("#WeatherClearLbl.Style.TextColor", weather.equals("clear") ? "#ffffff" : "#bfcdd5");
        // Cloudy
        cmd.set("#WeatherCloudy.Background.Color", weather.equals("cloudy") ? "#6a7a8a" : "#1a2332");
        cmd.set("#WeatherCloudyLbl.Style.TextColor", weather.equals("cloudy") ? "#ffffff" : "#bfcdd5");
        // Rain Light
        cmd.set("#WeatherRainLight.Background.Color", weather.equals("rainlight") ? "#5a8aaa" : "#1a2332");
        cmd.set("#WeatherRainLightLbl.Style.TextColor", weather.equals("rainlight") ? "#ffffff" : "#bfcdd5");
        // Rain
        cmd.set("#WeatherRain.Background.Color", weather.equals("rain") ? "#4a7aaa" : "#1a2332");
        cmd.set("#WeatherRainLbl.Style.TextColor", weather.equals("rain") ? "#ffffff" : "#bfcdd5");
        // Storm
        cmd.set("#WeatherStorm.Background.Color", weather.equals("storm") ? "#5a4a8a" : "#1a2332");
        cmd.set("#WeatherStormLbl.Style.TextColor", weather.equals("storm") ? "#ffffff" : "#bfcdd5");
        // Snow
        cmd.set("#WeatherSnow.Background.Color", weather.equals("snow") ? "#8aaacc" : "#1a2332");
        cmd.set("#WeatherSnowLbl.Style.TextColor", weather.equals("snow") ? "#ffffff" : "#bfcdd5");
    }

    private void updateTabDisplay(UICommandBuilder cmd, String selectedTab) {
        boolean isInfo = "info".equals(selectedTab);
        // Tab Info
        cmd.set("#TabInfo.Background.Color", isInfo ? "#2d5a2d" : "#1a2332");
        cmd.set("#TabInfoLbl.Style.TextColor", isInfo ? "#ffffff" : "#bfcdd5");
        cmd.set("#TabInfoLbl.Style.RenderBold", isInfo);
        // Tab Players
        cmd.set("#TabPlayers.Background.Color", !isInfo ? "#2d5a2d" : "#1a2332");
        cmd.set("#TabPlayersLbl.Style.TextColor", !isInfo ? "#ffffff" : "#bfcdd5");
        cmd.set("#TabPlayersLbl.Style.RenderBold", !isInfo);
        // Afficher/masquer le contenu
        cmd.set("#InfoContent.Visible", isInfo);
        cmd.set("#PlayersContent.Visible", !isInfo);
    }

    private void buildPlayersList(UICommandBuilder cmd, UIEventBuilder event, World world) {
        cmd.clear("#PlayersList");

        var playerRefs = world.getPlayerRefs();
        int playerCount = playerRefs.size();

        // Mettre a jour le label du tab avec le nombre de joueurs
        cmd.set("#TabPlayersLbl.Text", "Joueurs (" + playerCount + ")");

        if (playerRefs.isEmpty()) {
            cmd.appendInline("#PlayersList", "Label #NoPlayersLabel { Text: \"Aucun joueur dans ce monde\"; Anchor: (Height: 30); Style: (FontSize: 12, TextColor: #808080, HorizontalAlignment: Center); }");
        } else {
            int index = 0;
            for (PlayerRef pr : playerRefs) {
                String playerName = pr.getUsername();
                if (playerName == null) continue;

                String tpBtnId = "TpToPlayer" + index;
                String kickBtnId = "KickPlayer" + index;

                // Ligne pour chaque joueur avec son nom et les boutons d'action
                cmd.appendInline("#PlayersList",
                    "Group #PlayerRow" + index + " { " +
                        "Anchor: (Height: 40, Bottom: 4); " +
                        "Background: (Color: #151d28); " +
                        "LayoutMode: Left; " +
                        "Padding: (Horizontal: 8); " +

                        // Nom du joueur
                        "Label #PlayerName" + index + " { " +
                            "Anchor: (Width: 150); " +
                            "Text: \"" + playerName + "\"; " +
                            "Style: (FontSize: 12, TextColor: #bfcdd5, VerticalAlignment: Center); " +
                        "} " +

                        // Bouton TP vers ce joueur
                        "Button #" + tpBtnId + " { " +
                            "Anchor: (Width: 60, Left: 8, Height: 28); " +
                            "Background: (Color: #2d4a8b); " +
                            "Label { Text: \"TP\"; Style: (FontSize: 10, TextColor: #ffffff, HorizontalAlignment: Center, VerticalAlignment: Center); } " +
                        "} " +

                        // Bouton Kick
                        "Button #" + kickBtnId + " { " +
                            "Anchor: (Width: 60, Left: 4, Height: 28); " +
                            "Background: (Color: #8b4020); " +
                            "Label { Text: \"Kick\"; Style: (FontSize: 10, TextColor: #ffffff, HorizontalAlignment: Center, VerticalAlignment: Center); } " +
                        "} " +
                    "}"
                );

                // Events pour les boutons
                event.addEventBinding(CustomUIEventBindingType.Activating, "#" + tpBtnId, EventData.of("TpToPlayer", playerName), false);
                event.addEventBinding(CustomUIEventBindingType.Activating, "#" + kickBtnId, EventData.of("KickPlayer", playerName), false);

                index++;
            }
        }
    }

    /**
     * Parse le temps en ticks (0-24000).
     * Retourne -1 si invalide.
     */
    private long parseTimeTicks(String timeStr) {
        if (timeStr == null || timeStr.isBlank()) return -1;

        timeStr = timeStr.trim();

        try {
            long ticks = Long.parseLong(timeStr);
            if (ticks >= 0 && ticks <= 24000) {
                return ticks;
            }
        } catch (NumberFormatException ignored) {}

        return -1;
    }

    /**
     * Applique le temps instantanement via WorldTimeResource.
     * Convertit les ticks en Instant et met a jour la resource.
     */
    private void applyTimeInstant(World world, long ticks) {
        try {
            // Convertir ticks en heure (6:00 = 0 ticks dans Hytale)
            double hoursFromSix = ticks / 1000.0;
            double decimalHour = 6.0 + hoursFromSix;
            if (decimalHour >= 24.0) decimalHour -= 24.0;

            int hour = (int) decimalHour;
            double fractionalHour = decimalHour - hour;
            int minute = (int) (fractionalHour * 60.0);
            int second = (int) ((fractionalHour * 60.0 - minute) * 60.0);

            // Creer l'Instant cible
            long secondsIntoDay = hour * 3600L + minute * 60L + second;
            Instant targetGameTime = Instant.ofEpochSecond(secondsIntoDay);

            // Essayer via WorldTimeResource (application instantanee)
            try {
                Class<?> tmClass = Class.forName("com.hypixel.hytale.server.core.modules.time.TimeModule");
                Object tm = tmClass.getMethod("get").invoke(null);
                Object resType = tmClass.getMethod("getWorldTimeResourceType").invoke(tm);

                Object entityStore = world.getEntityStore();
                Object store = entityStore.getClass().getMethod("getStore").invoke(entityStore);

                if (store != null) {
                    Class<?> resourceTypeClass = Class.forName("com.hypixel.hytale.component.ResourceType");
                    Object timeResource = store.getClass().getMethod("getResource", resourceTypeClass).invoke(store, resType);

                    if (timeResource != null) {
                        Method setGameTime0 = timeResource.getClass().getMethod("setGameTime0", Instant.class);
                        setGameTime0.invoke(timeResource, targetGameTime);

                        // Aussi mettre a jour le LocalDateTime interne
                        try {
                            LocalDateTime updatedLocalDateTime = LocalDateTime.ofInstant(targetGameTime, ZoneOffset.UTC);
                            java.lang.reflect.Field field;
                            try {
                                field = timeResource.getClass().getDeclaredField("_gameTimeLocalDateTime");
                            } catch (NoSuchFieldException e) {
                                field = timeResource.getClass().getDeclaredField("gameTimeLocalDateTime");
                            }
                            field.setAccessible(true);
                            field.set(timeResource, updatedLocalDateTime);
                        } catch (Exception ignored) {}

                        return; // Succes
                    }
                }
            } catch (Exception ignored) {}

            // Fallback: via WorldConfig
            WorldConfig config = world.getWorldConfig();
            config.setGameTime(targetGameTime);
            config.markChanged();

        } catch (Exception e) {
            plugin.getLogger().at(Level.WARNING).log("Failed to apply time: " + e.getMessage());
        }
    }

    /**
     * Applique la meteo instantanement via WeatherResource.
     */
    private void applyWeatherInstant(World world, String weatherId) {
        try {
            // Essayer via WeatherResource (application instantanee)
            try {
                Object entityStore = world.getEntityStore();
                Object store = entityStore.getClass().getMethod("getStore").invoke(entityStore);

                if (store != null) {
                    Class<?> weatherResClass = Class.forName("com.hypixel.hytale.builtin.weather.resources.WeatherResource");
                    Method getResType = weatherResClass.getMethod("getResourceType");
                    Object resType = getResType.invoke(null);

                    Class<?> resourceTypeClass = Class.forName("com.hypixel.hytale.component.ResourceType");
                    Object weatherResource = store.getClass().getMethod("getResource", resourceTypeClass).invoke(store, resType);

                    if (weatherResource != null) {
                        Method setForced = weatherResource.getClass().getMethod("setForcedWeather", String.class);
                        setForced.invoke(weatherResource, weatherId);
                        return; // Succes
                    }
                }
            } catch (Exception ignored) {}

            // Fallback: via WorldConfig
            WorldConfig config = world.getWorldConfig();
            config.setForcedWeather(weatherId);
            config.markChanged();

        } catch (Exception e) {
            plugin.getLogger().at(Level.WARNING).log("Failed to apply weather: " + e.getMessage());
        }
    }

    /**
     * Convertit le nom simple de meteo en ID Hytale.
     */
    private String getHytaleWeatherId(String simpleName) {
        if (simpleName == null) return "Zone1_Sunny";
        return switch (simpleName.toLowerCase()) {
            case "clear", "sunny" -> "Zone1_Sunny";
            case "cloudy" -> "Skylands_Rapid_Marsh_Cloudy_Medium";
            case "rainlight" -> "Zone1_Rain_Light";
            case "rain" -> "Zone3_Rain";
            case "storm" -> "Zone3_Rain";
            case "snow" -> "Zone3_Snow";
            default -> "Zone1_Sunny";
        };
    }

    /**
     * Recupere le temps actuel du monde en ticks (0-24000).
     */
    private long getWorldTimeTicks(World world, WorldConfig config) {
        try {
            Instant gameTime = config.getGameTime();
            if (gameTime == null) return 0;

            // Convertir l'Instant en ticks
            // Dans Hytale, l'Instant represente l'heure du jour
            LocalDateTime ldt = LocalDateTime.ofInstant(gameTime, ZoneOffset.UTC);
            double hour = ldt.getHour() + ldt.getMinute() / 60.0 + ldt.getSecond() / 3600.0;

            // Convertir en ticks (6:00 = 0 ticks)
            double ticks = (hour - 6.0) * 1000.0;
            if (ticks < 0) ticks += 24000.0;

            return (long) ticks;
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Applique le jour actuel (day count) au monde.
     */
    private void applyDayCount(World world, int dayCount) {
        try {
            WorldConfig config = world.getWorldConfig();
            Instant currentGameTime = config.getGameTime();
            if (currentGameTime == null) currentGameTime = Instant.ofEpochSecond(0);

            // Garder l'heure actuelle mais changer le jour
            long secondsIntoDay = currentGameTime.getEpochSecond() % 86400;
            long targetEpochSeconds = (dayCount - 1L) * 86400L + secondsIntoDay;
            Instant targetGameTime = Instant.ofEpochSecond(targetEpochSeconds);

            config.setGameTime(targetGameTime);
        } catch (Exception e) {
            plugin.getLogger().at(Level.WARNING).log("Failed to apply day count: " + e.getMessage());
        }
    }

    /**
     * Applique la duree du jour.
     */
    private void applyDayDuration(WorldConfig config, int seconds) {
        try {
            // Utiliser la reflection pour acceder aux champs prives
            Field field = config.getClass().getDeclaredField("daytimeDurationSecondsOverride");
            field.setAccessible(true);
            field.set(config, seconds);
        } catch (Exception e) {
            plugin.getLogger().at(Level.WARNING).log("Failed to apply day duration: " + e.getMessage());
        }
    }

    /**
     * Applique la duree de la nuit.
     */
    private void applyNightDuration(WorldConfig config, int seconds) {
        try {
            // Utiliser la reflection pour acceder aux champs prives
            Field field = config.getClass().getDeclaredField("nighttimeDurationSecondsOverride");
            field.setAccessible(true);
            field.set(config, seconds);
        } catch (Exception e) {
            plugin.getLogger().at(Level.WARNING).log("Failed to apply night duration: " + e.getMessage());
        }
    }

    /**
     * Applique le time dilation (vitesse du temps).
     */
    private void applyTimeDilation(World world, float timeDilation) {
        try {
            // Utiliser la methode statique World.setTimeDilation via reflection
            Object entityStore = world.getEntityStore();
            Object store = entityStore.getClass().getMethod("getStore").invoke(entityStore);

            if (store != null) {
                // Obtenir TimeResource et modifier le time dilation
                Class<?> timeResClass = Class.forName("com.hypixel.hytale.server.core.modules.time.TimeResource");
                Method getResType = timeResClass.getMethod("getResourceType");
                Object resType = getResType.invoke(null);

                Class<?> resourceTypeClass = Class.forName("com.hypixel.hytale.component.ResourceType");
                Object timeResource = store.getClass().getMethod("getResource", resourceTypeClass).invoke(store, resType);

                if (timeResource != null) {
                    Method setTimeDilation = timeResource.getClass().getMethod("setTimeDilationModifier", float.class);
                    setTimeDilation.invoke(timeResource, timeDilation);
                }
            }
        } catch (Exception e) {
            plugin.getLogger().at(Level.WARNING).log("Failed to apply time dilation: " + e.getMessage());
        }
    }

    /**
     * Applique un preset flat au monde nouvellement cree.
     * Place une couche de blocs selon le preset selectionne.
     */
    private void applyFlatPreset(World world, String preset) {
        // Definir les blocs selon le preset
        String surfaceBlock = switch (preset) {
            case "grass" -> "grass_block";
            case "stone" -> "stone";
            case "sand" -> "sand";
            case "snow" -> "snow_block";
            default -> "grass_block";
        };

        String subSurfaceBlock = switch (preset) {
            case "grass" -> "dirt";
            case "stone" -> "stone";
            case "sand" -> "sandstone";
            case "snow" -> "snow_block";
            default -> "dirt";
        };

        String baseBlock = switch (preset) {
            case "sand" -> "sandstone";
            default -> "stone";
        };

        // Note: Le generateur Flat de Hytale gere automatiquement la structure
        // Ce code est une reference pour une implementation personnalisee si necessaire
        // La configuration des blocs peut etre passee au generateur via les parametres
    }

    private void buildEditor(UICommandBuilder cmd, UIEventBuilder event, World world, WorldConfig config) {
        String displayName = config.getDisplayName() != null ? config.getDisplayName() : selectedWorldName;
        cmd.set("#EditorTitle.Text", displayName);
        cmd.set("#CreateContent.Visible", false);
        cmd.set("#NoWorldSelected.Visible", false);

        // Afficher les tabs
        cmd.set("#TabsBar.Visible", true);

        // Section parametres
        cmd.set("#SettingsSection.Visible", true);
        cmd.set("#NoWorldSelectedSettings.Visible", false);

        // Informations
        cmd.set("#UuidLabel.Text", "UUID: " + config.getUuid().toString().substring(0, 8) + "...");
        cmd.set("#SeedLabel.Text", "Seed: " + config.getSeed());

        // Afficher le temps en ticks (recupere via WorldTimeResource ou WorldConfig)
        long currentTicks = getWorldTimeTicks(world, config);
        String timeStr = String.valueOf(currentTicks);
        cmd.set("#GameTimeLabel.Text", "Temps: " + currentTicks + " ticks");

        int playerCount = world.getPlayerCount();
        cmd.set("#PlayerCountInfoLabel.Text", "Joueurs: " + playerCount);

        // Construire la liste des joueurs et mettre a jour les tabs
        buildPlayersList(cmd, event, world);
        activeTab = "info"; // Reset au tab info par defaut
        updateTabDisplay(cmd, activeTab);

        // Initialiser les toggles
        formPvp = config.isPvpEnabled();
        formFallDamage = config.isFallDamageEnabled();
        formNpcSpawn = config.isSpawningNPC();
        formTimePaused = config.isGameTimePaused();
        formWeatherLocked = config.getForcedWeather() != null && !config.getForcedWeather().isBlank();
        formBlockTick = config.isBlockTicking();
        formSaveChunks = config.canSaveChunks();

        // Mettre a jour l'affichage des checkboxes
        updateCheckboxDisplay(cmd, "#PvpCheck", formPvp);
        updateCheckboxDisplay(cmd, "#FallDamageCheck", formFallDamage);
        updateCheckboxDisplay(cmd, "#NpcSpawnCheck", formNpcSpawn);
        updateCheckboxDisplay(cmd, "#TimePausedCheck", formTimePaused);
        updateCheckboxDisplay(cmd, "#WeatherLockedCheck", formWeatherLocked);
        updateCheckboxDisplay(cmd, "#BlockTickCheck", formBlockTick);
        updateCheckboxDisplay(cmd, "#SaveChunksCheck", formSaveChunks);

        // Initialiser le champ de temps avec les ticks actuels
        formTime = timeStr;
        cmd.set("#TimeField.Value", formTime);

        // Jour actuel (calcule depuis gameTime)
        Instant gameTime = config.getGameTime();
        if (gameTime != null) {
            int dayCount = (int) (gameTime.getEpochSecond() / 86400) + 1;
            formDayCount = String.valueOf(dayCount);
        } else {
            formDayCount = "1";
        }
        cmd.set("#DayCountField.Value", formDayCount);

        // Duree jour/nuit
        formDayDuration = String.valueOf(world.getDaytimeDurationSeconds());
        formNightDuration = String.valueOf(world.getNighttimeDurationSeconds());
        cmd.set("#DayDurationField.Value", formDayDuration);
        cmd.set("#NightDurationField.Value", formNightDuration);

        // Time dilation (vitesse du temps)
        formTimeDilation = "1.0"; // Valeur par defaut
        cmd.set("#TimeDilationField.Value", formTimeDilation);

        // Meteo et GameMode
        formWeather = config.getForcedWeather() != null ? config.getForcedWeather() : "";
        formGameMode = config.getGameMode() != null ? config.getGameMode().name() : "";
        updateWeatherDisplay(cmd, formWeather);
        cmd.set("#GameModeField.Value", formGameMode);
    }

    private void showCreateMode(UICommandBuilder cmd) {
        createMode = true;
        cmd.set("#EditorTitle.Text", "Nouveau Monde");
        cmd.set("#InfoContent.Visible", false);
        cmd.set("#PlayersContent.Visible", false);
        cmd.set("#CreateContent.Visible", true);
        cmd.set("#NoWorldSelected.Visible", false);
        cmd.set("#TabsBar.Visible", false);

        // Cacher la section parametres
        cmd.set("#SettingsSection.Visible", false);
        cmd.set("#NoWorldSelectedSettings.Visible", true);

        // Reset form values
        cmd.set("#NewWorldNameField.Value", "");
        cmd.set("#NewWorldSeedField.Value", "");
        cmd.set("#NewWorldGenField.Value", "");

        formNewWorldName = "";
        formNewWorldSeed = "";
        formNewWorldGen = "";

        // Reset et afficher les pre-parametres (valeurs par defaut)
        newPvp = true;
        newFallDamage = true;
        newNpcSpawn = true;
        newBlockTick = true;
        newSaveChunks = true;
        newWorldType = "normal";
        flatPreset = "grass";

        updateCheckboxDisplay(cmd, "#NewPvpCheck", newPvp);
        updateCheckboxDisplay(cmd, "#NewFallDamageCheck", newFallDamage);
        updateCheckboxDisplay(cmd, "#NewNpcSpawnCheck", newNpcSpawn);
        updateCheckboxDisplay(cmd, "#NewBlockTickCheck", newBlockTick);
        updateCheckboxDisplay(cmd, "#NewSaveChunksCheck", newSaveChunks);
        updateWorldTypeDisplay(cmd, newWorldType);
        updateFlatPresetDisplay(cmd, flatPreset);
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

        // Stocker les valeurs des champs
        if (data.time != null) formTime = data.time;
        if (data.dayCount != null) formDayCount = data.dayCount;
        if (data.dayDuration != null) formDayDuration = data.dayDuration;
        if (data.nightDuration != null) formNightDuration = data.nightDuration;
        if (data.timeDilation != null) formTimeDilation = data.timeDilation;
        if (data.gameMode != null) formGameMode = data.gameMode;
        if (data.newWorldName != null) formNewWorldName = data.newWorldName;
        if (data.newWorldSeed != null) formNewWorldSeed = data.newWorldSeed;
        if (data.newWorldGen != null) formNewWorldGen = data.newWorldGen;

        // Selection de meteo par boutons
        if (data.weatherSelect != null) {
            formWeather = data.weatherSelect;
            updateWeatherDisplay(cmd, formWeather);
            sendUpdate(cmd, event, false);
            return;
        }

        // Gestion des tabs
        if (data.tab != null) {
            activeTab = data.tab;
            updateTabDisplay(cmd, activeTab);
            sendUpdate(cmd, event, false);
            return;
        }

        // TP vers un joueur specifique
        if (data.tpToPlayer != null) {
            if (selectedWorldName != null) {
                World world = Universe.get().getWorlds().get(selectedWorldName);
                if (world != null) {
                    // Trouver le joueur cible par son nom
                    for (PlayerRef pr : world.getPlayerRefs()) {
                        if (pr.getUsername() != null && pr.getUsername().equals(data.tpToPlayer)) {
                            // Utiliser le TeleportService du plugin pour teleporter
                            var islandiumPlayerOpt = plugin.getPlayerManager().getOnlinePlayer(playerRef.getUuid());
                            var targetIslandiumPlayerOpt = plugin.getPlayerManager().getOnlinePlayer(pr.getUuid());
                            if (islandiumPlayerOpt.isPresent() && targetIslandiumPlayerOpt.isPresent()) {
                                var targetLoc = targetIslandiumPlayerOpt.get().getLocation();
                                if (targetLoc != null) {
                                    plugin.getTeleportService().teleportInstant(islandiumPlayerOpt.get(), targetLoc);
                                    player.sendMessage(Message.raw("Teleporte vers " + data.tpToPlayer + "!"));
                                }
                            }
                            break;
                        }
                    }
                }
            }
            return;
        }

        // Kick un joueur specifique
        if (data.kickPlayer != null) {
            if (selectedWorldName != null) {
                World world = Universe.get().getWorlds().get(selectedWorldName);
                if (world != null) {
                    for (PlayerRef pr : new ArrayList<>(world.getPlayerRefs())) {
                        if (pr.getUsername() != null && pr.getUsername().equals(data.kickPlayer)) {
                            // Teleporter vers le monde par defaut
                            World defaultWorld = Universe.get().getWorlds().values().stream()
                                .filter(w -> !w.getName().equals(selectedWorldName))
                                .findFirst().orElse(null);

                            if (defaultWorld != null) {
                                world.execute(() -> {
                                    pr.removeFromStore();
                                    defaultWorld.addPlayer(pr, null, Boolean.TRUE, Boolean.FALSE);
                                });
                                player.sendMessage(Message.raw(data.kickPlayer + " a ete deplace vers " + defaultWorld.getName() + "!"));
                            }

                            // Rafraichir la liste
                            buildPlayersList(cmd, event, world);
                            sendUpdate(cmd, event, false);
                            break;
                        }
                    }
                }
            }
            return;
        }

        // Toggle edition
        if (data.toggle != null) {
            switch (data.toggle) {
                case "pvp" -> {
                    formPvp = !formPvp;
                    updateCheckboxDisplay(cmd, "#PvpCheck", formPvp);
                }
                case "fallDamage" -> {
                    formFallDamage = !formFallDamage;
                    updateCheckboxDisplay(cmd, "#FallDamageCheck", formFallDamage);
                }
                case "npcSpawn" -> {
                    formNpcSpawn = !formNpcSpawn;
                    updateCheckboxDisplay(cmd, "#NpcSpawnCheck", formNpcSpawn);
                }
                case "timePaused" -> {
                    formTimePaused = !formTimePaused;
                    updateCheckboxDisplay(cmd, "#TimePausedCheck", formTimePaused);
                }
                case "weatherLocked" -> {
                    formWeatherLocked = !formWeatherLocked;
                    updateCheckboxDisplay(cmd, "#WeatherLockedCheck", formWeatherLocked);
                }
                case "blockTick" -> {
                    formBlockTick = !formBlockTick;
                    updateCheckboxDisplay(cmd, "#BlockTickCheck", formBlockTick);
                }
                case "saveChunks" -> {
                    formSaveChunks = !formSaveChunks;
                    updateCheckboxDisplay(cmd, "#SaveChunksCheck", formSaveChunks);
                }
            }
            sendUpdate(cmd, event, false);
            return;
        }

        // Toggle creation (pre-parametres)
        if (data.toggleNew != null) {
            switch (data.toggleNew) {
                case "pvp" -> {
                    newPvp = !newPvp;
                    updateCheckboxDisplay(cmd, "#NewPvpCheck", newPvp);
                }
                case "fallDamage" -> {
                    newFallDamage = !newFallDamage;
                    updateCheckboxDisplay(cmd, "#NewFallDamageCheck", newFallDamage);
                }
                case "npcSpawn" -> {
                    newNpcSpawn = !newNpcSpawn;
                    updateCheckboxDisplay(cmd, "#NewNpcSpawnCheck", newNpcSpawn);
                }
                case "blockTick" -> {
                    newBlockTick = !newBlockTick;
                    updateCheckboxDisplay(cmd, "#NewBlockTickCheck", newBlockTick);
                }
                case "saveChunks" -> {
                    newSaveChunks = !newSaveChunks;
                    updateCheckboxDisplay(cmd, "#NewSaveChunksCheck", newSaveChunks);
                }
            }
            sendUpdate(cmd, event, false);
            return;
        }

        // Type de monde
        if (data.worldType != null) {
            newWorldType = data.worldType;
            updateWorldTypeDisplay(cmd, newWorldType);
            sendUpdate(cmd, event, false);
            return;
        }

        // Preset flat
        if (data.flatPreset != null) {
            flatPreset = data.flatPreset;
            updateFlatPresetDisplay(cmd, flatPreset);
            sendUpdate(cmd, event, false);
            return;
        }

        // Gestion des actions
        if (data.action != null) {
            switch (data.action) {
                case "back" -> {
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
                    cmd.set("#EditorTitle.Text", "Selectionnez un monde");
                    cmd.set("#NoWorldSelected.Visible", true);
                    sendUpdate(cmd, event, false);
                    return;
                }
                case "confirmCreate" -> {
                    if (!formNewWorldName.isBlank()) {
                        // Verifier si le monde existe deja
                        if (Universe.get().getWorlds().containsKey(formNewWorldName)) {
                            player.sendMessage(Message.raw("Un monde avec ce nom existe deja!"));
                            return;
                        }

                        // Verifier si le monde existe sur le disque
                        if (Universe.get().isWorldLoadable(formNewWorldName)) {
                            player.sendMessage(Message.raw("Un monde avec ce nom existe deja sur le disque!"));
                            return;
                        }

                        // Determiner le generateur en fonction du type
                        // Pour flat, on utilise Void puis on genere le terrain nous-memes
                        // Generateurs disponibles: "Void", "Flat", "default" (Hytale worldgen)
                        String generatorType = switch (newWorldType) {
                            case "void", "island", "flat" -> "Void"; // Tous utilisent Void, flat est rempli apres
                            default -> formNewWorldGen.isBlank() ? "default" : formNewWorldGen;
                        };

                        // Pour les mondes flat, on utilise le preset selectionne
                        final String selectedFlatPreset = newWorldType.equals("flat") ? flatPreset : null;
                        final boolean isFlatWorld = newWorldType.equals("flat");

                        String typeMsg = switch (newWorldType) {
                            case "void" -> " (monde vide)";
                            case "island" -> " (monde vide - ile a creer manuellement)";
                            case "flat" -> " (monde plat - " + getFlatPresetDisplayName(flatPreset) + ")";
                            default -> "";
                        };

                        player.sendMessage(Message.raw("Creation du monde '" + formNewWorldName + "'" + typeMsg + "..."));

                        // Sauvegarder les valeurs avant reset
                        final String worldName = formNewWorldName;
                        final boolean pvpEnabled = newPvp;
                        final boolean npcSpawnEnabled = newNpcSpawn;
                        final boolean blockTickEnabled = newBlockTick;
                        final boolean saveChunksEnabled = newSaveChunks;

                        // Creer le monde via l'API Universe
                        Universe.get().addWorld(worldName, generatorType, "default")
                            .thenRun(() -> {
                                // Appliquer les pre-parametres apres creation
                                World newWorld = Universe.get().getWorlds().get(worldName);
                                if (newWorld != null) {
                                    WorldConfig config = newWorld.getWorldConfig();
                                    config.setPvpEnabled(pvpEnabled);
                                    config.setSpawningNPC(npcSpawnEnabled);
                                    config.setBlockTicking(blockTickEnabled);
                                    config.setCanSaveChunks(saveChunksEnabled);
                                    config.markChanged();

                                    // Si c'est un monde flat, generer le terrain
                                    if (isFlatWorld) {
                                        player.sendMessage(Message.raw("Generation du terrain plat en cours..."));

                                        // Obtenir le preset
                                        FlatWorldGenerator.FlatWorldPreset preset =
                                            FlatWorldGenerator.FlatWorldPreset.fromString(selectedFlatPreset);

                                        // Generer le terrain autour du spawn (8 chunks de rayon = 256 blocs)
                                        FlatWorldGenerator.generateWithPreset(newWorld, preset, 8)
                                            .thenAccept(blocksPlaced -> {
                                                player.sendMessage(Message.raw("Terrain plat genere! (~" + blocksPlaced + " blocs)"));
                                                player.sendMessage(Message.raw("Surface: " + preset.getSurfaceBlock() + " a Y=" + preset.getSurfaceY()));
                                                player.sendMessage(Message.raw("Sous-sol: " + preset.getUndergroundBlock() + " de Y=0 a Y=" + (preset.getSurfaceY() - 1)));
                                            })
                                            .exceptionally(ex -> {
                                                player.sendMessage(Message.raw("Erreur lors de la generation du terrain: " + ex.getMessage()));
                                                return null;
                                            });
                                    }
                                }
                                player.sendMessage(Message.raw("Monde '" + worldName + "' cree avec succes!"));
                            })
                            .exceptionally(throwable -> {
                                player.sendMessage(Message.raw("Erreur lors de la creation: " +
                                    (throwable.getCause() != null ? throwable.getCause().getMessage() : throwable.getMessage())));
                                return null;
                            });

                        createMode = false;
                        formNewWorldName = "";
                        formNewWorldSeed = "";
                        formNewWorldGen = "";

                        cmd.set("#CreateContent.Visible", false);
                        cmd.set("#EditorTitle.Text", "Selectionnez un monde");
                        cmd.set("#NoWorldSelected.Visible", true);
                        buildWorldList(cmd, event);
                        sendUpdate(cmd, event, false);
                    } else {
                        player.sendMessage(Message.raw("Veuillez entrer un nom pour le monde."));
                    }
                    return;
                }
                case "applySettings" -> {
                    if (selectedWorldName != null) {
                        World world = Universe.get().getWorlds().get(selectedWorldName);
                        if (world != null) {
                            WorldConfig config = world.getWorldConfig();

                            config.setPvpEnabled(formPvp);
                            config.setBlockTicking(formBlockTick);
                            config.setSpawningNPC(formNpcSpawn);
                            config.setGameTimePaused(formTimePaused);
                            config.setCanSaveChunks(formSaveChunks);

                            // Gestion du temps - application instantanee via WorldTimeResource
                            if (!formTime.isBlank()) {
                                long ticks = parseTimeTicks(formTime);
                                if (ticks >= 0) {
                                    applyTimeInstant(world, ticks);
                                }
                            }

                            // Gestion de la meteo - application instantanee via WeatherResource
                            if (!formWeather.isBlank()) {
                                String hytaleWeatherId = getHytaleWeatherId(formWeather);
                                applyWeatherInstant(world, hytaleWeatherId);
                            }

                            // Gestion de la meteo forcee dans config (pour persistence)
                            if (formWeatherLocked) {
                                if (!formWeather.isBlank()) {
                                    config.setForcedWeather(getHytaleWeatherId(formWeather));
                                }
                            } else {
                                config.setForcedWeather(null); // Laisser la meteo varier naturellement
                            }

                            // Gestion du jour actuel (day count)
                            if (!formDayCount.isBlank()) {
                                try {
                                    int dayCount = Integer.parseInt(formDayCount.trim());
                                    if (dayCount > 0) {
                                        applyDayCount(world, dayCount);
                                    }
                                } catch (NumberFormatException ignored) {}
                            }

                            // Gestion de la duree jour/nuit
                            if (!formDayDuration.isBlank()) {
                                try {
                                    int dayDuration = Integer.parseInt(formDayDuration.trim());
                                    if (dayDuration > 0) {
                                        applyDayDuration(config, dayDuration);
                                    }
                                } catch (NumberFormatException ignored) {}
                            }
                            if (!formNightDuration.isBlank()) {
                                try {
                                    int nightDuration = Integer.parseInt(formNightDuration.trim());
                                    if (nightDuration > 0) {
                                        applyNightDuration(config, nightDuration);
                                    }
                                } catch (NumberFormatException ignored) {}
                            }

                            // Gestion du time dilation
                            if (!formTimeDilation.isBlank()) {
                                try {
                                    float timeDilation = Float.parseFloat(formTimeDilation.trim());
                                    if (timeDilation >= 0.01f && timeDilation <= 4.0f) {
                                        applyTimeDilation(world, timeDilation);
                                    }
                                } catch (NumberFormatException ignored) {}
                            }

                            if (!formGameMode.isBlank()) {
                                try {
                                    GameMode gm = GameMode.valueOf(formGameMode.toUpperCase());
                                    config.setGameMode(gm);
                                } catch (IllegalArgumentException ignored) {}
                            }

                            config.markChanged();

                            // Message de confirmation
                            player.sendMessage(Message.raw("Parametres appliques au monde '" + selectedWorldName + "'!"));
                        }
                    }
                    return;
                }
                case "teleport" -> {
                    if (selectedWorldName != null) {
                        World targetWorld = Universe.get().getWorlds().get(selectedWorldName);
                        if (targetWorld != null) {
                            close();
                            // D'abord retirer le joueur du monde actuel
                            World currentWorld = ((EntityStore) store.getExternalData()).getWorld();
                            final String targetName = selectedWorldName;
                            currentWorld.execute(() -> {
                                playerRef.removeFromStore();
                                // Puis ajouter au nouveau monde
                                targetWorld.addPlayer(playerRef, null, Boolean.TRUE, Boolean.FALSE)
                                    .thenRun(() -> player.sendMessage(Message.raw("Teleporte vers '" + targetName + "'!")))
                                    .exceptionally(ex -> {
                                        player.sendMessage(Message.raw("Erreur de teleportation: " + ex.getMessage()));
                                        return null;
                                    });
                            });
                        }
                    }
                    return;
                }
                case "saveWorld" -> {
                    if (selectedWorldName != null) {
                        World world = Universe.get().getWorlds().get(selectedWorldName);
                        if (world != null) {
                            player.sendMessage(Message.raw("Utilisez /world save " + selectedWorldName + " pour sauvegarder."));
                        }
                    }
                    return;
                }
                case "setSpawn" -> {
                    if (selectedWorldName != null) {
                        var islandiumPlayerOpt = plugin.getPlayerManager().getOnlinePlayer(playerRef.getUuid());
                        if (islandiumPlayerOpt.isPresent()) {
                            var myLoc = islandiumPlayerOpt.get().getLocation();
                            if (myLoc != null) {
                                var spawnLoc = com.islandium.core.api.location.ServerLocation.of(
                                    plugin.getServerName(),
                                    selectedWorldName,
                                    myLoc.x(), myLoc.y(), myLoc.z(),
                                    myLoc.yaw(), myLoc.pitch()
                                );
                                plugin.getSpawnService().setWorldSpawn(selectedWorldName, spawnLoc);
                                player.sendMessage(Message.raw("Spawn du monde '" + selectedWorldName + "' defini a " +
                                    String.format("%.1f, %.1f, %.1f", myLoc.x(), myLoc.y(), myLoc.z()) + "!"));
                            } else {
                                player.sendMessage(Message.raw("Impossible de recuperer votre position!"));
                            }
                        }
                    }
                    return;
                }
                case "refresh" -> {
                    buildWorldList(cmd, event);
                    if (selectedWorldName != null) {
                        World world = Universe.get().getWorlds().get(selectedWorldName);
                        if (world != null) {
                            buildEditor(cmd, event, world, world.getWorldConfig());
                        }
                    }
                    sendUpdate(cmd, event, false);
                    player.sendMessage(Message.raw("Liste des mondes actualisee!"));
                    return;
                }
                case "kickAll" -> {
                    if (selectedWorldName != null) {
                        World world = Universe.get().getWorlds().get(selectedWorldName);
                        if (world != null) {
                            World defaultWorld = Universe.get().getWorlds().values().stream()
                                .filter(w -> !w.getName().equals(selectedWorldName))
                                .findFirst().orElse(null);

                            if (defaultWorld != null) {
                                int count = 0;
                                for (PlayerRef pr : new ArrayList<>(world.getPlayerRefs())) {
                                    // Ne pas kicker le joueur qui execute la commande s'il est dans ce monde
                                    if (pr.getUuid().equals(playerRef.getUuid())) continue;

                                    world.execute(() -> {
                                        pr.removeFromStore();
                                        defaultWorld.addPlayer(pr, null, Boolean.TRUE, Boolean.FALSE);
                                    });
                                    count++;
                                }
                                player.sendMessage(Message.raw(count + " joueur(s) deplace(s) vers " + defaultWorld.getName() + "!"));

                                // Rafraichir la liste
                                buildPlayersList(cmd, event, world);
                                sendUpdate(cmd, event, false);
                            } else {
                                player.sendMessage(Message.raw("Aucun autre monde disponible!"));
                            }
                        }
                    }
                    return;
                }
                case "tpAllToMe" -> {
                    if (selectedWorldName != null) {
                        World world = Universe.get().getWorlds().get(selectedWorldName);
                        if (world != null) {
                            // Obtenir ma position via IslandiumPlayer
                            var myIslandiumPlayerOpt = plugin.getPlayerManager().getOnlinePlayer(playerRef.getUuid());
                            if (myIslandiumPlayerOpt.isPresent()) {
                                var myLoc = myIslandiumPlayerOpt.get().getLocation();
                                if (myLoc != null) {
                                    int count = 0;
                                    for (PlayerRef pr : world.getPlayerRefs()) {
                                        // Ne pas se teleporter soi-meme
                                        if (pr.getUuid().equals(playerRef.getUuid())) continue;

                                        var targetIslandiumPlayerOpt = plugin.getPlayerManager().getOnlinePlayer(pr.getUuid());
                                        if (targetIslandiumPlayerOpt.isPresent()) {
                                            plugin.getTeleportService().teleportInstant(targetIslandiumPlayerOpt.get(), myLoc);
                                            count++;
                                        }
                                    }
                                    player.sendMessage(Message.raw(count + " joueur(s) teleporte(s) a votre position!"));
                                }
                            }
                        }
                    }
                    return;
                }
                case "deleteWorld" -> {
                    if (selectedWorldName != null) {
                        // Verifier qu'il reste au moins un monde
                        if (Universe.get().getWorlds().size() <= 1) {
                            player.sendMessage(Message.raw("Impossible de supprimer le dernier monde!"));
                            return;
                        }

                        String worldToDelete = selectedWorldName;
                        Universe.get().removeWorld(worldToDelete);
                        player.sendMessage(Message.raw("Monde '" + worldToDelete + "' supprime!"));

                        selectedWorldName = null;
                        cmd.set("#InfoContent.Visible", false);
                        cmd.set("#EditorTitle.Text", "Selectionnez un monde");
                        cmd.set("#NoWorldSelected.Visible", true);
                        cmd.set("#SettingsSection.Visible", false);
                        cmd.set("#NoWorldSelectedSettings.Visible", true);

                        buildWorldList(cmd, event);
                        sendUpdate(cmd, event, false);
                    }
                    return;
                }
                case "tpAllHere" -> {
                    if (selectedWorldName != null) {
                        World targetWorld = Universe.get().getWorlds().get(selectedWorldName);
                        if (targetWorld != null) {
                            // Teleporter tous les joueurs de tous les mondes vers ce monde
                            int count = 0;
                            for (World w : Universe.get().getWorlds().values()) {
                                if (!w.getName().equals(selectedWorldName)) {
                                    // Copier la liste pour eviter ConcurrentModification
                                    var playersToMove = new java.util.ArrayList<>(w.getPlayerRefs());
                                    for (PlayerRef pr : playersToMove) {
                                        // Retirer puis ajouter sur le thread du monde source
                                        w.execute(() -> {
                                            pr.removeFromStore();
                                            targetWorld.addPlayer(pr, null, Boolean.TRUE, Boolean.FALSE);
                                        });
                                        count++;
                                    }
                                }
                            }
                            player.sendMessage(Message.raw(count + " joueur(s) teleporte(s) vers '" + selectedWorldName + "'!"));
                        }
                    }
                    return;
                }
            }
        }

        // Selection d'un monde
        if (data.selectWorld != null) {
            selectedWorldName = data.selectWorld;
            World world = Universe.get().getWorlds().get(selectedWorldName);
            if (world != null) {
                buildWorldList(cmd, event);
                buildEditor(cmd, event, world, world.getWorldConfig());
                sendUpdate(cmd, event, false);
            }
            return;
        }
    }

    public static class PageData {
        public static final BuilderCodec<PageData> CODEC = BuilderCodec.builder(PageData.class, PageData::new)
                .addField(new KeyedCodec<>("Action", Codec.STRING), (d, v) -> d.action = v, d -> d.action)
                .addField(new KeyedCodec<>("NavBar", Codec.STRING), (d, v) -> d.navBar = v, d -> d.navBar)
                .addField(new KeyedCodec<>("SelectWorld", Codec.STRING), (d, v) -> d.selectWorld = v, d -> d.selectWorld)
                .addField(new KeyedCodec<>("Toggle", Codec.STRING), (d, v) -> d.toggle = v, d -> d.toggle)
                .addField(new KeyedCodec<>("ToggleNew", Codec.STRING), (d, v) -> d.toggleNew = v, d -> d.toggleNew)
                .addField(new KeyedCodec<>("WorldType", Codec.STRING), (d, v) -> d.worldType = v, d -> d.worldType)
                .addField(new KeyedCodec<>("FlatPreset", Codec.STRING), (d, v) -> d.flatPreset = v, d -> d.flatPreset)
                .addField(new KeyedCodec<>("Weather", Codec.STRING), (d, v) -> d.weatherSelect = v, d -> d.weatherSelect)
                .addField(new KeyedCodec<>("Tab", Codec.STRING), (d, v) -> d.tab = v, d -> d.tab)
                .addField(new KeyedCodec<>("TpToPlayer", Codec.STRING), (d, v) -> d.tpToPlayer = v, d -> d.tpToPlayer)
                .addField(new KeyedCodec<>("KickPlayer", Codec.STRING), (d, v) -> d.kickPlayer = v, d -> d.kickPlayer)
                .addField(new KeyedCodec<>("@Time", Codec.STRING), (d, v) -> d.time = v, d -> d.time)
                .addField(new KeyedCodec<>("@DayCount", Codec.STRING), (d, v) -> d.dayCount = v, d -> d.dayCount)
                .addField(new KeyedCodec<>("@DayDuration", Codec.STRING), (d, v) -> d.dayDuration = v, d -> d.dayDuration)
                .addField(new KeyedCodec<>("@NightDuration", Codec.STRING), (d, v) -> d.nightDuration = v, d -> d.nightDuration)
                .addField(new KeyedCodec<>("@TimeDilation", Codec.STRING), (d, v) -> d.timeDilation = v, d -> d.timeDilation)
                .addField(new KeyedCodec<>("@GameMode", Codec.STRING), (d, v) -> d.gameMode = v, d -> d.gameMode)
                .addField(new KeyedCodec<>("@NewWorldName", Codec.STRING), (d, v) -> d.newWorldName = v, d -> d.newWorldName)
                .addField(new KeyedCodec<>("@NewWorldSeed", Codec.STRING), (d, v) -> d.newWorldSeed = v, d -> d.newWorldSeed)
                .addField(new KeyedCodec<>("@NewWorldGen", Codec.STRING), (d, v) -> d.newWorldGen = v, d -> d.newWorldGen)
                .build();

        public String action;
        public String navBar;
        public String selectWorld;
        public String toggle;
        public String toggleNew;
        public String worldType;
        public String flatPreset;
        public String weatherSelect;
        public String tab;
        public String tpToPlayer;
        public String kickPlayer;
        public String time;
        public String dayCount;
        public String dayDuration;
        public String nightDuration;
        public String timeDilation;
        public String gameMode;
        public String newWorldName;
        public String newWorldSeed;
        public String newWorldGen;
    }
}
