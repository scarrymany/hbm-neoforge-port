package com.hbm.handler;

import com.hbm.config.MobConfig;
import com.hbm.entity.mob.EntityRADBeast;
import com.hbm.entity.mob.RadBeastEntityTypes;
import com.hbm.main.MainRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.List;

/**
 * Drives CE's RAD-Beast-only "elemental" slice of {@code com.hbm.handler.BossSpawnHandler.rollTheDice}
 * (250 lines, read in full) - see {@code docs/phase4/entities_bosses.md}'s RAD Beast section and this
 * task's own brief. Sibling of {@code MaskmanSpawnHandler} (same self-subscribing
 * {@code ServerTickEvent.Pre} pattern, same file-per-roll-type split that class's own javadoc
 * establishes) - deliberately does <b>not</b> port {@code rollTheDice}'s MaskMan/FBI-raid/meteor-strike
 * roll types (owned by {@code MaskmanSpawnHandler}/{@code MeteorStrikeHandler} already, or explicitly
 * out of this whole implement wave's scope per this task's own brief - see this package's knownGaps).
 * <p>
 * <b>CE's real gate this task's own brief did not name, preserved anyway</b> (CE is this task's sole
 * source of behavior/numbers): real CE's elemental roll additionally requires a per-player
 * {@code "radMark"} persisted-NBT boolean flag to already be {@code true} before it will ever fire -
 * confirmed by direct read of {@code BossSpawnHandler.java:128-161}. CE only ever sets that flag from
 * {@code TileEntityReactorResearch}/{@code TileEntityReactorZirnox} (reactor-meltdown escalation, Phase 2
 * territory not touched by this package - neither tile entity is ported in this port yet). This is the
 * exact same "escalation flag set by a different, not-yet-ported subsystem" shape as
 * {@code BossSpawnHandler.markFBI}, which {@code docs/phase4/entities_bosses.md}'s own Deferred scope
 * already names for the FBI-raid roll - preserved here for the same reason (silently dropping the gate
 * would make elementals spawn far more often than real CE ever does, a real behavioral divergence;
 * preserving it correctly-but-currently-unreachable matches this port's established convention for
 * mechanics gated on a sibling package that hasn't landed yet, e.g. {@code EntityCreeperTainted}'s
 * {@code BlockTaint}-gated spawn path). {@link net.minecraft.world.entity.Entity#getPersistentData()}
 * (the exact 1:1 modern replacement for CE's {@code EntityPlayer.PERSISTED_NBT_TAG} pattern) is a
 * long-stable Forge/NeoForge patch, not verified against a compiled jar in this sandbox - same
 * confidence tier this task's own foundation-wave code already accepts for e.g. {@code ServerBossEvent}.
 * Once reactor-meltdown content lands and starts setting this flag, this roll starts firing with zero
 * further changes needed here.
 * <p>
 * <b>Real CE bug found and fixed</b>: CE's own elemental-roll spawn-position vector reuses
 * {@code MobConfig.raidAttackDistance} (the <em>FBI raid</em> system's distance field) instead of its
 * own separately-declared-but-dead {@code elementalAttackDistance} config field - confirmed by an
 * exhaustive grep finding zero non-declaration references to {@code elementalAttackDistance} anywhere in
 * CE. This port's own {@code MobConfig.ELEMENTAL_ATTACK_DISTANCE} (default 32, identical to CE's real
 * live default via {@code raidAttackDistance}) is used here instead - the intended value CE's own dead
 * field documents, not a behavior change at default settings, just no longer silently depending on an
 * unrelated system's config field.
 */
@EventBusSubscriber(modid = MainRegistry.MODID)
public final class RadiationElementalSpawnHandler {

    private RadiationElementalSpawnHandler() {
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Pre event) {
        if (!MobConfig.ENABLE_MELTDOWN_ELEMENTALS.get()) return;

        for (ServerLevel level : event.getServer().getAllLevels()) {
            rollTheDice(level);
        }
    }

    private static void rollTheDice(ServerLevel level) {
        int delay = MobConfig.ELEMENTAL_DELAY.get();
        if (delay <= 0 || level.getGameTime() % delay != 0) return;

        List<ServerPlayer> players = level.players();
        // CE: world.provider.isSurfaceWorld() - hasSkyLight() is this port's already-established
        // modern equivalent (see MaskmanSpawnHandler).
        if (players.isEmpty() || !level.dimensionType().hasSkyLight()) return;

        RandomSource random = level.getRandom();
        if (random.nextInt(Math.max(1, MobConfig.ELEMENTAL_CHANCE.get())) != 0) return;

        ServerPlayer player = players.get(random.nextInt(players.size()));

        // CE: player.getEntityData().getCompoundTag(PERSISTED_NBT_TAG).getBoolean("radMark") - see
        // class javadoc for why this flag never currently gets set to true in this port.
        CompoundTag persisted = player.getPersistentData();
        if (!persisted.getBoolean("radMark")) return;
        persisted.putBoolean("radMark", false);

        player.sendSystemMessage(Component.literal("You hear a faint clicking...").withStyle(ChatFormatting.YELLOW));

        double distance = MobConfig.ELEMENTAL_ATTACK_DISTANCE.get();
        int amount = MobConfig.ELEMENTAL_AMOUNT.get();

        for (int i = 0; i < amount; i++) {
            double angle = random.nextDouble() * Math.PI * 2D;
            double spawnX = player.getX() + Math.cos(angle) * distance + random.nextGaussian();
            double spawnZ = player.getZ() + Math.sin(angle) * distance + random.nextGaussian();
            double spawnY = level.getHeight(Heightmap.Types.WORLD_SURFACE, (int) spawnX, (int) spawnZ);

            EntityRADBeast beast = new EntityRADBeast(RadBeastEntityTypes.RAD_BEAST.get(), level);
            beast.moveTo(spawnX, spawnY, spawnZ, random.nextFloat() * 360.0F, 0.0F);
            // CE: `if(i == 0) rad.makeLeader();` - the first elemental in each wave is the pack leader.
            if (i == 0) beast.makeLeader();
            level.addFreshEntity(beast);
        }
    }
}
