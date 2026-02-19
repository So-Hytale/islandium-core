package com.islandium.core.api.event;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Bus d'evenements centralise pour tous les evenements Islandium.
 * Thread-safe, supporte les priorites d'execution.
 */
public final class IslandiumEventBus {

    private static final Logger LOGGER = Logger.getLogger("IslandiumEventBus");
    private static IslandiumEventBus instance;

    private final Map<Class<?>, List<HandlerEntry<?>>> handlers = new ConcurrentHashMap<>();

    private IslandiumEventBus() {}

    public static IslandiumEventBus get() {
        return instance;
    }

    public static boolean isAvailable() {
        return instance != null;
    }

    public static void init() {
        if (instance != null) {
            throw new IllegalStateException("IslandiumEventBus already initialized!");
        }
        instance = new IslandiumEventBus();
        LOGGER.info("[IslandiumEventBus] Event bus initialized");
    }

    public static void shutdown() {
        if (instance != null) {
            instance.handlers.clear();
            instance = null;
        }
    }

    /**
     * Enregistre un handler avec priorite NORMAL.
     */
    public <T extends IslandiumEvent> void register(Class<T> eventClass, Consumer<T> handler) {
        register(eventClass, handler, EventPriority.NORMAL);
    }

    /**
     * Enregistre un handler avec une priorite specifique.
     */
    @SuppressWarnings("unchecked")
    public <T extends IslandiumEvent> void register(Class<T> eventClass, Consumer<T> handler, EventPriority priority) {
        handlers.computeIfAbsent(eventClass, k -> Collections.synchronizedList(new ArrayList<>()))
                .add(new HandlerEntry<>(handler, priority));
        // Tri par priorite
        List<HandlerEntry<?>> list = handlers.get(eventClass);
        synchronized (list) {
            list.sort(Comparator.comparingInt(e -> e.priority.ordinal()));
        }
    }

    /**
     * Desenregistre un handler.
     */
    public <T extends IslandiumEvent> void unregister(Class<T> eventClass, Consumer<T> handler) {
        List<HandlerEntry<?>> list = handlers.get(eventClass);
        if (list != null) {
            synchronized (list) {
                list.removeIf(entry -> entry.handler == handler);
            }
        }
    }

    /**
     * Declenche un evenement. Retourne l'evenement pour verifier s'il a ete annule.
     */
    @SuppressWarnings("unchecked")
    public <T extends IslandiumEvent> T fire(T event) {
        List<HandlerEntry<?>> list = handlers.get(event.getClass());
        if (list == null || list.isEmpty()) {
            return event;
        }

        List<HandlerEntry<?>> snapshot;
        synchronized (list) {
            snapshot = new ArrayList<>(list);
        }

        for (HandlerEntry<?> entry : snapshot) {
            try {
                ((Consumer<T>) entry.handler).accept(event);
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "[IslandiumEventBus] Error in event handler for " + event.getClass().getSimpleName(), e);
            }
        }
        return event;
    }

    private record HandlerEntry<T>(Consumer<T> handler, EventPriority priority) {}
}
