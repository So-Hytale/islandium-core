package com.islandium.core.hook;

import com.islandium.core.api.event.IslandiumEventBus;
import com.islandium.core.api.event.command.CommandPreProcessEvent;
import com.islandium.core.api.event.inventory.HammerCycleEvent;
import com.islandium.core.api.event.inventory.HotbarSwitchEvent;
import com.islandium.core.api.event.network.PacketReceiveEvent;
import com.islandium.core.api.event.network.PacketSendEvent;
import com.islandium.core.api.event.server.ServerBootEvent;
import com.islandium.mixins.HookRegistry;
import com.islandium.mixins.hooks.*;

import com.hypixel.hytale.protocol.ToClientPacket;
import com.hypixel.hytale.protocol.ToServerPacket;
import com.hypixel.hytale.protocol.packets.inventory.SetActiveSlot;
import com.hypixel.hytale.protocol.packets.inventory.SwitchHotbarBlockSet;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Enregistre les implementations de hook dans le HookRegistry.
 * Chaque hook delegue a IslandiumEventBus pour que les listeners
 * (ex: HarvestBlockBusListener dans islandium-regions) fonctionnent.
 *
 * Appele depuis IslandiumPlugin.setup() apres l'init de l'EventBus.
 */
public final class HookRegistrar {

    private static final Logger LOGGER = Logger.getLogger("Essentials");

    public static void registerAll() {
        LOGGER.log(Level.INFO, "[Essentials] Registering mixin hooks...");

        // 1. Harvest hook (touche F sur rubble)
        HookRegistry.register(HookRegistry.HARVEST_HOOK, (HarvestHook) (playerUuid, blockTypeId, x, y, z) -> {
            if (!IslandiumEventBus.isAvailable()) return false;
            HarvestBlockSimpleEvent event = new HarvestBlockSimpleEvent(playerUuid, blockTypeId, x, y, z);
            IslandiumEventBus.get().fire(event);
            return event.isCancelled();
        });

        // 2. Item pickup hook
        HookRegistry.register(HookRegistry.ITEM_PICKUP_HOOK, (ItemPickupHook) (playerUuid, itemId) -> {
            if (!IslandiumEventBus.isAvailable()) return false;
            ItemPickupSimpleEvent event = new ItemPickupSimpleEvent(playerUuid, itemId);
            IslandiumEventBus.get().fire(event);
            return event.isCancelled();
        });

        // 3. Damage hook
        HookRegistry.register(HookRegistry.DAMAGE_HOOK, (DamageHook) (victimUuid, cause, amount) -> {
            if (!IslandiumEventBus.isAvailable()) return false;
            DamageSimpleEvent event = new DamageSimpleEvent(victimUuid, cause, amount);
            IslandiumEventBus.get().fire(event);
            return event.isCancelled();
        });

        // 4. Command hook
        HookRegistry.register(HookRegistry.COMMAND_HOOK, (CommandHook) (sender, commandLine) -> {
            if (!IslandiumEventBus.isAvailable()) return false;
            CommandPreProcessEvent event = new CommandPreProcessEvent(sender, commandLine);
            IslandiumEventBus.get().fire(event);
            return event.isCancelled();
        });

        // 5. Packet receive hook
        HookRegistry.register(HookRegistry.PACKET_RECEIVE_HOOK, (PacketReceiveHook) (packet, packetId) -> {
            if (!IslandiumEventBus.isAvailable()) return false;
            PacketReceiveEvent event = new PacketReceiveEvent((ToServerPacket) packet, packetId);
            IslandiumEventBus.get().fire(event);
            return event.isCancelled();
        });

        // 6. Packet send hook
        HookRegistry.register(HookRegistry.PACKET_SEND_HOOK, (PacketSendHook) (packet, packetId) -> {
            if (!IslandiumEventBus.isAvailable()) return false;
            PacketSendEvent event = new PacketSendEvent((ToClientPacket) packet, packetId);
            IslandiumEventBus.get().fire(event);
            return event.isCancelled();
        });

        // 7. Inventory hook
        HookRegistry.register(HookRegistry.INVENTORY_HOOK, (InventoryHook) (actionType, packet) -> {
            if (!IslandiumEventBus.isAvailable()) return false;
            if ("HOTBAR_SWITCH".equals(actionType) && packet instanceof SetActiveSlot setSlot) {
                HotbarSwitchEvent event = new HotbarSwitchEvent(setSlot.inventorySectionId, setSlot.activeSlot);
                IslandiumEventBus.get().fire(event);
                return event.isCancelled();
            } else if ("HAMMER_CYCLE".equals(actionType) && packet instanceof SwitchHotbarBlockSet switchSet) {
                HammerCycleEvent event = new HammerCycleEvent(switchSet.itemId);
                IslandiumEventBus.get().fire(event);
                return event.isCancelled();
            }
            return false;
        });

        // 8. Server boot hook
        HookRegistry.register(HookRegistry.SERVER_BOOT_HOOK, (ServerBootHook) () -> {
            if (!IslandiumEventBus.isAvailable()) return;
            IslandiumEventBus.get().fire(new ServerBootEvent());
        });

        LOGGER.log(Level.INFO, "[Essentials] All mixin hooks registered successfully!");
    }

    public static void unregisterAll() {
        HookRegistry.unregister(HookRegistry.HARVEST_HOOK);
        HookRegistry.unregister(HookRegistry.ITEM_PICKUP_HOOK);
        HookRegistry.unregister(HookRegistry.DAMAGE_HOOK);
        HookRegistry.unregister(HookRegistry.COMMAND_HOOK);
        HookRegistry.unregister(HookRegistry.PACKET_RECEIVE_HOOK);
        HookRegistry.unregister(HookRegistry.PACKET_SEND_HOOK);
        HookRegistry.unregister(HookRegistry.INVENTORY_HOOK);
        HookRegistry.unregister(HookRegistry.SERVER_BOOT_HOOK);
        LOGGER.log(Level.INFO, "[Essentials] All mixin hooks unregistered.");
    }
}
