package io.github.ruok0214.showinteraction.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import io.github.ruok0214.showinteraction.ShowInteractionClient;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MultiPlayerGameMode.class)
public class MultiPlayerGameModeMixin {
    @WrapMethod(method = "useItemOn")
    private InteractionResult showInteraction$use(LocalPlayer player, InteractionHand hand, BlockHitResult hit, Operation<InteractionResult> original) {
        var tracker = ShowInteractionClient.tracker;
        var attempt = tracker == null ? null : tracker.beginUse(hit.getBlockPos());
        InteractionResult result = original.call(player, hand, hit);
        if (tracker != null) tracker.endUse(attempt, result);
        return result;
    }

    @WrapMethod(method = "destroyBlock")
    private boolean showInteraction$break(BlockPos pos, Operation<Boolean> original) {
        var tracker = ShowInteractionClient.tracker;
        var before = tracker == null ? null : tracker.capture(pos);
        boolean result = original.call(pos);
        if (tracker != null) tracker.broken(before, result);
        return result;
    }

    @Inject(method = "handlePickItemFromBlock", at = @At("RETURN"))
    private void showInteraction$inspect(BlockPos pos, boolean includeData, CallbackInfo ci) {
        if (ShowInteractionClient.tracker != null) ShowInteractionClient.tracker.inspected(pos);
    }
}
