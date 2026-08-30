package com.hbm.items.machine;

import com.hbm.items.ItemBase;
import com.hbm.lib.HBMSoundHandler;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

/**
 * Siren sound cassette. The single registered item carries a track reference as a data component
 * instead of metadata; {@link TrackType} is an open, dynamically-registered pseudo-registry (CE's
 * own design - {@link TrackType#register} lets other code add tracks at runtime), so unlike the
 * other metadata-multi items in this package it is deliberately not flattened into one item per
 * track.
 */
public class ItemCassette extends ItemBase {

    public ItemCassette(Properties properties) {
        super(properties.stacksTo(1));
    }

    public static TrackType getType(ItemStack stack) {
        if (stack == null || !(stack.getItem() instanceof ItemCassette)) return TrackType.NULL;
        return TrackType.byIndex(stack.getOrDefault(MachineDataComponents.CASSETTE_TRACK.get(), 0));
    }

    public static void setType(ItemStack stack, TrackType type) {
        stack.set(MachineDataComponents.CASSETTE_TRACK.get(), type.getId());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, java.util.List<Component> tooltip, TooltipFlag flag) {
        TrackType type = getType(stack);
        tooltip.add(Component.literal("Siren sound cassette:"));
        tooltip.add(Component.literal("   Name: " + type.getTrackTitle()));
        tooltip.add(Component.literal("   Type: " + type.getSoundType().name()));
        tooltip.add(Component.literal("   Volume: " + type.getVolume()));
    }

    public enum SoundType {
        LOOP, PASS, SOUND
    }

    @SuppressWarnings("unused")
    public static class TrackType {

        public static final Int2ObjectMap<TrackType> VALUES = new Int2ObjectArrayMap<>(20);

        public static final TrackType NULL = new TrackType(" ", null, SoundType.SOUND, 0, 0, 0);
        public static final TrackType HATCH = new TrackType("Hatch Siren", HBMSoundHandler.alarmHatch, SoundType.LOOP, 3358839, 250, 1);
        public static final TrackType AUTOPILOT = new TrackType("Autopilot Disconnected", HBMSoundHandler.alarmAutopilot, SoundType.LOOP, 11908533, 50, 2);
        public static final TrackType AMS_SIREN = new TrackType("AMS Siren", HBMSoundHandler.alarmAMSSiren, SoundType.LOOP, 15055698, 50, 3);
        public static final TrackType BLAST_DOOR = new TrackType("Blast Door Alarm", HBMSoundHandler.alarmBlastDoor, SoundType.LOOP, 11665408, 50, 4);
        public static final TrackType APC_LOOP = new TrackType("APC Siren", HBMSoundHandler.alarmAPCLoop, SoundType.LOOP, 3565216, 50, 5);
        public static final TrackType KLAXON = new TrackType("Klaxon", HBMSoundHandler.alarmKlaxon, SoundType.LOOP, 8421504, 50, 6);
        public static final TrackType KLAXON_A = new TrackType("Vault Door Alarm", HBMSoundHandler.alarmFoKlaxonA, SoundType.LOOP, 0x8c810b, 50, 7);
        public static final TrackType KLAXON_B = new TrackType("Security Alert", HBMSoundHandler.alarmFoKlaxonB, SoundType.LOOP, 0x76818e, 50, 8);
        public static final TrackType SIREN = new TrackType("Standard Siren", HBMSoundHandler.alarmRegular, SoundType.LOOP, 6684672, 100, 9);
        public static final TrackType CLASSIC = new TrackType("Classic Siren", HBMSoundHandler.alarmClassic, SoundType.LOOP, 0xc0cfe8, 100, 10);
        public static final TrackType BANK_ALARM = new TrackType("Bank Alarm", HBMSoundHandler.alarmBank, SoundType.LOOP, 3572962, 100, 11);
        public static final TrackType BEEP_SIREN = new TrackType("Beep Siren", HBMSoundHandler.alarmBeep, SoundType.LOOP, 13882323, 100, 12);
        public static final TrackType CONTAINER_ALARM = new TrackType("Container Alarm", HBMSoundHandler.alarmContainer, SoundType.LOOP, 14727839, 100, 13);
        public static final TrackType SWEEP_SIREN = new TrackType("Sweep Siren", HBMSoundHandler.alarmSweep, SoundType.LOOP, 15592026, 500, 14);
        public static final TrackType STRIDER_SIREN = new TrackType("Missile Silo Siren", HBMSoundHandler.alarmStrider, SoundType.LOOP, 11250586, 500, 15);
        public static final TrackType AIR_RAID = new TrackType("Air Raid Siren", HBMSoundHandler.alarmAirRaid, SoundType.LOOP, 0xDF3795, 500, 16);
        public static final TrackType NOSTROMO_SIREN = new TrackType("Nostromo Self Destruct", HBMSoundHandler.alarmNostromo, SoundType.LOOP, 0x5dd800, 100, 17);
        public static final TrackType EAS_ALARM = new TrackType("EAS Alarm Screech", HBMSoundHandler.alarmEas, SoundType.LOOP, 0xb3a8c1, 50, 18);
        public static final TrackType APC_PASS = new TrackType("APC Pass", HBMSoundHandler.alarmAPCPass, SoundType.PASS, 3422163, 50, 19);
        public static final TrackType RAZORTRAIN = new TrackType("Razortrain Horn", HBMSoundHandler.alarmRazorTrain, SoundType.SOUND, 7819501, 250, 20);

        private static final AtomicInteger nextId = new AtomicInteger(21);

        private final String title;
        private final Supplier<SoundEvent> location;
        private final SoundType type;
        private final int color;
        private final int volume;
        private final int id;

        private TrackType(String title, Supplier<SoundEvent> location, SoundType type, int color, int volume, int id) {
            this.title = title;
            this.location = location;
            this.type = type;
            this.color = color;
            this.volume = volume;
            this.id = id;
            VALUES.put(id, this);
        }

        public static TrackType register(String title, Supplier<SoundEvent> location, SoundType type, int color, int volume) {
            return new TrackType(title, location, type, color, volume, nextId.getAndIncrement());
        }

        public String getTrackTitle() { return this.title; }
        public SoundEvent getSoundLocation() { return this.location == null ? null : this.location.get(); }
        public SoundType getSoundType() { return this.type; }
        public int getColor() { return this.color; }
        public int getVolume() { return this.volume; }
        public int getId() { return this.id; }

        public static TrackType byIndex(int i) {
            TrackType track = VALUES.get(i);
            return track != null ? track : NULL;
        }
    }
}
