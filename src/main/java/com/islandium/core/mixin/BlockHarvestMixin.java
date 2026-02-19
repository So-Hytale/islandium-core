package com.islandium.core.mixin;

import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.interaction.BlockHarvestUtils;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.islandium.core.api.event.IslandiumEventBus;
import com.islandium.core.api.event.block.HarvestBlockEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Mixin sur BlockHarvestUtils.performPickupByInteraction() — touche F sur un bloc ramassable.
 *
 * Fire un HarvestBlockEvent AVANT la destruction du bloc.
 * Si cancel, le bloc n'est pas détruit et l'item n'est pas donné.
 */
@Mixin(BlockHarvestUtils.class)
public abstract class BlockHarvestMixin {

    private static final Logger LOGGER = Logger.getLogger("IslandiumCore");

    @Inject(method = "performPickupByInteraction(Lcom/hypixel/hytale/component/Ref;Lcom/hypixel/hytale/math/vector/Vector3i;Lcom/hypixel/hytale/server/core/asset/type/blocktype/config/BlockType;ILcom/hypixel/hytale/component/Ref;Lcom/hypixel/hytale/component/ComponentAccessor;Lcom/hypixel/hytale/component/ComponentAccessor;)V", at = @At("HEAD"), cancellable = true)
    private static void onPerformPickupByInteraction(
            Ref<EntityStore> entityRef,
            Vector3i blockPos,
            BlockType blockType,
            int filler,
            Ref<ChunkStore> chunkRef,
            ComponentAccessor<EntityStore> entityAccessor,
            ComponentAccessor<ChunkStore> chunkAccessor,
            CallbackInfo ci) {

        LOGGER.info("[BlockHarvestMixin] performPickupByInteraction called! block=" + (blockType != null ? blockType.getId() : "null"));

        if (!IslandiumEventBus.isAvailable()) {
            LOGGER.warning("[BlockHarvestMixin] EventBus not available!");
            return;
        }
        if (blockType == null) return;

        try {
            // Résoudre le Player depuis la ref
            Player player = null;
            try {
                player = entityAccessor.getComponent(entityRef, Player.getComponentType());
            } catch (Exception ignored) {}

            LOGGER.info("[BlockHarvestMixin] Firing HarvestBlockEvent, player=" + (player != null ? player.getDisplayName() : "null"));

            HarvestBlockEvent event = new HarvestBlockEvent(entityRef, player, blockPos, blockType);
            IslandiumEventBus.get().fire(event);

            if (event.isCancelled()) {
                LOGGER.info("[BlockHarvestMixin] Event CANCELLED, blocking harvest");
                ci.cancel();
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "[IslandiumCore] Error in BlockHarvestMixin: " + e.getMessage());
        }
    }
}
