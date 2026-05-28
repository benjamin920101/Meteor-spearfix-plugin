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
            String message = ex.getMessage();
            // 如果是 "larger than expected"，且 PacketDecoderMixin 沒攔截到（例如在其他 handler 拋出），
            // 我們依然攔截它但可以選擇不 log 或降低日誌等級。
            if (message != null && message.contains("larger than I expected")) {
                ci.cancel();
                return;
            }

            SpearFixAddon.LOG.warn("[SpearFix] Suppressed DecoderException: {}", message);
            ci.cancel();
        }
    }
}
