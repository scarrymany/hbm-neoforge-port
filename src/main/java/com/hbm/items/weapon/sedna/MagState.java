package com.hbm.items.weapon.sedna;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * One magazine's worth of {@code IMagazine}-backed state - the Data Component replacement for CE's
 * {@code MagazineSingleTypeBase}/{@code MagazineFluid}/{@code MagazineEnergy} per-index flat NBT keys
 * ({@code magcount_N}/{@code magtype_N}/{@code magprev_N}/{@code magafter_N}), per
 * {@code docs/phase3/gun_framework.md}'s "a separate {@code List<MagState>} component for mags"
 * recommendation. Indexed by a magazine's own {@code index} field (a content-author-assigned number
 * disambiguating multiple mags on the same gun, distinct from - and not necessarily equal to - the
 * owning {@code GunConfig}'s own index; see {@code MagazineSingleTypeBase}'s javadoc), not by the
 * config index.
 * <p>
 * {@link #type} is a generic string slot reused across magazine flavors with different notions of
 * "ammo type": a {@link BulletConfig} id ({@link BulletConfig#id}, a namespaced string - CE's
 * {@code MagazineSingleTypeBase}/{@code MagazineBelt} stored a raw {@code int} index into a global
 * append-only list instead, which this port's {@code BulletConfig} deliberately replaced, see that
 * class's own javadoc) for bullet-backed mags, or a decimal {@code FluidType} id for
 * {@link com.hbm.items.weapon.sedna.mags.MagazineFluid}; empty string means "unset," matching CE's
 * effective fresh-stack behavior (see {@code MagazineSingleTypeBase.getType}'s own javadoc note on
 * this). {@code MagazineEnergy} has no type concept at all and leaves this empty always.
 */
public record MagState(String type, int amount, int before, int after) {

    public static final MagState EMPTY = new MagState("", 0, 0, 0);

    public MagState withType(String value) {
        return new MagState(value, amount, before, after);
    }

    public MagState withAmount(int value) {
        return new MagState(type, value, before, after);
    }

    public MagState withBefore(int value) {
        return new MagState(type, amount, value, after);
    }

    public MagState withAfter(int value) {
        return new MagState(type, amount, before, value);
    }

    public static final Codec<MagState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("type").forGetter(MagState::type),
            Codec.INT.fieldOf("amount").forGetter(MagState::amount),
            Codec.INT.fieldOf("before").forGetter(MagState::before),
            Codec.INT.fieldOf("after").forGetter(MagState::after)
    ).apply(instance, MagState::new));

    /** Declared over plain {@link ByteBuf}, not {@code RegistryFriendlyByteBuf} - matches {@code BookLoreContent}'s already-proven-working pattern for a {@code DataComponentType}'s {@code networkSynchronized} (a {@code StreamCodec<ByteBuf, T>} satisfies that method's {@code StreamCodec<? super RegistryFriendlyByteBuf, T>} bound directly, confirmed by {@code SpecialItemComponents.BOOK_LORE}'s own registration). */
    public static final StreamCodec<ByteBuf, MagState> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, MagState::type,
            ByteBufCodecs.INT, MagState::amount,
            ByteBufCodecs.INT, MagState::before,
            ByteBufCodecs.INT, MagState::after,
            MagState::new
    );
}
