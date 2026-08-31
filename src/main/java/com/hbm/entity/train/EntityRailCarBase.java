package com.hbm.entity.train;

import com.hbm.blocks.ILookOverlay;
import com.hbm.blocks.rail.IRailNTM;
import com.hbm.blocks.rail.IRailNTM.MoveContext;
import com.hbm.blocks.rail.IRailNTM.RailCheckType;
import com.hbm.blocks.rail.IRailNTM.RailContext;
import com.hbm.blocks.rail.IRailNTM.TrackGauge;
import com.hbm.items.tool.ToolItems;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Port of CE's {@code com.hbm.entity.train.EntityRailCarBase} (782 lines) - the base rail-car
 * entity, per {@code docs/phase4/entities_vehicles_aircraft.md}'s rail/train table. As that report's
 * Headline finding #4 stresses: this is <b>not</b> a {@code motionX/Y/Z} vehicle - locomotion is a
 * pure block-query walk via {@link IRailNTM#getTravelLocation}, and {@link #move}/vanilla velocity is
 * only ever used for the "fell off the rails, coast to a stop" fallback in {@link #tick()}.
 * <p>
 * <b>Client interpolation</b>: per this port's own Key design decision (cross-checked against Neo
 * Edition's real {@code PlaneBase} and this port's own already-compiling
 * {@link com.hbm.entity.item.EntityMovingConveyorObject}), CE's hand-rolled {@code turnProgress}/
 * {@code trainX/Y/Z}/{@code trainYaw}/{@code trainPitch} fields and the {@code setVelocity} hack that
 * smuggled full-precision yaw/pitch through the entity-velocity network channel are replaced by
 * vanilla's own {@link #lerpTo}/{@link #lerpTargetX()}/{@link #lerpTargetY()}/{@link #lerpTargetZ()}/
 * {@link #lerpTargetXRot()}/{@link #lerpTargetYRot()} hooks - the standard entity-tracker packet path
 * calls these automatically, exactly like {@code PlaneBase} needs zero custom networking. The
 * anchor-vs-render distinction itself (CE's actual reason for the interpolation complexity - a
 * multi-block-long car whose visual position differs from its single-point rail-query anchor) is kept
 * exactly as CE built it: {@link #renderX}/{@link #renderY}/{@link #renderZ} are recomputed fresh each
 * tick from two additional rail queries (front axle, rear axle) off the just-interpolated anchor
 * position/yaw - a pure derived function of already-synced state, not something that itself needs
 * syncing, which is why CE's own server side only ever sets it once per {@link #updateMotion} pass
 * rather than every tick.
 * <p>
 * <b>{@link #updateMotion(ServerLevel)} - CE's own dead code, now actually wired up</b>: a full
 * repo-wide grep of {@code upstream/hbm-ce} found <b>zero callers</b> of
 * {@code EntityRailCarBase.updateMotion(World)} anywhere in real CE 2.5.0.5 - the entire consist-speed/
 * movement pass this class defines is unreachable in the mod this port is based on (a genuine CE bug/
 * dead feature, not a mis-port - see this task's {@code realBugsFound}). Preserving that as literally
 * dead code would ship trains that sit motionless forever, which is not a faithful port of "what the
 * mod's trains do" (CE's trains functionally do nothing) so much as a faithful port of an oversight.
 * This port wires {@link #updateMotion(ServerLevel)} to fire every server tick from
 * {@link com.hbm.entity.train.TrainEntityTypes}'s own {@code ServerTickEvent.Pre} subscriber (mirroring
 * CE's evident intent - a {@code WorldTickEvent}-driven pass - and this port's own confirmed-real
 * {@code PollutionHandler}/{@code ServerTickEvent.Pre} precedent for a per-level tick pass).
 */
public abstract class EntityRailCarBase extends Entity implements ILookOverlay {

    public LogicalTrainUnit ltu;
    public int ltuIndex = 0;
    public boolean isOnRail = true;

    /** Vanilla lerp-target fields (see class javadoc) - matches
     * {@link com.hbm.entity.item.EntityMovingConveyorObject}'s own identical fields exactly. */
    protected int lerpSteps;
    protected double lerpX;
    protected double lerpY;
    protected double lerpZ;
    protected float lerpYRot;
    protected float lerpXRot;

    public double lastRenderX;
    public double lastRenderY;
    public double lastRenderZ;
    public double renderX;
    public double renderY;
    public double renderZ;
    public double cachedSpeed;

    public EntityRailCarBase coupledFront;
    public EntityRailCarBase coupledBack;

    private boolean initDummies = false;
    private BoundingBoxDummyEntity[] dummies = new BoundingBoxDummyEntity[0];

    protected EntityRailCarBase(EntityType<? extends EntityRailCarBase> type, Level level) {
        super(type, level);
    }

    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) { }
    @Override protected void readAdditionalSaveData(CompoundTag tag) { }
    @Override protected void addAdditionalSaveData(CompoundTag tag) { }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {

        ItemStack held = player.getItemInHand(hand);

        if (!held.isEmpty() && held.getItem() == ToolItems.COUPLING_TOOL.get()) {

            List<EntityRailCarBase> intersecting = this.level().getEntitiesOfClass(EntityRailCarBase.class, this.getBoundingBox().inflate(2D, 0D, 2D));

            for (EntityRailCarBase neighbor : intersecting) {
                if (neighbor == this) continue;
                if (neighbor.getGauge() != this.getGauge()) continue;

                TrainCoupling closestOwnCoupling = null;
                TrainCoupling closestNeighborCoupling = null;
                double closestDist = Double.POSITIVE_INFINITY;

                for (TrainCoupling ownCoupling : TrainCoupling.values()) {
                    for (TrainCoupling neighborCoupling : TrainCoupling.values()) {
                        Vec3 ownPos = this.getCouplingPos(ownCoupling);
                        Vec3 neighborPos = neighbor.getCouplingPos(neighborCoupling);
                        if (ownPos != null && neighborPos != null) {
                            double length = ownPos.subtract(neighborPos).length();

                            if (length < 1 && length < closestDist) {
                                closestDist = length;
                                closestOwnCoupling = ownCoupling;
                                closestNeighborCoupling = neighborCoupling;
                            }
                        }
                    }
                }

                if (closestOwnCoupling != null && closestNeighborCoupling != null) {
                    if (this.getCoupledTo(closestOwnCoupling) != null) continue;
                    if (neighbor.getCoupledTo(closestNeighborCoupling) != null) continue;
                    this.couple(closestOwnCoupling, neighbor);
                    neighbor.couple(closestNeighborCoupling, this);
                    if (this.ltu != null) this.ltu.dissolveTrain();
                    if (neighbor.ltu != null) neighbor.ltu.dissolveTrain();
                    player.swing(hand);

                    player.sendSystemMessage(Component.literal("Coupled " + this.hashCode() + " (" + closestOwnCoupling.name() + ") to " + neighbor.hashCode() + " (" + closestNeighborCoupling.name() + ")"));

                    return InteractionResult.SUCCESS;
                }
            }
        }

        return InteractionResult.PASS;
    }

    @Override
    public void lerpTo(double x, double y, double z, float yRot, float xRot, int steps) {
        this.lerpX = x;
        this.lerpY = y;
        this.lerpZ = z;
        this.lerpYRot = yRot;
        this.lerpXRot = xRot;
        this.lerpSteps = 10;
    }

    @Override public double lerpTargetX() { return this.lerpSteps > 0 ? this.lerpX : this.getX(); }
    @Override public double lerpTargetY() { return this.lerpSteps > 0 ? this.lerpY : this.getY(); }
    @Override public double lerpTargetZ() { return this.lerpSteps > 0 ? this.lerpZ : this.getZ(); }
    @Override public float lerpTargetXRot() { return this.lerpSteps > 0 ? this.lerpXRot : this.getXRot(); }
    @Override public float lerpTargetYRot() { return this.lerpSteps > 0 ? this.lerpYRot : this.getYRot(); }

    @Override
    public void tick() {

        Level level = this.level();

        if (level.isClientSide) {

            if (this.lerpSteps > 0) {
                this.lerpPositionAndRotationStep(this.lerpSteps, this.lerpX, this.lerpY, this.lerpZ, this.lerpYRot, this.lerpXRot);
                this.lerpSteps--;
            } else {
                this.reapplyPosition();
            }

            BlockPos anchor = this.getCurrentAnchorPos();
            Vec3 frontPos = getRelPosAlongRail(anchor, this.getLengthSpan(), new MoveContext(RailCheckType.FRONT, this.getCollisionSpan() - this.getLengthSpan()));
            Vec3 backPos = getRelPosAlongRail(anchor, -this.getLengthSpan(), new MoveContext(RailCheckType.BACK, this.getCollisionSpan() - this.getLengthSpan()));

            this.lastRenderX = this.renderX;
            this.lastRenderY = this.renderY;
            this.lastRenderZ = this.renderZ;

            if (frontPos != null && backPos != null) {
                this.renderX = (frontPos.x + backPos.x) / 2D;
                this.renderY = (frontPos.y + backPos.y) / 2D;
                this.renderZ = (frontPos.z + backPos.z) / 2D;
            } else {
                this.renderX = this.getX();
                this.renderY = this.getY();
                this.renderZ = this.getZ();
            }

            return;
        }

        if (!this.isOnRail) {
            if (this.coupledFront != null) this.coupledFront.couple(this.coupledFront.getCouplingFrom(this), null);
            if (this.coupledBack != null) this.coupledBack.couple(this.coupledBack.getCouplingFrom(this), null);
            this.coupledFront = null;
            this.coupledBack = null;
        }

        if (this.coupledFront != null && this.coupledFront.isRemoved()) {
            this.coupledFront = null;
            if (this.ltu != null) this.ltu.dissolveTrain();
        }
        if (this.coupledBack != null && this.coupledBack.isRemoved()) {
            this.coupledBack = null;
            if (this.ltu != null) this.ltu.dissolveTrain();
        }

        if (this.ltu == null && (this.coupledFront == null || this.coupledBack == null) && this.isOnRail) {
            LogicalTrainUnit.generateTrain(this);
        }

        if (!this.isOnRail) {
            Vec3 motion = new Vec3(0, 0, this.cachedSpeed).yRot((float) (-this.getYRot() * Math.PI / 180D));
            this.move(MoverType.SELF, new Vec3(motion.x, motion.y - 0.04, motion.z));
            this.renderX = this.getX();
            this.renderY = this.getY();
            this.renderZ = this.getZ();
            this.cachedSpeed *= 0.95D;
        }

        DummyConfig[] definitions = this.getDummies();

        if (!this.initDummies) {
            this.dummies = new BoundingBoxDummyEntity[definitions.length];

            for (int i = 0; i < definitions.length; i++) {
                DummyConfig def = definitions[i];
                BoundingBoxDummyEntity dummy = new BoundingBoxDummyEntity(level, this, def.width, def.height);
                Vec3 rot = def.offset.yRot((float) (-this.getYRot() * Math.PI / 180));
                dummy.setPos(this.getX() + rot.x, this.getY() + rot.y, this.getZ() + rot.z);
                dummy.setSize(def.width, def.height);
                level.addFreshEntity(dummy);
                this.dummies[i] = dummy;
            }

            this.initDummies = true;
        }

        if (this.renderY != 0) {
            for (int i = 0; i < definitions.length; i++) {
                DummyConfig def = definitions[i];
                BoundingBoxDummyEntity dummy = this.dummies[i];
                Vec3 rot = def.offset
                        .xRot((float) (this.getXRot() * Math.PI / 180D))
                        .yRot((float) (-this.getYRot() * Math.PI / 180));
                dummy.setPos(this.renderX + rot.x, this.renderY + rot.y, this.renderZ + rot.z);
            }
        }
    }

    public Vec3 getRelPosAlongRail(BlockPos anchor, double distanceToCover, MoveContext context) {
        return getRelPosAlongRail(anchor, distanceToCover, this.getGauge(), this.level(), new Vec3(this.getX(), this.getY(), this.getZ()), this.getYRot(), context);
    }

    public static Vec3 getRelPosAlongRail(BlockPos anchor, double distanceToCover, TrackGauge gauge, Level level, Vec3 next, float yaw, MoveContext context) {

        if (distanceToCover < 0) {
            distanceToCover *= -1;
            yaw += 180;
        }

        int it = 0;

        do {

            it++;

            if (it > 30) {
                return null;
            }

            int x = anchor.getX();
            int y = anchor.getY();
            int z = anchor.getZ();
            Block block = level.getBlockState(anchor).getBlock();

            Vec3 rot = new Vec3(0, 0, 1).yRot((float) (-yaw * Math.PI / 180D));

            if (block instanceof IRailNTM rail) {

                if (it == 1) {
                    next = rail.getTravelLocation(level, x, y, z, next.x, next.y, next.z, rot.x, rot.y, rot.z, 0, new RailContext(), context);
                }

                boolean flip = distanceToCover < 0;

                if (rail.getGauge(level, x, y, z) == gauge) {
                    RailContext info = new RailContext();
                    Vec3 prev = next;
                    next = rail.getTravelLocation(level, x, y, z, prev.x, prev.y, prev.z, rot.x, rot.y, rot.z, distanceToCover, info, context);
                    distanceToCover = info.overshoot;
                    anchor = info.pos;

                    yaw = generateYaw(next, prev) * (flip ? -1 : 1);

                } else {
                    return null;
                }
            } else {
                return null;
            }

        } while (distanceToCover != 0);

        return next;
    }

    public static float generateYaw(Vec3 front, Vec3 back) {
        double deltaX = front.x - back.x;
        double deltaZ = front.z - back.z;
        double radians = -Math.atan2(deltaX, deltaZ);
        return Mth.wrapDegrees((float) (radians * 180D / Math.PI));
    }

    /** See class javadoc - wired to actually fire (unlike CE's own unreachable original) from
     * {@link TrainEntityTypes}'s {@code ServerTickEvent.Pre} subscriber. */
    public static void updateMotion(ServerLevel level) {
        Set<LogicalTrainUnit> ltus = new HashSet<>();

        for (Entity o : level.getEntities().getAll()) {
            if (o instanceof EntityRailCarBase train) {
                if (train.ltu != null) ltus.add(train.ltu);
            }
        }

        for (LogicalTrainUnit ltu : ltus) {

            double speed = ltu.getTotalSpeed() + ltu.pushForce;

            if (Math.abs(speed) < 0.001) speed = 0;

            for (EntityRailCarBase car : ltu.trains) car.cachedSpeed = speed;

            if (ltu.trains.length == 1) {

                EntityRailCarBase train = ltu.trains[0];

                BlockPos anchor = BlockPos.containing(train.getX(), train.getY(), train.getZ());
                Vec3 newPos = train.getRelPosAlongRail(anchor, speed, new MoveContext(RailCheckType.CORE, 0));
                if (newPos == null) {
                    train.derail();
                    ltu.dissolveTrain();
                    continue;
                }
                train.setPos(newPos.x, newPos.y, newPos.z);
                anchor = train.getCurrentAnchorPos();
                Vec3 frontPos = train.getRelPosAlongRail(anchor, train.getLengthSpan(), new MoveContext(RailCheckType.FRONT, train.getCollisionSpan() - train.getLengthSpan()));
                Vec3 backPos = train.getRelPosAlongRail(anchor, -train.getLengthSpan(), new MoveContext(RailCheckType.BACK, train.getCollisionSpan() - train.getLengthSpan()));

                if (frontPos == null || backPos == null) {
                    train.derail();
                    ltu.dissolveTrain();
                    continue;
                } else {
                    ltu.setRenderPos(train, frontPos, backPos);
                }

                ltu.pushForce = 0;
                ltu.collideTrain(speed);

                continue;
            }

            if (speed == 0) {
                ltu.combineWagons();
            } else {
                ltu.moveTrainByApproach(speed);
            }

            ltu.pushForce = 0;
            ltu.collideTrain(speed);
        }
    }

    public abstract double getCurrentSpeed();
    public abstract double getMaxRailSpeed();
    public abstract TrackGauge getGauge();
    public abstract double getLengthSpan();
    public abstract double getCollisionSpan();

    public BlockPos getCurrentAnchorPos() {
        return BlockPos.containing(this.getX(), this.getY() + 0.25, this.getZ());
    }

    public void derail() {
        isOnRail = false;
    }

    public DummyConfig[] getDummies() {
        return new DummyConfig[0];
    }

    public static class DummyConfig {
        public Vec3 offset;
        public float width;
        public float height;

        public DummyConfig(float width, float height, Vec3 offset) {
            this.width = width;
            this.height = height;
            this.offset = offset;
        }
    }

    public enum TrainCoupling {
        FRONT,
        BACK
    }

    public double getCouplingDist(TrainCoupling coupling) {
        return 0D;
    }

    public Vec3 getCouplingPos(TrainCoupling coupling) {
        double dist = this.getCouplingDist(coupling);

        if (dist <= 0) return null;

        if (coupling == TrainCoupling.BACK) dist *= -1;

        Vec3 rot = new Vec3(0, 0, dist).yRot((float) (-this.getYRot() * Math.PI / 180D));
        return rot.add(this.renderX, this.renderY, this.renderZ);
    }

    public EntityRailCarBase getCoupledTo(TrainCoupling coupling) {
        return coupling == TrainCoupling.FRONT ? this.coupledFront : coupling == TrainCoupling.BACK ? this.coupledBack : null;
    }

    public TrainCoupling getCouplingFrom(EntityRailCarBase coupledTo) {
        return coupledTo == this.coupledFront ? TrainCoupling.FRONT : coupledTo == this.coupledBack ? TrainCoupling.BACK : null;
    }

    public void couple(TrainCoupling coupling, EntityRailCarBase to) {
        if (coupling == TrainCoupling.FRONT) this.coupledFront = to;
        if (coupling == TrainCoupling.BACK) this.coupledBack = to;
    }

    /**
     * The consist/train-physics core, ported verbatim (algorithm-for-algorithm) from CE's own nested
     * class of the same name - per {@code docs/phase4/entities_vehicles_aircraft.md}'s own risk note,
     * this is the single largest pure-logic surface in the whole rail/train system (multi-car speed
     * clamping, concertina-compression wagon spacing, push-force collision resolution). Kept as
     * clearly-named, individually testable methods exactly as CE structured them, rather than being
     * folded into {@link EntityRailCarBase} itself.
     */
    public static class LogicalTrainUnit {

        protected double pushForce;
        protected EntityRailCarBase[] trains;

        public static LogicalTrainUnit generateTrain(EntityRailCarBase train) {
            List<EntityRailCarBase> links = new ArrayList<>();
            Set<EntityRailCarBase> brake = new HashSet<>();
            LogicalTrainUnit ltu = new LogicalTrainUnit();

            if (train.coupledFront == null && train.coupledBack == null) {
                ltu.trains = new EntityRailCarBase[] {train};
                train.ltu = ltu;
                train.ltuIndex = 0;
                return ltu;
            }

            EntityRailCarBase current = train;
            EntityRailCarBase next;

            do {
                next = null;

                if (current.coupledFront != null && !brake.contains(current.coupledFront)) next = current.coupledFront;
                if (current.coupledBack != null && !brake.contains(current.coupledBack)) next = current.coupledBack;

                links.add(current);
                brake.add(current);

                current = next;

            } while (next != null);

            ltu.trains = new EntityRailCarBase[links.size()];
            for (int i = 0; i < ltu.trains.length; i++) {
                ltu.trains[i] = links.get(i);
                ltu.trains[i].ltu = ltu;
                ltu.trains[i].ltuIndex = i;
            }

            return ltu;
        }

        public void dissolveTrain() {
            for (EntityRailCarBase train : trains) {
                train.ltu = null;
                train.ltuIndex = 0;
            }
        }

        public void combineWagons() {

            if (trains.length <= 1) return;

            boolean odd = trains.length % 2 == 1;
            int centerIndex = odd ? trains.length / 2 : trains.length / 2 - 1;
            EntityRailCarBase center = trains[centerIndex];
            EntityRailCarBase prev = center;

            for (int i = centerIndex - 1; i >= 0; i--) {
                EntityRailCarBase next = trains[i];
                moveWagonTo(prev, next);
                prev = next;
            }

            prev = center;
            for (int i = centerIndex + 1; i < trains.length; i++) {
                EntityRailCarBase next = trains[i];
                moveWagonTo(prev, next);
                prev = next;
            }
        }

        public void moveWagonTo(EntityRailCarBase moveTo, EntityRailCarBase moving) {
            TrainCoupling prevCouple = moveTo.getCouplingFrom(moving);
            TrainCoupling nextCouple = moving.getCouplingFrom(moveTo);
            Vec3 prevLoc = moveTo.getCouplingPos(prevCouple);
            Vec3 nextLoc = moving.getCouplingPos(nextCouple);
            Vec3 delta = new Vec3(prevLoc.x - nextLoc.x, 0, prevLoc.z - nextLoc.z);
            double len = delta.length();
            len = (len / (0.5D / (len * len) + 1D));
            BlockPos anchor = BlockPos.containing(moving.getX(), moving.getY(), moving.getZ());
            Vec3 trainPos = new Vec3(moving.getX(), moving.getY(), moving.getZ());
            float yaw = EntityRailCarBase.generateYaw(prevLoc, nextLoc);
            Vec3 newPos = EntityRailCarBase.getRelPosAlongRail(anchor, len, moving.getGauge(), moving.level(), trainPos, yaw, new MoveContext(RailCheckType.CORE, 0));

            if (newPos == null) {
                moving.derail();
                this.dissolveTrain();
                return;
            }

            moving.setPos(newPos.x, newPos.y, newPos.z);
            anchor = moving.getCurrentAnchorPos();
            Vec3 frontPos = moving.getRelPosAlongRail(anchor, moving.getLengthSpan(), new MoveContext(RailCheckType.FRONT, moving.getCollisionSpan() - moving.getLengthSpan()));
            Vec3 backPos = moving.getRelPosAlongRail(anchor, -moving.getLengthSpan(), new MoveContext(RailCheckType.BACK, moving.getCollisionSpan() - moving.getLengthSpan()));

            if (frontPos == null || backPos == null) {
                moving.derail();
                this.dissolveTrain();
            } else {
                setRenderPos(moving, frontPos, backPos);
            }
        }

        public double getTotalSpeed() {

            EntityRailCarBase prev = trains[0];
            double totalSpeed = 0;
            double maxSpeed = Double.POSITIVE_INFINITY;
            boolean reverseTheReverse = prev.getCouplingFrom(null) == TrainCoupling.BACK;

            if (trains.length == 1) {
                return prev.getCurrentSpeed();
            }

            for (EntityRailCarBase train : this.trains) {
                boolean reverse = false;

                EntityRailCarBase conFront = train.getCoupledTo(TrainCoupling.FRONT);
                EntityRailCarBase conBack = train.getCoupledTo(TrainCoupling.BACK);

                if (conFront != null && conFront.ltuIndex > train.ltuIndex) reverse = true;
                if (conBack != null && conBack.ltuIndex < train.ltuIndex) reverse = true;

                reverse ^= reverseTheReverse;

                double speed = train.getCurrentSpeed();
                if (reverse) speed *= -1;
                totalSpeed += speed;
                maxSpeed = Math.min(maxSpeed, train.getMaxRailSpeed());
            }

            if (Math.abs(totalSpeed) > maxSpeed) {
                totalSpeed = maxSpeed * Math.signum(totalSpeed);
            }

            return totalSpeed;
        }

        public void moveTrainByApproach(double speed) {
            EntityRailCarBase previous = null;
            EntityRailCarBase first = this.trains[0];
            boolean forward = speed > 0;
            boolean order = forward ^ first.getCouplingFrom(null) == TrainCoupling.BACK;

            for (int i = order ? 0 : this.trains.length - 1; order ? i < this.trains.length : i >= 0; i += order ? 1 : -1) {
                EntityRailCarBase current = this.trains[i];

                if (previous == null) {

                    if (first == current) speed *= -1;

                    boolean inReverse = first.getCouplingFrom(null) == current.getCouplingFrom(null);
                    int sigNum = inReverse ? 1 : -1;
                    BlockPos anchor = current.getCurrentAnchorPos();

                    Vec3 frontPos = current.getRelPosAlongRail(anchor, (speed + current.getLengthSpan()) * -sigNum, new MoveContext(RailCheckType.FRONT, current.getCollisionSpan() - current.getLengthSpan()));

                    if (frontPos == null) {
                        current.derail();
                        this.dissolveTrain();
                        return;
                    } else {
                        anchor = current.getCurrentAnchorPos();
                        Vec3 corePos = current.getRelPosAlongRail(anchor, speed * -sigNum, new MoveContext(RailCheckType.CORE, 0));

                        if (corePos == null) {
                            current.derail();
                            this.dissolveTrain();
                            return;
                        }

                        current.setPos(corePos.x, corePos.y, corePos.z);
                        Vec3 backPos = current.getRelPosAlongRail(anchor, (speed - current.getLengthSpan()) * -sigNum, new MoveContext(RailCheckType.BACK, current.getCollisionSpan() - current.getLengthSpan()));

                        if (backPos == null) {
                            current.derail();
                            this.dissolveTrain();
                            return;
                        } else {
                            setRenderPos(current, inReverse ? backPos : frontPos, inReverse ? frontPos : backPos);
                        }
                    }

                } else {
                    this.moveWagonTo(previous, current);
                }

                previous = current;
            }
        }

        /** CE's own version routes the derived yaw/pitch through a client-velocity-packet hack for
         * smoother interpolation (see class javadoc) - dropped here in favor of plain
         * {@link Entity#setYRot}/{@link Entity#setXRot}, which vanilla's own entity tracker already
         * syncs to clients every tick same as any other moving entity. */
        public void setRenderPos(EntityRailCarBase current, Vec3 frontPos, Vec3 backPos) {
            current.renderX = (frontPos.x + backPos.x) / 2D;
            current.renderY = (frontPos.y + backPos.y) / 2D;
            current.renderZ = (frontPos.z + backPos.z) / 2D;
            current.yRotO = current.getYRot();
            float yaw = generateYaw(frontPos, backPos);
            current.setYRot(yaw);
            Vec3 delta = new Vec3(frontPos.x - backPos.x, frontPos.y - backPos.y, frontPos.z - backPos.z);
            current.xRotO = current.getXRot();
            float pitch = (float) (Math.asin(delta.y / delta.length()) * 180D / Math.PI);
            current.setXRot(pitch);
        }

        public void collideTrain(double speed) {
            EntityRailCarBase collidingTrain = speed > 0 ? trains[0] : trains[trains.length - 1];
            List<EntityRailCarBase> intersect = collidingTrain.level().getEntitiesOfClass(EntityRailCarBase.class, collidingTrain.getBoundingBox().inflate(1, 1, 1));
            EntityRailCarBase collidesWith = null;

            for (EntityRailCarBase train : intersect) {
                if (train.ltu != null && train.ltu != this) {
                    collidesWith = train;
                    break;
                }
            }

            if (collidesWith == null) return;

            Vec3 delta = new Vec3(collidingTrain.getX() - collidesWith.getX(), 0, collidingTrain.getZ() - collidesWith.getZ());
            double totalSpan = collidingTrain.getCollisionSpan() + collidesWith.getCollisionSpan();
            double diff = delta.length();
            if (diff > totalSpan) return;
            double push = (totalSpan - diff);

            EntityRailCarBase[][] whatever = new EntityRailCarBase[][] {{collidingTrain, collidesWith}, {collidesWith, collidingTrain}};
            for (EntityRailCarBase[] array : whatever) {
                LogicalTrainUnit ltu = array[0].ltu;
                if (ltu.trains.length == 1) {
                    Vec3 rot = new Vec3(0, 0, array[0].getCollisionSpan())
                            .xRot((float) (array[0].getXRot() * Math.PI / 180D))
                            .yRot((float) (-array[0].getYRot() * Math.PI / 180));
                    Vec3 forward = new Vec3(array[1].getX() - (array[0].getX() + rot.x), 0, array[1].getZ() - (array[0].getZ() + rot.z));
                    Vec3 backward = new Vec3(array[1].getX() - (array[0].getX() - rot.x), 0, array[1].getZ() - (array[0].getZ() - rot.z));

                    if (forward.length() > backward.length()) {
                        ltu.pushForce += push;
                    } else {
                        ltu.pushForce -= push;
                    }
                } else {

                    if (array[0].ltuIndex < ltu.trains.length / 2) {
                        ltu.pushForce -= push;
                    } else {
                        ltu.pushForce += push;
                    }
                }
            }
        }
    }

    /**
     * Port of CE's nested {@code BoundingBoxDummyEntity} - a separate spawned collision-box "shadow"
     * entity for one segment of a multi-block-long car (vanilla only allows one AABB per entity), per
     * {@link #getDummies()}. Width/height are per-instance and therefore synced (unlike the parent
     * car's own fixed {@link net.minecraft.world.entity.EntityType.Builder#sized} size) - see
     * {@link #setSize(float, float)}.
     */
    public static class BoundingBoxDummyEntity extends Entity implements ILookOverlay {

        private static final EntityDataAccessor<Integer> TRAIN_ID =
                SynchedEntityData.defineId(BoundingBoxDummyEntity.class, EntityDataSerializers.INT);
        private static final EntityDataAccessor<Float> DUMMY_WIDTH =
                SynchedEntityData.defineId(BoundingBoxDummyEntity.class, EntityDataSerializers.FLOAT);
        private static final EntityDataAccessor<Float> DUMMY_HEIGHT =
                SynchedEntityData.defineId(BoundingBoxDummyEntity.class, EntityDataSerializers.FLOAT);

        public EntityRailCarBase train;

        /** Vanilla lerp-target fields, same treatment as the parent car (see class javadoc) - CE's own
         * dummy likewise just smoothed between whatever absolute positions the server last sent it. */
        protected int lerpSteps;
        protected double lerpX;
        protected double lerpY;
        protected double lerpZ;

        public BoundingBoxDummyEntity(EntityType<? extends BoundingBoxDummyEntity> type, Level level) {
            super(type, level);
        }

        public BoundingBoxDummyEntity(Level level, EntityRailCarBase train, float width, float height) {
            this(TrainEntityTypes.BOUNDING_DUMMY.get(), level);
            this.setSize(width, height);
            this.train = train;
            if (train != null) this.entityData.set(TRAIN_ID, train.getId());
        }

        public void setSize(float width, float height) {
            this.entityData.set(DUMMY_WIDTH, width);
            this.entityData.set(DUMMY_HEIGHT, height);
            this.refreshDimensions();
        }

        @Override
        public EntityDimensions getDimensions(Pose pose) {
            if (this.entityData == null) return super.getDimensions(pose);
            return EntityDimensions.scalable(this.entityData.get(DUMMY_WIDTH), this.entityData.get(DUMMY_HEIGHT));
        }

        @Override
        protected void defineSynchedData(SynchedEntityData.Builder builder) {
            builder.define(TRAIN_ID, 0);
            builder.define(DUMMY_WIDTH, 1F);
            builder.define(DUMMY_HEIGHT, 1F);
        }

        @Override protected void readAdditionalSaveData(CompoundTag tag) { this.discard(); }
        @Override protected void addAdditionalSaveData(CompoundTag tag) { }

        @Override public boolean isPickable() { return !this.isRemoved(); }

        @Override
        public boolean hurt(DamageSource source, float amount) {
            if (train != null) return train.hurt(source, amount);
            return super.hurt(source, amount);
        }

        @Override
        public InteractionResult interact(Player player, InteractionHand hand) {
            if (train != null) return train.interact(player, hand);
            return super.interact(player, hand);
        }

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
        @OnlyIn(Dist.CLIENT)
        public void printHook(RenderGuiEvent.Pre event, Level level, BlockPos pos) {
            Entity e = this.level().getEntity(this.entityData.get(TRAIN_ID));
            if (e instanceof EntityRailCarBase railCar) {
                railCar.printHook(event, level, pos);
            }
        }
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void printHook(RenderGuiEvent.Pre event, Level level, BlockPos pos) {
    }
}
