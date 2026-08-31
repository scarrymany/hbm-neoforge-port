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
 * {@code HBMSoundHandler.openDoor} is a {@code DeferredHolder<SoundEvent, SoundEvent>}, which
 * implements {@code Holder<SoundEvent>} directly - satisfying {@link BlockSetType}'s
 * {@code Holder<SoundEvent>}-typed sound parameters with no {@code .get()} needed, the same
 * pattern already confirmed live in this port's {@code Level#playSound} call sites (see
 * {@code docs/phase5/sound_wiring_and_assets.md} Finding 4). Trapdoor/pressure-plate/button sounds
 * keep vanilla's metal defaults - CE's {@code BlockModDoor} has no trapdoor variant of its own
 * (that is {@link BlockNTMTrapdoor}, a separate class) and never touches those four slots.
 */
public class BlockModDoor extends DoorBlock {

    public static final BlockSetType HAND_OPENABLE_METAL = new BlockSetType(
            "hbm_hand_openable_metal",
            true,
            false,
            false,
            BlockSetType.PressurePlateSensitivity.EVERYTHING,
            SoundType.METAL,
            HBMSoundHandler.openDoor,
            HBMSoundHandler.openDoor,
            SoundEvents.IRON_TRAPDOOR_CLOSE,
            SoundEvents.IRON_TRAPDOOR_OPEN,
            SoundEvents.METAL_PRESSURE_PLATE_CLICK_OFF,
            SoundEvents.METAL_PRESSURE_PLATE_CLICK_ON,
            SoundEvents.STONE_BUTTON_CLICK_OFF,
            SoundEvents.STONE_BUTTON_CLICK_ON);

    public BlockModDoor(Properties properties) {
        super(HAND_OPENABLE_METAL, properties);
    }
}
