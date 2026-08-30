package com.hbm.blocks.generic;

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
 * CE's custom {@code HBMSoundHandler.openDoor} sound event does not exist in this port tree yet;
 * vanilla's iron door open/close sounds are used as the closest stand-in until it lands. CE's
 * {@code INBTBlockTransformable} structure-rotation hook is intentionally not implemented yet, per
 * the port report's cross-cutting note - it only affects correctness under world-gen structure
 * rotation, not placement/interaction as a plain block.
 */
public class BlockModDoor extends DoorBlock {

    public static final BlockSetType HAND_OPENABLE_METAL = new BlockSetType(
            "hbm_hand_openable_metal",
            true,
            false,
            false,
            BlockSetType.PressurePlateSensitivity.EVERYTHING,
            SoundType.METAL,
            SoundEvents.IRON_DOOR_CLOSE,
            SoundEvents.IRON_DOOR_OPEN,
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
