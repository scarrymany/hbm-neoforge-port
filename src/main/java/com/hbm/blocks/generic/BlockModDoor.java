package com.hbm.blocks.generic;

import com.hbm.lib.HBMSoundHandler;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.properties.BlockSetType;

/**
 * Modded metal door, ported from CE's {@code BlockModDoor}. Re-reading CE's overrides shows every
 * one of them ({@code onBlockActivated} cycling {@code OPEN} and playing a sound,
 * {@code neighborChanged} syncing to redstone, the two-tall placement/break bookkeeping) reproduces
 * behavior vanilla's own door block already provides natively in both 1.12 and 1.21 - CE only
 * needed a subclass at all to get a distinct registry entry with its own texture and a custom open
 * sound. In 1.21 that collapses to a plain {@link DoorBlock} subclass carrying one custom
 * {@link BlockSetType} (hand-openable, unlike vanilla's non-hand-openable
 * {@link BlockSetType#IRON}, matching CE's always-click-to-open metal doors) - no behavioral
 * overrides are needed at all.
 * <p>
 * <b>c12-sound-wiring:</b> CE's {@code BlockModDoor.onBlockActivated} plays exactly one custom
 * sound sample for both the open AND close transition - {@code HBMSoundHandler.openDoor}, at pitch
 * 1.3F when opening / 0.7F when closing (CE never touches its own registered {@code closeDoor}
 * field here). Vanilla's {@link DoorBlock}/{@link BlockSetType} plumbing has no per-transition
 * pitch hook, only two fixed, separately-pitched {@code SoundEvent} slots - so both slots below
 * are wired to {@code openDoor} (matching CE's real sample choice exactly), just without CE's
 * pitch shift, which vanilla's {@code BlockSetType} has no parameter to express.
 * {@code BlockSetType}'s constructor requires plain {@code SoundEvent} arguments, not
 * {@code Holder<SoundEvent>} (confirmed against the real NeoForge 1.21.1 compiler - a prior
 * assumption that {@code DeferredHolder} would satisfy a {@code Holder<SoundEvent>}-typed
 * parameter directly was wrong, since the real parameter type is the unwrapped
 * {@code SoundEvent}), so {@code HBMSoundHandler.openDoor.get()} is required. Resolved lazily
 * (a method, not an eager {@code static final} field) rather than at class-load time, matching
 * this port's standing rule against calling {@code .get()} on a {@code DeferredHolder} in a
 * static field initializer (see {@code ModSoundTypes.grate()/pipe()} for the same pattern) -
 * {@link #HAND_OPENABLE_METAL} caches the result after its first real call, which only happens
 * once a {@code BlockModDoor}/{@link BlockNTMTrapdoor} is actually constructed (i.e. during the
 * block {@code RegisterEvent}, by which point {@code SoundEvent} registration has completed).
 * Trapdoor/pressure-plate/button sounds keep vanilla's metal defaults - CE's {@code BlockModDoor}
 * has no trapdoor variant of its own (that is {@link BlockNTMTrapdoor}, a separate class) and
 * never touches those four slots.
 */
public class BlockModDoor extends DoorBlock {

    private static BlockSetType handOpenableMetal;

    public static BlockSetType HAND_OPENABLE_METAL() {
        if (handOpenableMetal == null) {
            handOpenableMetal = new BlockSetType(
                    "hbm_hand_openable_metal",
                    true,
                    false,
                    false,
                    BlockSetType.PressurePlateSensitivity.EVERYTHING,
                    SoundType.METAL,
                    HBMSoundHandler.openDoor.get(),
                    HBMSoundHandler.openDoor.get(),
                    SoundEvents.IRON_TRAPDOOR_CLOSE,
                    SoundEvents.IRON_TRAPDOOR_OPEN,
                    SoundEvents.METAL_PRESSURE_PLATE_CLICK_OFF,
                    SoundEvents.METAL_PRESSURE_PLATE_CLICK_ON,
                    SoundEvents.STONE_BUTTON_CLICK_OFF,
                    SoundEvents.STONE_BUTTON_CLICK_ON);
        }
        return handOpenableMetal;
    }

    public BlockModDoor(Properties properties) {
        super(HAND_OPENABLE_METAL(), properties);
    }
}
