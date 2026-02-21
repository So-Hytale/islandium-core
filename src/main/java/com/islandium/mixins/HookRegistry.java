package com.islandium.mixins;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registre de hooks partage entre earlyplugins/ et mods/ via System.getProperties().
 *
 * Pattern OrbisGuard : un ConcurrentHashMap stocke dans System.getProperties()
 * sous la cle "islandium.hook.registry". Les mixins (earlyplugins/) lisent les hooks,
 * le plugin principal (mods/) les enregistre.
 *
 * DUPLIQUE depuis islandium-mixins (meme package) pour eviter
 * une dependance compile entre les deux JARs.
 */
public final class HookRegistry {

    private static final String REGISTRY_KEY = "islandium.hook.registry";

    // Hook keys
    public static final String HARVEST_HOOK = "islandium.harvest.hook";
    public static final String ITEM_PICKUP_HOOK = "islandium.item.pickup.hook";
    public static final String DAMAGE_HOOK = "islandium.damage.hook";
    public static final String COMMAND_HOOK = "islandium.command.hook";
    public static final String PACKET_RECEIVE_HOOK = "islandium.packet.receive.hook";
    public static final String PACKET_SEND_HOOK = "islandium.packet.send.hook";
    public static final String INVENTORY_HOOK = "islandium.inventory.hook";
    public static final String SERVER_BOOT_HOOK = "islandium.server.boot.hook";
    public static final String ENTITY_SPAWN_HOOK = "islandium.entity.spawn.hook";

    // Status keys
    public static final String MIXINS_LOADED = "islandium.mixins.loaded";

    @SuppressWarnings("unchecked")
    private static Map<String, Object> getRegistry() {
        Object registry = System.getProperties().get(REGISTRY_KEY);
        if (registry instanceof Map) {
            return (Map<String, Object>) registry;
        }
        synchronized (HookRegistry.class) {
            registry = System.getProperties().get(REGISTRY_KEY);
            if (registry instanceof Map) {
                return (Map<String, Object>) registry;
            }
            Map<String, Object> newRegistry = new ConcurrentHashMap<>();
            System.getProperties().put(REGISTRY_KEY, newRegistry);
            return newRegistry;
        }
    }

    public static void register(String key, Object hook) {
        getRegistry().put(key, hook);
    }

    @SuppressWarnings("unchecked")
    public static <T> T get(String key) {
        return (T) getRegistry().get(key);
    }

    public static boolean isRegistered(String key) {
        return getRegistry().containsKey(key);
    }

    public static void unregister(String key) {
        getRegistry().remove(key);
    }
}
