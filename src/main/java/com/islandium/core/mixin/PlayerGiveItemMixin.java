package com.islandium.core.mixin;

import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.transaction.ItemStackTransaction;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.islandium.core.api.event.IslandiumEventBus;
import com.islandium.core.api.event.item.ItemPickupEvent;
import com.islandium.core.api.event.item.PickupType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Mixin sur Player.giveItem() — point de passage UNIQUE de tous les pickups.
 *
 * Intercepte TOUS les ramassages d'items :
 * - Pickup passif (marcher sur l'item)
 * - Pickup interactif (touche F)
 * - Pickup par harvest (casser un bloc)
 * - Pickup par commande ou script
 *
 * Fire un ItemPickupEvent qui peut etre annule.
 * Si annule, retourne FAILED_ADD pour empecher le pickup.
 */
@Mixin(Player.class)
public abstract class PlayerGiveItemMixin {

    private static final Logger LOGGER = Logger.getLogger("IslandiumCore");

    @Inject(method = "giveItem", at = @At("HEAD"), cancellable = true)
    private void onGiveItem(ItemStack itemStack, Ref<EntityStore> entityRef,
                            ComponentAccessor<EntityStore> accessor,
                            CallbackInfoReturnable<ItemStackTransaction> cir) {
        if (!IslandiumEventBus.isAvailable()) return;
        if (itemStack == null || ItemStack.isEmpty(itemStack)) return;

        try {
            Player self = (Player) (Object) this;

            // Fire l'event IslandiumEventBus avec le Player pour les checks de region
            ItemPickupEvent event = new ItemPickupEvent(
                entityRef, self, itemStack, PickupType.PASSIVE
            );
            IslandiumEventBus.get().fire(event);

            if (event.isCancelled()) {
                // Retourner FAILED_ADD pour signaler que le pickup n'a pas eu lieu
                cir.setReturnValue(ItemStackTransaction.FAILED_ADD);
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "[IslandiumCore] Error in PlayerGiveItemMixin: " + e.getMessage());
        }
    }
}
