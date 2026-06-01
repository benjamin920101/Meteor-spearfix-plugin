package com.spearfix.addon.mixin;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.network.handler.DecoderHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
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

    /**
     * 攔截 list.add(packet) 調用。
     * 如果後續 handler（如 PacketBundleHandler）在處理此封包時拋出異常，
     * 我們在此處攔截它，防止異常回傳至 DecoderHandler 導致重複嘗試解碼相同位元組（進而引發 Timeout）。
     */
    @Redirect(
        method = "decode",
        at = @At(
            value = "INVOKE",
            target = "Ljava/util/List;add(Ljava/lang/Object;)Z"
        )
    )
    private boolean onAddPacket(List<Object> list, Object packet) {
        try {
            return list.add(packet);
        } catch (Throwable t) {
            com.spearfix.addon.SpearFixAddon.LOG.warn("[SpearFix] Suppressed exception during packet processing: {}", t.getMessage());
            return false;
        }
    }
}
