package com.spearfix.addon.mixin;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.handler.PacketDecoder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.List;

@Mixin(PacketDecoder.class)
public abstract class PacketDecoderMixin {

    /**
     * 攔截 PacketDecoder.decode，在檢查 "larger than I expected" 之前，
     * 強制消耗掉 PacketByteBuf 中的剩餘位元組。
     * 這樣既能防止拋出 IOException，也能讓解碼成功的封包正常進入後續處理。
     */
    @Inject(
        method = "decode",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/network/PacketByteBuf;readableBytes()I",
            ordinal = 0
        ),
        locals = LocalCapture.CAPTURE_FAILHARD,
        require = 0 // 使用 require = 0 以防不同版本間的混淆或結構差異導致崩潰
    )
    private void onCheckReadableBytes(ChannelHandlerContext ctx, ByteBuf buf, List<Object> objects, CallbackInfo ci, PacketByteBuf packetByteBuf) {
        if (packetByteBuf.readableBytes() > 0) {
            // 消耗剩餘位元組，避免 IOException("Packet ... was larger than I expected")
            packetByteBuf.skipBytes(packetByteBuf.readableBytes());
        }
    }
}
