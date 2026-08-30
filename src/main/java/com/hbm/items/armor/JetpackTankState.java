package com.hbm.items.armor;

import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.mojang.serialization.Codec;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

/**
 * Data-component value backing {@code JetpackGlider}'s ({@code com.hbm.items.gear}) fuel state -
 * CE's raw {@code "fuelTank"} NBT-compound-in-NBT-compound ({@code FluidTankNTM#writeToNBT}/
 * {@code #readFromNBT} keyed under the stack's tag). Only {@link #type}/{@link #fill} are stored;
 * {@code maxFill} is intentionally omitted since {@code JetpackGlider.capacity} is a fixed per-item
 * constant already known at the call site reconstructing a {@link com.hbm.inventory.fluid.tank.FluidTankNTM}
 * - there is nothing per-stack to persist for it (unlike CE, which round-trips it purely because its
 * {@code FluidTankNTM} NBT format is generic/shared with block-entity tanks).
 *
 * <p>Deliberately its own narrow record rather than a shared "item fluid tank" component: per
 * {@code docs/phase3/fsb_armor_and_jetpacks.md} Deferred scope, no other fluid-holding item in this
 * port has settled on a common shape yet ({@code ItemPipette}/{@code ArmorFSBFueled}/
 * {@code JetpackFueledBase} all use a flat single-fluid-type int amount instead, not an arbitrary
 * runtime-chosen {@link FluidType}), so inventing a shared one here would be guessing at a
 * cross-cutting convention this package doesn't own. {@code JetpackGlider} is this record's only
 * consumer today; safe to fold into a general item-tank component later with no behavior change.
 *
 * <p>{@code CompoundTag}-detour {@link Codec}/{@link StreamCodec} pair, following
 * {@code com.hbm.items.weapon.sedna.GunStateComponent}'s already-committed identical pattern (itself
 * following {@code com.hbm.capability.HbmLivingAttachment}'s).
 */
public record JetpackTankState(FluidType type, int fill) {

    public static final JetpackTankState EMPTY = new JetpackTankState(Fluids.NONE, 0);

    private CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        Fluids.writeType(tag, "type", type);
        tag.putInt("fill", fill);
        return tag;
    }

    private static JetpackTankState fromTag(CompoundTag tag) {
        return new JetpackTankState(Fluids.readType(tag, "type"), tag.getInt("fill"));
    }

    public static final Codec<JetpackTankState> CODEC = CompoundTag.CODEC.xmap(JetpackTankState::fromTag, JetpackTankState::toTag);

    public static final StreamCodec<RegistryFriendlyByteBuf, JetpackTankState> STREAM_CODEC = StreamCodec.of(
            (buf, value) -> buf.writeNbt(value.toTag()),
            buf -> fromTag(buf.readNbt())
    );
}
