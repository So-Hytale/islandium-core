package com.islandium.core.mixin;

import com.hypixel.hytale.server.core.command.system.CommandManager;
import com.hypixel.hytale.server.core.command.system.CommandSender;
import com.islandium.core.api.event.IslandiumEventBus;
import com.islandium.core.api.event.command.CommandPreProcessEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.concurrent.CompletableFuture;

@Mixin(CommandManager.class)
public abstract class CommandManagerMixin {

    @Inject(method = "handleCommand(Lcom/hypixel/hytale/server/core/command/system/CommandSender;Ljava/lang/String;)Ljava/util/concurrent/CompletableFuture;",
            at = @At("HEAD"), cancellable = true)
    private void onHandleCommand(CommandSender sender, String commandLine,
                                 CallbackInfoReturnable<CompletableFuture<Void>> cir) {
        if (!IslandiumEventBus.isAvailable()) return;

        CommandPreProcessEvent event = new CommandPreProcessEvent(sender, commandLine);
        IslandiumEventBus.get().fire(event);

        if (event.isCancelled()) {
            cir.setReturnValue(CompletableFuture.completedFuture(null));
        }
    }
}
