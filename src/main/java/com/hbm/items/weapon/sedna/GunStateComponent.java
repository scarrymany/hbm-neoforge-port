package com.hbm.items.weapon.sedna;

import com.hbm.util.EnumUtil;
import com.mojang.serialization.Codec;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

/**
 * One {@code configs_DNA} index's worth of {@link ItemGunBaseNT} runtime state - the Data Component
 * replacement for CE's per-index flat NBT keys ({@code state_N}/{@code timer_N}/{@code mode_N}/
 * {@code wear_N}/{@code mouse1_N}/{@code mouse2_N}/{@code mouse3_N}/{@code reload_N}/
 * {@code lastanim_N}/{@code animtimer_N}), per {@code docs/phase3/gun_framework.md}'s "NBT -> Data
 * Component notes" section (read in full - this record is exactly what that section recommends: one
 * record per config slot, held in a {@code List<GunStateComponent>} sized to
 * {@code configs_DNA.length}). The 4 boolean button-press flags collapse into one {@link #buttons}
 * bitset rather than 4 separate fields, matching the section's "button-bitset" wording.
 * <p>
 * {@code lastAnim} is stored as a plain ordinal into whichever {@link com.hbm.weapon.anim.HbmAnimationType}
 * enum the gun uses (this port's {@link com.hbm.weapon.anim.GunAnimationType}, not CE's
 * {@code AnimationEnums.GunAnimation} - see {@code ItemGunBaseNT}'s own class javadoc for why) rather
 * than a typed enum field, since a Data Component record field can't itself be generic over "whichever
 * animation enum this particular gun family uses."
 */
public record GunStateComponent(ItemGunBaseNT.GunState state, int timer, int mode, float wear, byte buttons, int lastAnim, int animTimer) {

    /** Bit flags for {@link #buttons} - CE's separate {@code mouse1_N}/{@code mouse2_N}/{@code mouse3_N}/{@code reload_N} booleans. */
    public static final byte BUTTON_PRIMARY = 1;
    public static final byte BUTTON_SECONDARY = 2;
    public static final byte BUTTON_TERTIARY = 4;
    public static final byte BUTTON_RELOAD = 8;

    /** CE's implicit all-NBT-absent default for a config index nothing has touched yet: state ordinal 0 (DRAWING), everything else zero. */
    public static final GunStateComponent DEFAULT = new GunStateComponent(ItemGunBaseNT.GunState.DRAWING, 0, 0, 0F, (byte) 0, 0, 0);

    public boolean button(byte mask) {
        return (buttons & mask) != 0;
    }

    public GunStateComponent withButton(byte mask, boolean value) {
        byte updated = (byte) (value ? (buttons | mask) : (buttons & ~mask));
        return new GunStateComponent(state, timer, mode, wear, updated, lastAnim, animTimer);
    }

    public GunStateComponent withState(ItemGunBaseNT.GunState value) {
        return new GunStateComponent(value, timer, mode, wear, buttons, lastAnim, animTimer);
    }

    public GunStateComponent withTimer(int value) {
        return new GunStateComponent(state, value, mode, wear, buttons, lastAnim, animTimer);
    }

    public GunStateComponent withMode(int value) {
        return new GunStateComponent(state, timer, value, wear, buttons, lastAnim, animTimer);
    }

    public GunStateComponent withWear(float value) {
        return new GunStateComponent(state, timer, mode, value, buttons, lastAnim, animTimer);
    }

    /** Sets a newly-triggered animation and resets its timer to 0, matching CE's {@code setLastAnim}+{@code setAnimTimer(0)} pair in {@code playAnimation}. */
    public GunStateComponent withAnim(int animOrdinal) {
        return new GunStateComponent(state, timer, mode, wear, buttons, animOrdinal, 0);
    }

    public GunStateComponent withAnimTimer(int value) {
        return new GunStateComponent(state, timer, mode, wear, buttons, lastAnim, value);
    }

    private CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        tag.putByte("state", (byte) state.ordinal());
        tag.putInt("timer", timer);
        tag.putInt("mode", mode);
        tag.putFloat("wear", wear);
        tag.putByte("buttons", buttons);
        tag.putInt("lastAnim", lastAnim);
        tag.putInt("animTimer", animTimer);
        return tag;
    }

    private static GunStateComponent fromTag(CompoundTag tag) {
        return new GunStateComponent(
                EnumUtil.grabEnumSafely(ItemGunBaseNT.GunState.VALUES, tag.getByte("state")),
                tag.getInt("timer"), tag.getInt("mode"), tag.getFloat("wear"),
                tag.getByte("buttons"), tag.getInt("lastAnim"), tag.getInt("animTimer"));
    }

    /** Persistent (world-save) codec, keyed through a small compound tag - sidesteps {@code RecordCodecBuilder}'s/{@code StreamCodec.composite}'s practical field-count ceiling for this 7-field record, following {@code HbmLivingAttachment}'s already-committed same-shaped pattern. */
    public static final Codec<GunStateComponent> CODEC = CompoundTag.CODEC.xmap(GunStateComponent::fromTag, GunStateComponent::toTag);

    /** Network codec, same compound-tag detour as {@link #CODEC} for the same reason. */
    public static final StreamCodec<RegistryFriendlyByteBuf, GunStateComponent> STREAM_CODEC = StreamCodec.of(
            (buf, value) -> buf.writeNbt(value.toTag()),
            buf -> fromTag(buf.readNbt())
    );
}
