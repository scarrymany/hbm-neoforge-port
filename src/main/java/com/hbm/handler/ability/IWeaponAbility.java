package com.hbm.handler.ability;

import com.hbm.potion.HbmPotionEffects;
import com.hbm.util.ContaminationUtil;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.MagmaCube;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.entity.monster.WitherSkeleton;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.level.Level;

/**
 * On-hit ability contract for melee weapons. Ported from CE's
 * {@code com.hbm.handler.ability.IWeaponAbility}.
 * <p>
 * Portable now (this package's task): {@link #NONE}, {@link #RADIATION} (via the already-ported
 * {@link ContaminationUtil}), {@link #VAMPIRE}, {@link #STUN}, {@link #PHOSPHORUS} (via
 * {@code com.hbm.potion.HbmPotionEffects}, the Phase 4 {@code MobEffect} registry), {@link #FIRE},
 * {@link #BEHEADER}.
 * <p>
 * Deliberately <b>not</b> ported yet, per the task's own deferral list - each needs a system that
 * genuinely does not exist anywhere in this port:
 * <ul>
 *     <li>{@code CHAINSAW} - CE's on-kill "shred into nitra + XP orbs" payoff spawns
 *     {@code ModItems.nitra_small} (not yet a registered item in this port) and a
 *     {@code HbmEffectNT.VanillaBurst_BlockDust} particle burst (the CE generic effect-dispatch
 *     table this port deliberately does not build - see {@code docs/phase3/weapon_animation_hooks.md}).</li>
 *     <li>{@code BOBBLE} - CE's on-kill drop is {@code ModBlocks.bobblehead} with
 *     {@code BlockBobble.BobbleType}, neither of which exists in this port yet.</li>
 * </ul>
 * Whoever first ports {@code nitra_small} or {@code ModBlocks.bobblehead} should add the matching
 * singleton here and into {@link #abilities}, following CE's {@code IWeaponAbility} exactly - the
 * on-hit dispatch machinery ({@link AvailableAbilities#getWeaponAbilities()},
 * {@code ItemSwordAbility#hurtEnemy}) already supports an arbitrary number of these with no further
 * changes needed.
 */
public interface IWeaponAbility extends IBaseAbility {

    void onHit(int level, Level level_, Player player, Entity victim, Item tool);

    int SORT_ORDER_BASE = 200;

    IWeaponAbility NONE = new IWeaponAbility() {
        @Override
        public String getName() {
            return "";
        }

        @Override
        public int sortOrder() {
            return SORT_ORDER_BASE;
        }

        @Override
        public void onHit(int level, Level level_, Player player, Entity victim, Item tool) {
        }
    };

    /** CE: {@code IWeaponAbility.RADIATION} - contaminates the victim with creative-tier radiation. */
    IWeaponAbility RADIATION = new IWeaponAbility() {
        private final float[] radAtLevel = {15F, 50F, 500F};

        @Override
        public String getName() {
            return "weapon.ability.radiation";
        }

        @Override
        public int levels() {
            return radAtLevel.length;
        }

        @Override
        public String getExtension(int level) {
            return " (" + radAtLevel[level] + ")";
        }

        @Override
        public int sortOrder() {
            return SORT_ORDER_BASE + 1;
        }

        @Override
        public void onHit(int level, Level level_, Player player, Entity victim, Item tool) {
            if (victim instanceof LivingEntity living) {
                ContaminationUtil.contaminate(living, ContaminationUtil.HazardType.RADIATION, ContaminationUtil.ContaminationType.CREATIVE, radAtLevel[level]);
            }
        }
    };

    /** CE: {@code IWeaponAbility.VAMPIRE} - deals bonus true damage to the victim, healing the attacker by the same amount. */
    IWeaponAbility VAMPIRE = new IWeaponAbility() {
        private final float[] amountAtLevel = {2F, 3F, 5F, 10F, 50F};

        @Override
        public String getName() {
            return "weapon.ability.vampire";
        }

        @Override
        public int levels() {
            return amountAtLevel.length;
        }

        @Override
        public String getExtension(int level) {
            return " (" + amountAtLevel[level] + ")";
        }

        @Override
        public int sortOrder() {
            return SORT_ORDER_BASE + 2;
        }

        @Override
        public void onHit(int level, Level level_, Player player, Entity victim, Item tool) {
            float amount = amountAtLevel[level];

            if (victim instanceof LivingEntity living) {
                if (living.getHealth() <= 0) return;

                living.setHealth(living.getHealth() - amount);
                if (living.getHealth() <= 0) {
                    // Confirmed API shape against the Neo Edition reference's own IWeaponAbility.VAMPIRE
                    // port (upstream/neo-edition, real compiling 1.21.1 NeoForge source) - DamageSource's
                    // constructor takes a Holder<DamageType> looked up from the registry, not a
                    // DamageSources.magic()-style convenience accessor.
                    living.die(new DamageSource(level_.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(DamageTypes.MAGIC)));
                }
                player.heal(amount);
            }
        }
    };

    /** CE: {@code IWeaponAbility.STUN} - slowness + weakness IV on the victim. */
    IWeaponAbility STUN = new IWeaponAbility() {
        private final int[] durationAtLevel = {2, 3, 5, 10, 15};

        @Override
        public String getName() {
            return "weapon.ability.stun";
        }

        @Override
        public int levels() {
            return durationAtLevel.length;
        }

        @Override
        public String getExtension(int level) {
            return " (" + durationAtLevel[level] + ")";
        }

        @Override
        public int sortOrder() {
            return SORT_ORDER_BASE + 3;
        }

        @Override
        public void onHit(int level, Level level_, Player player, Entity victim, Item tool) {
            int duration = durationAtLevel[level];

            if (victim instanceof LivingEntity living) {
                living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, duration * 20, 4));
                living.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, duration * 20, 4));
            }
        }
    };

    /**
     * CE: {@code IWeaponAbility.PHOSPHORUS} - grants the victim
     * {@code com.hbm.potion.HbmPotionEffects#PHOSPHORUS} (amplifier 4) for a 60/90-tick-by-level
     * duration. Applied by e.g. {@code mese_gavel}.
     */
    IWeaponAbility PHOSPHORUS = new IWeaponAbility() {
        private final int[] durationAtLevel = {60, 90};

        @Override
        public String getName() {
            return "weapon.ability.phosphorus";
        }

        @Override
        public int levels() {
            return durationAtLevel.length;
        }

        @Override
        public String getExtension(int level) {
            return " (" + durationAtLevel[level] + ")";
        }

        @Override
        public int sortOrder() {
            return SORT_ORDER_BASE + 4;
        }

        @Override
        public void onHit(int level, Level level_, Player player, Entity victim, Item tool) {
            int duration = durationAtLevel[level];

            if (victim instanceof LivingEntity living) {
                living.addEffect(new MobEffectInstance(HbmPotionEffects.PHOSPHORUS, duration * 20, 4));
            }
        }
    };

    /** CE: {@code IWeaponAbility.FIRE} - sets the victim on fire. */
    IWeaponAbility FIRE = new IWeaponAbility() {
        private final int[] durationAtLevel = {5, 10};

        @Override
        public String getName() {
            return "weapon.ability.fire";
        }

        @Override
        public int levels() {
            return durationAtLevel.length;
        }

        @Override
        public String getExtension(int level) {
            return " (" + durationAtLevel[level] + ")";
        }

        @Override
        public int sortOrder() {
            return SORT_ORDER_BASE + 6;
        }

        @Override
        public void onHit(int level, Level level_, Player player, Entity victim, Item tool) {
            if (victim instanceof LivingEntity) {
                victim.igniteForSeconds(durationAtLevel[level]);
            }
        }
    };

    /**
     * CE: {@code IWeaponAbility.BEHEADER} - on kill, drops the victim's "head" (or a
     * species-appropriate substitute), engraving the victim's name onto a player's skull via the
     * modern {@link ResolvableProfile} data component (1.21's replacement for CE's
     * {@code SkullOwner} NBT string) - both the component and {@code spawnAtLocation(ItemStack,
     * float)}'s exact two-arg shape confirmed against the Neo Edition reference's own
     * {@code IWeaponAbility.BEHEADER} port (upstream/neo-edition, real compiling 1.21.1 NeoForge
     * source), not guessed at.
     */
    IWeaponAbility BEHEADER = new IWeaponAbility() {
        @Override
        public String getName() {
            return "weapon.ability.beheader";
        }

        @Override
        public int sortOrder() {
            return SORT_ORDER_BASE + 8;
        }

        @Override
        public void onHit(int level, Level level_, Player player, Entity victim, Item tool) {
            if (!(victim instanceof LivingEntity living) || living.getHealth() > 0.0F) {
                return;
            }

            if (living instanceof Skeleton) {
                living.spawnAtLocation(new ItemStack(Items.SKELETON_SKULL), 0.0F);
            } else if (living instanceof WitherSkeleton) {
                if (level_.random.nextInt(20) == 0) {
                    living.spawnAtLocation(new ItemStack(Items.WITHER_SKELETON_SKULL), 0.0F);
                } else {
                    living.spawnAtLocation(new ItemStack(Items.COAL, 3), 0.0F);
                }
            } else if (living instanceof Zombie) {
                living.spawnAtLocation(new ItemStack(Items.ZOMBIE_HEAD), 0.0F);
            } else if (living instanceof Creeper) {
                living.spawnAtLocation(new ItemStack(Items.CREEPER_HEAD), 0.0F);
            } else if (living instanceof MagmaCube) {
                living.spawnAtLocation(new ItemStack(Items.MAGMA_CREAM, 3), 0.0F);
            } else if (living instanceof Slime) {
                living.spawnAtLocation(new ItemStack(Items.SLIME_BALL, 3), 0.0F);
            } else if (living instanceof Player deadPlayer) {
                ItemStack head = new ItemStack(Items.PLAYER_HEAD);
                head.set(DataComponents.PROFILE, new ResolvableProfile(deadPlayer.getGameProfile()));
                living.spawnAtLocation(head, 0.0F);
            } else {
                living.spawnAtLocation(new ItemStack(Items.ROTTEN_FLESH, 3), 0.0F);
                living.spawnAtLocation(new ItemStack(Items.BONE, 2), 0.0F);
            }
        }
    };

    IWeaponAbility[] abilities = { NONE, RADIATION, VAMPIRE, STUN, PHOSPHORUS, FIRE, BEHEADER };

    static IWeaponAbility getByName(String name) {
        for (IWeaponAbility ability : abilities) {
            if (ability.getName().equals(name)) {
                return ability;
            }
        }

        return NONE;
    }
}
