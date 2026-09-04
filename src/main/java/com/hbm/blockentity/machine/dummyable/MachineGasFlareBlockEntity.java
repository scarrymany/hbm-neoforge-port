package com.hbm.blockentity.machine.dummyable;

import com.hbm.api.energymk2.IEnergyProviderMK2;
import com.hbm.api.fluidmk2.IFluidStandardReceiverMK2;
import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.LoadedBaseBlockEntity.TiltType;
import com.hbm.blockentity.MachineBaseBlockEntity;
import com.hbm.inventory.container.machine.dummyable.GasFlareMenu;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.fluid.trait.FT_Polluting;
import com.hbm.inventory.fluid.trait.FluidTrait;
import com.hbm.inventory.recipes.FlareRecipes;
import com.hbm.items.machine.IItemFluidIdentifier;
import com.hbm.items.machine.ItemMachineUpgrade;
import com.hbm.items.machine.ItemMachineUpgrade.UpgradeType;
import com.hbm.lib.DirPos;
import com.hbm.lib.HBMSoundHandler;
import com.hbm.lib.Library;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * CE {@code TileEntityMachineGasFlare}: vent 50 mB/t or burn 10 mB/t. Upgrades via slot scan
 * ({@code UpgradeManagerNT} not ported).
 * checkTilt(CONFIG) / 2×2 floor / standardFloor3x3 Exact CE {@code :125} / {@code :141} / {@code :383-384}.
 * {@code setType(3)} / {@code loadTank(1,2)} Exact CE {@code :135-136}.
 * Vent {@code onFluidRelease} + {@code FT_Polluting.pollute(SPILL, eject*5)} Exact CE {@code :156-162}.
 * Burn fire box + {@code pollute(BURN, eject*5)} Exact CE {@code :188-199}.
 * Tower / {@code spawnGasFlame} / VanillaExt_Smoke stay skipped (VFX).
 */
public class MachineGasFlareBlockEntity extends MachineBaseBlockEntity
        implements IEnergyProviderMK2, IFluidStandardReceiverMK2, ITickableBE, MenuProvider {

    public static final long MAX_POWER = 1_000_000;

    public final FluidTankNTM tank;
    public long power;
    public boolean isOn;
    public boolean doesBurn;

    public MachineGasFlareBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, 6, true, true);
        this.tank = new FluidTankNTM(Fluids.GAS, 64_000).withOwner(this);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.gasFlare");
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot == 0) return Library.isBattery(stack);
        if (slot == 3) return stack.getItem() instanceof IItemFluidIdentifier;
        if (slot == 4 || slot == 5) return stack.getItem() instanceof ItemMachineUpgrade;
        return slot == 1;
    }

    @Override
    public boolean canExtractItem(int slot, ItemStack stack, int amount) {
        return slot == 0 || slot == 2;
    }

    @Override
    public int[] getAccessibleSlotsFromSide(Direction side) {
        return new int[]{0, 1, 2};
    }

    @Override
    public int getFloorCount() {
        return 2 * 2;
    }

    @Override
    public BlockPos getFloorPosFromIndex(int index) {
        return standardFloor3x3(index);
    }

    @Override
    public void updateEntity() {
        if (level == null || level.isClientSide) return;

        // CE TileEntityMachineGasFlare.java:125
        checkTilt(TiltType.CONFIG, false);

        for (DirPos pos : getConPos()) {
            tryProvide(level, pos.getPos(), pos.getDir());
            trySubscribe(tank.getTankType(), level, pos);
        }

        // CE TileEntityMachineGasFlare.java:135-136
        tank.setType(3, inventory);
        tank.loadTank(1, 2, inventory);

        int maxVent = 50;
        int maxBurn = 10;
        // CE TileEntityMachineGasFlare.java:141
        if (isOn && tank.getFill() > 0 && !this.tilted) {
            int burn = upgradeLevel(UpgradeType.SPEED);
            int yield = upgradeLevel(UpgradeType.EFFECT);
            maxVent += maxVent * burn;
            maxBurn += maxBurn * burn;

            if (!doesBurn || !FlareRecipes.canBurn(tank.getTankType())) {
                if (FlareRecipes.canVent(tank.getTankType())) {
                    int eject = Math.min(maxVent, tank.getFill());
                    tank.setFill(tank.getFill() - eject);
                    // CE TileEntityMachineGasFlare.java:156
                    tank.getTankType().onFluidRelease(this, tank, eject);

                    // CE TileEntityMachineGasFlare.java:158-159
                    if (level.getGameTime() % 7 == 0) {
                        level.playSound(null, worldPosition.above(11), SoundEvents.FIRE_EXTINGUISH,
                                SoundSource.BLOCKS, getVolume(1.5F), 0.5F);
                    }

                    // CE TileEntityMachineGasFlare.java:161-162
                    if (level.getGameTime() % 5 == 0 && eject > 0) {
                        FT_Polluting.pollute(level, worldPosition, tank.getTankType(),
                                FluidTrait.FluidReleaseType.SPILL, eject * 5);
                    }
                }
            } else {
                int eject = Math.min(maxBurn, tank.getFill());
                tank.setFill(tank.getFill() - eject);
                int penalty = FlareRecipes.canVent(tank.getTankType()) ? 5 : 10;
                long prod = FlareRecipes.burnEnergyPerMb(tank.getTankType()) * eject / penalty;
                prod += prod * yield / 3;
                power = Math.min(MAX_POWER, power + prod);

                // CE TileEntityMachineGasFlare.java:186 ParticleUtil.spawnGasFlame — VFX skip

                // CE TileEntityMachineGasFlare.java:188-192
                AABB flame = new AABB(
                        worldPosition.getX() - 1, worldPosition.getY() + 12, worldPosition.getZ() - 2,
                        worldPosition.getX() + 2, worldPosition.getY() + 17, worldPosition.getZ() + 2);
                for (Entity e : level.getEntitiesOfClass(Entity.class, flame)) {
                    e.igniteForSeconds(5);
                    e.hurt(level.damageSources().onFire(), 5F);
                }

                // CE TileEntityMachineGasFlare.java:194-195
                if (level.getGameTime() % 3 == 0) {
                    level.playSound(null, worldPosition.above(11), HBMSoundHandler.flamethrowerShoot.get(),
                            SoundSource.BLOCKS, getVolume(1.5F), 0.75F);
                }

                // CE TileEntityMachineGasFlare.java:197-198
                if (level.getGameTime() % 5 == 0 && eject > 0) {
                    FT_Polluting.pollute(level, worldPosition, tank.getTankType(),
                            FluidTrait.FluidReleaseType.BURN, eject * 5);
                }
            }
        }

        power = Library.chargeItemsFromTE(inventory, 0, power, MAX_POWER);
        dataChanged();
        networkPackMK2(25);
    }

    private int upgradeLevel(UpgradeType type) {
        int level = 0;
        for (int slot : new int[]{4, 5}) {
            ItemStack stack = inventory.getStackInSlot(slot);
            if (stack.getItem() instanceof ItemMachineUpgrade up && up.getType() == type) {
                level = Math.max(level, up.getTier());
            }
        }
        return level;
    }

    public void toggleValve() {
        isOn = !isOn;
        setChanged();
    }

    public void toggleBurn() {
        doesBurn = !doesBurn;
        setChanged();
    }

    public DirPos[] getConPos() {
        return new DirPos[]{
                new DirPos(worldPosition.getX() + 2, worldPosition.getY(), worldPosition.getZ(), Direction.EAST),
                new DirPos(worldPosition.getX() - 2, worldPosition.getY(), worldPosition.getZ(), Direction.WEST),
                new DirPos(worldPosition.getX(), worldPosition.getY(), worldPosition.getZ() + 2, Direction.SOUTH),
                new DirPos(worldPosition.getX(), worldPosition.getY(), worldPosition.getZ() - 2, Direction.NORTH),
        };
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
    public @NotNull List<FluidTankNTM> getReceivingTanks() {
        return List.of(tank);
    }

    @Override
    public @NotNull List<FluidTankNTM> getAllTanks() {
        return List.of(tank);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putLong("powerTime", power);
        tank.writeToNBT(tag, "gas");
        tag.putBoolean("isOn", isOn);
        tag.putBoolean("doesBurn", doesBurn);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        power = tag.getLong("powerTime");
        tank.readFromNBT(tag, "gas");
        isOn = tag.getBoolean("isOn");
        doesBurn = tag.getBoolean("doesBurn");
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeLong(power);
        buf.writeBoolean(isOn);
        buf.writeBoolean(doesBurn);
        tank.serialize(buf);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        power = buf.readLong();
        isOn = buf.readBoolean();
        doesBurn = buf.readBoolean();
        tank.deserialize(buf);
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new GasFlareMenu(id, inv, this);
    }
}
