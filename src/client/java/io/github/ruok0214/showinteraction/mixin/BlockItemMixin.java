package io.github.ruok0214.showinteraction.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import io.github.ruok0214.showinteraction.ShowInteractionClient;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(BlockItem.class)
public class BlockItemMixin {
    @WrapMethod(method = "placeBlock")
    private boolean showInteraction$place(BlockPlaceContext context, BlockState state, Operation<Boolean> original) {
        // Integrated servers use this class too. Never touch client state from their thread.
        if (!context.getLevel().isClientSide()) return original.call(context, state);
        var pos = context.getClickedPos().immutable();
        var before = context.getLevel().getBlockState(pos);
        boolean result = original.call(context, state);
        var mc = Minecraft.getInstance();
        if (result && context.getLevel() == mc.level && context.getPlayer() == mc.player && ShowInteractionClient.tracker != null) {
            ShowInteractionClient.tracker.placed(pos, before);
        }
        return result;
    }
}
