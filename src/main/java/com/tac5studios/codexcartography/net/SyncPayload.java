package com.tac5studios.codexcartography.net;

import com.tac5studios.codexcartography.CodexCartography;
import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jetbrains.annotations.NotNull;

/** Server to client: rules, world borders, markers, slime chunks and discovered structures ("kind" says which). */
public record SyncPayload(CompoundTag tag) implements CustomPacketPayload {

    public static final Type<SyncPayload> TYPE = new Type<>(CodexCartography.id("sync"));
    public static final StreamCodec<ByteBuf, SyncPayload> CODEC = ByteBufCodecs.COMPOUND_TAG.map(SyncPayload::new, SyncPayload::tag);

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
