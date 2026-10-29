package io.github.ruok0214.showinteraction.mixin;

import io.github.ruok0214.showinteraction.ShowInteractionClient;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundOpenScreenPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public class ClientPacketListenerMixin {
    @Inject(method = "handleOpenScreen", at = @At("RETURN"))
    private void showInteraction$opened(ClientboundOpenScreenPacket packet, CallbackInfo ci) {
        if (ShowInteractionClient.tracker != null) ShowInteractionClient.tracker.containerOpened();
    }
}
