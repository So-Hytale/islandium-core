package com.islandium.core.api;

import com.islandium.core.api.economy.EconomyService;
import com.islandium.core.api.messaging.CrossServerMessenger;
import com.islandium.core.api.moderation.ModerationService;
import com.islandium.core.api.player.PlayerProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Point d'accès principal à l'API Islandium.
 * Permet d'accéder aux services Islandium.
 */
public final class IslandiumAPI {

    private static IslandiumAPI instance;

    private final PlayerProvider playerProvider;
    private final EconomyService economyService;
    private final ModerationService moderationService;
    private final CrossServerMessenger messenger;

    private IslandiumAPI(
            @NotNull PlayerProvider playerProvider,
            @NotNull EconomyService economyService,
            @NotNull ModerationService moderationService,
            @NotNull CrossServerMessenger messenger
    ) {
        this.playerProvider = playerProvider;
        this.economyService = economyService;
        this.moderationService = moderationService;
        this.messenger = messenger;
    }

    /**
     * Récupère l'instance de l'API Islandium.
     *
     * @return l'instance de l'API ou null si non initialisée
     */
    @Nullable
    public static IslandiumAPI get() {
        return instance;
    }

    /**
     * Vérifie si l'API est disponible.
     *
     * @return true si l'API est initialisée
     */
    public static boolean isAvailable() {
        return instance != null;
    }

    /**
     * Initialise l'API.
     */
    public static void init(@NotNull IslandiumAPI api) {
        if (instance != null) {
            throw new IllegalStateException("IslandiumAPI already initialized!");
        }
        instance = api;
    }

    /**
     * Ferme l'API.
     */
    public static void shutdown() {
        instance = null;
    }

    // === Services Getters ===

    @NotNull
    public PlayerProvider getPlayerProvider() {
        return playerProvider;
    }

    @NotNull
    public EconomyService getEconomyService() {
        return economyService;
    }

    @NotNull
    public ModerationService getModerationService() {
        return moderationService;
    }

    @NotNull
    public CrossServerMessenger getMessenger() {
        return messenger;
    }

    // === Builder ===

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private PlayerProvider playerProvider;
        private EconomyService economyService;
        private ModerationService moderationService;
        private CrossServerMessenger messenger;

        public Builder playerProvider(@NotNull PlayerProvider provider) {
            this.playerProvider = provider;
            return this;
        }

        public Builder economyService(@NotNull EconomyService service) {
            this.economyService = service;
            return this;
        }

        public Builder moderationService(@NotNull ModerationService service) {
            this.moderationService = service;
            return this;
        }

        public Builder messenger(@NotNull CrossServerMessenger messenger) {
            this.messenger = messenger;
            return this;
        }

        public IslandiumAPI build() {
            return new IslandiumAPI(
                    playerProvider,
                    economyService,
                    moderationService,
                    messenger
            );
        }
    }
}
