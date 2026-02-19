package com.islandium.core.api.event.item;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.islandium.core.api.event.IslandiumEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Declenche quand un joueur ramasse un item (auto ou touche F).
 * Cancellable : annuler empeche le ramassage.
 *
 * Fire depuis PlayerGiveItemMixin (intercepte Player.giveItem()).
 * Couvre TOUS les types de pickup : passif, interactif, harvest, commande.
 */
public class ItemPickupEvent extends IslandiumEvent {

    private final Ref<EntityStore> playerRef;
    private final Player playerEntity;
    private ItemStack itemStack;
    private final PickupType pickupType;

    public ItemPickupEvent(@NotNull Ref<EntityStore> playerRef, @Nullable Player playerEntity,
                           @NotNull ItemStack itemStack, @NotNull PickupType pickupType) {
        this.playerRef = playerRef;
        this.playerEntity = playerEntity;
        this.itemStack = itemStack;
        this.pickupType = pickupType;
    }

    @NotNull
    public Ref<EntityStore> getPlayerRef() { return playerRef; }

    /**
     * Le Player Hytale. Peut etre null si le pickup vient d'un contexte non-joueur.
     * Permet d'acceder au UUID, position, monde, etc.
     */
    @Nullable
    public Player getPlayerEntity() { return playerEntity; }

    @NotNull
    public ItemStack getItemStack() { return itemStack; }

    public void setItemStack(@NotNull ItemStack itemStack) { this.itemStack = itemStack; }

    @NotNull
    public PickupType getPickupType() { return pickupType; }
}
