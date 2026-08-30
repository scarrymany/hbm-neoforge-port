package com.hbm.items.special;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * Replaces CE's {@code ItemBedrockOreBase} NBT shape: one double per {@link BedrockOreType#suffix}
 * ("light", "heavy", "rare", "actinide", "nonmetal", "crystal"), written once at world-gen scan
 * time ({@code ItemBedrockOreBase.setOreAmount}) and read back for its tooltip / the Phase 2
 * ore-slopper machinery ({@code ItemBedrockOreBase.getOreAmount}).
 */
public record BedrockOreAmounts(double light, double heavy, double rare, double actinide, double nonmetal, double crystal) {

    public static final BedrockOreAmounts EMPTY = new BedrockOreAmounts(0, 0, 0, 0, 0, 0);

    public double get(BedrockOreType type) {
        return switch (type) {
            case LIGHT_METAL -> light;
            case HEAVY_METAL -> heavy;
            case RARE_EARTH -> rare;
            case ACTINIDE -> actinide;
            case NON_METAL -> nonmetal;
            case CRYSTALLINE -> crystal;
        };
    }

    public static final Codec<BedrockOreAmounts> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.DOUBLE.fieldOf("light").forGetter(BedrockOreAmounts::light),
            Codec.DOUBLE.fieldOf("heavy").forGetter(BedrockOreAmounts::heavy),
            Codec.DOUBLE.fieldOf("rare").forGetter(BedrockOreAmounts::rare),
            Codec.DOUBLE.fieldOf("actinide").forGetter(BedrockOreAmounts::actinide),
            Codec.DOUBLE.fieldOf("nonmetal").forGetter(BedrockOreAmounts::nonmetal),
            Codec.DOUBLE.fieldOf("crystal").forGetter(BedrockOreAmounts::crystal)
    ).apply(instance, BedrockOreAmounts::new));

    public static final StreamCodec<ByteBuf, BedrockOreAmounts> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.DOUBLE, BedrockOreAmounts::light,
            ByteBufCodecs.DOUBLE, BedrockOreAmounts::heavy,
            ByteBufCodecs.DOUBLE, BedrockOreAmounts::rare,
            ByteBufCodecs.DOUBLE, BedrockOreAmounts::actinide,
            ByteBufCodecs.DOUBLE, BedrockOreAmounts::nonmetal,
            ByteBufCodecs.DOUBLE, BedrockOreAmounts::crystal,
            BedrockOreAmounts::new
    );
}
