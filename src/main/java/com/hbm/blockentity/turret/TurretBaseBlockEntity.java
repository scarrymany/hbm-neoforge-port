package com.hbm.blockentity.turret;

import com.hbm.api.energymk2.IEnergyReceiverMK2;
import com.hbm.api.redstoneoverradio.IRORInteractive;
import com.hbm.api.redstoneoverradio.IRORValueProvider;
import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.MachineBaseBlockEntity;
import com.hbm.blocks.BlockDummyable;
import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.container.turret.TurretMenu;
import com.hbm.items.machine.ItemTurretBiometry;
import com.hbm.items.machine.ItemTurretChip;
import com.hbm.items.weapon.sedna.BulletConfig;
import com.hbm.lib.Library;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.neoforged.neoforge.entity.PartEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.npc.Npc;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.MenuProvider;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Abstract shared core for every turret, ported from CE's {@code TileEntityTurretBaseNT} (1,301
 * lines, read in full - see {@code docs/phase3/turret_system.md}). Targeting math
 * ({@link #entityInLOS}/{@link #entityAcceptableTarget}/{@link #seekNewTarget}/{@link #alignTurret}/
 * {@link #turnTowards}/{@link #turnTowardsAngle}), power (against the already-ported
 * {@link IEnergyReceiverMK2}), the 11-slot inventory (0 = biometry/chip, 1-9 = ammo, 10 = battery),
 * and targeting-filter state are all ported function-for-function. See the report's "Key
 * design/API decisions" for why this state is plain block-entity NBT rather than a data component,
 * and why the id scheme changed from CE's construction-order {@code int} to this port's
 * {@link BulletConfig} {@link ResourceLocation} registry.
 *
 * <h2>Deliberately out of scope for this pass (see the report's Deferred scope)</h2>
 * <ul>
 *   <li>{@code IControllable}/{@code IControlReceiver}'s remote-automation event names
 *   ("turret_set_target"/"turret_switch") - left unwired, per the report's explicit recommendation
 *   (the control-panel package is separate and much larger).</li>
 *   <li>OpenComputers {@code SimpleComponent} integration - dropped entirely, this port declares no
 *   dependency on {@code li.cil.oc} anywhere.</li>
 *   <li>{@code CompatExternal}'s reflection-based cross-mod target hooks
 *   ({@code turretTargetPlayer}/{@code Friendly}/{@code Hostile}/{@code Machine}/{@code Blacklist}/
 *   {@code Condition}) - dropped, this port declares no other-mod dependency anywhere.</li>
 *   <li>Casing-ejection/muzzle-flash VFX ({@code CasingEjector}, {@code SpentCasing},
 *   {@code AuxParticlePacketNT}) - shared gun-VFX substrate every hand-held gun also needs, not
 *   turret-specific; {@link #spawnCasing()} is a documented no-op until that package lands.</li>
 *   <li>OpenComputers dropped. ROR: CE {@code TileEntityTurretBaseNT.java:1248-1299}.</li>
 * </ul>
 */
public abstract class TurretBaseBlockEntity extends MachineBaseBlockEntity
        implements IEnergyReceiverMK2, ITickableBE, MenuProvider, IRORValueProvider, IRORInteractive {

    // this time we do all rotations in radians
    public double rotationYaw;
    public double rotationPitch;
    // only used by clients for interpolation
    public double lastRotationYaw;
    public double lastRotationPitch;
    // only used by client for approach
    public double syncRotationYaw;
    public double syncRotationPitch;
    protected int turnProgress;
    public boolean isOn = false;
    public boolean aligned = false;
    public int searchTimer;

    protected long power;

    public boolean targetPlayers = false;
    public boolean targetAnimals = false;
    public boolean targetMobs = true;
    public boolean targetMachines = true;

    public boolean isBlacklistMobFilter = true;

    public boolean manualOverride = false;

    @Nullable
    public Entity target;
    @Nullable
    public Vec3 tPos;

    public int stattrak;
    public int casingDelay;
    public List<String> mobFilter = new ArrayList<>();
    @Nullable
    protected List<String> cachedWhitelist = null;

    protected TurretBaseBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, 11, false, true);
    }

    @Override
    protected ItemStackHandler getNewInventory(int scount, int slotlimit) {
        return new ItemStackHandler(scount) {
            @Override
            protected void onContentsChanged(int slot) {
                super.onContentsChanged(slot);
                setChanged();
                if (slot == 0) cachedWhitelist = null;
            }
        };
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.power = tag.getLong("power");
        this.isOn = tag.getBoolean("isOn");
        this.isBlacklistMobFilter = tag.getBoolean("isBlacklistFilter");
        this.targetPlayers = tag.getBoolean("targetPlayers");
        this.targetAnimals = tag.getBoolean("targetAnimals");
        this.targetMobs = tag.getBoolean("targetMobs");
        this.targetMachines = tag.getBoolean("targetMachines");
        this.stattrak = tag.getInt("stattrak");

        mobFilter.clear();
        ListTag filter = tag.getList("mobFilter", Tag.TAG_STRING);
        for (int i = 0; i < filter.size(); i++) mobFilter.add(filter.getString(i));
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putLong("power", this.power);
        tag.putBoolean("isOn", this.isOn);
        tag.putBoolean("isBlacklistFilter", this.isBlacklistMobFilter);
        tag.putBoolean("targetPlayers", this.targetPlayers);
        tag.putBoolean("targetAnimals", this.targetAnimals);
        tag.putBoolean("targetMobs", this.targetMobs);
        tag.putBoolean("targetMachines", this.targetMachines);
        tag.putInt("stattrak", this.stattrak);

        ListTag filter = new ListTag();
        for (String id : mobFilter) filter.add(net.minecraft.nbt.StringTag.valueOf(id));
        tag.put("mobFilter", filter);
    }

    /** Overridden by {@code TurretChekhovBlockEntity} to keep the barrel spin-up active while a player has the GUI open. */
    public void manualSetup() {
    }

    @Override
    public void updateEntity() {
        if (level == null) return;

        if (level.isClientSide) {
            this.lastRotationPitch = this.rotationPitch;
            this.lastRotationYaw = this.rotationYaw;
            this.rotationPitch = this.syncRotationPitch;
            this.rotationYaw = this.syncRotationYaw;
        }

        this.aligned = false;

        if (!level.isClientSide) {
            updateConnections();
            if (this.target != null && !this.target.isAlive()) {
                this.target = null;
                this.stattrak++;
            }
        }

        if (target != null && !entityInLOS(target)) {
            this.target = null;
        }

        if (!level.isClientSide) {
            if (target != null) {
                this.tPos = getEntityPos(target);
            } else if (!manualOverride) {
                this.tPos = null;
            }
        }

        if (isOn() && hasPower()) {
            if (tPos != null) alignTurret();
        } else {
            this.target = null;
            this.tPos = null;
        }

        if (!level.isClientSide) {
            if (this.target != null && !this.target.isAlive() && !manualOverride) {
                this.target = null;
                this.tPos = null;
                this.stattrak++;
            }

            if (isOn() && hasPower()) {
                searchTimer--;
                setPower(getPower() - getConsumption());

                if (searchTimer <= 0) {
                    searchTimer = getDecetorInterval();
                    if (this.target == null && !manualOverride) seekNewTarget();
                }
            } else {
                searchTimer = 0;
            }

            if (this.aligned) updateFiringTick();

            setPower(Library.chargeTEFromItems(inventory, 10, getPower(), getMaxPower()));
            manualOverride = false;
            networkPackNT(250);

            if (usesCasings() && casingDelay() > 0) {
                if (casingDelay > 0) {
                    casingDelay--;
                } else {
                    spawnCasing();
                }
            }
        } else {
            // fixes the interpolation error when the turret crosses the 360deg point
            if (Math.abs(this.lastRotationYaw - this.rotationYaw) > Math.PI) {
                if (this.lastRotationYaw < this.rotationYaw) this.lastRotationYaw += Math.PI * 2;
                else this.lastRotationYaw -= Math.PI * 2;
            }
        }
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        buf.writeBoolean(this.tPos != null);
        if (this.tPos != null) {
            buf.writeDouble(this.tPos.x);
            buf.writeDouble(this.tPos.y);
            buf.writeDouble(this.tPos.z);
        }
        buf.writeDouble(this.rotationPitch);
        buf.writeDouble(this.rotationYaw);
        buf.writeLong(this.power);
        buf.writeBoolean(this.isOn);
        buf.writeBoolean(this.targetPlayers);
        buf.writeBoolean(this.targetAnimals);
        buf.writeBoolean(this.targetMobs);
        buf.writeBoolean(this.targetMachines);
        buf.writeVarInt(this.stattrak);
        buf.writeBoolean(this.isBlacklistMobFilter);
        buf.writeVarInt(mobFilter.size());
        for (String id : mobFilter) buf.writeUtf(id);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        this.turnProgress = 2;
        this.tPos = buf.readBoolean() ? new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble()) : null;
        this.syncRotationPitch = buf.readDouble();
        this.syncRotationYaw = buf.readDouble();
        this.power = buf.readLong();
        this.isOn = buf.readBoolean();
        this.targetPlayers = buf.readBoolean();
        this.targetAnimals = buf.readBoolean();
        this.targetMobs = buf.readBoolean();
        this.targetMachines = buf.readBoolean();
        this.stattrak = buf.readVarInt();
        this.isBlacklistMobFilter = buf.readBoolean();

        int size = buf.readVarInt();
        List<String> synced = new ArrayList<>(size);
        for (int i = 0; i < size; i++) synced.add(buf.readUtf());
        mobFilter.clear();
        mobFilter.addAll(synced);
    }

    /**
     * Server-side dispatch for {@link com.hbm.packet.toserver.TurretControlPacket}, matching CE's
     * own {@code receiveControl(NBTTagCompound)} (formerly the {@code IControlReceiver} contract) -
     * this is the whitelist/mob-filter mutation path the mob-filter screen (no backing
     * {@code AbstractContainerMenu}, see that screen's own javadoc) uses instead of a Menu button.
     */
    public void receiveControl(CompoundTag data) {
        if (data.contains("del")) {
            removeName(data.getInt("del"));
        } else if (data.contains("name")) {
            addName(data.getString("name"));
        } else if (data.contains("addMobFilter")) {
            String id = data.getString("addMobFilter");
            if (!mobFilter.contains(id)) mobFilter.add(id);
            setChanged();
        } else if (data.contains("removeMobFilter")) {
            mobFilter.remove(data.getString("removeMobFilter"));
            setChanged();
        } else if (data.contains("toggleBlacklist")) {
            // The mob-filter screen has no backing Menu (see TurretMobFilterScreen's own javadoc),
            // so it cannot route through TurretMenu#clickMenuButton like the main turret Screen's
            // blacklist/whitelist button does - this NBT key is that screen's own equivalent.
            this.isBlacklistMobFilter = data.getBoolean("toggleBlacklist");
            setChanged();
        }
    }

    /** Dispatch target for {@link AbstractContainerMenu#clickMenuButton} - see {@code TurretMenu}. */
    public void handleButtonPacket(int value, int meta) {
        switch (meta) {
            case 0 -> this.isOn = !this.isOn;
            case 1 -> this.targetPlayers = !this.targetPlayers;
            case 2 -> this.targetAnimals = !this.targetAnimals;
            case 3 -> this.targetMobs = !this.targetMobs;
            case 4 -> this.targetMachines = !this.targetMachines;
            case 6 -> this.isBlacklistMobFilter = !this.isBlacklistMobFilter;
            default -> {
            }
        }
        setChanged();
    }

    /**
     * HE cable auto-subscribe for the standard 2x2 {@link BlockDummyable} footprint, ported from
     * CE's own eight-neighbor + four-down subscribe fan-out. Reads the core's own rotation meta
     * directly (matching the confirmed {@code coreDirection()} pattern already used by this port's
     * other multiblocks, e.g. {@code MachineSteamEngineBlockEntity}) rather than
     * {@code getRotationFromState}'s extra clockwise step (that one is for shape/render offset, a
     * different concern). Overridden to a trivial down-only subscribe by the single-block Sentry
     * turrets, which are not {@link BlockDummyable} at all.
     */
    protected void updateConnections() {
        if (level == null) return;
        BlockState state = getBlockState();
        if (!(state.getBlock() instanceof BlockDummyable) || !state.hasProperty(BlockDummyable.META)) return;

        int metaValue = state.getValue(BlockDummyable.META) - BlockDummyable.offset;
        if (metaValue < 0 || metaValue > 5) return;

        Direction dir = Direction.from3DDataValue(metaValue).getOpposite();
        Direction rot = dir.getClockWise(Direction.Axis.Y);
        int x = worldPosition.getX();
        int y = worldPosition.getY();
        int z = worldPosition.getZ();

        trySubscribe(level, x + dir.getStepX() * -1, y, z + dir.getStepZ() * -1, dir.getOpposite());
        trySubscribe(level, x + dir.getStepX() * -1 + rot.getStepX() * -1, y, z + dir.getStepZ() * -1 + rot.getStepZ() * -1, dir.getOpposite());
        trySubscribe(level, x + rot.getStepX() * -2, y, z + rot.getStepZ() * -2, rot.getOpposite());
        trySubscribe(level, x + dir.getStepX() + rot.getStepX() * -2, y, z + dir.getStepZ() + rot.getStepZ() * -2, rot.getOpposite());
        trySubscribe(level, x + rot.getStepX(), y, z + rot.getStepZ(), rot);
        trySubscribe(level, x + dir.getStepX() + rot.getStepX(), y, z + dir.getStepZ() + rot.getStepZ(), rot);
        trySubscribe(level, x + dir.getStepX() * 2, y, z + dir.getStepZ() * 2, dir);
        trySubscribe(level, x + dir.getStepX() * 2 + rot.getStepX() * -1, y, z + dir.getStepZ() * 2 + rot.getStepZ() * -1, dir);

        trySubscribe(level, x, y - 1, z, Direction.DOWN);
        trySubscribe(level, x, y - 1, z + dir.getStepZ() - rot.getStepZ(), Direction.DOWN);
        trySubscribe(level, x + dir.getStepX() - rot.getStepX(), y - 1, z, Direction.DOWN);
        trySubscribe(level, x + dir.getStepX() - rot.getStepX(), y - 1, z + dir.getStepZ() - rot.getStepZ(), Direction.DOWN);
    }

    public abstract void updateFiringTick();

    /**
     * Yes, new turrets fire {@code EntityBulletBaseMK4}s. Every concrete turret returns the same
     * fixed list of {@link BulletConfig}s CE's own {@code getAmmoList()} returned (as ids into a
     * static registry there); this port returns the resolved configs directly.
     * <p>
     * <b>Every concrete override below returns an empty list with a {@code TODO(phase3-gun-content)}
     * comment</b> naming the exact CE {@code XFactory*} constants it needs - the gun/ammo content
     * package ({@code com.hbm.items.weapon.sedna.factory}, ~23 files) is a separate, much larger
     * dependency this task's own report explicitly scopes out (Package D). {@link #getFirstConfigLoaded()}
     * already treats an empty list as "no ammo loaded" (returns {@code null}), so every firing method
     * below is a correct no-op until that package lands - filling in the list is then the only change
     * needed.
     */
    protected abstract List<BulletConfig> getAmmoList();

    @Nullable
    public BulletConfig getFirstConfigLoaded() {
        List<BulletConfig> list = getAmmoList();
        if (list == null || list.isEmpty()) return null;

        // fires slots in inventory order, not config order - matches CE's own comment on this exact loop
        for (int i = 1; i < 10; i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (stack.isEmpty()) continue;

            for (BulletConfig conf : list) {
                ComparableStack ammo = conf.getAmmo();
                if (ammo != null && ammo.matchesRecipe(stack, true)) return conf;
            }
        }

        return null;
    }

    public void spawnBullet(BulletConfig bullet, float baseDamage) {
        if (level == null) return;

        Vec3 pos = getTurretPos();
        Vec3 vec = new Vec3(getBarrelLength(), 0, 0);
        vec = com.hbm.util.Vec3dUtil.rotateRoll(vec, (float) -this.rotationPitch);
        vec = vec.yRot((float) -(this.rotationYaw + Math.PI * 0.5));

        com.hbm.entity.projectile.EntityBulletBaseMK4 proj =
                new com.hbm.entity.projectile.EntityBulletBaseMK4(level, bullet, baseDamage, bullet.spread, (float) rotationYaw, (float) rotationPitch);
        proj.moveTo(pos.x + vec.x, pos.y + vec.y, pos.z + vec.z, proj.getYRot(), proj.getXRot());
        level.addFreshEntity(proj);

        if (usesCasings()) {
            if (casingDelay() == 0) {
                spawnCasing();
            } else {
                casingDelay = casingDelay();
            }
        }
    }

    public void consumeAmmo(@Nullable ComparableStack ammo) {
        if (ammo == null) return;

        for (int i = 1; i < 10; i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (!stack.isEmpty() && ammo.matchesRecipe(stack, true)) {
                stack.shrink(1);
                return;
            }
        }

        setChanged();
    }

    /**
     * Reads the name list from the biometry chip in slot 0.
     *
     * @return null if there is no chip present or the name list is empty
     */
    @Nullable
    public List<String> getWhitelist() {
        if (cachedWhitelist != null) return cachedWhitelist;

        ItemStack chip = inventory.getStackInSlot(0);
        if (chip.getItem() instanceof ItemTurretChip) {
            String[] array = ItemTurretBiometry.getNames(chip);
            if (array == null) return null;

            List<String> list = new ArrayList<>();
            java.util.Collections.addAll(list, array);
            return cachedWhitelist = list;
        }

        return null;
    }

    public void addName(String name) {
        ItemStack chip = inventory.getStackInSlot(0);
        if (chip.getItem() instanceof ItemTurretChip) {
            ItemTurretBiometry.addName(chip, name);
            if (cachedWhitelist != null) cachedWhitelist.add(name);
        }
    }

    public void removeName(int index) {
        ItemStack chip = inventory.getStackInSlot(0);
        if (!(chip.getItem() instanceof ItemTurretChip)) return;

        String[] array = ItemTurretBiometry.getNames(chip);
        if (array == null) return;

        List<String> names = new ArrayList<>();
        java.util.Collections.addAll(names, array);
        ItemTurretBiometry.clearNames(chip);

        if (index >= 0 && index < names.size()) names.remove(index);
        for (String name : names) ItemTurretBiometry.addName(chip, name);

        if (cachedWhitelist != null && index >= 0 && index < cachedWhitelist.size()) cachedWhitelist.remove(index);
    }

    /** Finds the nearest acceptable target within range and in line of sight. */
    protected void seekNewTarget() {
        if (level == null) return;

        Vec3 pos = getTurretPos();
        double range = getDecetorRange();
        AABB box = new AABB(pos.x, pos.y, pos.z, pos.x, pos.y, pos.z).inflate(range);
        List<Entity> entities = level.getEntitiesOfClass(Entity.class, box);

        Entity best = null;
        double closest = range;

        for (Entity entity : entities) {
            Vec3 ent = getEntityPos(entity);
            double dist = ent.subtract(pos).length();

            if (dist > range) continue;
            if (!entityAcceptableTarget(entity)) continue;
            if (!entityInLOS(entity)) continue;

            if (dist < closest) {
                closest = dist;
                best = entity;
            }
        }

        this.target = best;
        if (best != null) this.tPos = getEntityPos(best);
    }

    /** Turns the turret towards the current target position. Assumes {@link #tPos} is not null. */
    protected void alignTurret() {
        turnTowards(tPos);
    }

    public void turnTowards(Vec3 ent) {
        Vec3 pos = getTurretPos();
        Vec3 delta = ent.subtract(pos);

        double targetPitch = Math.asin(delta.y / delta.length());
        double targetYaw = -Math.atan2(delta.x, delta.z);

        turnTowardsAngle(targetPitch, targetYaw);
    }

    public void turnTowardsAngle(double targetPitch, double targetYaw) {
        double turnYaw = Math.toRadians(getTurretYawSpeed());
        double turnPitch = Math.toRadians(getTurretPitchSpeed());
        double pi2 = Math.PI * 2;

        if (Math.abs(this.rotationPitch - targetPitch) < turnPitch || Math.abs(this.rotationPitch - targetPitch) > pi2 - turnPitch) {
            this.rotationPitch = targetPitch;
        } else {
            if (targetPitch > this.rotationPitch) this.rotationPitch += turnPitch;
            else this.rotationPitch -= turnPitch;
        }

        double deltaYaw = (targetYaw - this.rotationYaw) % pi2;

        int dir = 0;
        if (deltaYaw < -Math.PI) dir = 1;
        else if (deltaYaw < 0) dir = -1;
        else if (deltaYaw > Math.PI) dir = -1;
        else if (deltaYaw > 0) dir = 1;

        if (Math.abs(this.rotationYaw - targetYaw) < turnYaw || Math.abs(this.rotationYaw - targetYaw) > pi2 - turnYaw) {
            this.rotationYaw = targetYaw;
        } else {
            this.rotationYaw += turnYaw * dir;
        }

        double deltaPitch = targetPitch - this.rotationPitch;
        deltaYaw = targetYaw - this.rotationYaw;

        double deltaAngle = Math.sqrt(deltaYaw * deltaYaw + deltaPitch * deltaPitch);

        this.rotationYaw = this.rotationYaw % pi2;
        this.rotationPitch = this.rotationPitch % pi2;

        if (deltaAngle <= Math.toRadians(getAcceptableInaccuracy())) {
            this.aligned = true;
        }
    }

    /**
     * Checks line of sight to the passed entity along with whether the angle falls within swivel
     * range. The block-obstruction check uses {@link Level#clip(ClipContext)} with
     * {@code ClipContext.Block.COLLIDER} - the confirmed real 1.21.1 replacement for CE's
     * {@code Library.isObstructedOpaque} raytrace (same pattern already used by this port's
     * {@code ItemLaserDetonator}/{@code ItemDiscord}/{@code IToolAreaAbility}).
     */
    public boolean entityInLOS(Entity e) {
        if (level == null || e.isRemoved() || !e.isAlive()) return false;

        if (e instanceof Player player && player.isCreative()) return false;

        if (!hasThermalVision() && e instanceof LivingEntity living && living.hasEffect(MobEffects.INVISIBILITY)) return false;

        Vec3 pos = getTurretPos();
        Vec3 ent = getEntityPos(e);
        Vec3 delta = ent.subtract(pos);
        double length = delta.length();

        if (length < getDecetorGrace() || length > getDecetorRange() * 1.1) return false;

        Vec3 norm = delta.normalize();
        double pitchDeg = Math.toDegrees(Math.asin(norm.y));

        if (pitchDeg < -getTurretDepression() || pitchDeg > getTurretElevation()) return false;

        // null entity: unlike every other ClipContext call site in this port (all owned by a
        // specific held item or projectile), a turret's LOS check has no "requester" entity of its
        // own - vanilla's ClipContext accepts a @Nullable entity (falls back to
        // CollisionContext.empty() internally), matching CE's own entity-less
        // Library.isObstructedOpaque raytrace exactly.
        ClipContext ctx = new ClipContext(pos, ent, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, CollisionContext.empty());
        return level.clip(ctx).getType() == HitResult.Type.MISS;
    }

    /**
     * Returns true if the entity is considered for targeting. {@code CompatExternal}'s reflection
     * hooks (cross-mod target extension points) are dropped - see this class's own javadoc.
     * {@code IAnimals}/{@code INpc}/{@code IMob} (1.12 marker interfaces with no 1:1 modern
     * equivalent) are approximated with {@link Animal}/{@link Npc}/{@link
     * net.minecraft.world.entity.monster.Enemy} respectively - the closest real vanilla marker
     * types for each concept.
     */
    public boolean entityAcceptableTarget(Entity e) {
        if (e.isRemoved() || !e.isAlive()) return false;

        List<String> wl = getWhitelist();
        if (wl != null) {
            if (e instanceof Player) {
                if (wl.contains(e.getName().getString())) return false;
            } else if (e instanceof Mob mob) {
                if (mob.hasCustomName() && mob.getCustomName() != null && wl.contains(mob.getCustomName().getString())) return false;
            }
        }

        if (!mobFilter.isEmpty() && e instanceof Mob) {
            ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(e.getType());
            if (id != null) {
                boolean listed = mobFilter.contains(id.toString());
                if (isBlacklistMobFilter ? listed : !listed) return false;
            }
        }

        if (targetAnimals) {
            if (e instanceof Animal) return true;
            if (e instanceof Npc) return true;
        }

        if (targetMobs) {
            if (e instanceof EnderDragon) return false; // never target the ender dragon directly
            if (e instanceof PartEntity<?>) return true;
            if (e instanceof net.minecraft.world.entity.monster.Enemy) return true;
        }

        if (targetMachines) {
            // Phase 4 (entities_vehicles_aircraft/entities_bosses): closes this method's own
            // long-standing TODO now that EntityMissileBaseNT/EntityMissileCustom and EntityBomber are
            // real. Also fixes a real dangling-conditional bug found while closing it: the old
            // `instanceof IRadarDetectableNT detectable && !detectable.canBeSeenBy(this)) return false`
            // guard only ever short-circuited the "not visible" case and had no matching `return true`
            // for the "visible" case, so a visible IRadarDetectableNT entity (e.g. any missile, whose
            // own canBeSeenBy is an unconditional `true`) fell through every remaining branch and was
            // never actually targetable - this now completes that check.
            if (e instanceof AbstractMinecart) return true;
            // CE: missiles are only turret-targetable while descending (a turret-side carve-out,
            // checked before the generic IRadarDetectableNT branch below since EntityMissileBaseNT's
            // own canBeSeenBy is an unconditional `true` and would otherwise shadow this gate).
            if (e instanceof com.hbm.entity.missile.EntityMissileBaseNT missile) return missile.getDeltaMovement().y < 0;
            if (e instanceof com.hbm.entity.logic.EntityBomber) return true;
            if (e instanceof com.hbm.api.entity.IRadarDetectableNT detectable) return detectable.canBeSeenBy(this);
        }

        if (targetPlayers) {
            if (e instanceof FakePlayer) return false;
            if (e instanceof Player) return true;
        }

        return false;
    }

    public double getAcceptableInaccuracy() {
        return 5;
    }

    /** Degrees the turret can rotate per tick (4.5deg/t = 90deg/s, a half turn in two seconds). */
    public double getTurretYawSpeed() {
        return 4.5D;
    }

    public double getTurretPitchSpeed() {
        return 3D;
    }

    public double getTurretDepression() {
        return 30D;
    }

    public double getTurretElevation() {
        return 30D;
    }

    public int getDecetorInterval() {
        return 10;
    }

    public double getDecetorRange() {
        return 32D;
    }

    public double getDecetorGrace() {
        return 3D;
    }

    /** The pivot point of the turret, larger models have a default of 1.5. */
    public double getHeightOffset() {
        return 1.5D;
    }

    public double getBarrelLength() {
        return 1.0D;
    }

    public boolean hasThermalVision() {
        return true;
    }

    /** The pivot point of the turret, used for LOS calculation and more. */
    public Vec3 getTurretPos() {
        Vec3 offset = byHorizontalIndexOffset();
        return new Vec3(worldPosition.getX() + offset.x, worldPosition.getY() + getHeightOffset(), worldPosition.getZ() + offset.z);
    }

    /**
     * The XZ offset for a standard 2x2 turret base, read directly off the core's own rotation meta
     * (matching CE's {@code getBlockMetadata() - BlockDummyable.offset} exactly). Single-block
     * turrets (Sentry/SentryDamaged, not a {@link BlockDummyable} at all) fall through to the zero
     * offset, matching CE's own behavior there (their meta never falls in the dummy-rotation range).
     */
    public Vec3 byHorizontalIndexOffset() {
        BlockState state = getBlockState();
        if (!(state.getBlock() instanceof BlockDummyable) || !state.hasProperty(BlockDummyable.META)) return Vec3.ZERO;

        int meta = state.getValue(BlockDummyable.META) - BlockDummyable.offset;
        if (meta == 2) return new Vec3(1, 0, 1);
        if (meta == 4) return new Vec3(1, 0, 0);
        if (meta == 5) return new Vec3(0, 0, 1);
        return Vec3.ZERO;
    }

    /**
     * The pivot point of a target entity used for LOS/aiming. CE's minor {@code getYOffset()}
     * riding-vehicle offset term is dropped (near-zero for the vast majority of entities, and this
     * port has no direct equivalent) - a documented simplification, not silently different behavior
     * for anything but a rider being targeted mid-ride.
     */
    public Vec3 getEntityPos(Entity e) {
        return new Vec3(e.getX(), e.getY() + e.getBbHeight() * 0.5, e.getZ());
    }

    @Override
    public int[] getAccessibleSlotsFromSide(Direction side) {
        return new int[]{1, 2, 3, 4, 5, 6, 7, 8, 9};
    }

    @Override
    public boolean isItemValidForSlot(int i, ItemStack stack) {
        return true;
    }

    public boolean hasPower() {
        return getPower() >= getConsumption();
    }

    public boolean isOn() {
        return this.isOn;
    }

    @Override
    public void setPower(long newPower) {
        this.power = Math.max(0, Math.min(newPower, getMaxPower()));
    }

    @Override
    public long getPower() {
        return this.power;
    }

    public int getPowerScaled(int scale) {
        return (int) (power * scale / getMaxPower());
    }

    public long getConsumption() {
        return 100;
    }

    public boolean usesCasings() {
        return false;
    }

    public int casingDelay() {
        return 0;
    }

    protected Vec3 getCasingSpawnPos() {
        return getTurretPos();
    }

    /**
     * No-op forward reference: CE's {@code CasingEjector}/{@code SpentCasing}/
     * {@code AuxParticlePacketNT} muzzle-flash/casing-eject VFX network is shared gun-VFX substrate
     * every hand-held gun also needs, not turret-specific - see this class's own javadoc. Every
     * turret's {@code usesCasings()}/{@code casingDelay()}/{@link #getCasingSpawnPos()} stat methods
     * are kept so a future VFX pass only needs to fill in this one method's body.
     */
    protected void spawnCasing() {
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new TurretMenu(containerId, playerInventory, this);
    }

    @Override
    public String[] getFunctionInfo() {
        // CE TileEntityTurretBaseNT.java:1248
        return new String[]{
                PREFIX_FUNCTION + "setActive" + NAME_SEPARATOR + "active (0 or 1)",
                PREFIX_FUNCTION + "targetPlayers" + NAME_SEPARATOR + "enabled (0 or 1)",
                PREFIX_FUNCTION + "targetAnimals" + NAME_SEPARATOR + "enabled (0 or 1)",
                PREFIX_FUNCTION + "targetMobs" + NAME_SEPARATOR + "enabled (0 or 1)",
                PREFIX_FUNCTION + "targetMachines" + NAME_SEPARATOR + "enabled (0 or 1)",
                PREFIX_FUNCTION + "addWhitelist" + NAME_SEPARATOR + "name",
                PREFIX_FUNCTION + "removeWhitelist" + NAME_SEPARATOR + "name",
                PREFIX_FUNCTION + "addMobFilter" + NAME_SEPARATOR + "name",
                PREFIX_FUNCTION + "removeMobFilter" + NAME_SEPARATOR + "name",
                PREFIX_FUNCTION + "toggleBlacklistMobFilter" + NAME_SEPARATOR + "enabled (0 or 1)"
        };
    }

    @Override
    public String provideRORValue(String name) {
        return null;
    }

    @Override
    public String runRORFunction(String name, String[] params) {
        // CE :1253-1299
        if ((PREFIX_FUNCTION + "setActive").equals(name) && params.length > 0) {
            this.isOn = IRORInteractive.parseInt(params[0], 0, 1) == 1;
            setChanged();
        }
        if ((PREFIX_FUNCTION + "targetPlayers").equals(name) && params.length > 0) {
            this.targetPlayers = IRORInteractive.parseInt(params[0], 0, 1) == 1;
            setChanged();
        }
        if ((PREFIX_FUNCTION + "targetAnimals").equals(name) && params.length > 0) {
            this.targetAnimals = IRORInteractive.parseInt(params[0], 0, 1) == 1;
            setChanged();
        }
        if ((PREFIX_FUNCTION + "targetMobs").equals(name) && params.length > 0) {
            this.targetMobs = IRORInteractive.parseInt(params[0], 0, 1) == 1;
            setChanged();
        }
        if ((PREFIX_FUNCTION + "targetMachines").equals(name) && params.length > 0) {
            this.targetMachines = IRORInteractive.parseInt(params[0], 0, 1) == 1;
            setChanged();
        }
        if ((PREFIX_FUNCTION + "addWhitelist").equals(name) && params.length > 0) {
            String playerName = params[0];
            List<String> whitelist = this.getWhitelist();
            if (whitelist == null || !whitelist.contains(playerName)) this.addName(playerName);
            setChanged();
        }
        if ((PREFIX_FUNCTION + "removeWhitelist").equals(name) && params.length > 0) {
            String playerName = params[0];
            List<String> whitelist = this.getWhitelist();
            if (whitelist != null && whitelist.contains(playerName)) this.removeName(whitelist.indexOf(playerName));
            setChanged();
        }
        if ((PREFIX_FUNCTION + "toggleBlacklistMobFilter").equals(name) && params.length > 0) {
            this.isBlacklistMobFilter = IRORInteractive.parseInt(params[0], 0, 1) == 1;
            setChanged();
        }
        if ((PREFIX_FUNCTION + "addMobFilter").equals(name) && params.length > 0) {
            String mobName = params[0];
            if (!mobFilter.contains(mobName)) mobFilter.add(mobName);
            setChanged();
        }
        if ((PREFIX_FUNCTION + "removeMobFilter").equals(name) && params.length > 0) {
            mobFilter.remove(params[0]);
            setChanged();
        }
        return null;
    }
}
