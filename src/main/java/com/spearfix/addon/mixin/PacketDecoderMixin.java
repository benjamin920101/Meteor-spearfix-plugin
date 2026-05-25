package com.spearfix.addon.mixin;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.handler.DecoderHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;

@Mixin(DecoderHandler.class)
public class PacketDecoderMixin {
    private static final Logger LOGGER = LoggerFactory.getLogger("SpearFix");

    @Redirect(method = "decode", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/codec/PacketCodec;decode(Ljava/lang/Object;)Ljava/lang/Object;"))
    private Object spearfix$onDecode(PacketCodec instance, Object buf) {
        try {
            return instance.decode(buf);
        } catch (Throwable t) {
            LOGGER.error("Failed to decode packet", t);
            if (buf instanceof ByteBuf byteBuf) {
                byteBuf.skipBytes(byteBuf.readableBytes());
            }
            return null;
        }
    }

    @Redirect(method = "decode", at = @At(value = "INVOKE", target = "Ljava/util/List;add(Ljava/lang/Object;)Z"))
    private boolean spearfix$onAdd(List<Object> instance, Object packet) {
        if (packet == null) return false;
        return instance.add(packet);
    }
}
