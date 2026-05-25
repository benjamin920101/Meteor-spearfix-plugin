package com.spearfix.addon.mixin;

import com.spearfix.addon.SpearFixAddon;
import io.netty.handler.codec.DecoderException;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.network.ClientConnection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 攔截 ClientConnection.exceptionCaught 中的 DecoderException，
 * 防止解碼錯誤導致客戶端斷開連線。
 *
 * Minecraft 本身已對 PacketEncoderException 做相同處理（僅 log 後 return），
 * 此 Mixin 為 DecoderException 提供同等保護。
 */
@Mixin(ClientConnection.class)
public abstract class ClientConnectionMixin {

    @Inject(method = "exceptionCaught", at = @At("HEAD"), cancellable = true)
    private void onExceptionCaught(ChannelHandlerContext ctx, Throwable ex, CallbackInfo ci) {
        if (ex instanceof DecoderException) {
            SpearFixAddon.LOG.warn("[SpearFix] Suppressed DecoderException: {}", ex.getMessage());
            ci.cancel();
        }
    }
}
