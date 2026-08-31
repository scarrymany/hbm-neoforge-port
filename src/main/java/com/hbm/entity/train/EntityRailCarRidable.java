package com.hbm.entity.train;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Entity.MoveFunction;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Port of CE's {@code com.hbm.entity.train.EntityRailCarRidable} (287 lines) - the only
 * rider-carrying vehicle in {@code docs/phase4/entities_vehicles_aircraft.md}'s whole vehicle survey.
 * A player's forward input drives {@link #engineSpeed} directly while they are the
 * {@link #getControllingPassenger()}; extra seats each get their own free-floating
 * {@link SeatDummyEntity} (vanilla only supports one AABB/one direct riding position scheme per
 * entity, so additional seats are separate zero-size passenger-carrying entities repositioned every
 * tick relative to this car - same technique CE itself used).
 * <p>
 * <b>Unconfirmed against a real 1.21.1 jar (sandbox has none - see this package's own
 * {@code knownGaps})</b>: {@link #getForwardInput(Player)} reads {@code player.zza} - the modern
 * (post-{@code moveForward} rename) forward-input axis field vanilla's own {@code AbstractHorse}/
 * {@code Boat} riding code reads directly off a controlling {@code LivingEntity} passenger. This is
 * well-established Mojang-mapping/modding-community knowledge (this exact field is what every rideable
 * modded vehicle reads for driver input), not verified against a compiled jar in this sandbox -
 * flagged explicitly rather than silently assumed, matching {@code docs/phase4/
 * entities_vehicles_aircraft.md}'s own "Open questions" treatment of this exact accessor.
 * Likewise {@link #positionRider}'s {@code Entity.MoveFunction} signature (the modern replacement for
 * CE's {@code updatePassenger(Entity)}) is well-established vanilla API, not independently
 * cross-checked by any file already in this port.
 */
public abstract class EntityRailCarRidable extends EntityRailCarCargo {

    public double engineSpeed;
    public SeatDummyEntity[] passengerSeats;

    protected EntityRailCarRidable(EntityType<? extends EntityRailCarRidable> type, Level level) {
        super(type, level);
        this.passengerSeats = new SeatDummyEntity[this.getPassengerSeats().length];
    }

    public abstract double getPoweredAcceleration();
    public abstract double getPassivBrake();
    public abstract boolean shouldUseEngineBrake(Player player);
    public abstract double getMaxPoweredSpeed();
    public abstract boolean canAccelerate();
    public void consumeFuel() { }

    public double getGravitySpeed() {
        return 0D;
    }

    /** See class javadoc - not independently jar-verified in this sandbox. */
    private static float getForwardInput(Player player) {
        return player.zza;
    }

    @Override
    public double getCurrentSpeed() {

        Entity controller = this.getControllingPassenger();

        if (controller instanceof Player player) {

            if (this.canAccelerate()) {
                float forward = getForwardInput(player);

                if (forward > 0) {
                    engineSpeed += this.getPoweredAcceleration();
                    this.consumeFuel();
                } else if (forward < 0) {
                    engineSpeed -= this.getPoweredAcceleration();
                    this.consumeFuel();
                } else {
                    if (this.shouldUseEngineBrake(player)) {
                        engineSpeed *= this.getPassivBrake();
                    } else {
                        this.consumeFuel();
                    }
                }
            } else {
                engineSpeed *= this.getPassivBrake();
            }

        } else {
            engineSpeed *= this.getPassivBrake();
        }

        double maxSpeed = this.getMaxPoweredSpeed();
        engineSpeed = Mth.clamp(engineSpeed, -maxSpeed, maxSpeed);

        return engineSpeed + this.getGravitySpeed();
    }

    @Override
    public LivingEntity getControllingPassenger() {
        if (this.getPassengers().isEmpty()) return null;
        Entity passenger = this.getPassengers().get(0);
        return passenger instanceof LivingEntity living ? living : null;
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {

        InteractionResult superResult = super.interact(player, hand);
        if (superResult != InteractionResult.PASS) return superResult;
        if (this.level().isClientSide) return InteractionResult.SUCCESS;

        int nearestSeat = this.getNearestSeat(player);

        if (nearestSeat == -1) {
            player.startRiding(this);
        } else if (nearestSeat >= 0) {
            SeatDummyEntity dummySeat = new SeatDummyEntity(this.level(), this, nearestSeat);
            Vec3 passengerSeat = this.getPassengerSeats()[nearestSeat].yRot((float) (-this.getYRot() * Math.PI / 180));
            dummySeat.setPos(renderX + passengerSeat.x, renderY + passengerSeat.y - 1, renderZ + passengerSeat.z);
            passengerSeats[nearestSeat] = dummySeat;
            this.level().addFreshEntity(dummySeat);
            player.startRiding(dummySeat);
        }

        return InteractionResult.SUCCESS;
    }

    public int getNearestSeat(Player player) {

        if (player == null) return -2;

        double nearestDist = Double.POSITIVE_INFINITY;
        int nearestSeat = -3;

        Vec3[] seats = getPassengerSeats();
        Vec3 look = player.getEyePosition(1F).add(player.getLookAngle());

        for (int i = 0; i < seats.length; i++) {

            Vec3 seat = seats[i];
            if (seat == null) continue;
            if (passengerSeats[i] != null) continue;

            Vec3 rot = seat.yRot((float) (-this.getYRot() * Math.PI / 180));
            Vec3 delta = new Vec3(look.x - (renderX + rot.x), look.y - (renderY + rot.y), look.z - (renderZ + rot.z));
            double dist = delta.length();

            if (dist < nearestDist) {
                nearestDist = dist;
                nearestSeat = i;
            }
        }

        if (!this.isVehicle()) {
            Vec3 rot = getRiderSeatPosition().yRot((float) (-this.getYRot() * Math.PI / 180));
            Vec3 delta = new Vec3(look.x - (renderX + rot.x), look.y - (renderY + rot.y), look.z - (renderZ + rot.z));
            double dist = delta.length();

            if (dist < nearestDist) {
                nearestDist = dist;
                nearestSeat = -1;
            }
        }

        if (nearestDist > 180) return -2;

        return nearestSeat;
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide) {

            Vec3[] seats = this.getPassengerSeats();
            for (int i = 0; i < passengerSeats.length; i++) {
                SeatDummyEntity seat = passengerSeats[i];

                if (seat != null) {
                    if (!seat.isVehicle()) {
                        passengerSeats[i] = null;
                        seat.discard();
                    } else {
                        Vec3 rot = seats[i]
                                .xRot((float) (this.getXRot() * Math.PI / 180))
                                .yRot((float) (-this.getYRot() * Math.PI / 180));
                        seat.setPos(renderX + rot.x, renderY + rot.y - 1, renderZ + rot.z);
                    }
                }
            }
        }
    }

    @Override
    protected void positionRider(Entity passenger, MoveFunction callback) {
        Vec3 offset = getRiderSeatPosition()
                .xRot((float) (this.getXRot() * Math.PI / 180))
                .yRot((float) (-this.getYRot() * Math.PI / 180));

        callback.accept(passenger, this.renderX + offset.x, this.renderY + offset.y, this.renderZ + offset.z);
    }

    public abstract Vec3 getRiderSeatPosition();

    public abstract Vec3[] getPassengerSeats();

    /**
     * Port of CE's nested {@code SeatDummyEntity} - a zero-size passenger-carrying entity for one
     * non-driver seat (see class javadoc). Repositioned every server tick by the parent
     * {@link EntityRailCarRidable#tick()}; client-side smoothing uses the same vanilla lerp-target
     * treatment as {@link EntityRailCarBase.BoundingBoxDummyEntity}.
     */
    public static class SeatDummyEntity extends Entity {

        private static final EntityDataAccessor<Integer> TRAIN_ID =
                SynchedEntityData.defineId(SeatDummyEntity.class, EntityDataSerializers.INT);
        private static final EntityDataAccessor<Integer> SEAT_INDEX =
                SynchedEntityData.defineId(SeatDummyEntity.class, EntityDataSerializers.INT);

        protected int lerpSteps;
        protected double lerpX;
        protected double lerpY;
        protected double lerpZ;

        public EntityRailCarRidable train;

        public SeatDummyEntity(EntityType<? extends SeatDummyEntity> type, Level level) {
            super(type, level);
        }

        public SeatDummyEntity(Level level, EntityRailCarRidable train, int index) {
            this(TrainEntityTypes.TRAIN_SEAT.get(), level);
            this.train = train;
            if (train != null) this.entityData.set(TRAIN_ID, train.getId());
            this.entityData.set(SEAT_INDEX, index);
        }

        @Override
        protected void defineSynchedData(SynchedEntityData.Builder builder) {
            builder.define(TRAIN_ID, 0);
            builder.define(SEAT_INDEX, 0);
        }

        @Override protected void readAdditionalSaveData(CompoundTag tag) { this.discard(); }
        @Override protected void addAdditionalSaveData(CompoundTag tag) { }

        @Override
        public void lerpTo(double x, double y, double z, float yRot, float xRot, int steps) {
            this.lerpX = x;
            this.lerpY = y;
            this.lerpZ = z;
            this.lerpSteps = 10;
        }

        @Override public double lerpTargetX() { return this.lerpSteps > 0 ? this.lerpX : this.getX(); }
        @Override public double lerpTargetY() { return this.lerpSteps > 0 ? this.lerpY : this.getY(); }
        @Override public double lerpTargetZ() { return this.lerpSteps > 0 ? this.lerpZ : this.getZ(); }

        @Override
        public void tick() {
            if (this.level().isClientSide) {
                if (this.lerpSteps > 0) {
                    this.lerpPositionAndRotationStep(this.lerpSteps, this.lerpX, this.lerpY, this.lerpZ, this.getYRot(), this.getXRot());
                    this.lerpSteps--;
                } else {
                    this.reapplyPosition();
                }
            } else if (this.train == null || this.train.isRemoved()) {
                this.discard();
            }
        }

        @Override
        protected void positionRider(Entity passenger, MoveFunction callback) {

            if (train == null) {
                Entity entity = this.level().getEntity(this.entityData.get(TRAIN_ID));
                if (entity instanceof EntityRailCarRidable ridable) train = ridable;
            }

            if (train == null) {
                callback.accept(passenger, this.getX(), this.getY() + 1, this.getZ());
                return;
            }

            int index = this.entityData.get(SEAT_INDEX);
            Vec3[] seats = this.train.getPassengerSeats();

            if (index < 0 || index >= seats.length) {
                callback.accept(passenger, this.getX(), this.getY() + 1, this.getZ());
                return;
            }

            Vec3 rot = seats[index]
                    .xRot((float) (train.getXRot() * Math.PI / 180))
                    .yRot((float) (-train.getYRot() * Math.PI / 180));
            callback.accept(passenger, train.renderX + rot.x, train.renderY + rot.y, train.renderZ + rot.z);
        }
    }
}
