package com.hbm.items.weapon;

import com.hbm.entity.effect.EntityMist;
import com.hbm.entity.effect.EntityNukeTorex;
import com.hbm.entity.logic.EntityNukeExplosionMK5;
import com.hbm.entity.projectile.EntityArtilleryRocket;
import com.hbm.entity.projectile.EntityArtilleryShell;
import com.hbm.entity.projectile.Phase9TailEntityTypes;
import com.hbm.explosion.ExplosionChaos;
import com.hbm.explosion.ExplosionLarge;
import com.hbm.explosion.vanillant.ExplosionVNT;
import com.hbm.explosion.vanillant.standard.BlockAllocatorStandard;
import com.hbm.explosion.vanillant.standard.BlockMutatorDebris;
import com.hbm.explosion.vanillant.standard.BlockProcessorStandard;
import com.hbm.explosion.vanillant.standard.EntityProcessorCross;
import com.hbm.explosion.vanillant.standard.ExplosionEffectStandard;
import com.hbm.explosion.vanillant.standard.PlayerProcessorStandard;
import com.hbm.handler.pollution.PollutionHandler;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.lib.HBMSoundHandler;
import com.hbm.main.MainRegistry;
import com.hbm.particle.HbmEffect;
import com.hbm.potion.HbmPotionEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Flattened CE {@code ItemAmmoArty}/{@code ItemAmmoHIMARS} tables.
 * TODO(CE: ItemAmmoArty.java:242-244): {@code ExplosionCreator.composeEffect} — not ported (VFX).
 * TODO(CE: ItemAmmoArty.java:285-287): {@code HbmEffectNT.RBMKMush} — not in {@link HbmEffect}.
 * TODO(CE: CargoShellCraftingHandler.java:28): shapeless empty cargo + any item → NBT {@code cargo}.
 * TODO(CE: ItemAmmoHIMARS.java:256-260): {@code volcanic_lava_block} — slag stand-in.
 */
public final class ArtilleryAmmo {

    public static final int ARTY_NORMAL = 0;
    public static final int ARTY_CLASSIC = 1;
    public static final int ARTY_HE = 2;
    public static final int ARTY_MINI_NUKE = 3;
    public static final int ARTY_NUKE = 4;
    public static final int ARTY_PHOSPHORUS = 5;
    public static final int ARTY_MINI_NUKE_MULTI = 6;
    public static final int ARTY_PHOSPHORUS_MULTI = 7;
    public static final int ARTY_CARGO = 8;
    public static final int ARTY_CHLORINE = 9;
    public static final int ARTY_PHOSGENE = 10;
    public static final int ARTY_MUSTARD = 11;

    public static final int HIMARS_SMALL = 0;
    public static final int HIMARS_SMALL_HE = 1;
    public static final int HIMARS_SMALL_WP = 2;
    public static final int HIMARS_SMALL_TB = 3;
    public static final int HIMARS_SMALL_LAVA = 4;
    public static final int HIMARS_SMALL_MINI_NUKE = 5;
    public static final int HIMARS_LARGE = 6;
    public static final int HIMARS_LARGE_TB = 7;

    private ArtilleryAmmo() {
    }

    public static Item item(String id) {
        return BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, id));
    }

    public static boolean isArtyShell(Item item) {
        return typeOfArty(item) >= 0;
    }

    public static boolean isHimarsRocket(Item item) {
        return typeOfHimars(item) >= 0;
    }

    public static int typeOfArty(Item item) {
        if (item == Items.AIR) return -1;
        if (item == item("ammo_arty_normal")) return ARTY_NORMAL;
        if (item == item("ammo_arty_classic")) return ARTY_CLASSIC;
        if (item == item("ammo_arty_he")) return ARTY_HE;
        if (item == item("ammo_arty_mini_nuke")) return ARTY_MINI_NUKE;
        if (item == item("ammo_arty_nuke")) return ARTY_NUKE;
        if (item == item("ammo_arty_phosphorus")) return ARTY_PHOSPHORUS;
        if (item == item("ammo_arty_mini_nuke_multi")) return ARTY_MINI_NUKE_MULTI;
        if (item == item("ammo_arty_phosphorus_multi")) return ARTY_PHOSPHORUS_MULTI;
        if (item == item("ammo_arty_cargo")) return ARTY_CARGO;
        if (item == item("ammo_arty_chlorine")) return ARTY_CHLORINE;
        if (item == item("ammo_arty_phosgene")) return ARTY_PHOSGENE;
        if (item == item("ammo_arty_mustard")) return ARTY_MUSTARD;
        return -1;
    }

    /** CE {@code ItemAmmoArty} cargo NBT key {@code "cargo"} on the item stack. */
    public static ItemStack getCargo(ItemStack stack, HolderLookup.Provider registries) {
        if (typeOfArty(stack.getItem()) != ARTY_CARGO) {
            return ItemStack.EMPTY;
        }
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) {
            return ItemStack.EMPTY;
        }
        CompoundTag tag = data.copyTag();
        if (!tag.contains("cargo")) {
            return ItemStack.EMPTY;
        }
        return ItemStack.parseOptional(registries, tag.getCompound("cargo"));
    }

    public static int typeOfHimars(Item item) {
        if (item == Items.AIR) return -1;
        if (item == item("ammo_himars_small")) return HIMARS_SMALL;
        if (item == item("ammo_himars_small_he")) return HIMARS_SMALL_HE;
        if (item == item("ammo_himars_small_wp")) return HIMARS_SMALL_WP;
        if (item == item("ammo_himars_small_tb")) return HIMARS_SMALL_TB;
        if (item == item("ammo_himars_small_lava")) return HIMARS_SMALL_LAVA;
        if (item == item("ammo_himars_small_mini_nuke")) return HIMARS_SMALL_MINI_NUKE;
        if (item == item("ammo_himars_large")) return HIMARS_LARGE;
        if (item == item("ammo_himars_large_tb")) return HIMARS_LARGE_TB;
        return -1;
    }

    /** CE {@code HIMARSRocket.Type}: Standard=6, Single=1. */
    public static int himarsAmount(int type) {
        return type == HIMARS_LARGE || type == HIMARS_LARGE_TB ? 1 : 6;
    }

    /** CE {@code ArtilleryShell.onUpdate} — cluster split for multi metas. */
    public static void onShellUpdate(EntityArtilleryShell shell) {
        switch (shell.getShellType()) {
            case ARTY_MINI_NUKE_MULTI -> standardCluster(shell, ARTY_MINI_NUKE, 5, 300, 5);
            case ARTY_PHOSPHORUS_MULTI -> standardCluster(shell, ARTY_PHOSPHORUS, 10, 300, 5);
            default -> {
            }
        }
    }

    public static void onShellImpact(EntityArtilleryShell shell, HitResult mop, int type) {
        Vec3 hit = mop.getLocation();
        switch (type) {
            case ARTY_CLASSIC -> {
                // TODO(CE: ItemAmmoArty.java:243): ExplosionCreator.composeEffect 15/5/1/45/10/0/50.
                standardExplosion(shell, mop, 15F, 5F, false);
            }
            case ARTY_HE -> {
                // TODO(CE: ItemAmmoArty.java:244): ExplosionCreator.composeEffect 15/5/1/45/10/16/50.
                standardExplosion(shell, mop, 15F, 3F, true);
            }
            case ARTY_MINI_NUKE, ARTY_MINI_NUKE_MULTI -> {
                shell.killAndClear();
                shell.level().addFreshEntity(EntityNukeExplosionMK5.statFac(shell.level(), 20, hit.x, hit.y, hit.z));
                EntityNukeTorex.statFac(shell.level(), hit.x, hit.y, hit.z, 20);
            }
            case ARTY_NUKE -> {
                shell.killAndClear();
                shell.level().addFreshEntity(EntityNukeExplosionMK5.statFac(shell.level(), 100, hit.x, hit.y, hit.z));
                EntityNukeTorex.statFac(shell.level(), hit.x, hit.y, hit.z, 100);
            }
            case ARTY_PHOSPHORUS, ARTY_PHOSPHORUS_MULTI -> {
                standardExplosion(shell, mop, 10F, 3F, false);
                ExplosionLarge.spawnShrapnels(shell.level(), (int) hit.x, (int) hit.y, (int) hit.z, 15);
                ExplosionChaos.burn(shell.level(), null, BlockPos.containing(hit), 12);
                splashPhosphorus(shell, 15);
                for (int i = 0; i < 5; i++) {
                    HbmEffect.sendPacket(shell.level(), HbmEffect.HAZE,
                            hit.x + shell.level().random.nextGaussian() * 10, hit.y,
                            hit.z + shell.level().random.nextGaussian() * 10, 150, null);
                }
                // TODO(CE: ItemAmmoArty.java:285-287): HbmEffectNT.RBMKMush scale=10 r=250.
            }
            case ARTY_CARGO -> {
                if (mop instanceof BlockHitResult blockHit) {
                    shell.setPos(hit.x, hit.y, hit.z);
                    shell.getStuck(blockHit.getBlockPos(), blockHit.getDirection().ordinal());
                }
            }
            case ARTY_CHLORINE -> {
                shell.killAndClear();
                Vec3 vec = shell.getDeltaMovement().normalize();
                shell.level().explode(shell, hit.x - vec.x, hit.y - vec.y, hit.z - vec.z, 0F, Level.ExplosionInteraction.NONE);
                EntityMist.spawn(shell.level(), hit.x - vec.x, hit.y - vec.y - 3, hit.z - vec.z, Fluids.CHLORINE, 15, 7.5F, 150);
                PollutionHandler.incrementPollution(shell.level(), BlockPos.containing(hit), PollutionHandler.PollutionType.HEAVYMETAL, 5F);
            }
            case ARTY_PHOSGENE -> {
                shell.killAndClear();
                Vec3 vec = shell.getDeltaMovement().normalize();
                shell.level().explode(shell, hit.x - vec.x, hit.y - vec.y, hit.z - vec.z, 5F, Level.ExplosionInteraction.NONE);
                for (int i = 0; i < 3; i++) {
                    double x = hit.x - vec.x;
                    double z = hit.z - vec.z;
                    if (i > 0) {
                        x += shell.level().random.nextGaussian() * 15;
                        z += shell.level().random.nextGaussian() * 15;
                    }
                    EntityMist.spawn(shell.level(), x, hit.y - vec.y - 5, z, Fluids.PHOSGENE, 15, 10, 150);
                }
                PollutionHandler.incrementPollution(shell.level(), BlockPos.containing(hit), PollutionHandler.PollutionType.HEAVYMETAL, 10F);
                PollutionHandler.incrementPollution(shell.level(), BlockPos.containing(hit), PollutionHandler.PollutionType.POISON, 15F);
            }
            case ARTY_MUSTARD -> {
                shell.killAndClear();
                Vec3 vec = shell.getDeltaMovement().normalize();
                shell.level().explode(shell, hit.x - vec.x, hit.y - vec.y, hit.z - vec.z, 5F, Level.ExplosionInteraction.NONE);
                for (int i = 0; i < 5; i++) {
                    double x = hit.x - vec.x;
                    double z = hit.z - vec.z;
                    if (i > 0) {
                        x += shell.level().random.nextGaussian() * 25;
                        z += shell.level().random.nextGaussian() * 25;
                    }
                    EntityMist.spawn(shell.level(), x, hit.y - vec.y - 5, z, Fluids.MUSTARDGAS, 20, 10, 150);
                }
                PollutionHandler.incrementPollution(shell.level(), BlockPos.containing(hit), PollutionHandler.PollutionType.HEAVYMETAL, 15F);
                PollutionHandler.incrementPollution(shell.level(), BlockPos.containing(hit), PollutionHandler.PollutionType.POISON, 30F);
            }
            default -> {
                // TODO(CE: ItemAmmoArty.java:242): ExplosionCreator.composeEffect 10/2/0.5/25/5/0/20.
                standardExplosion(shell, mop, 10F, 3F, false);
            }
        }
    }

    /** CE {@code ItemAmmoArty.standardCluster} :212-237. */
    public static void standardCluster(EntityArtilleryShell shell, int clusterType, int amount, double splitHeight, double deviation) {
        if (!shell.getWhistle() || shell.getDeltaMovement().y > 0) return;
        if (shell.getTargetHeight() + splitHeight < shell.getY()) return;

        shell.killAndClear();

        CompoundTag data = new CompoundTag();
        data.putFloat("r", 1.0F);
        data.putFloat("g", 1.0F);
        data.putFloat("b", 1.0F);
        data.putFloat("scale", 50F);
        HbmEffect.sendPacket(shell.level(), HbmEffect.PLASMA_BLAST, shell.getX(), shell.getY(), shell.getZ(), 500, data);

        Vec3 motion = shell.getDeltaMovement();
        for (int i = 0; i < amount; i++) {
            EntityArtilleryShell cluster = new EntityArtilleryShell(Phase9TailEntityTypes.ARTILLERY_SHELL.get(), shell.level());
            cluster.setType(clusterType);
            if (i == 0) {
                cluster.setDeltaMovement(motion);
            } else {
                cluster.setDeltaMovement(
                        motion.x + shell.level().random.nextGaussian() * deviation,
                        motion.y,
                        motion.z + shell.level().random.nextGaussian() * deviation);
            }
            cluster.setPos(shell.getX(), shell.getY(), shell.getZ());
            cluster.setYRot(shell.getYRot());
            cluster.setXRot(shell.getXRot());
            double[] target = shell.getTarget();
            cluster.setTarget(target[0], target[1], target[2]);
            cluster.setWhistle(shell.getWhistle() && !shell.didWhistle());
            shell.level().addFreshEntity(cluster);
        }
    }

    /** CE {@code ItemAmmoArty.standardExplosion} :197-209 — shell path plays {@code explosion_medium}. */
    private static void standardExplosion(EntityArtilleryShell shell, HitResult mop, float size, float rangeMod, boolean breaksBlocks) {
        shell.level().playSound(null, shell.getX(), shell.getY(), shell.getZ(),
                HBMSoundHandler.explosion_medium.get(), SoundSource.PLAYERS, 20.0F,
                0.9F + shell.level().random.nextFloat() * 0.2F);
        standardExplosion(shell.level(), shell, mop.getLocation(), size, rangeMod, breaksBlocks);
    }

    public static void onRocketImpact(EntityArtilleryRocket rocket, HitResult mop, int type) {
        Vec3 hit = mop.getLocation();
        switch (type) {
            case HIMARS_SMALL_HE -> standardExplosion(rocket.level(), rocket, hit, 20F, 3F, true);
            case HIMARS_SMALL_WP -> {
                standardExplosion(rocket.level(), rocket, hit, 20F, 3F, false);
                ExplosionLarge.spawnShrapnels(rocket.level(), hit.x, hit.y, hit.z, 30);
                ExplosionChaos.burn(rocket.level(), null, BlockPos.containing(hit), 20);
                splashPhosphorus(rocket, 30);
            }
            case HIMARS_SMALL_TB -> standardExplosion(rocket.level(), rocket, hit, 20F, 10F, true);
            case HIMARS_SMALL_LAVA ->
                    // TODO(CE: ItemAmmoHIMARS.java:256-260): volcanic_lava_block — slag stand-in.
                    standardExplosion(rocket.level(), rocket, hit, 20F, 3F, true);
            case HIMARS_SMALL_MINI_NUKE -> {
                rocket.killAndClear();
                rocket.level().addFreshEntity(EntityNukeExplosionMK5.statFac(rocket.level(), 100, hit.x, hit.y, hit.z));
                EntityNukeTorex.statFac(rocket.level(), hit.x, hit.y, hit.z, 100);
            }
            case HIMARS_LARGE -> standardExplosion(rocket.level(), rocket, hit, 50F, 5F, true);
            case HIMARS_LARGE_TB -> standardExplosion(rocket.level(), rocket, hit, 50F, 12F, true);
            default -> standardExplosion(rocket.level(), rocket, hit, 20F, 3F, false);
        }
    }

    private static void splashPhosphorus(Entity rocket, int radius) {
        List<Entity> hit = rocket.level().getEntities(rocket, new AABB(
                rocket.getX() - radius, rocket.getY() - radius, rocket.getZ() - radius,
                rocket.getX() + radius, rocket.getY() + radius, rocket.getZ() + radius));
        for (Entity e : hit) {
            e.igniteForSeconds(5);
            if (e instanceof LivingEntity living) {
                living.addEffect(new MobEffectInstance(HbmPotionEffects.PHOSPHORUS, 30 * 20, 0, true, false));
            }
        }
    }

    private static void standardExplosion(Level level, Entity exploder, Vec3 hit, float size, float rangeMod, boolean breaksBlocks) {
        Vec3 vec = exploder.getDeltaMovement().normalize();
        ExplosionVNT xnt = new ExplosionVNT(level, hit.x - vec.x, hit.y - vec.y, hit.z - vec.z, size, exploder);
        if (breaksBlocks) {
            xnt.setBlockAllocator(new BlockAllocatorStandard(48));
            xnt.setBlockProcessor(new BlockProcessorStandard().setNoDrop()
                    .withBlockEffect(new BlockMutatorDebris(BuiltInRegistries.BLOCK
                            .getOptional(ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "block_slag"))
                            .orElse(Blocks.STONE))));
        }
        xnt.setEntityProcessor(new EntityProcessorCross(7.5D).withRangeMod(rangeMod));
        xnt.setPlayerProcessor(new PlayerProcessorStandard());
        xnt.setSFX(new ExplosionEffectStandard());
        xnt.explode();
        exploder.discard();
    }
}
