package com.islandium.core.api.player;

/**
 * Énumération des différents états d'un joueur.
 *
 * Note: Les états suivants sont gérés nativement par Hytale:
 * - Vanish -> HiddenPlayersManager.hidePlayer()/showPlayer()
 * - God Mode -> Invulnerable component (ECS)
 * - Fly -> MovementManager.canFly
 */
public enum PlayerState {

    /**
     * Le joueur est AFK (Away From Keyboard).
     */
    AFK,

    /**
     * Le joueur est muté.
     */
    MUTED,

    /**
     * Le joueur est banni.
     */
    BANNED
}
