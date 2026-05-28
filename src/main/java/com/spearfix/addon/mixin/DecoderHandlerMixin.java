package com.spearfix.addon.mixin;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.network.handler.DecoderHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(DecoderHandler.class)
public abstract class DecoderHandlerMixin {

    /**
     * 攔截 DecoderHandler.decode，在檢查 "larger than I expected" 之前，
     * 強制消耗掉 ByteBuf 中的剩餘位元組。
     * 這樣既能防止拋出 IOException，也能讓解碼成功的封包正常進入後續處理。
     */
    @Inject(
        method = "decode",
        at = @At(
            value = "INVOKE",
            target = "Lio/netty/buffer/ByteBuf;readableBytes()I",
            ordinal = 1,
            remap = false
        ),
        require = 0
    )
    private void onCheckReadableBytes(ChannelHandlerContext ctx, ByteBuf buf, List<Object> objects, CallbackInfo ci) {
        if (buf.readableBytes() > 0) {
            // 消耗剩餘位元組，避免 IOException("Packet ... was larger than I expected")
            buf.skipBytes(buf.readableBytes());
        }
    }
}
