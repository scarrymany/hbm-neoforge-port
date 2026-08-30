package com.hbm.items.weapon;

import com.mojang.serialization.Codec;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;

/**
 * The {@code missile_custom} item stack's part references - the Data Component replacement for
 * CE's {@code ItemCustomMissile} raw registry-int NBT keys ({@code chip}/{@code warhead}/
 * {@code fuselage}/{@code stability}/{@code thruster}, written by {@code buildMissile} via
 * {@code Item.getIdFromItem}). Forge-1.12's numeric, session-stable-only registry ids don't exist
 * in 1.21.1 - each part is instead stored as the {@link ItemMissile} instance's registry
 * {@link ResourceLocation} and resolved back against {@code BuiltInRegistries.ITEM} on read (see
 * {@link ItemCustomMissile#resolve(ResourceLocation)}), per {@code docs/phase3/missile_framework.md}'s
 * "Data Components" section.
 * <p>
 * {@code fins} is nullable, matching CE's own optional stability-fin slot (a missile can be built
 * with {@code stability == null} - see {@code MissileTab}'s "Hightower Missile" showcase, which
 * exercises exactly this). {@code chip} is carried here (unlike CE's {@code MissileStruct}, which
 * has no chip field at all) because {@code ItemCustomMissile}'s tooltip needs it; {@link
 * com.hbm.handler.MissileStruct} itself still omits chip when built from this component, preserving
 * CE's exact chip-is-write-only-into-NBT asymmetry documented in the research report's Open
 * questions.
 * <p>
 * Follows {@code com.hbm.items.weapon.sedna.GunStateComponent}'s established "detour through a
 * small {@link CompoundTag}" codec shape for a multi-field record, storing each id as a string
 * (empty = absent) rather than composing 5 separate {@code ResourceLocation} sub-codecs.
 */
public record MissileMultipartComponent(
        @Nullable ResourceLocation chip,
        ResourceLocation warhead,
        ResourceLocation fuselage,
        @Nullable ResourceLocation fins,
        ResourceLocation thruster
) {

    private static String str(@Nullable ResourceLocation id) {
        return id == null ? "" : id.toString();
    }

    @Nullable
    private static ResourceLocation parse(String s) {
        return s.isEmpty() ? null : ResourceLocation.parse(s);
    }

    private CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        tag.putString("chip", str(chip));
        tag.putString("warhead", str(warhead));
        tag.putString("fuselage", str(fuselage));
        tag.putString("fins", str(fins));
        tag.putString("thruster", str(thruster));
        return tag;
    }

    private static MissileMultipartComponent fromTag(CompoundTag tag) {
        return new MissileMultipartComponent(
                parse(tag.getString("chip")),
                ResourceLocation.parse(tag.getString("warhead")),
                ResourceLocation.parse(tag.getString("fuselage")),
                parse(tag.getString("fins")),
                ResourceLocation.parse(tag.getString("thruster")));
    }

    public static final Codec<MissileMultipartComponent> CODEC = CompoundTag.CODEC.xmap(MissileMultipartComponent::fromTag, MissileMultipartComponent::toTag);

    public static final StreamCodec<RegistryFriendlyByteBuf, MissileMultipartComponent> STREAM_CODEC = StreamCodec.of(
            (buf, value) -> buf.writeNbt(value.toTag()),
            buf -> fromTag(buf.readNbt())
    );
}
