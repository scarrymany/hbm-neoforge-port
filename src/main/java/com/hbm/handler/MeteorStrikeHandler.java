package com.hbm.handler;

import com.hbm.config.GeneralConfig;
import com.hbm.config.WorldConfig;
import com.hbm.entity.projectile.EntityMeteor;
import com.hbm.items.armor.ModCharmItems;
import com.hbm.main.MainRegistry;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.List;

/**
 * Drives CE's live meteor-strike roll once per server tick. Ported from the meteor-specific ~70
 * lines of CE's {@code com.hbm.handler.BossSpawnHandler} (the shared static fields at line 37/185,
 * {@code meteorUpdate}/{@code spawnMeteorAtPlayer} at lines 186-248, and the
 * {@code WorldConfig.enableMeteorStrikes} gate at line 163) - see {@code docs/phase4/
 * meteor_events.md}. Deliberately does <b>not</b> port {@code rollTheDice}'s other three roll types
 * (maskman/FBI-raid/RAD-beast-elemental spawns) - those belong to whichever package researches
 * {@code EntityMaskMan}/{@code EntityFBI}/{@code EntityFBIDrone}/{@code EntityRADBeast}, per this
 * report's own Deferred scope.
 * <p>
 * Self-subscribing {@code @EventBusSubscriber} on {@code ServerTickEvent.Pre}, matching this port's
 * already-established convention for exactly this class of problem
 * ({@code com.hbm.handler.neutron.NeutronHandler}'s javadoc explains the same rationale: no shared,
 * multi-package event-handler aggregator file to modify).
 */
@EventBusSubscriber(modid = MainRegistry.MODID)
public final class MeteorStrikeHandler {

    private MeteorStrikeHandler() {
    }

    /**
     * CE: {@code BossSpawnHandler.meteorRand} - "because some dimwit keeps resetting the world
     * rand", a private static {@code Random} deliberately independent of any one level's own RNG.
     * {@link RandomSource#create()} is the equivalent unseeded, not-tied-to-any-world generator.
     */
    private static final RandomSource METEOR_RAND = RandomSource.create();

    /**
     * CE: {@code BossSpawnHandler.meteorShower} - a single, deliberately <b>shared</b> static
     * counter across every loaded dimension, not a per-dimension one. With N dimensions loaded, this
     * counter decays N times per real game tick and the shower-start roll is independently re-rolled
     * N times per tick too - even though the {@code Level.OVERWORLD} gate below means a shower can
     * only ever actually manifest in the Overworld. Confirmed CE quirk, not a bug introduced here
     * (see {@code docs/phase4/meteor_events.md}'s Open questions, which reads
     * {@code BossSpawnHandler.java:37,185-225} and {@code ModEventHandler.java:662-672} in full).
     * <p>
     * <b>DECISION (explicit, not silent)</b>: this port preserves CE's shared-counter quirk verbatim
     * for faithful behavior parity, rather than adopting Neo Edition's non-canonical
     * {@code Map<String, Integer>} per-dimension fix. The report treats either choice as defensible
     * but flags silently picking one without saying so as bad practice - this comment is that
     * disclosure.
     */
    private static int meteorShower = 0;

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Pre event) {
        if (!WorldConfig.ENABLE_METEOR_STRIKES.get()) return;

        // CE's ModEventHandler#worldTick calls rollTheDice(World)/meteorUpdate(world) once per
        // loaded World per tick (one Forge WorldTickEvent per loaded dimension in 1.12) - mirrored
        // here by iterating every loaded ServerLevel, which is exactly what drives the shared-counter
        // quirk documented on meteorShower above.
        for (ServerLevel level : event.getServer().getAllLevels()) {
            meteorUpdate(level);
        }
    }

    private static void meteorUpdate(ServerLevel level) {
        int chance = meteorShower > 0 ? WorldConfig.METEOR_SHOWER_CHANCE.get() : WorldConfig.METEOR_STRIKE_CHANCE.get();

        if (METEOR_RAND.nextInt(chance) == 0) {
            List<ServerPlayer> players = level.players();
            // CE: `p.dimension == 0` - preserved verbatim (not dropped, unlike Neo Edition's own
            // MeteorStrikeSystem.meteorUpdate, which iterates every loaded level with no dimension
            // filter at all). level.players() is already scoped to this one ServerLevel, exactly as
            // CE's own per-dimension world.playerEntities list was.
            if (!players.isEmpty() && level.dimension() == Level.OVERWORLD) {
                ServerPlayer p = players.get(level.getRandom().nextInt(players.size()));

                boolean repel = false;
                boolean strike = true;

                ItemStack armor = p.getItemBySlot(EquipmentSlot.CHEST);
                if (!armor.isEmpty() && ArmorModHandler.hasMods(armor)) {
                    ItemStack mod = ArmorModHandler.pryMods(armor)[ArmorModHandler.helmet_only];

                    if (!mod.isEmpty()) {
                        if (mod.getItem() == ModCharmItems.PROTECTION_CHARM.get()) {
                            repel = true;
                        }
                        if (mod.getItem() == ModCharmItems.METEOR_CHARM.get()) {
                            strike = false;
                        }
                    }
                }

                if (strike) spawnMeteorAtPlayer(p, repel);
            }
        }

        if (meteorShower > 0) {
            meteorShower--;
            if (meteorShower == 0 && GeneralConfig.ENABLE_EXTENDED_LOGGING.get()) {
                MainRegistry.logger.info("Ended meteor shower.");
            }
        }

        if (METEOR_RAND.nextInt(WorldConfig.METEOR_STRIKE_CHANCE.get() * 100) == 0 && WorldConfig.ENABLE_METEOR_SHOWERS.get()) {
            int duration = WorldConfig.METEOR_SHOWER_DURATION.get();
            meteorShower = (int) (duration * 0.75 + duration * 0.25 * METEOR_RAND.nextFloat());

            if (GeneralConfig.ENABLE_EXTENDED_LOGGING.get()) {
                MainRegistry.logger.info("Started meteor shower! Duration: " + meteorShower);
            }
        }
    }

    /**
     * CE: {@code BossSpawnHandler.spawnMeteorAtPlayer}. {@code repel} corresponds to
     * {@code protection_charm}'s "divert" branch: the meteor's horizontal drift is redirected away
     * from the player instead of toward/past them, and {@link EntityMeteor#safe} is set - but per
     * this report's Open questions, the 1000-damage impact AoE still fires unconditionally at the
     * (redirected) landing spot; only block damage is suppressed.
     */
    public static void spawnMeteorAtPlayer(ServerPlayer player, boolean repel) {
        Level level = player.level();
        EntityMeteor meteor = new EntityMeteor(level);

        double spawnX = player.getX() + METEOR_RAND.nextInt(201) - 100;
        double spawnY = 384;
        double spawnZ = player.getZ() + METEOR_RAND.nextInt(201) - 100;
        meteor.setPos(spawnX, spawnY, spawnZ);

        double vx;
        double vz;
        if (repel) {
            // CE: new MutableVec3d(meteor.posX - player.posX, 0, meteor.posZ - player.posZ).normalizeSelf(),
            // scaled by a random 0..1 speed.
            double dx = spawnX - player.getX();
            double dz = spawnZ - player.getZ();
            double len = Math.sqrt(dx * dx + dz * dz);
            if (len < 1.0E-4D) {
                dx = 0D;
                dz = 0D;
            } else {
                dx /= len;
                dz /= len;
            }
            double vel = METEOR_RAND.nextDouble();
            vx = dx * vel;
            vz = dz * vel;
            meteor.safe = true;
        } else {
            // CE: new MutableVec3d(meteorRand.nextDouble() - 0.5D, 0, 0).rotateYawSelf(PI * rand.nextDouble()).
            // MutableVec3d#rotateYawSelf(yaw): nx = x*cos(yaw) + z*sin(yaw), nz = z*cos(yaw) - x*sin(yaw);
            // with z=0 this reduces to nx = x*cos(yaw), nz = -x*sin(yaw).
            double x0 = METEOR_RAND.nextDouble() - 0.5D;
            double yaw = Math.PI * METEOR_RAND.nextDouble();
            vx = x0 * Math.cos(yaw);
            vz = -x0 * Math.sin(yaw);
        }

        meteor.setDeltaMovement(vx, -2.5D, vz);
        level.addFreshEntity(meteor);
    }
}
