package com.tac5studios.codexcartography.net;

import com.tac5studios.codexcartography.CodexCartography;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jetbrains.annotations.NotNull;

/** Client to server: the block under the crosshair might belong to a structure; the server checks and answers. */
public record LookPayload(BlockPos pos) implements CustomPacketPayload {

    public static final Type<LookPayload> TYPE = new Type<>(CodexCartography.id("look"));
    public static final StreamCodec<ByteBuf, LookPayload> CODEC = BlockPos.STREAM_CODEC.map(LookPayload::new, LookPayload::pos);

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
