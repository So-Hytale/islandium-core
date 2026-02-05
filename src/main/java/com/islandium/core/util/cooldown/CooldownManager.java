package com.islandium.core.util.cooldown;

import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gestionnaire des cooldowns.
 */
public class CooldownManager {

    // Map: UUID -> (key -> expiration timestamp)
    private final Map<UUID, Map<String, Long>> cooldowns = new ConcurrentHashMap<>();

    /**
     * Sets a cooldown for a player.
     *
     * @param uuid the player UUID
     * @param key  the cooldown key
     * @param seconds the duration in seconds
     */
    public void setCooldown(@NotNull UUID uuid, @NotNull String key, int seconds) {
        cooldowns.computeIfAbsent(uuid, k -> new ConcurrentHashMap<>())
                .put(key, System.currentTimeMillis() + (seconds * 1000L));
    }

    /**
     * Gets the remaining cooldown time in seconds.
     *
     * @return remaining seconds, or 0 if no cooldown
     */
    public int getRemainingCooldown(@NotNull UUID uuid, @NotNull String key) {
        Map<String, Long> playerCooldowns = cooldowns.get(uuid);
        if (playerCooldowns == null) return 0;

        Long expiration = playerCooldowns.get(key);
        if (expiration == null) return 0;

        long remaining = expiration - System.currentTimeMillis();
        return remaining > 0 ? (int) (remaining / 1000) : 0;
    }

    /**
     * Checks if a player has an active cooldown.
     */
    public boolean hasCooldown(@NotNull UUID uuid, @NotNull String key) {
        return getRemainingCooldown(uuid, key) > 0;
    }

    /**
     * Removes a cooldown.
     */
    public void removeCooldown(@NotNull UUID uuid, @NotNull String key) {
        Map<String, Long> playerCooldowns = cooldowns.get(uuid);
        if (playerCooldowns != null) {
            playerCooldowns.remove(key);
        }
    }

    /**
     * Clears all cooldowns for a player.
     */
    public void clearCooldowns(@NotNull UUID uuid) {
        cooldowns.remove(uuid);
    }

    /**
     * Cleans up expired cooldowns.
     */
    public void cleanup() {
        long now = System.currentTimeMillis();
        cooldowns.values().forEach(map ->
                map.entrySet().removeIf(e -> e.getValue() < now)
        );
    }
}
