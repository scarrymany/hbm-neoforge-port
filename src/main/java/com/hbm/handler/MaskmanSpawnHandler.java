package com.hbm.handler;

import com.hbm.blocks.machine.ProcessingBlocks;
import com.hbm.config.MobConfig;
import com.hbm.entity.mob.EntityMaskMan;
import com.hbm.entity.mob.MaskmanEntityTypes;
import com.hbm.main.MainRegistry;
import com.hbm.util.ContaminationUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.List;

/**
 * Drives CE's MaskMan-only slice of {@code com.hbm.handler.BossSpawnHandler.rollTheDice} (250 lines,
 * read in full) - see {@code docs/phase4/entities_bosses.md}. Deliberately does <b>not</b> port
 * {@code rollTheDice}'s other 3 roll types: meteor strikes are already owned by
 * {@link MeteorStrikeHandler} ({@code docs/phase4/meteor_events.md}); FBI-raid and RAD-Beast
 * "elemental" rolls belong to whichever package researches {@code EntityFBI}/{@code EntityFBIDrone}/
 * {@code EntityRADBeast}, per this report's own Deferred scope.
 * <p>
 * Self-subscribing {@code @EventBusSubscriber} on {@code ServerTickEvent.Pre}, matching
 * {@link MeteorStrikeHandler}/{@code com.hbm.handler.neutron.NeutronHandler}'s already-established
 * convention for exactly this class of problem (no shared, multi-package event-handler aggregator
 * file to modify).
 * <p>
 * <b>Real CE gate this task's own brief did not name, preserved anyway</b> (CE is this task's sole
 * source of behavior/numbers): CE's real {@code rollTheDice}, confirmed by direct read of
 * {@code BossSpawnHandler.java:41-66}, additionally requires the randomly-chosen player to have
 * crafted OR placed at least one {@code machine_crystallizer} (a vanilla-stats check -
 * {@code StatList.getCraftStats}/{@code getObjectUseStats} in CE, ported here as
 * {@code Stats.ITEM_CRAFTED}/{@code Stats.ITEM_USED}) before MaskMan will ever target them, on top of
 * the 5 {@code MobConfig} fields the task brief does name. Not mentioned in
 * {@code docs/phase4/entities_bosses.md}'s own MaskMan spawn-mechanism row either - preserved for
 * behavioral fidelity regardless, per this task's own ground rules (CE is authoritative over the
 * report's summary of it).
 */
@EventBusSubscriber(modid = MainRegistry.MODID)
public final class MaskmanSpawnHandler {

    private MaskmanSpawnHandler() {
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Pre event) {
        if (!MobConfig.ENABLE_MASKMAN.get()) return;

        // CE's ModEventHandler#worldTick calls rollTheDice(World) once per loaded World per tick (one
        // Forge WorldTickEvent per loaded dimension in 1.12) - mirrored here by iterating every loaded
        // ServerLevel, matching MeteorStrikeHandler's own already-established precedent.
        for (ServerLevel level : event.getServer().getAllLevels()) {
            rollTheDice(level);
        }
    }

    private static void rollTheDice(ServerLevel level) {
        int delay = MobConfig.MASKMAN_DELAY.get();
        if (delay <= 0 || level.getGameTime() % delay != 0) return;

        List<ServerPlayer> players = level.players();
        // CE: world.provider.isSurfaceWorld() - hasSkyLight() is this port's already-established
        // modern equivalent concept (true for the Overworld, false for Nether/End-like dimensions).
        if (players.isEmpty() || !level.dimensionType().hasSkyLight()) return;

        RandomSource random = level.getRandom();
        if (random.nextInt(Math.max(1, MobConfig.MASKMAN_CHANCE.get())) != 0) return;

        ServerPlayer player = players.get(random.nextInt(players.size()));

        // CE: the acidizer/crystallizer stat gate - see class javadoc.
        Item crystallizerItem = ProcessingBlocks.MACHINE_CRYSTALLIZER.get().asItem();
        boolean acidizerStat = player.getStats().getValue(Stats.ITEM_CRAFTED.get(crystallizerItem)) > 0
                || player.getStats().getValue(Stats.ITEM_USED.get(crystallizerItem)) > 0;
        if (!acidizerStat) return;

        if (ContaminationUtil.getRads(player) < MobConfig.MASKMAN_MIN_RAD.get()) return;

        int surfaceHeight = level.getHeight(Heightmap.Types.WORLD_SURFACE, (int) player.getX(), (int) player.getZ());
        boolean underground = surfaceHeight > player.getY() + 3;
        if (MobConfig.MASKMAN_UNDERGROUND.get() && !underground) return;

        player.sendSystemMessage(Component.literal("The mask man is about to claim another victim.")
                .withStyle(ChatFormatting.RED));

        double spawnX = player.getX() + random.nextGaussian() * 20D;
        double spawnZ = player.getZ() + random.nextGaussian() * 20D;
        double spawnY = level.getHeight(Heightmap.Types.WORLD_SURFACE, (int) spawnX, (int) spawnZ);

        EntityMaskMan maskMan = new EntityMaskMan(MaskmanEntityTypes.MASK_MAN.get(), level);
        maskMan.moveTo(spawnX, spawnY, spawnZ, random.nextFloat() * 360.0F, 0.0F);
        level.addFreshEntity(maskMan);
    }
}
