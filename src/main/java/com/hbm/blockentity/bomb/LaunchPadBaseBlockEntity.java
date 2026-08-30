package com.hbm.blockentity.bomb;

import com.hbm.api.energymk2.IEnergyReceiverMK2;
import com.hbm.api.entity.IThrowable;
import com.hbm.api.fluidmk2.IFluidStandardReceiverMK2;
import com.hbm.api.item.IDesignatorItem;
import com.hbm.blockentity.IRadarCommandReceiver;
import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.MachineBaseBlockEntity;
import com.hbm.blockentity.network.IFluidCopiable;
import com.hbm.entity.missile.EntityMissileAntiBallistic;
import com.hbm.entity.missile.EntityMissileBaseNT;
import com.hbm.entity.missile.EntityMissileShuttle;
import com.hbm.entity.missile.EntityMissileStealth;
import com.hbm.entity.missile.EntityMissileTier0;
import com.hbm.entity.missile.EntityMissileTier1;
import com.hbm.entity.missile.EntityMissileTier2;
import com.hbm.entity.missile.EntityMissileTier3;
import com.hbm.entity.missile.EntityMissileTier4;
import com.hbm.entity.missile.MissileEntityTypes;
import com.hbm.interfaces.IBomb.BombReturnCode;
import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.container.LaunchPadMenu;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.items.weapon.ItemMissileStandard;
import com.hbm.items.weapon.ItemMissileStandard.MissileFuel;
import com.hbm.items.weapon.MissileItems;
import com.hbm.lib.DirPos;
import com.hbm.lib.HBMSoundHandler;
import com.hbm.lib.Library;
import com.hbm.main.ModContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Ported from CE's {@code com.hbm.tileentity.bomb.TileEntityLaunchPadBase} (568 lines, read in
 * full) - the shared centerpiece of every {@code BlockDummyable} launch pad multiblock (small
 * {@link LaunchPadBlockEntity}, {@link LaunchPadLargeBlockEntity}). Owns the {@link #MISSILES}
 * static factory map, the two {@link FluidTankNTM} fuel tanks + {@link IEnergyReceiverMK2} power
 * buffer, redstone-edge-triggered auto-launch, and the three launch entry points
 * ({@link #launchFromDesignator}/{@link #launchToCoordinate}/{@link #launchToEntity}).
 * <p>
 * <b>Not ported, documented</b>:
 * <ul>
 *   <li>OpenComputers {@code SimpleComponent}/{@code @Callback} integration (5 methods) - a
 *   CE-1.12-only third-party mod bridge with no mod-wide OpenComputers-parity decision anywhere in
 *   this port (same open policy question {@code docs/phase3/missile_launch_infra.md} flags);
 *   dropped entirely rather than guessed at here.</li>
 *   <li>{@code TrackerUtil.setTrackingRange(world, missile, 500)} - subsumed by
 *   {@link MissileEntityTypes}'s own declarative {@code .setTrackingRange(1000)} on every missile
 *   {@code EntityType.Builder} (already shipped), matching the research report's own predicted
 *   simplification.</li>
 *   <li>{@code toRender}/canister-slot fuel loading ({@code tanks[].loadTank(slot, slot, inventory)})
 *   - CE's item-canister tank-fill mechanic has no port equivalent yet ({@link FluidTankNTM}'s own
 *   javadoc documents this exact gap: "The item-canister loading subsystem ... is left out - neither
 *   exists in this port yet"). Tanks still fill correctly through the fluid pipe network
 *   ({@link IFluidStandardReceiverMK2}); only the "insert a canister directly into the GUI slot"
 *   convenience is missing, a real but narrow gameplay gap, not a compile blocker.</li>
 * </ul>
 */
public abstract class LaunchPadBaseBlockEntity extends MachineBaseBlockEntity
        implements ITickableBE, IEnergyReceiverMK2, IFluidStandardReceiverMK2, IFluidCopiable, IRadarCommandReceiver, MenuProvider {

    /** Automatic instantiation of generic missiles, i.e. everything that both extends {@link EntityMissileBaseNT} and needs a designator. */
    public static final Map<ComparableStack, MissileFactory> MISSILES = new HashMap<>(28);

    @FunctionalInterface
    public interface MissileFactory {
        EntityMissileBaseNT create(Level level);
    }

    public static void registerLaunchables() {
        // Tier 0
        MISSILES.put(new ComparableStack(MissileItems.MISSILE_MICRO.get()),
                level -> new EntityMissileTier0.EntityMissileMicro(MissileEntityTypes.MICRO.get(), level));
        MISSILES.put(new ComparableStack(MissileItems.MISSILE_SCHRABIDIUM.get()),
                level -> new EntityMissileTier0.EntityMissileSchrabidium(MissileEntityTypes.SCHRABIDIUM.get(), level));
        MISSILES.put(new ComparableStack(MissileItems.MISSILE_BHOLE.get()),
                level -> new EntityMissileTier0.EntityMissileBHole(MissileEntityTypes.BHOLE.get(), level));
        MISSILES.put(new ComparableStack(MissileItems.MISSILE_TAINT.get()),
                level -> new EntityMissileTier0.EntityMissileTaint(MissileEntityTypes.TAINT.get(), level));
        MISSILES.put(new ComparableStack(MissileItems.MISSILE_EMP.get()),
                level -> new EntityMissileTier0.EntityMissileEMP(MissileEntityTypes.EMP.get(), level));
        // Tier 1
        MISSILES.put(new ComparableStack(MissileItems.MISSILE_GENERIC.get()),
                level -> new EntityMissileTier1.EntityMissileGeneric(MissileEntityTypes.GENERIC.get(), level));
        MISSILES.put(new ComparableStack(MissileItems.MISSILE_DECOY.get()),
                level -> new EntityMissileTier1.EntityMissileDecoy(MissileEntityTypes.DECOY.get(), level));
        MISSILES.put(new ComparableStack(MissileItems.MISSILE_INCENDIARY.get()),
                level -> new EntityMissileTier1.EntityMissileIncendiary(MissileEntityTypes.INCENDIARY.get(), level));
        MISSILES.put(new ComparableStack(MissileItems.MISSILE_CLUSTER.get()),
                level -> new EntityMissileTier1.EntityMissileCluster(MissileEntityTypes.CLUSTER.get(), level));
        MISSILES.put(new ComparableStack(MissileItems.MISSILE_BUSTER.get()),
                level -> new EntityMissileTier1.EntityMissileBunkerBuster(MissileEntityTypes.BUNKER_BUSTER.get(), level));
        // Tier 2
        MISSILES.put(new ComparableStack(MissileItems.MISSILE_STRONG.get()),
                level -> new EntityMissileTier2.EntityMissileStrong(MissileEntityTypes.STRONG.get(), level));
        MISSILES.put(new ComparableStack(MissileItems.MISSILE_INCENDIARY_STRONG.get()),
                level -> new EntityMissileTier2.EntityMissileIncendiaryStrong(MissileEntityTypes.INCENDIARY_STRONG.get(), level));
        MISSILES.put(new ComparableStack(MissileItems.MISSILE_CLUSTER_STRONG.get()),
                level -> new EntityMissileTier2.EntityMissileClusterStrong(MissileEntityTypes.CLUSTER_STRONG.get(), level));
        MISSILES.put(new ComparableStack(MissileItems.MISSILE_BUSTER_STRONG.get()),
                level -> new EntityMissileTier2.EntityMissileBusterStrong(MissileEntityTypes.BUSTER_STRONG.get(), level));
        MISSILES.put(new ComparableStack(MissileItems.MISSILE_EMP_STRONG.get()),
                level -> new EntityMissileTier2.EntityMissileEMPStrong(MissileEntityTypes.EMP_STRONG.get(), level));
        // Tier 3
        MISSILES.put(new ComparableStack(MissileItems.MISSILE_BURST.get()),
                level -> new EntityMissileTier3.EntityMissileBurst(MissileEntityTypes.BURST.get(), level));
        MISSILES.put(new ComparableStack(MissileItems.MISSILE_INFERNO.get()),
                level -> new EntityMissileTier3.EntityMissileInferno(MissileEntityTypes.INFERNO.get(), level));
        MISSILES.put(new ComparableStack(MissileItems.MISSILE_RAIN.get()),
                level -> new EntityMissileTier3.EntityMissileRain(MissileEntityTypes.RAIN.get(), level));
        MISSILES.put(new ComparableStack(MissileItems.MISSILE_DRILL.get()),
                level -> new EntityMissileTier3.EntityMissileDrill(MissileEntityTypes.DRILL.get(), level));
        MISSILES.put(new ComparableStack(MissileItems.MISSILE_SHUTTLE.get()),
                level -> new EntityMissileShuttle(MissileEntityTypes.SHUTTLE.get(), level));
        // Tier 4
        MISSILES.put(new ComparableStack(MissileItems.MISSILE_NUCLEAR.get()),
                level -> new EntityMissileTier4.EntityMissileNuclear(MissileEntityTypes.NUCLEAR.get(), level));
        MISSILES.put(new ComparableStack(MissileItems.MISSILE_NUCLEAR_CLUSTER.get()),
                level -> new EntityMissileTier4.EntityMissileMirv(MissileEntityTypes.NUCLEAR_MIRV.get(), level));
        MISSILES.put(new ComparableStack(MissileItems.MISSILE_VOLCANO.get()),
                level -> new EntityMissileTier4.EntityMissileVolcano(MissileEntityTypes.VOLCANO.get(), level));
        MISSILES.put(new ComparableStack(MissileItems.MISSILE_DOOMSDAY.get()),
                level -> new EntityMissileTier4.EntityMissileDoomsday(MissileEntityTypes.DOOMSDAY.get(), level));
        MISSILES.put(new ComparableStack(MissileItems.MISSILE_N2.get()),
                level -> new EntityMissileTier4.EntityMissileN2(MissileEntityTypes.N2.get(), level));

        MISSILES.put(new ComparableStack(MissileItems.MISSILE_STEALTH.get()),
                level -> new EntityMissileStealth(MissileEntityTypes.STEALTH.get(), level));
    }

    public long power;
    public static final long MAX_POWER = 100_000;

    public int prevRedstonePower;
    public int redstonePower;
    public final Set<BlockPos> activatedBlocks = new HashSet<>(4);

    public int state = STATE_MISSING;
    public static final int STATE_MISSING = 0;
    public static final int STATE_LOADING = 1;
    public static final int STATE_READY = 2;

    public final FluidTankNTM[] tanks;

    protected LaunchPadBaseBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, int scount) {
        super(type, pos, state, scount, true, true);
        this.tanks = new FluidTankNTM[]{
                new FluidTankNTM(Fluids.NONE, 24_000).withOwner(this),
                new FluidTankNTM(Fluids.NONE, 24_000).withOwner(this)
        };
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.launchPad");
    }

    @Override
    public boolean canExtractItem(int slot, ItemStack stack, int amount) {
        return false;
    }

    @Override
    public int[] getAccessibleSlotsFromSide(Direction side) {
        return new int[]{0};
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        return slot == 0 && this.isMissileValid(stack);
    }

    public abstract DirPos[] getConPos();

    @Override
    public void updateEntity() {
        if (level == null || level.isClientSide) return;

        if (level.getGameTime() % 20 == 0) {
            for (DirPos pos : getConPos()) {
                this.trySubscribe(level, pos);
                if (tanks[0].getTankType() != Fluids.NONE) this.trySubscribe(tanks[0].getTankType(), level, pos);
                if (tanks[1].getTankType() != Fluids.NONE) this.trySubscribe(tanks[1].getTankType(), level, pos);
            }
        }

        if (this.redstonePower > 0 && this.prevRedstonePower <= 0) {
            this.launchFromDesignator();
        }

        this.prevRedstonePower = this.redstonePower;

        this.power = Library.chargeTEFromItems(inventory, 2, power, MAX_POWER);

        if (this.isMissileValid() && inventory.getStackInSlot(0).getItem() instanceof ItemMissileStandard missile) {
            setFuel(missile);
        }

        networkPackNT(250);
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeLong(this.power);
        buf.writeInt(this.state);
        tanks[0].serialize(buf);
        tanks[1].serialize(buf);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        this.power = buf.readLong();
        this.state = buf.readInt();
        tanks[0].deserialize(buf);
        tanks[1].deserialize(buf);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putLong("power", power);
        tanks[0].writeToNBT(tag, "t0");
        tanks[1].writeToNBT(tag, "t1");

        tag.putInt("redstonePower", redstonePower);
        tag.putInt("prevRedstonePower", prevRedstonePower);
        CompoundTag activated = new CompoundTag();
        int i = 0;
        for (BlockPos p : this.activatedBlocks) {
            activated.putInt("x" + i, p.getX());
            activated.putInt("y" + i, p.getY());
            activated.putInt("z" + i, p.getZ());
            i++;
        }
        activated.putInt("count", i);
        tag.put("activatedBlocks", activated);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        power = tag.getLong("power");
        tanks[0].readFromNBT(tag, "t0");
        tanks[1].readFromNBT(tag, "t1");

        this.redstonePower = tag.getInt("redstonePower");
        this.prevRedstonePower = tag.getInt("prevRedstonePower");
        CompoundTag activated = tag.getCompound("activatedBlocks");
        this.activatedBlocks.clear();
        int count = activated.getInt("count");
        for (int i = 0; i < count; i++) {
            this.activatedBlocks.add(new BlockPos(activated.getInt("x" + i), activated.getInt("y" + i), activated.getInt("z" + i)));
        }
    }

    public void updateRedstonePower(int x, int y, int z) {
        if (level == null) return;
        BlockPos pos = new BlockPos(x, y, z);
        boolean powered = level.hasNeighborSignal(pos);
        boolean contained = activatedBlocks.contains(pos);
        if (!contained && powered) {
            activatedBlocks.add(pos);
            if (redstonePower == -1) redstonePower = 0;
            redstonePower++;
        } else if (contained && !powered) {
            activatedBlocks.remove(pos);
            redstonePower--;
            if (redstonePower == 0) redstonePower = -1;
        }
    }

    @Override
    public long getPower() {
        return power;
    }

    @Override
    public void setPower(long power) {
        this.power = power;
    }

    @Override
    public long getMaxPower() {
        return MAX_POWER;
    }

    @Override
    public List<FluidTankNTM> getAllTanks() {
        return List.of(tanks[0], tanks[1]);
    }

    @Override
    public List<FluidTankNTM> getReceivingTanks() {
        return List.of(tanks[0], tanks[1]);
    }

    @Override
    public boolean canConnect(Direction dir) {
        return dir != Direction.UP && dir != Direction.DOWN;
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new LaunchPadMenu(containerId, playerInventory, this);
    }

    /** CE: {@code switch(missile.fuel)}, {@code SuppressWarnings("incomplete-switch")} - SOLID has no matching branch, preserved exactly. */
    public void setFuel(ItemMissileStandard missile) {
        switch (missile.fuel) {
            case ETHANOL_PEROXIDE -> {
                tanks[0].setTankType(Fluids.ETHANOL);
                tanks[1].setTankType(Fluids.PEROXIDE);
            }
            case KEROSENE_PEROXIDE -> {
                tanks[0].setTankType(Fluids.KEROSENE);
                tanks[1].setTankType(Fluids.PEROXIDE);
            }
            case KEROSENE_LOXY -> {
                tanks[0].setTankType(Fluids.KEROSENE);
                tanks[1].setTankType(Fluids.OXYGEN);
            }
            case JETFUEL_LOXY -> {
                tanks[0].setTankType(Fluids.KEROSENE_REFORM);
                tanks[1].setTankType(Fluids.OXYGEN);
            }
            default -> {
                // SOLID: no tanks needed, matching CE's incomplete switch.
            }
        }
    }

    /** Requires the missile slot to be non-empty and the item to be compatible. */
    public boolean isMissileValid() {
        return !inventory.getStackInSlot(0).isEmpty() && isMissileValid(inventory.getStackInSlot(0));
    }

    public boolean isMissileValid(ItemStack stack) {
        return stack.getItem() instanceof ItemMissileStandard missile && missile.launchable;
    }

    public boolean hasFuel() {
        if (this.power < 75_000) return false;

        ItemStack stack = inventory.getStackInSlot(0);
        if (!stack.isEmpty() && stack.getItem() instanceof ItemMissileStandard missile) {
            if (this.tanks[0].getFill() < missile.fuelCap) return false;
            return this.tanks[1].getFill() >= missile.fuelCap;
        }

        return false;
    }

    @Nullable
    public Entity instantiateMissile(int targetX, int targetZ) {
        if (level == null) return null;
        ItemStack stack = inventory.getStackInSlot(0);
        if (stack.isEmpty()) return null;

        if (stack.getItem() == MissileItems.MISSILE_ANTI_BALLISTIC.get()) {
            EntityMissileAntiBallistic missile = new EntityMissileAntiBallistic(MissileEntityTypes.ANTI_BALLISTIC.get(), level);
            missile.setPos(worldPosition.getX() + 0.5D, worldPosition.getY() + getLaunchOffset(), worldPosition.getZ() + 0.5D);
            return missile;
        }

        MissileFactory factory = MISSILES.get(new ComparableStack(stack).makeSingular());
        if (factory == null) return null;

        EntityMissileBaseNT missile = factory.create(level);
        missile.initTrajectory(worldPosition.getX() + 0.5D, worldPosition.getY() + getLaunchOffset(), worldPosition.getZ() + 0.5D, targetX, targetZ);
        return missile;
    }

    public void finalizeLaunch(Entity missile) {
        if (level == null) return;

        Entity detonatorEntity = ModContext.DETONATOR_CONTEXT.get();
        if (detonatorEntity instanceof LivingEntity livingEntity && missile instanceof IThrowable throwable) {
            throwable.setThrower(livingEntity);
        }

        level.addFreshEntity(missile);
        level.playSound(null, worldPosition.getX() + 0.5, worldPosition.getY(), worldPosition.getZ() + 0.5,
                HBMSoundHandler.missileTakeoff.get(), SoundSource.BLOCKS, 2.0F, 1.0F);

        this.power -= 75_000;

        ItemStack stack = inventory.getStackInSlot(0);
        if (!stack.isEmpty() && stack.getItem() instanceof ItemMissileStandard item) {
            tanks[0].setFill(tanks[0].getFill() - item.fuelCap);
            tanks[1].setFill(tanks[1].getFill() - item.fuelCap);
        }

        inventory.extractItem(0, 1, false);
    }

    public BombReturnCode launchFromDesignator() {
        if (level == null || !canLaunch()) return BombReturnCode.ERROR_MISSING_COMPONENT;

        boolean needsDesignator = needsDesignator(inventory.getStackInSlot(0).getItem());

        int targetX = 0;
        int targetZ = 0;

        ItemStack designatorStack = inventory.getStackInSlot(1);
        if (!designatorStack.isEmpty() && designatorStack.getItem() instanceof IDesignatorItem designator) {
            if (!designator.isReady(level, designatorStack, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ()) && needsDesignator) {
                return BombReturnCode.ERROR_MISSING_COMPONENT;
            }

            Vec3 coords = designator.getCoords(level, designatorStack, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ());
            targetX = (int) Math.floor(coords.x);
            targetZ = (int) Math.floor(coords.z);
        } else if (needsDesignator) {
            return BombReturnCode.ERROR_MISSING_COMPONENT;
        }

        return this.launchToCoordinate(targetX, targetZ);
    }

    public BombReturnCode launchToEntity(Entity entity) {
        if (!canLaunch()) return BombReturnCode.ERROR_MISSING_COMPONENT;

        Entity e = instantiateMissile((int) Math.floor(entity.getX()), (int) Math.floor(entity.getZ()));
        if (e != null) {
            if (e instanceof EntityMissileAntiBallistic abm) {
                abm.tracking = entity;
            }

            finalizeLaunch(e);
            return BombReturnCode.LAUNCHED;
        }
        return BombReturnCode.ERROR_MISSING_COMPONENT;
    }

    public BombReturnCode launchToCoordinate(int targetX, int targetZ) {
        if (!canLaunch()) return BombReturnCode.ERROR_MISSING_COMPONENT;

        Entity e = instantiateMissile(targetX, targetZ);
        if (e != null) {
            finalizeLaunch(e);
            return BombReturnCode.LAUNCHED;
        }
        return BombReturnCode.ERROR_MISSING_COMPONENT;
    }

    @Override
    public boolean sendCommandPosition(int x, int y, int z) {
        return this.launchToCoordinate(x, z) == BombReturnCode.LAUNCHED;
    }

    @Override
    public boolean sendCommandEntity(Entity target) {
        return this.launchToEntity(target) == BombReturnCode.LAUNCHED;
    }

    public boolean needsDesignator(Item item) {
        return item != MissileItems.MISSILE_ANTI_BALLISTIC.get();
    }

    /** Full launch condition: item launchable, fuel and power present, plus any per-type extra checks. */
    public boolean canLaunch() {
        return this.isMissileValid() && this.hasFuel() && this.isReadyForLaunch();
    }

    public int getFuelState() {
        return getGaugeState(0);
    }

    public int getOxidizerState() {
        return getGaugeState(1);
    }

    public int getGaugeState(int tank) {
        ItemStack stack = inventory.getStackInSlot(0);
        if (stack.isEmpty() || !(stack.getItem() instanceof ItemMissileStandard missile)) return 0;

        MissileFuel fuel = missile.fuel;
        if (fuel == MissileFuel.SOLID) return 0;
        return tanks[tank].getFill() >= missile.fuelCap ? 1 : -1;
    }

    /** Any extra conditions for launching in addition to the missile being valid and fueled. */
    public abstract boolean isReadyForLaunch();

    public abstract double getLaunchOffset();
}
