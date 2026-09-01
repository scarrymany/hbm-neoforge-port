package com.hbm.tileentity;

import com.hbm.blockentity.machine.DoorGenericBlockEntity;
import com.hbm.interfaces.IDoor;
import com.hbm.lib.HBMSoundHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.AABB;

import javax.annotation.Nullable;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * CE {@code DoorDecl} numbers for live {@link com.hbm.blocks.generic.BlockDoorGeneric}.
 * TODO(CE: DoorDecl.java:1285-1292): SEDNA BusAnimation / IRenderDoors TESR — cube models this wave.
 */
public class DoorDecl {

    public static final AABB FULL = new AABB(0, 0, 0, 1, 1, 1);
    public static final AABB EMPTY = new AABB(0, 0, 0, 0, 0, 0);

    public static final DoorDecl VAULT_DOOR = new DoorDecl(
            120, new int[]{4, 0, 0, 0, 2, 2}, new int[][]{{-1, 1, 0, 3, 3, 2}}, 0,
            new int[][]{{0, 0, 1, -1, 2, 2}}, false, true,
            (rel, open) -> !open || rel.getY() == 0 ? FULL : EMPTY, null)
            .tick(door -> {
                if (door.getLevel() == null || door.getLevel().isClientSide) return;
                IDoor.DoorState st = door.state;
                int t = door.openTicks;
                if (st == IDoor.DoorState.OPENING) {
                    if (t == 0) play(door, HBMSoundHandler.vaultScrapeNew, 1F);
                    for (int i = 45; i <= 115; i += 10) {
                        if (t == i) play(door, HBMSoundHandler.vaultThudNew, 1F);
                    }
                } else if (st == IDoor.DoorState.CLOSING) {
                    if (t == 30) play(door, HBMSoundHandler.vaultScrapeNew, 1F);
                    for (int i = 45; i <= 115; i += 10) {
                        if (t == i) play(door, HBMSoundHandler.vaultThudNew, 1F);
                    }
                }
            });

    public static final DoorDecl SLIDING_SEAL_DOOR = new DoorDecl(
            20, new int[]{1, 0, 0, 0, 0, 0}, new int[][]{{0, 0, 0, 1, 2, 2}}, 0,
            null, false, false,
            (rel, open) -> open
                    ? (rel.getY() == 0 ? new AABB(0, 0, 0.75, 1, 0.125, 1) : EMPTY)
                    : new AABB(0, 0, 0.75, 1, 1, 1),
            HBMSoundHandler.sliding_seal_open).end(HBMSoundHandler.nullMine).vol(1F);

    public static final DoorDecl FIRE_DOOR = new DoorDecl(
            160, new int[]{2, 0, 0, 0, 2, 1}, new int[][]{{-1, 0, 0, 3, 4, 1}}, 0,
            null, false, true,
            (rel, open) -> {
                if (!open) return FULL;
                if (rel.getZ() == 1) return new AABB(0.5, 0, 0, 1, 1, 1);
                if (rel.getZ() == -2) return new AABB(0, 0, 0, 0.5, 1, 1);
                if (rel.getY() > 1) return new AABB(0, 0.75, 0, 1, 1, 1);
                if (rel.getY() == 0) return new AABB(0, 0, 0, 1, 0.1, 1);
                return EMPTY;
            }, HBMSoundHandler.wgh_start).end(HBMSoundHandler.wgh_stop).loop2(HBMSoundHandler.alarm6).vol(2F);

    public static final DoorDecl SLIDE_DOOR = new DoorDecl(
            24, new int[]{3, 0, 0, 0, 3, 3}, new int[][]{{-2, 0, 0, 4, 5, 1}}, 0,
            null, false, true,
            (rel, open) -> {
                if (!open) return FULL;
                if (rel.getY() == 3) return new AABB(0, 0.5, 0, 1, 1, 1);
                if (rel.getY() == 0) return new AABB(0, 0, 0, 1, 0.08, 1);
                return EMPTY;
            }, HBMSoundHandler.qe_sliding_opening).end(HBMSoundHandler.qe_sliding_opened)
            .closeEnd(HBMSoundHandler.qe_sliding_shut).loop2(HBMSoundHandler.qe_sliding_opening).vol(2F);

    public static final DoorDecl QE_SLIDING = new DoorDecl(
            10, new int[]{1, 0, 0, 0, 1, 0}, new int[][]{{0, 0, 0, 2, 2, 2}}, 0,
            null, false, false,
            (rel, open) -> {
                if (!open) return new AABB(0, 0, 0.875, 1, 1, 1);
                if (rel.getZ() == 0) return new AABB(0.875, 0, 0.875, 1, 1, 1);
                return new AABB(0, 0, 0.875, 0.125, 1, 1);
            }, HBMSoundHandler.qe_sliding_opening).end(HBMSoundHandler.qe_sliding_opened)
            .closeEnd(HBMSoundHandler.qe_sliding_shut).vol(2F);

    public static final DoorDecl QE_CONTAINMENT = new DoorDecl(
            160, new int[]{2, 0, 0, 0, 1, 1}, new int[][]{{-1, 0, 0, 3, 3, 1}}, 0,
            null, false, true,
            (rel, open) -> {
                if (!open) return new AABB(0, 0, 0.5, 1, 1, 1);
                if (rel.getY() > 1) return new AABB(0, 0.5, 0.5, 1, 1, 1);
                if (rel.getY() == 0) return new AABB(0, 0, 0.5, 1, 0.1, 1);
                return EMPTY;
            }, HBMSoundHandler.wgh_start).end(HBMSoundHandler.wgh_stop).vol(2F);

    public static final DoorDecl WATER_DOOR = new DoorDecl(
            60, new int[]{2, 0, 0, 0, 1, 1}, new int[][]{{1, 0, 0, -3, 3, 2}}, 0,
            null, false, true,
            (rel, open) -> {
                if (!open) return new AABB(0, 0, 0.75, 1, 1, 1);
                if (rel.getY() > 1) return new AABB(0, 0.85, 0.75, 1, 1, 1);
                if (rel.getY() == 0) return new AABB(0, 0, 0.75, 1, 0.15, 1);
                return EMPTY;
            }, HBMSoundHandler.wgh_big_start).end(HBMSoundHandler.wgh_big_stop)
            .start(HBMSoundHandler.door_spinny).closeStart(null).closeEnd(HBMSoundHandler.door_spinny)
            .rangeTime((ticks, idx) -> remap01(ticks, 35, 40)).vol(2F);

    public static final DoorDecl LARGE_VEHICLE_DOOR = new DoorDecl(
            60, new int[]{5, 0, 0, 0, 3, 3}, new int[][]{{0, 0, 0, -4, 6, 2}, {0, 0, 0, 4, 6, 2}}, 0,
            null, false, false,
            (rel, open) -> {
                if (!open) return FULL;
                if (rel.getZ() == 3) return new AABB(0.4, 0, 0, 1, 1, 1);
                if (rel.getZ() == -3) return new AABB(0, 0, 0, 0.6, 1, 1);
                return EMPTY;
            }, HBMSoundHandler.garage).end(HBMSoundHandler.garage_stop).vol(2F);

    public static final DoorDecl CARGO_DOOR = new DoorDecl(
            60, new int[]{2, 0, 0, 0, 1, 1}, new int[][]{{-1, -1, 0, 3, 3, 1}}, 0,
            null, false, false,
            (rel, open) -> {
                if (!open) return new AABB(0, 0, 0.375, 1, 1, 0.625);
                if (rel.getY() > 1) return new AABB(0, 0.25, 0.375, 1, 1, 0.625);
                if (rel.getY() == 0) return new AABB(0, 0, 0.375, 1, 0.125, 0.625);
                return EMPTY;
            }, HBMSoundHandler.garage).end(HBMSoundHandler.garage_stop)
            .closeLoop(HBMSoundHandler.garage).closeEnd(HBMSoundHandler.garage_stop).vol(2F);

    public static final DoorDecl SILO_HATCH = new DoorDecl(
            60, new int[]{0, 0, 2, 2, 2, 2},
            new int[][]{{1, 0, 1, -3, 3, 0}, {0, 0, 1, -3, 3, 0}, {-1, 0, 1, -3, 3, 0}}, 2,
            null, true, false, (rel, open) -> open ? EMPTY : FULL,
            HBMSoundHandler.wgh_big_start).end(HBMSoundHandler.wgh_big_stop)
            .start(null).closeStart(null).closeEnd(HBMSoundHandler.wgh_big_stop)
            .rangeTime((ticks, idx) -> remap01(ticks, 20, 20)).vol(2F);

    public static final DoorDecl SILO_HATCH_LARGE = new DoorDecl(
            60, new int[]{0, 0, 3, 3, 3, 3},
            new int[][]{{2, 0, 1, -3, 3, 0}, {1, 0, 2, -5, 3, 0}, {0, 0, 2, -5, 3, 0},
                    {-1, 0, 2, -5, 3, 0}, {-2, 0, 1, -3, 3, 0}}, 3,
            null, true, false, (rel, open) -> open ? EMPTY : FULL,
            HBMSoundHandler.wgh_big_start).end(HBMSoundHandler.wgh_big_stop)
            .start(null).closeStart(null).closeEnd(HBMSoundHandler.wgh_big_stop)
            .rangeTime((ticks, idx) -> remap01(ticks, 20, 20)).vol(2F);

    public static final DoorDecl SECURE_ACCESS_DOOR = new DoorDecl(
            120, new int[]{4, 0, 0, 0, 2, 2}, new int[][]{{-2, 1, 0, 4, 5, 1}}, 0,
            null, false, true,
            (rel, open) -> {
                if (!open) return rel.getY() > 0 ? new AABB(0, 0, 0.375, 1, 1, 0.625) : FULL;
                if (rel.getY() == 1) return new AABB(0, 0, 0, 1, 0.0625, 1);
                if (rel.getY() == 4) return new AABB(0, 0.5, 0.15, 1, 1, 0.85);
                return EMPTY;
            }, HBMSoundHandler.garage).end(HBMSoundHandler.garage_stop).vol(2F);

    public static final DoorDecl ROUND_AIRLOCK_DOOR = new DoorDecl(
            60, new int[]{3, 0, 0, 0, 2, 1}, new int[][]{{0, 0, 0, -2, 4, 2}, {0, 0, 0, 3, 4, 2}}, 0,
            null, false, true,
            (rel, open) -> {
                if (!open) return FULL;
                if (rel.getZ() == 1) return new AABB(0.4, 0, 0, 1, 1, 1);
                if (rel.getZ() == -2) return new AABB(0, 0, 0, 0.6, 1, 1);
                if (rel.getY() == 3) return new AABB(0, 0.5, 0, 1, 1, 1);
                if (rel.getY() == 0) return new AABB(0, 0, 0, 1, 0.0625, 1);
                return EMPTY;
            }, HBMSoundHandler.garage).end(HBMSoundHandler.garage_stop).vol(2F);

    public static final DoorDecl TRANSITION_SEAL = new DoorDecl(
            480, new int[]{23, 0, 0, 0, 13, 12}, new int[][]{{-9, 2, 0, 20, 20, 1}}, 0,
            null, false, false, (rel, open) -> open ? EMPTY : FULL,
            HBMSoundHandler.transitionSealOpen).vol(6F);

    @FunctionalInterface
    public interface BoundFn {
        AABB apply(BlockPos rel, boolean open);
    }

    @FunctionalInterface
    public interface RangeTimeFn {
        float apply(int ticks, int idx);
    }

    private final int timeToOpen;
    private final int[] dimensions;
    @Nullable
    private final int[][] extraDimensions;
    private final int[][] doorOpenRanges;
    private final int blockOffset;
    private final boolean remoteControllable;
    private final boolean skins;
    private final BoundFn bounds;
    @Nullable
    private Supplier<SoundEvent> openLoop;
    @Nullable
    private Supplier<SoundEvent> openStart;
    @Nullable
    private Supplier<SoundEvent> openEnd;
    @Nullable
    private Supplier<SoundEvent> closeLoop;
    @Nullable
    private Supplier<SoundEvent> closeStart;
    @Nullable
    private Supplier<SoundEvent> closeEnd;
    @Nullable
    private Supplier<SoundEvent> loop2;
    private float volume = 1F;
    @Nullable
    private RangeTimeFn rangeTime;
    @Nullable
    private Consumer<DoorGenericBlockEntity> onUpdate;

    private DoorDecl(int time, int[] dims, int[][] ranges, int offset, @Nullable int[][] extra,
                     boolean remote, boolean skins, BoundFn bounds, @Nullable Supplier<SoundEvent> loop) {
        this.timeToOpen = time;
        this.dimensions = dims;
        this.doorOpenRanges = ranges;
        this.blockOffset = offset;
        this.extraDimensions = extra;
        this.remoteControllable = remote;
        this.skins = skins;
        this.bounds = bounds;
        this.openLoop = loop;
        this.closeLoop = loop;
    }

    private DoorDecl tick(Consumer<DoorGenericBlockEntity> u) {
        this.onUpdate = u;
        return this;
    }

    private DoorDecl start(@Nullable Supplier<SoundEvent> s) {
        this.openStart = s;
        return this;
    }

    private DoorDecl end(Supplier<SoundEvent> s) {
        this.openEnd = s;
        this.closeEnd = s;
        return this;
    }

    private DoorDecl closeEnd(Supplier<SoundEvent> s) {
        this.closeEnd = s;
        return this;
    }

    private DoorDecl closeStart(@Nullable Supplier<SoundEvent> s) {
        this.closeStart = s;
        return this;
    }

    private DoorDecl closeLoop(Supplier<SoundEvent> s) {
        this.closeLoop = s;
        return this;
    }

    private DoorDecl loop2(Supplier<SoundEvent> s) {
        this.loop2 = s;
        return this;
    }

    private DoorDecl vol(float v) {
        this.volume = v;
        return this;
    }

    private DoorDecl rangeTime(RangeTimeFn fn) {
        this.rangeTime = fn;
        return this;
    }

    public int timeToOpen() {
        return timeToOpen;
    }

    public int[] getDimensions() {
        return dimensions;
    }

    @Nullable
    public int[][] getExtraDimensions() {
        return extraDimensions;
    }

    public int[][] getDoorOpenRanges() {
        return doorOpenRanges;
    }

    public int getBlockOffset() {
        return blockOffset;
    }

    public boolean remoteControllable() {
        return remoteControllable;
    }

    public boolean hasSkins() {
        return skins;
    }

    public int getSkinCount() {
        return skins ? 7 : 0;
    }

    public boolean isLadder(boolean open) {
        return false;
    }

    public AABB getBlockBound(BlockPos rel, boolean open) {
        return bounds.apply(rel, open);
    }

    public float getDoorRangeOpenTime(int ticks, int idx) {
        return rangeTime != null ? rangeTime.apply(ticks, idx) : remap01(ticks, 0, timeToOpen);
    }

    @Nullable
    public Consumer<DoorGenericBlockEntity> onDoorUpdate() {
        return onUpdate;
    }

    @Nullable
    public SoundEvent getOpenSoundLoop() {
        return get(openLoop);
    }

    @Nullable
    public SoundEvent getCloseSoundLoop() {
        return get(closeLoop);
    }

    @Nullable
    public SoundEvent getOpenSoundStart() {
        return get(openStart);
    }

    @Nullable
    public SoundEvent getCloseSoundStart() {
        return closeStart != null ? get(closeStart) : get(openStart);
    }

    @Nullable
    public SoundEvent getOpenSoundEnd() {
        return get(openEnd);
    }

    @Nullable
    public SoundEvent getCloseSoundEnd() {
        return closeEnd != null ? get(closeEnd) : get(openEnd);
    }

    @Nullable
    public SoundEvent getSoundLoop2() {
        return get(loop2);
    }

    public float getSoundVolume() {
        return volume;
    }

    public static float remap01(float time, float min, float max) {
        if (max == min) return time >= max ? 1F : 0F;
        if (time < min) return 0F;
        if (time > max) return 1F;
        return (time - min) / (max - min);
    }

    @Nullable
    private static SoundEvent get(@Nullable Supplier<SoundEvent> s) {
        return s == null ? null : s.get();
    }

    private static void play(DoorGenericBlockEntity door, Supplier<SoundEvent> sound, float vol) {
        if (door.getLevel() == null) return;
        door.getLevel().playSound(null, door.getBlockPos(), sound.get(), SoundSource.BLOCKS, vol, 1F);
    }
}
