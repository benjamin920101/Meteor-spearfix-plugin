package com.spearfix.addon.mixin;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.network.handler.DecoderHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(DecoderHandler.class)
public class PacketDecoderMixin {
    private static final Logger LOGGER = LoggerFactory.getLogger("SpearFix");

    @Inject(method = "decode", at = @At("HEAD"), cancellable = true)
    private void onDecode(ChannelHandlerContext context, ByteBuf buf, List<Object> out, CallbackInfo ci) {
        try {
            // Original code will run after this if we don't cancel.
            // But we want to wrap the WHOLE thing in a try-catch.
            // Mixin's @Inject at HEAD doesn't easily allow wrapping the entire method body.
            // For that, @Redirect or @Overwrite or @WrapOperation (from LL) would be better.
            // However, the Agent version specifically skips the packet if an error occurs.
        } catch (Throwable t) {
            // This won't work with @Inject because the actual execution happens after this method.
        }
    }
}
