package com.tac5studios.codexcartography.net;

import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

/**
 * Elements: Claims' optional map channel (elements_claims:map). Both directions carry one NBT tag with an "op".
 * The format is documented in Elements: Claims' docs/MAP_CHANNEL.md.
 */
public record ClaimsPayload(CompoundTag tag) implements CustomPacketPayload {

    public static final Type<ClaimsPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("elements_claims", "map"));
    public static final StreamCodec<ByteBuf, ClaimsPayload> CODEC = ByteBufCodecs.COMPOUND_TAG.map(ClaimsPayload::new, ClaimsPayload::tag);

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
