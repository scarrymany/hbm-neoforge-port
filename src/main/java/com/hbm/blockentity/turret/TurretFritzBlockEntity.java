package com.hbm.blockentity.turret;

import com.hbm.api.fluidmk2.IFluidStandardReceiverMK2;
import com.hbm.blocks.BlockDummyable;
import com.hbm.capability.NTMFluidHandlerWrapper;
import com.hbm.entity.projectile.EntityBulletBaseMK4;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.fluid.trait.FT_Flammable;
import com.hbm.inventory.fluid.trait.FluidTraitSimple;
import com.hbm.items.weapon.sedna.BulletConfig;
import com.hbm.items.weapon.sedna.content.GunHeavyItems;
import com.hbm.lib.CapabilityContextProvider;
import com.hbm.lib.HBMSoundHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Ported from CE's {@code TileEntityTurretFritz} - the diesel-fueled flamethrower turret. Gates
 * firing on its own {@link FluidTankNTM} fuel tank ({@link IFluidStandardReceiverMK2}, matching
 * this port's already-ported fluid network exactly as the report calls for) rather than the ammo
 * inventory - {@link #getAmmoList()} always returns an empty list here, matching CE's own
 * {@code getAmmoList() { return null; }}.
 * <p>
 * <b>Scope trim vs. CE, documented</b>: CE's flame projectile uses
 * {@code XFactoryFlamer.flame_nograv} ({@code flame_diesel.clone().setGrav(0)}), which itself pulls
 * in {@code EntityFireLingering}, {@code GunFactory.EnumAmmo.FLAME_DIESEL}, and
 * {@code HbmLivingCapability} fire-effect lambdas from the gun-content/flamethrower package (Package
 * D, out of this turret package's scope - see {@code docs/phase3/turret_system.md}). Until that
 * lands, {@link #getFlameConfig()} returns {@code null} and firing is a documented no-op, matching
 * every other bullet-firing turret's ammo-gated inert state.
 * {@code tank.setType(9, 9, inventory)} Exact CE {@code TileEntityTurretFritz.java:178}.
 * Slot 9 is the last ammo-grid cell (CE {@code ContainerTurretBase} 3×3). Hopper excludes 9
 * Exact CE {@code :242-244}. FLAME_DIESEL fill loop Exact CE {@code :181-189} — flattened
 * {@link GunHeavyItems#FLAME_DIESEL} is the registered stand-in for CE {@code ammo_standard}
 * + {@code EnumAmmo.FLAME_DIESEL}.
 */
public class TurretFritzBlockEntity extends TurretBaseBlockEntity implements IFluidStandardReceiverMK2 {

    public final FluidTankNTM tank;
    private final Map<BlockPos, NTMFluidHandlerWrapper> fluidWrapperCache = new HashMap<>();

    public TurretFritzBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        this.tank = new FluidTankNTM(Fluids.DIESEL, 16_000).withOwner(this);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.turretFritz");
    }

    @Override
    protected List<BulletConfig> getAmmoList() {
        return Collections.emptyList();
    }

    /**
     * TODO(phase3-gun-content): CE uses {@code XFactoryFlamer.flame_nograv} - not ported yet, see
     * this class's own javadoc.
     */
    @Nullable
    protected BulletConfig getFlameConfig() {
        return null;
    }

    @Override
    public double getDecetorRange() {
        return 32D;
    }

    @Override
    public double getDecetorGrace() {
        return 2D;
    }

    @Override
    public double getTurretElevation() {
        return 45D;
    }

    @Override
    public long getMaxPower() {
        return 10_000;
    }

    @Override
    public double getBarrelLength() {
        return 2.25D;
    }

    @Override
    public double getAcceptableInaccuracy() {
        return 15;
    }

    @Override
    public void updateFiringTick() {
        if (level == null) return;
        if (!(tank.getTankType().hasTrait(FT_Flammable.class) && tank.getTankType().hasTrait(FluidTraitSimple.FT_Liquid.class) && tank.getFill() >= 2)) {
            return;
        }

        BulletConfig flame = getFlameConfig();
        if (flame == null) return;

        FT_Flammable trait = tank.getTankType().getTrait(FT_Flammable.class);
        tank.setFill(tank.getFill() - 2);

        Vec3 turretPos = getTurretPos();
        Vec3 muzzleOffset = new Vec3(getBarrelLength(), 0, 0);
        muzzleOffset = com.hbm.util.Vec3dUtil.rotateRoll(muzzleOffset, (float) -this.rotationPitch);
        muzzleOffset = muzzleOffset.yRot((float) -(this.rotationYaw + Math.PI * 0.5));

        EntityBulletBaseMK4 proj = new EntityBulletBaseMK4(level, flame, trait.getHeatEnergy() / 500_000F, 0.05F, (float) rotationYaw, (float) rotationPitch);

        double muzzleX = turretPos.x + muzzleOffset.x;
        double muzzleY = turretPos.y + muzzleOffset.y;
        double muzzleZ = turretPos.z + muzzleOffset.z;

        proj.moveTo(muzzleX, muzzleY, muzzleZ, proj.getYRot(), proj.getXRot());
        level.addFreshEntity(proj);

        level.playSound(null, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(),
                HBMSoundHandler.flamethrowerShoot.get(), SoundSource.BLOCKS, 2F, 1F + level.random.nextFloat() * 0.5F);

        // TODO(phase3-gun-vfx): CE spawns a flame-burst particle effect here - deferred.
    }

    @Override
    public void updateEntity() {
        super.updateEntity();
        if (level == null || level.isClientSide) return;
        // CE TileEntityTurretFritz.java:178
        tank.setType(9, 9, inventory);

        // CE TileEntityTurretFritz.java:181-189 — slots 1-9, +1000 diesel per flame_diesel.
        for (int i = 1; i < 10; i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (!stack.isEmpty() && stack.getItem() == GunHeavyItems.FLAME_DIESEL.get()) {
                if (this.tank.getTankType() == Fluids.DIESEL && this.tank.getFill() + 1000 <= this.tank.getMaxFill()) {
                    this.tank.setFill(this.tank.getFill() + 1000);
                    stack.shrink(1);
                }
            }
        }
    }

    @Override
    protected void updateConnections() {
        if (level == null) return;
        for (com.hbm.lib.DirPos p : getConPos()) {
            trySubscribe(level, p.getPos(), p.getDir());
            trySubscribe(tank.getTankType(), level, p.getPos(), p.getDir());
        }
    }

    private com.hbm.lib.DirPos[] getConPos() {
        BlockState state = getBlockState();
        Direction dir = state.hasProperty(BlockDummyable.META)
                ? Direction.from3DDataValue(state.getValue(BlockDummyable.META) - BlockDummyable.offset).getOpposite()
                : Direction.NORTH;
        Direction rot = dir.getClockWise(Direction.Axis.Y);
        int x = worldPosition.getX();
        int y = worldPosition.getY();
        int z = worldPosition.getZ();

        return new com.hbm.lib.DirPos[]{
                new com.hbm.lib.DirPos(x + dir.getStepX() * -1, y, z + dir.getStepZ() * -1, dir.getOpposite()),
                new com.hbm.lib.DirPos(x + dir.getStepX() * -1 + rot.getStepX() * -1, y, z + dir.getStepZ() * -1 + rot.getStepZ() * -1, dir.getOpposite()),
                new com.hbm.lib.DirPos(x + rot.getStepX() * -2, y, z + rot.getStepZ() * -2, rot.getOpposite()),
                new com.hbm.lib.DirPos(x + dir.getStepX() + rot.getStepX() * -2, y, z + dir.getStepZ() + rot.getStepZ() * -2, rot.getOpposite()),
                new com.hbm.lib.DirPos(x + rot.getStepX(), y, z + rot.getStepZ(), rot),
                new com.hbm.lib.DirPos(x + dir.getStepX() + rot.getStepX(), y, z + dir.getStepZ() + rot.getStepZ(), rot),
                new com.hbm.lib.DirPos(x + dir.getStepX() * 2, y, z + dir.getStepZ() * 2, dir),
                new com.hbm.lib.DirPos(x + dir.getStepX() * 2 + rot.getStepX() * -1, y, z + dir.getStepZ() * 2 + rot.getStepZ() * -1, dir)
        };
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.tank.readFromNBT(tag, "diesel");
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        this.tank.writeToNBT(tag, "diesel");
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        tank.serialize(buf);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        tank.deserialize(buf);
    }

    @Override
    public int[] getAccessibleSlotsFromSide(Direction side) {
        return new int[]{1, 2, 3, 4, 5, 6, 7, 8};
    }

    @Override
    public @NotNull List<FluidTankNTM> getReceivingTanks() {
        return List.of(tank);
    }

    @Override
    public @NotNull List<FluidTankNTM> getAllTanks() {
        return List.of(tank);
    }

    /**
     * {@link TurretBaseBlockEntity}'s constructor hardcodes {@code enableFluidWrapper=false}
     * (matching CE's shared {@code TileEntityTurretBaseNT.super(11, false, true)}) - Fritz is the
     * one turret that needs a fluid capability, so it builds its own wrapper directly here instead,
     * exactly like CE's own {@code TileEntityTurretFritz} overrides
     * {@code hasCapability}/{@code getCapability} directly rather than going through the shared
     * boolean flag. Body matches {@code MachineBaseBlockEntity#getFluidHandlerCapability} exactly
     * (same per-accessor-position cache contract), just gated by {@code true} unconditionally
     * instead of a constructor flag.
     */
    @Nullable
    @Override
    public IFluidHandler getFluidHandlerCapability(@Nullable Direction side) {
        if (side == null) return new NTMFluidHandlerWrapper(this, null);
        BlockPos accessorPos = CapabilityContextProvider.getAccessor(this.worldPosition).immutable();
        return fluidWrapperCache.computeIfAbsent(accessorPos, acc -> new NTMFluidHandlerWrapper(this, acc));
    }

    @Override
    public boolean hasFluidHandlerCapability() {
        return true;
    }
}
