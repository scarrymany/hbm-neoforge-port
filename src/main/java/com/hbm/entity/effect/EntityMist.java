package com.hbm.entity.effect;

import com.hbm.capability.HbmLivingProps;
import com.hbm.damage.ModDamageTypes;
import com.hbm.handler.ArmorUtil;
import com.hbm.handler.radiation.ChunkRadiationManager;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.trait.FT_Corrosive;
import com.hbm.inventory.fluid.trait.FT_Flammable;
import com.hbm.inventory.fluid.trait.FT_Pheromone;
import com.hbm.inventory.fluid.trait.FT_Poison;
import com.hbm.inventory.fluid.trait.FT_Toxin;
import com.hbm.inventory.fluid.trait.FT_VentRadiation;
import com.hbm.inventory.fluid.trait.FluidTraitSimple.FT_Gaseous;
import com.hbm.inventory.fluid.trait.FluidTraitSimple.FT_Gaseous_ART;
import com.hbm.inventory.fluid.trait.FluidTraitSimple.FT_Liquid;
import com.hbm.inventory.fluid.trait.FluidTraitSimple.FT_Viscous;
import com.hbm.util.ContaminationUtil;
import com.hbm.util.ContaminationUtil.ContaminationType;
import com.hbm.util.ContaminationUtil.HazardType;
import com.hbm.util.EntityDamageUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Port of CE's {@code com.hbm.entity.effect.EntityMist} (368 lines, read in full) - the fluid-cloud
 * area-effect entity spawned by disperser grenades/canisters and gas-creeper explosions. Per
 * {@code docs/phase4/entities_orbital_and_beam_payloads.md}'s headline finding #3, every
 * {@link com.hbm.inventory.fluid.trait.FluidTrait} branch {@link #affect} needs is already real and
 * API-identical in this port except two narrow, separately-owned sub-branches (see below) - ported
 * 1:1 otherwise.
 * <p>
 * <b>{@code FT_VentRadiation} - wired for real, not stubbed.</b> The Phase 4 foundation wave already
 * landed {@link ChunkRadiationManager}; both the ambient per-tick {@code incrementRad} call and the
 * per-entity {@link ContaminationUtil#contaminate} call are ported exactly as CE has them.
 * <p>
 * <b>{@code FT_Pheromone}'s glyphid half dropped, player half kept</b>: CE's
 * {@code instanceof EntityGlyphid && pheromone.getType() == 1} branch needs a wholly unstarted mob
 * line ({@code com.hbm.entity.mob.glyphid.*}) not owned by any named Phase 4 sub-area - dropped per
 * the report's Deferred scope (the class literally cannot be referenced without existing). The
 * {@code instanceof EntityPlayer && pheromone.getType() == 2} buff-potion half has no missing
 * dependency and is ported in full.
 * <p>
 * <b>{@code ArmorUtil.damageSuit}</b> takes a real {@link EquipmentSlot} in this port, not CE's raw
 * {@code int} 0-3 loop index - the corrosive-armor-damage loop below iterates
 * {@code FEET/LEGS/CHEST/HEAD} instead.
 * <p>
 * <b>{@code ENDERJUICE} teleport</b>: CE hand-rolls ~65 lines of its own comment's words "terribly
 * copy-pasted from EntityChemical.class, whose method was terribly copy-pasted from
 * EntityEnderman.class" Enderman-teleport logic. Replaced with vanilla's own public
 * {@link Entity#randomTeleport(double, double, double, boolean)} (the same method
 * {@code ChorusFruitItem} calls externally) - same offset math (computed around the mist's own
 * position, exactly as CE's own {@code teleportRandomly} does, not the target's position), vanilla
 * handles the ground-safety search/particle burst/sound itself.
 * <p>
 * <b>Not ported</b>: CE's client-side {@code Tower} particle-effect broadcast (cosmetic, Phase 5
 * scope per the report's Deferred scope).
 */
public class EntityMist extends Entity {

    private static final EntityDataAccessor<Float> WIDTH = SynchedEntityData.defineId(EntityMist.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> HEIGHT = SynchedEntityData.defineId(EntityMist.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> TYPE = SynchedEntityData.defineId(EntityMist.class, EntityDataSerializers.INT);

    public int maxAge = 150;
    /**
     * CE's {@code ticksExisted}, tracked manually - {@link #tick()} never calls {@code super.tick()}/
     * {@code baseTick()} (matching CE's own onEntityUpdate() fully overriding the base class), and
     * {@code tickCount} is only ever incremented from inside {@code baseTick()}; same manual-counter
     * pattern {@code EntityCloudFleija}/{@code EntityCloudSolinium}/{@code EntityEMPBlast} already use
     * in this same file's sibling classes, for the same reason.
     */
    private int age;

    public EntityMist(EntityType<? extends EntityMist> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public static SprayStyle getStyleFromType(FluidType type) {

        if (type.hasTrait(FT_Viscous.class)) {
            return SprayStyle.NULL;
        }

        if (type.hasTrait(FT_Gaseous.class) || type.hasTrait(FT_Gaseous_ART.class)) {
            return SprayStyle.GAS;
        }

        if (type.hasTrait(FT_Liquid.class)) {
            return SprayStyle.MIST;
        }

        return SprayStyle.NULL;
    }

    public EntityMist setArea(float width, float height) {
        this.entityData.set(WIDTH, width);
        this.entityData.set(HEIGHT, height);
        return this;
    }

    public EntityMist setDuration(int duration) {
        this.maxAge = duration;
        return this;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(TYPE, 0);
        builder.define(WIDTH, 0F);
        builder.define(HEIGHT, 0F);
    }

    public FluidType getType() {
        return Fluids.fromID(this.entityData.get(TYPE));
    }

    public EntityMist setType(FluidType fluid) {
        this.entityData.set(TYPE, fluid.getID());
        return this;
    }

    public int getMaxAge() {
        return maxAge;
    }

    @Override
    public void tick() {
        // No super.tick() call - matches CE's onEntityUpdate() fully overriding Entity#onEntityUpdate();
        // this cloud has no vanilla movement/gravity/collision behavior of its own (see the move()/
        // push()/setPos() overrides below).

        float width = this.entityData.get(WIDTH);
        float height = this.entityData.get(HEIGHT);

        if (!level().isClientSide()) {

            if (this.age >= getMaxAge()) {
                this.discard();
                return;
            }
            this.age++;

            FluidType type = getType();

            if (type.hasTrait(FT_VentRadiation.class)) {
                FT_VentRadiation trait = type.getTrait(FT_VentRadiation.class);
                ChunkRadiationManager.proxy.incrementRad(level(), blockPosition(), trait.getRadPerMB() * 2);
            }

            double intensity = 1D - (double) this.age / (double) getMaxAge();

            if (type.hasTrait(FT_Flammable.class) && isOnFire()) {
                level().explode(this, getX(), getY() + height / 2D, getZ(), (float) intensity * 15F, true, Level.ExplosionInteraction.TNT);
                this.discard();
                return;
            }

            AABB aabb = new AABB(
                    getX() - width / 2D, getY(), getZ() - width / 2D,
                    getX() + width / 2D, getY() + height, getZ() + width / 2D);

            List<Entity> affected = level().getEntities(this, aabb,
                    e -> !(e instanceof Player p && (p.isSpectator() || p.isCreative())));

            for (Entity e : affected) {
                if (!(e instanceof EntityMist)) {
                    affect(e, intensity);
                }
            }
        } else {
            // TODO(Phase 5): CE's client-side HbmEffectNT.Tower particle broadcast (2 per tick, purely
            // cosmetic) lives here - see class javadoc.
        }
    }

    /** Can't reuse a generic chemical-cloud entity here - while similar in some places, the actual effects often differ. */
    protected void affect(Entity entity, double intensity) {

        FluidType type = getType();
        LivingEntity living = entity instanceof LivingEntity le ? le : null;

        if (type.temperature >= 100) {
            EntityDamageUtil.attackEntityFromIgnoreIFrame(entity, entity.damageSources().source(ModDamageTypes.BOIL), 0.2F + (type.temperature - 100) * 0.02F);

            if (type.temperature >= 500) {
                entity.igniteForSeconds(10); // afterburn for 10 seconds
            }
        }
        if (type.temperature < -20) {
            if (living != null) { // only living things are affected
                EntityDamageUtil.attackEntityFromIgnoreIFrame(living, living.damageSources().source(ModDamageTypes.ICE), 0.2F + (type.temperature + 20) * -0.05F); // 5 damage at -20C with one extra damage every -20C
                living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 2));
                living.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 100, 4));
            }
        }

        if (type.hasTrait(Fluids.DELICIOUS.getClass())) {
            if (living != null && living.isAlive()) {
                living.heal(2F * (float) intensity);
            }
        }

        if (type.hasTrait(FT_Flammable.class) && type.hasTrait(FT_Liquid.class)) {
            if (living != null) {
                HbmLivingProps.setOil(living, 200); // doused in oil for 10 seconds
            }
        }

        if (isExtinguishing(type)) {
            entity.clearFire();
        }

        if (type.hasTrait(FT_Corrosive.class)) {
            FT_Corrosive trait = type.getTrait(FT_Corrosive.class);

            if (living != null) {
                EntityDamageUtil.attackEntityFromIgnoreIFrame(living, living.damageSources().source(ModDamageTypes.ACID), trait.getRating() / 60F);
                if (living instanceof Player player) {
                    ArmorUtil.damageSuit(player, EquipmentSlot.FEET, trait.getRating() / 50);
                    ArmorUtil.damageSuit(player, EquipmentSlot.LEGS, trait.getRating() / 50);
                    ArmorUtil.damageSuit(player, EquipmentSlot.CHEST, trait.getRating() / 50);
                    ArmorUtil.damageSuit(player, EquipmentSlot.HEAD, trait.getRating() / 50);
                }
            }
        }

        if (type.hasTrait(FT_VentRadiation.class)) {
            FT_VentRadiation trait = type.getTrait(FT_VentRadiation.class);
            if (living != null) {
                ContaminationUtil.contaminate(living, HazardType.RADIATION, ContaminationType.CREATIVE, trait.getRadPerMB() * 5);
            }
        }

        if (type.hasTrait(FT_Poison.class)) {
            FT_Poison trait = type.getTrait(FT_Poison.class);

            if (living != null) {
                living.addEffect(new MobEffectInstance(trait.isWithering() ? MobEffects.WITHER : MobEffects.POISON, (int) (5 * 20 * intensity)));
            }
        }

        if (type.hasTrait(FT_Toxin.class)) {
            FT_Toxin trait = type.getTrait(FT_Toxin.class);

            if (living != null) {
                trait.affect(living, intensity);
            }
        }

        if (type == Fluids.ENDERJUICE && living != null) {
            teleportRandomly(living);
        }

        if (type.hasTrait(FT_Pheromone.class)) {

            FT_Pheromone pheromone = type.getTrait(FT_Pheromone.class);

            // CE's glyphid-aggro half (instanceof EntityGlyphid && pheromone.getType() == 1) is
            // dropped - com.hbm.entity.mob.glyphid.* does not exist anywhere in this port, see class
            // javadoc. The player-buff half has no missing dependency and is kept in full.
            if (living instanceof Player && pheromone.getType() == 2) {
                int mult = pheromone.getType();

                living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, mult * 60 * 20, 1));
                living.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, mult * 60 * 20, 1));
                living.addEffect(new MobEffectInstance(MobEffects.REGENERATION, mult * 2 * 20, 0));
                living.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, mult * 60 * 20, 0));
                living.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, mult * 60 * 20, 1));
                living.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, mult * 60 * 20, 0));
            }
        }
    }

    protected boolean isExtinguishing(FluidType type) {
        return getType().temperature < 50 && !type.hasTrait(FT_Flammable.class);
    }

    // terribly copy-pasted from EntityChemical.class, whose method was terribly copy-pasted from
    // EntityEnderman.class - the fun never ends (CE's own comment) - not applicable to this port's
    // vanilla-randomTeleport-based replacement, kept for provenance.
    private void teleportRandomly(LivingEntity living) {
        double x = getX() + (random.nextDouble() - 0.5D) * 64.0D;
        double y = getY() + (random.nextInt(64) - 32);
        double z = getZ() + (random.nextDouble() - 0.5D) * 64.0D;
        living.randomTeleport(x, y, z, true);
    }

    @Override
    public void move(MoverType type, Vec3 pos) {
        // CE's own empty override - the cloud never moves under vanilla physics.
    }

    @Override
    public void push(double x, double y, double z) {
        // CE's own empty override - the cloud is never pushed by collisions/explosions.
    }

    @Override
    public void setPos(double x, double y, double z) {
        // CE's own guard ("honest to god mojang" comment): only the very first position assignment
        // (at spawn) is allowed through - every later vanilla-internal reposition attempt is ignored,
        // keeping the cloud pinned exactly where it was spawned.
        if (this.tickCount == 0) super.setPos(x, y, z);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        setType(Fluids.readType(tag, "type"));
        setArea(tag.getFloat("width"), tag.getFloat("height"));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        Fluids.writeType(tag, "type", getType());
        tag.putFloat("width", this.entityData.get(WIDTH));
        tag.putFloat("height", this.entityData.get(HEIGHT));
    }

    /**
     * Convenience factory matching CE's real call shape (every actual spawn site constructs, then
     * chains {@code setType/setPosition/setArea/setDuration}) - the natural landing spot for
     * {@code EntityDisperserCanister}/{@code EntityCreeperPhosgene} and future callers.
     */
    public static EntityMist spawn(Level level, double x, double y, double z, FluidType type, float width, float height, int duration) {
        EntityMist mist = new EntityMist(EffectEntityTypes.MIST.get(), level)
                .setType(type)
                .setArea(width, height)
                .setDuration(duration);
        mist.setPos(x, y, z);
        level.addFreshEntity(mist);
        return mist;
    }

    public enum SprayStyle {
        MIST, // liquids that have been sprayed into a mist
        GAS,  // things that were already gaseous
        NULL
    }
}
