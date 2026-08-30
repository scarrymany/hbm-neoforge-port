package com.hbm.inventory.fluid;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public class FluidStack {

    public FluidType type;
    public int fill;
    public int pressure;

    public FluidStack(int fill, FluidType type) {
        this.fill = fill;
        this.type = type;
    }

    public FluidStack(FluidType type, int fill) {
        this(type, fill, 0);
    }

    public FluidStack(FluidType type, int fill, int pressure) {
        this.fill = fill;
        this.type = type;
        this.pressure = pressure;
    }

    /**
     * World-save/config shape: registry-name-keyed via {@link FluidType#CODEC} (like
     * {@link Fluids#writeType}/{@link Fluids#readType}, since fluid ids shift when fluids are
     * added/removed but names don't). Follows the {@code RecordCodecBuilder.create(instance ->
     * instance.group(...).apply(...))} idiom already proven in this port by
     * {@code com.hbm.items.special.BedrockOreAmounts#CODEC}.
     */
    public static final Codec<FluidStack> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            FluidType.CODEC.fieldOf("type").forGetter(stack -> stack.type),
            Codec.INT.fieldOf("fill").forGetter(stack -> stack.fill),
            Codec.INT.optionalFieldOf("pressure", 0).forGetter(stack -> stack.pressure)
    ).apply(instance, (type, fill, pressure) -> new FluidStack(type, fill, pressure)));

    /**
     * Network shape: id-keyed (matches CE's own {@code FluidTankNTM#serialize}/{@code #deserialize}
     * wire format, i.e. {@link Fluids#fromID}/{@link FluidType#getID}) - a packet is short-lived, so
     * it doesn't carry the "ids shift across saves/reloads" risk {@link #CODEC} avoids by using
     * names instead. Follows the {@code StreamCodec.composite(...)} idiom already proven in this
     * port by {@code BedrockOreAmounts#STREAM_CODEC}.
     */
    public static final StreamCodec<RegistryFriendlyByteBuf, FluidStack> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT.map(Fluids::fromID, FluidType::getID), stack -> stack.type,
            ByteBufCodecs.VAR_INT, stack -> stack.fill,
            ByteBufCodecs.VAR_INT, stack -> stack.pressure,
            FluidStack::new
    );
}
