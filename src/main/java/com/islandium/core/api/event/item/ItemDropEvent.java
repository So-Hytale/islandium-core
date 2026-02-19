package com.islandium.core.api.event.item;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.islandium.core.api.event.IslandiumEvent;
import org.jetbrains.annotations.NotNull;

/**
 * Declenche quand un joueur drop un item.
 * Cancellable : annuler empeche le drop.
 */
public class ItemDropEvent extends IslandiumEvent {

    private final Ref<EntityStore> player;
    private ItemStack itemStack;
    private float throwSpeed;

    public ItemDropEvent(@NotNull Ref<EntityStore> player, @NotNull ItemStack itemStack, float throwSpeed) {
        this.player = player;
        this.itemStack = itemStack;
        this.throwSpeed = throwSpeed;
    }

    @NotNull
    public Ref<EntityStore> getPlayer() { return player; }

    @NotNull
    public ItemStack getItemStack() { return itemStack; }

    public void setItemStack(@NotNull ItemStack itemStack) { this.itemStack = itemStack; }

    public float getThrowSpeed() { return throwSpeed; }

    public void setThrowSpeed(float throwSpeed) { this.throwSpeed = throwSpeed; }
}
