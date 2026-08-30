package com.hbm.items.weapon.grenade;

import com.mojang.serialization.Codec;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

import javax.annotation.Nullable;

/**
 * The Data Component replacement for CE's 4 flat {@code ItemGrenadeUniversal} NBT ints
 * ({@code KEY_SHELL}/{@code KEY_FILLING}/{@code KEY_FUZE}/{@code KEY_EXTRA}), per
 * {@code docs/phase3/grenades.md}'s "Key design/API decisions" section. {@code extra} is nullable -
 * CE's own {@code KEY_EXTRA} tag is entirely absent for a no-extra grenade, not present-with-a-
 * sentinel-value, so {@code null} here is the faithful absence-representation, not a magic value.
 * <p>
 * Round-trips through a small {@link CompoundTag} rather than {@code RecordCodecBuilder}/
 * {@code StreamCodec.composite}, matching {@code com.hbm.items.weapon.sedna.GunStateComponent}'s
 * already-committed same-shaped precedent in this port. Per the research report's explicit
 * recommendation, the 4 fields are stored by serialized *name* (e.g. {@code "frag"}), not CE's own
 * raw ordinal - these enums are small (4/13/5/4 values) and reordering-safety is worth more than the
 * few saved wire bytes an ordinal would buy, unlike {@code BulletConfig}'s legitimately
 * wire-size-sensitive append-only id scheme.
 */
public record GrenadeLoadout(EnumGrenadeShell shell, EnumGrenadeFilling filling, EnumGrenadeFuze fuze, @Nullable EnumGrenadeExtra extra) {

    /** CE's implicit missing-tag-compound fallback (see {@code ItemGrenadeUniversal.getShell/getFilling/getFuze/getExtra}). */
    public static final GrenadeLoadout DEFAULT = new GrenadeLoadout(EnumGrenadeShell.FRAG, EnumGrenadeFilling.HE, EnumGrenadeFuze.S3, null);

    private CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        tag.putString("shell", shell.getSerializedName());
        tag.putString("filling", filling.getSerializedName());
        tag.putString("fuze", fuze.getSerializedName());
        if (extra != null) tag.putString("extra", extra.getSerializedName());
        return tag;
    }

    private static GrenadeLoadout fromTag(CompoundTag tag) {
        return new GrenadeLoadout(
                byName(EnumGrenadeShell.VALUES, tag.getString("shell"), EnumGrenadeShell.FRAG),
                byName(EnumGrenadeFilling.VALUES, tag.getString("filling"), EnumGrenadeFilling.HE),
                byName(EnumGrenadeFuze.VALUES, tag.getString("fuze"), EnumGrenadeFuze.S3),
                tag.contains("extra") ? byName(EnumGrenadeExtra.VALUES, tag.getString("extra"), null) : null);
    }

    private static <E extends Enum<E> & net.minecraft.util.StringRepresentable> E byName(E[] values, String name, @Nullable E fallback) {
        for (E value : values) {
            if (value.getSerializedName().equals(name)) return value;
        }
        return fallback;
    }

    public static final Codec<GrenadeLoadout> CODEC = CompoundTag.CODEC.xmap(GrenadeLoadout::fromTag, GrenadeLoadout::toTag);

    public static final StreamCodec<RegistryFriendlyByteBuf, GrenadeLoadout> STREAM_CODEC = StreamCodec.of(
            (buf, value) -> buf.writeNbt(value.toTag()),
            buf -> fromTag(buf.readNbt())
    );
}
