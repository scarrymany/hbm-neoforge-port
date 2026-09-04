package com.hbm.handler;

import com.hbm.capability.ContaminationEffect;
import com.hbm.capability.HbmLivingAttachment;
import com.hbm.capability.HbmLivingProps;
import com.hbm.capability.HbmPlayerAttachment;
import com.hbm.capability.ModAttachments;
import com.hbm.config.GeneralConfig;
import com.hbm.config.RadiationConfig;
import com.hbm.config.WorldConfig;
import com.hbm.damage.ModDamageTypes;
import com.hbm.handler.HbmKeybinds.EnumKeybind;
import com.hbm.interfaces.IArmorModDash;
import com.hbm.items.gear.ArmorFSB;
import com.hbm.lib.HBMSoundHandler;
import com.hbm.entity.mob.CreeperVariantEntityTypes;
import com.hbm.entity.mob.EntityCreeperNuclear;
import com.hbm.entity.mob.EntityDuck;
import com.hbm.entity.mob.EntityQuackos;
import com.hbm.entity.mob.EntityRADBeast;
import com.hbm.entity.mob.Phase4BossEntityTypes2;
import com.hbm.entity.mob.RadBeastEntityTypes;
import com.hbm.handler.pollution.PollutionHandler;
import com.hbm.handler.pollution.PollutionHandler.PollutionType;
import com.hbm.main.MainRegistry;
import com.hbm.potion.HbmPotionEffects;
import com.hbm.util.ArmorRegistry;
import com.hbm.util.ArmorRegistry.HazardClass;
import com.hbm.util.ContaminationUtil;
import com.hbm.util.ContaminationUtil.ContaminationType;
import com.hbm.util.ContaminationUtil.HazardType;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.animal.MushroomCow;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.ZombieVillager;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import java.util.Iterator;
import java.util.Random;

/**
 * Real, self-subscribing port of a narrow slice of CE's 759-line {@code com.hbm.handler.EntityEffectHandler}
 * - see this task's own brief and the 3 research reports it names ({@code entities_bosses.md}'s RAD Beast
 * section, {@code entities_creeper_variants.md}'s Headline finding #2, {@code pollution_system.md}'s
 * {@code handlePollution} reference). Deliberately implements <b>only</b>:
 * <ol>
 *     <li>the radiation-mutation cascade table (Creeper/Cow/Villager/Blaze/Duck), from CE's real
 *     {@code handleRadiationEffect} (lines 233-285 of the CE file, read in full);</li>
 *     <li>the crater-biome ambient-radiation tick, from CE's real {@code onUpdate} (lines 81-94, read
 *     in full);</li>
 *     <li>{@code handlePollution}'s two ambient-exposure branches (lines 572-607, read in full);</li>
 *     <li>the lead-poisoning-on-ore-break hook, from CE's real {@code ModEventHandler.blockBreak}
 *     (lines 1294-1307, read in full).</li>
 * </ol>
 * {@link #handleDashing} / {@link #handlePlinking} are Exact CE {@code :655-754}, dispatched from
 * {@code CommonTickEvents#onPlayerTick} (both sides). Dash-bar HUD stays skipped.
 * {@link #handleContamination}/{@link #handleLungDisease}/{@link #handleOil}/{@link #handleTemperature}
 * are Exact CE {@code :136-650} (server tick). Vomit/sweat/FlameCreator/Confetti packets stay skipped.
 * <p>
 * <b>Review pass finding (not fixed here)</b>: CE's real {@code handleRadiationEffect} table actually has
 * a <em>6th</em> branch this file's own scope list above omits - {@code eRad >= 800 && entity instanceof
 * EntityHorse -> EntityZombieHorse} (copying growing age/temper/saddled/tamed/owner, then
 * {@code makeMad()}). Not ported: CE's {@code temper}/{@code makeMad()} enrage-on-zombification have no
 * confirmed 1.21.1 {@code AbstractHorse}/{@code ZombieHorse} equivalent this review could verify without a
 * compiled jar, and guessing the modern saddle-equipment API wrong risks a real compile break in a sandbox
 * that cannot run Gradle - left as a documented gap rather than an unverified guess (see this task's own
 * remainingConcerns).
 * <p>
 * <b>Review pass fix</b>: {@link #handleMutationCascade} was missing CE's own top-level
 * {@code !GeneralConfig.enableRads -> return} gate ({@code [CE: 1.16_enableRadiation]}, this port's
 * {@link GeneralConfig#ENABLE_RADIATION}) - added, since without it the whole cascade kept mutating
 * entities even with radiation disabled server-wide (this file's own {@code handleCraterRadiation} and
 * {@code handlePollution} correctly gate on their own config flags already; this one branch didn't).
 * <p>
 * <b>Dispatch pattern</b>: a separate self-subscribing {@code @EventBusSubscriber} class on the same
 * {@link EntityTickEvent.Pre} event type {@code com.hbm.main.CommonTickEvents} already listens on -
 * "sitting alongside" it rather than editing that shared file or duplicating its dispatch loop.
 * NeoForge permits any number of independent subscribers to the same event type.
 * <p>
 * <b>Creeper -&gt; {@link EntityCreeperNuclear} at real CE's actual, broader condition</b>: CE's check is
 * literally {@code entity instanceof EntityCreeper} (vanilla's own base class) - since every one of this
 * port's 4 creeper variants ({@code EntityCreeperGold}/{@code Volatile}/{@code Phosgene}/{@code Tainted})
 * also extends {@link Creeper}, CE's real condition is broader than "vanilla Creeper" as this task's own
 * plain-language brief describes it. In practice this only matters for the 2 non-rad-immune variants
 * (Volatile/Phosgene) - Gold/Tainted/Nuclear are all {@code IRadiationImmune} and therefore never
 * accumulate real radiation via {@link ContaminationUtil#contaminate} in the first place. Preserved
 * faithfully (broader {@code instanceof Creeper} check) rather than narrowed to the task brief's own
 * summary, per this task's "CE is sole source of behavior" ground rule - with one added, harmless,
 * behavior-preserving guard (excluding {@link EntityCreeperNuclear} itself) to avoid a pointless
 * remutate-self edge case CE's own code technically doesn't special-case either (but which never fires
 * in practice, since Nuclear Creepers are themselves immune).
 * <p>
 * <b>Duck -&gt; {@link com.hbm.entity.mob.EntityQuackos} (&gt;=200 rad)</b>: implemented - the
 * boss-ufo-chopper-crabs sibling package landed {@code EntityDuck}/{@code EntityQuackos} (and their
 * {@code Phase4BossEntityTypes2} registry) while this package was in progress; re-checked at the end of
 * this implementation pass and wired for real, matching every other branch's {@link #mutate} shape.
 */
@EventBusSubscriber(modid = MainRegistry.MODID)
public final class EntityEffectHandler {

    private EntityEffectHandler() {
    }

    @SubscribeEvent
    public static void onLivingTick(EntityTickEvent.Pre event) {
        if (!(event.getEntity() instanceof LivingEntity entity)) return;
        if (!(entity.level() instanceof ServerLevel level)) return;

        handleCraterRadiation(entity, level);
        handleMutationCascade(entity, level);
        handlePollution(entity, level);
        handleContamination(entity);
        handleLungDisease(entity);
        handleOil(entity);
        handleTemperature(entity);
    }

    // ==================== crater-biome ambient radiation (CE onUpdate, lines 81-94) ====================

    /**
     * CE never gates this specific tick on {@code WorldConfig.enableCraterBiomes} (confirmed by a full
     * read of the real snippet) - that flag only gates whether {@code EntityFalloutRain} ever *paints* a
     * crater biome in the first place, not whether standing in one already-painted still radiates. Not
     * reproducing an extra gate CE's own code doesn't have here.
     */
    private static void handleCraterRadiation(LivingEntity entity, ServerLevel level) {
        ResourceKey<Biome> biome = level.getBiome(entity.blockPosition()).unwrapKey().orElse(null);

        double radiation;
        if (biome == com.hbm.world.biome.ModCraterBiomes.CRATER_OUTER) {
            radiation = WorldConfig.CRATER_BIOME_OUTER_RAD.get();
        } else if (biome == com.hbm.world.biome.ModCraterBiomes.CRATER) {
            radiation = WorldConfig.CRATER_BIOME_RAD.get();
        } else if (biome == com.hbm.world.biome.ModCraterBiomes.CRATER_INNER) {
            radiation = WorldConfig.CRATER_BIOME_INNER_RAD.get();
        } else {
            radiation = 0D;
        }

        if (radiation <= 0D) return;
        if (entity.isInWater()) radiation *= WorldConfig.CRATER_BIOME_WATER_MULT.get();
        if (radiation > 0D) {
            ContaminationUtil.contaminate(entity, HazardType.RADIATION, ContaminationType.CREATIVE, radiation / 20D);
        }
    }

    // ==================== radiation-mutation cascade (CE handleRadiationEffect, lines 233-285) =======

    private static void handleMutationCascade(LivingEntity entity, ServerLevel level) {
        // CE: handleRadiationEffect's own top-level gate - `if (!GeneralConfig.enableRads ...) return;`
        // ([CE: 1.16_enableRadiation], this port's GeneralConfig.ENABLE_RADIATION) - was missing here,
        // meaning the whole mutation cascade would still fire even with radiation disabled server-wide.
        if (!GeneralConfig.ENABLE_RADIATION.get()) return;
        if (!entity.isAlive()) return;
        if (entity instanceof Player player && (player.isCreative() || player.isSpectator())) return;

        double eRad = HbmLivingProps.getRadiation(entity);
        if (eRad < 50D) return;

        // See class javadoc: CE's real condition is `instanceof EntityCreeper` (broader than "vanilla
        // Creeper"), guarded here against remutating an already-Nuclear creeper (a harmless no-op in
        // real CE too, since Nuclear Creepers are radiation-immune and never reach this threshold).
        if (entity instanceof Creeper creeper && !(creeper instanceof EntityCreeperNuclear) && eRad >= 200D) {
            if (level.random.nextInt(3) == 0) {
                EntityCreeperNuclear mutated = new EntityCreeperNuclear(CreeperVariantEntityTypes.CREEPER_NUCLEAR.get(), level);
                mutate(creeper, mutated, level);
            } else {
                entity.hurt(entity.damageSources().source(ModDamageTypes.RADIATION), 100F);
            }
            return;
        }

        if (entity instanceof Cow cow && !(cow instanceof MushroomCow) && eRad >= 50D) {
            MushroomCow mooshroom = new MushroomCow(EntityType.MOOSHROOM, level);
            mutate(cow, mooshroom, level);
            return;
        }

        if (entity instanceof Villager villager && eRad >= 500D) {
            ZombieVillager zombie = new ZombieVillager(EntityType.ZOMBIE_VILLAGER, level);
            zombie.setVillagerData(villager.getVillagerData());
            zombie.setBaby(villager.isBaby());
            mutate(villager, zombie, level);
            return;
        }

        if (entity instanceof Blaze && eRad >= 700D) {
            EntityRADBeast beast = new EntityRADBeast(RadBeastEntityTypes.RAD_BEAST.get(), level);
            mutate(entity, beast, level);
            return;
        }

        if (entity instanceof EntityDuck duck && !(duck instanceof EntityQuackos) && eRad >= 200D) {
            EntityQuackos quacc = new EntityQuackos(Phase4BossEntityTypes2.QUACKOS.get(), level);
            mutate(duck, quacc, level);
        }
    }

    private static void mutate(LivingEntity original, LivingEntity replacement, ServerLevel level) {
        replacement.moveTo(original.getX(), original.getY(), original.getZ(), original.getYRot(), original.getXRot());
        if (original.isAlive() && level.addFreshEntity(replacement)) {
            original.discard();
        }
    }

    // ==================== ambient pollution exposure (CE handlePollution, lines 572-607) ==============

    private static void handlePollution(LivingEntity entity, ServerLevel level) {
        if (!RadiationConfig.ENABLE_POLLUTION.get()) return;
        if (entity.tickCount % 60 != 0) return;

        BlockPos pos = BlockPos.containing(entity.getX(), entity.getY() + entity.getEyeHeight(), entity.getZ());

        if (RadiationConfig.ENABLE_POISON.get()
                && !ArmorRegistry.hasProtection(entity, EquipmentSlot.HEAD, HazardClass.GAS_BLISTERING)) {
            float poison = PollutionHandler.getPollution(level, pos, PollutionType.POISON);
            if (poison > 10F) {
                if (poison < 25F) {
                    entity.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 0));
                } else if (poison < 50F) {
                    entity.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 1));
                } else {
                    entity.addEffect(new MobEffectInstance(MobEffects.WITHER, 100, 2));
                }
            }
        }

        if (RadiationConfig.ENABLE_LEAD_POISONING.get()
                && !ArmorRegistry.hasProtection(entity, EquipmentSlot.HEAD, HazardClass.PARTICLE_FINE)) {
            float metal = PollutionHandler.getPollution(level, pos, PollutionType.HEAVYMETAL);
            if (metal > 25F) {
                // CE: <50 -> amplifier 0, >=50 -> amplifier 2 in BOTH remaining branches (a real,
                // confirmed-by-full-read CE quirk - amplifier 1 is never actually reached for lead from
                // ambient exposure) - preserved exactly, not "fixed" into a 0/1/2 ladder.
                int amplifier = metal < 50F ? 0 : 2;
                entity.addEffect(new MobEffectInstance(HbmPotionEffects.LEAD, 100, amplifier));
            }
        }
    }

    // ==================== lead-poisoning-on-ore-break (CE ModEventHandler#blockBreak, 1294-1307) ======

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (event.isCanceled()) return;

        Player player = event.getPlayer();
        if (player == null || player.level().isClientSide) return;

        if (!RadiationConfig.ENABLE_POLLUTION.get() || !RadiationConfig.ENABLE_LEAD_FROM_BLOCKS.get()) return;
        if (ArmorRegistry.hasProtection(player, EquipmentSlot.HEAD, HazardClass.PARTICLE_FINE)) return;

        float metal = PollutionHandler.getPollution(player.level(), event.getPos(), PollutionType.HEAVYMETAL);
        if (metal < 5F) return;

        int amplifier;
        if (metal < 10F) amplifier = 0;
        else if (metal < 25F) amplifier = 1;
        else amplifier = 2;

        player.addEffect(new MobEffectInstance(HbmPotionEffects.LEAD, 100, amplifier));
    }

    // ==================== contamination / lungs / oil / temperature (CE :136-151, :464-650) ========

    /** Exact CE {@code handleContamination} {@code :136-151}. AuxParticle stay skipped. */
    private static void handleContamination(LivingEntity entity) {
        Iterator<ContaminationEffect> iterator = HbmLivingProps.getCont(entity).iterator();
        boolean dirty = false;
        while (iterator.hasNext()) {
            ContaminationEffect con = iterator.next();
            ContaminationUtil.contaminate(entity, HazardType.RADIATION,
                    con.ignoreArmor ? ContaminationType.RAD_BYPASS : ContaminationType.CREATIVE, con.getRad());
            con.time--;
            dirty = true;
            if (con.time <= 0) {
                iterator.remove();
            }
        }
        if (dirty) {
            persistLiving(entity);
        }
    }

    /**
     * Exact CE {@code handleLungDisease} {@code :464-544}. Cough/potion/decay only;
     * vomit AuxParticle packets stay skipped. Caps use CE {@code EntityHbmProps.maxBlacklung}
     * ({@code 60*60*20}), not {@link HbmLivingAttachment#MAX_BLACKLUNG} (2x).
     */
    private static void handleLungDisease(LivingEntity entity) {
        if (entity instanceof Player player && player.isCreative()) {
            HbmLivingProps.setBlackLung(entity, 0);
            HbmLivingProps.setAsbestos(entity, 0);
            return;
        }

        // CE EntityHbmProps.maxBlacklung (HbmLivingCapability.java:140), not HbmLivingProps.maxBlacklung.
        final int maxBlacklung = 60 * 60 * 20;
        final int maxAsbestos = HbmLivingAttachment.MAX_ASBESTOS;

        int bl = HbmLivingProps.getBlackLung(entity);
        if (bl > 0 && bl < maxBlacklung * 0.25) {
            HbmLivingProps.setBlackLung(entity, HbmLivingProps.getBlackLung(entity) - 1);
        }

        double blacklung = Math.min(HbmLivingProps.getBlackLung(entity), maxBlacklung);
        double asbestos = Math.min(HbmLivingProps.getAsbestos(entity), maxAsbestos);

        boolean coughs = blacklung / maxBlacklung > 0.25D || asbestos / maxAsbestos > 0.25D;
        if (!coughs) {
            return;
        }

        double blacklungDelta = 1D - (blacklung / (double) maxBlacklung);
        double asbestosDelta = 1D - (asbestos / (double) maxAsbestos);
        double total = 1 - (blacklungDelta * asbestosDelta);
        int freq = Math.max((int) (1000 - 950 * total), 20);

        Random rand = new Random(entity.getId());

        if (total > 0.8D) {
            entity.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 6));
            entity.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 100, 0));
            entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 3));
            if (rand.nextInt(250) == 0) {
                entity.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 2));
            }
        } else if (total > 0.65D) {
            entity.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 2));
            entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 2));
            if (rand.nextInt(500) == 0) {
                entity.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 0));
            }
        } else if (total > 0.45D) {
            entity.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 1));
            entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 1));
        } else if (total > 0.25D) {
            entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 0));
        }

        if (entity.level().getGameTime() % freq == entity.getId() % freq) {
            entity.level().playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                    HBMSoundHandler.cough.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        }
    }

    /** Exact CE {@code handleOil} {@code :546-569}. Sweat AuxParticle stays skipped. */
    private static void handleOil(LivingEntity entity) {
        int oil = HbmLivingProps.getOil(entity);
        if (oil <= 0) {
            return;
        }

        if (entity.isOnFire()) {
            HbmLivingProps.setOil(entity, 0);
            entity.level().explode(null, entity.getX(), entity.getY() + entity.getBbHeight() / 2,
                    entity.getZ(), 3F, false, Level.ExplosionInteraction.TNT);
        } else {
            HbmLivingProps.setOil(entity, oil - 1);
        }
    }

    /**
     * Exact CE {@code handleTemperature} {@code :609-653}. FlameCreator / Confetti stay skipped.
     * Fire/phosphorus/balefire timers are already written by registered flamers / 12ga / lingering fire.
     */
    private static void handleTemperature(LivingEntity living) {
        if (!living.isAlive()) {
            return;
        }

        HbmLivingAttachment props = HbmLivingAttachment.getData(living);
        RandomSource rand = living.getRandom();
        boolean dirty = false;

        if (living.fireImmune()) {
            if (props.getFire() > 0 || props.getPhosphorus() > 0) {
                props.setFire(0);
                props.setPhosphorus(0);
                dirty = true;
            }
        }

        if (living.isInWaterOrRain() && props.getFire() > 0) {
            props.setFire(0);
            dirty = true;
        }

        if (props.getFire() > 0) {
            props.setFire(props.getFire() - 1);
            dirty = true;
            if ((living.tickCount + living.getId()) % 15 == 0) {
                living.level().playSound(null, living.getX(), living.getY() + living.getBbHeight() / 2, living.getZ(),
                        SoundEvents.FIRE_EXTINGUISH, SoundSource.HOSTILE, 1F, 1.5F + rand.nextFloat() * 0.5F);
            }
            if ((living.tickCount + living.getId()) % 40 == 0) {
                living.hurt(living.damageSources().onFire(), 2F);
            }
        }

        if (props.getPhosphorus() > 0) {
            props.setPhosphorus(props.getPhosphorus() - 1);
            dirty = true;
            if ((living.tickCount + living.getId()) % 15 == 0) {
                living.level().playSound(null, living.getX(), living.getY() + living.getBbHeight() / 2, living.getZ(),
                        SoundEvents.FIRE_EXTINGUISH, SoundSource.NEUTRAL, 1F, 1.5F + rand.nextFloat() * 0.5F);
            }
            if ((living.tickCount + living.getId()) % 40 == 0) {
                living.hurt(living.damageSources().onFire(), 5F);
            }
        }

        if (props.getBalefire() > 0) {
            props.setBalefire(props.getBalefire() - 1);
            dirty = true;
            if ((living.tickCount + living.getId()) % 15 == 0) {
                living.level().playSound(null, living.getX(), living.getY() + living.getBbHeight() / 2, living.getZ(),
                        SoundEvents.FIRE_EXTINGUISH, SoundSource.NEUTRAL, 1F, 1.5F + rand.nextFloat() * 0.5F);
            }
            ContaminationUtil.contaminate(living, HazardType.RADIATION, ContaminationType.CREATIVE, 5F);
            if ((living.tickCount + living.getId()) % 20 == 0) {
                living.hurt(living.damageSources().onFire(), 5F);
            }
        }

        if (dirty) {
            living.setData(ModAttachments.LIVING_ATTACHMENT, props);
        }
    }

    private static void persistLiving(LivingEntity entity) {
        entity.setData(ModAttachments.LIVING_ATTACHMENT, HbmLivingAttachment.getData(entity));
    }

    // ==================== dash / plink (CE EntityEffectHandler :655-754) ====================

    private static final EquipmentSlot[] DASH_ARMOR_SLOTS = {
            EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD
    };

    /**
     * Exact CE {@code handleDashing} {@code :655-743}. LSHIFT via {@code EnumKeybind.DASH} /
     * {@code KeybindPacket}; stamina 30/dash; cooldown {@link HbmPlayerAttachment#DASH_COOLDOWN_LENGTH}.
     * Dash-bar overlay stays skipped.
     */
    public static void handleDashing(LivingEntity entity) {
        if (!(entity instanceof Player player)) return;

        HbmPlayerAttachment props = HbmPlayerAttachment.getData(player);
        props.setDashCount(0);

        ArmorFSB chestplate = null;
        int armorDashCount = 0;
        int armorModDashCount = 0;

        if (ArmorFSB.hasFSBArmor(player)) {
            ItemStack plate = player.getItemBySlot(EquipmentSlot.CHEST);
            chestplate = (ArmorFSB) plate.getItem();
        }

        if (chestplate != null) {
            armorDashCount = chestplate.dashCount;
        }

        for (EquipmentSlot armorSlot : DASH_ARMOR_SLOTS) {
            ItemStack armorStack = player.getItemBySlot(armorSlot);
            if (!armorStack.isEmpty() && armorStack.getItem() instanceof ArmorItem) {
                ItemStack[] mods = ArmorModHandler.pryMods(armorStack);
                // CE loops modSlot < 8 — battery slot 8 is excluded.
                int limit = Math.min(8, mods.length);
                for (int modSlot = 0; modSlot < limit; modSlot++) {
                    ItemStack mod = mods[modSlot];
                    if (!mod.isEmpty() && mod.getItem() instanceof IArmorModDash dashMod) {
                        armorModDashCount += dashMod.getDashes();
                    }
                }
            }
        }

        int dashCount = armorDashCount + armorModDashCount;
        boolean dashActivated = props.getKeyPressed(EnumKeybind.DASH);

        if (dashCount * 30 < props.getStamina()) {
            props.setStamina(dashCount * 30);
        }

        if (dashCount > 0) {
            int perDash = 30;
            int stamina = props.getStamina();

            props.setDashCount(dashCount);

            if (props.getDashCooldown() <= 0) {
                if (dashActivated && stamina >= perDash) {
                    Vec3 lookingIn = player.getLookAngle();
                    Vec3 strafeVec = lookingIn.yRot((float) Math.PI * 0.5F);

                    int forward = (int) Math.signum(player.zza);
                    int strafe = (int) Math.signum(player.xxa);
                    if (forward == 0 && strafe == 0) {
                        forward = 1;
                    }

                    player.push(
                            lookingIn.x * forward + strafeVec.x * strafe,
                            0,
                            lookingIn.z * forward + strafeVec.z * strafe);
                    var mot = player.getDeltaMovement();
                    player.setDeltaMovement(mot.x, 0, mot.z);
                    player.fallDistance = 0F;
                    player.playSound(HBMSoundHandler.rocketFlame.get(), 1.0F, 1.0F);

                    props.setDashCooldown(HbmPlayerAttachment.DASH_COOLDOWN_LENGTH);
                    stamina -= perDash;
                }
            } else {
                props.setDashCooldown(props.getDashCooldown() - 1);
                props.setKeyPressed(EnumKeybind.DASH, false);
            }

            if (stamina < props.getDashCount() * perDash) {
                stamina++;
                if (stamina % perDash == perDash - 1) {
                    player.playSound(HBMSoundHandler.techBoop.get(), 1.0F,
                            1.0F + ((1F / 12F) * (stamina / perDash)));
                    stamina++;
                }
            }

            props.setStamina(stamina);
        }
    }

    /** Exact CE {@code handlePlinking} {@code :745-754}. */
    public static void handlePlinking(LivingEntity entity) {
        if (!(entity instanceof Player player)) return;
        HbmPlayerAttachment props = HbmPlayerAttachment.getData(player);
        if (props.getPlinkCooldown() > 0) {
            props.setPlinkCooldown(props.getPlinkCooldown() - 1);
        }
    }
}
