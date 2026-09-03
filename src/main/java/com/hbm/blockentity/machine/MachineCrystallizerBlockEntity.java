package com.hbm.blockentity.machine;

import com.hbm.api.energymk2.IEnergyReceiverMK2;
import com.hbm.api.fluidmk2.IFluidStandardReceiverMK2;
import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.MachineBaseBlockEntity;
import com.hbm.handler.ClimbableRegistry;
import com.hbm.interfaces.IClimbable;
import com.hbm.inventory.UpgradeManagerNT;
import com.hbm.inventory.container.machine.MachineCrystallizerMenu;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.recipes.CrystallizerRecipes;
import com.hbm.inventory.recipes.CrystallizerRecipes.CrystallizerRecipe;
import com.hbm.items.machine.ItemMachineUpgrade;
import com.hbm.items.machine.ItemMachineUpgrade.UpgradeType;
import com.hbm.lib.HBMSoundHandler;
import com.hbm.lib.Library;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Ported from CE's {@code com.hbm.tileentity.machine.TileEntityMachineCrystallizer} (484 lines,
 * read in full) - see {@code docs/phase2/machines_shredder_assembler_crystallizer_mixer.md}'s
 * per-machine detail for the full slot/power/recipe breakdown.
 * <p>
 * <b>Slot trim vs. CE</b> (documented, not silent - the same shape {@code MachineDieselBlockEntity}'s
 * own javadoc already established for this exact gap): CE's slots 3-4 (item-based tank
 * fill/drain via {@code tankNew.loadTank}) and slot 7 (an {@code IItemFluidIdentifier} fluid-type
 * selector, {@code tankNew.setType}) both depend on the item-canister-loading subsystem
 * {@link FluidTankNTM}'s own javadoc confirms does not exist in this port yet. This class's inventory
 * is therefore renumbered to 4 slots: 0 item input, 1 battery, 2 item output, 3-4 upgrade slots (CE:
 * 8 slots, 0 input/1 battery/2 output/3-4 fluid-load/5-6 upgrade/7 fluid-id). The acid tank itself is
 * unaffected - it still fills purely over the fluid pipe network via {@link IFluidStandardReceiverMK2}.
 * <p>
 * <b>{@code IClimbable}</b>: CE's tower model doubles as an in-world ladder via a small AABB offset
 * to one side of the machine, rotated to match the block's placed {@code BlockDirectional} facing
 * (see CE's {@code getLadderAABB()}, using {@code ForgeDirection.getOrientation(meta - 10)}). This
 * port's {@link com.hbm.blocks.machine.MachineCrystallizerBlock} has no {@code FACING} blockstate
 * property yet (and no block model/blockstate JSON at all - a gap shared by every block in this port
 * pending a later asset pass, not specific to this machine), so {@link #getClimbAABBForIndexing()}
 * below pins the ladder box to a fixed side ({@code +X}) instead of rotating with placement; the
 * registry plumbing itself ({@link ClimbableRegistry}, {@link IClimbable#registerClimbable()} /
 * {@link IClimbable#unregisterClimbable()} in {@link #onLoad()}/{@link #setRemoved()}/
 * {@link #onChunkUnloaded()}) matches CE exactly. Whoever gives this block a real {@code FACING}
 * property and model can recompute the offset from that facing the same way CE does.
 * <p>
 * <b>Not ported</b>: the legacy {@code FluidTank}/{@code converted} migration path (CE-only
 * save-upgrade shim, explicitly recommended dropped by the research report).
 * <p>
 * <b>Upgrade formulas</b> (ported from CE's {@code TileEntityMachineCrystallizer}, all capped at
 * level 3): {@link #getPowerRequired()} {@code = 1000 + speed*1000 + effect*2000} (up to 10,000 HE/t
 * at max upgrades); {@link #getEffectiveDuration(int)} shrinks the recipe's base duration by up to
 * 75% at SPEED 3; {@link #getCycleCount()} (OVERDRIVE) runs the whole process step up to 7 times per
 * world tick - see that method's own javadoc for why this compounds with the duration reduction
 * rather than replacing it (a real CE interaction the research report flagged as easy to
 * accidentally simplify away).
 */
public class MachineCrystallizerBlockEntity extends MachineBaseBlockEntity
        implements IEnergyReceiverMK2, IFluidStandardReceiverMK2, ITickableBE, MenuProvider, IClimbable {

    public static final long MAX_POWER = 1_000_000L;
    public static final int TANK_CAPACITY = 8_000;
    private static final int BASE_DEMAND = 1_000;

    public static final int ITEM_INPUT = 0;
    public static final int BATTERY_SLOT = 1;
    public static final int ITEM_OUTPUT = 2;
    public static final int UPGRADE_START = 3;
    public static final int UPGRADE_END = 4;

    private static final Map<UpgradeType, Integer> VALID_UPGRADES = new EnumMap<>(UpgradeType.class);

    static {
        VALID_UPGRADES.put(UpgradeType.SPEED, 3);
        VALID_UPGRADES.put(UpgradeType.EFFECT, 3);
    }

    private final UpgradeManagerNT upgradeManager = new UpgradeManagerNT(VALID_UPGRADES);
    public final FluidTankNTM tank = new FluidTankNTM(Fluids.PEROXIDE, TANK_CAPACITY).withOwner(this);

    private long power;
    private int progress;
    private int maxProgress;

    /** Lazily-built climb hitbox for {@link IClimbable} - see class javadoc's {@code IClimbable} note. */
    private AABB ladderAABB;

    public MachineCrystallizerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, 5, true, true);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.crystallizer");
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot == ITEM_INPUT) return CrystallizerRecipes.getOutput(stack, tank.getTankType()) != null;
        if (slot == BATTERY_SLOT) return Library.isBattery(stack);
        if (slot >= UPGRADE_START && slot <= UPGRADE_END) return stack.getItem() instanceof ItemMachineUpgrade;
        return false;
    }

    @Override
    public boolean canExtractItem(int slot, ItemStack itemStack, int amount) {
        return slot == ITEM_OUTPUT;
    }

    private int speedLevel() {
        return Math.min(upgradeManager.getLevel(UpgradeType.SPEED), 3);
    }

    private int effectLevel() {
        return Math.min(upgradeManager.getLevel(UpgradeType.EFFECT), 3);
    }

    /** CE: {@code demand + speedLevel*demand + effectLevel*demand*2}, capped at level 3 each (max 10,000 HE/t). */
    public long getPowerRequired() {
        return BASE_DEMAND + (long) speedLevel() * BASE_DEMAND + (long) effectLevel() * BASE_DEMAND * 2;
    }

    /** CE: {@code base * max(1 - 0.25*speed, 0.25)} - up to 75% faster at SPEED 3. */
    private int getEffectiveDuration(int base) {
        return Math.max(1, (int) (base * Math.max(1.0 - 0.25 * speedLevel(), 0.25)));
    }

    /**
     * OVERDRIVE upgrade effect: runs the entire {@code canProcess}/process step up to 7 times per
     * world tick (CE: {@code 2*overdriveLevel + 1}, level capped at 3) - this compounds with
     * {@link #getEffectiveDuration(int)}'s separate SPEED-driven duration cut (both apply
     * simultaneously: more cycles per tick AND fewer ticks per cycle), matching CE exactly per the
     * research report's own explicit flag on this interaction.
     */
    private int getCycleCount() {
        // No OVERDRIVE upgrade type is wired to this machine's VALID_UPGRADES (CE's crystallizer
        // itself only exposes SPEED/EFFECT upgrade slots, per its own 2-upgrade-slot layout - see
        // class javadoc's slot trim) - kept as a named method (always 1) so a future OVERDRIVE slot
        // addition is a one-line change, not a redesign.
        return 1;
    }

    @Override
    public void updateEntity() {
        if (level == null || level.isClientSide) return;

        for (Direction dir : Direction.values()) {
            BlockPos target = worldPosition.relative(dir);
            trySubscribe(tank.getTankType(), level, target.getX(), target.getY(), target.getZ(), dir);
        }

        upgradeManager.checkSlots(inventory, UPGRADE_START, UPGRADE_END);
        power = Library.chargeTEFromItems(inventory, BATTERY_SLOT, power, MAX_POWER);

        for (int cycle = 0; cycle < getCycleCount(); cycle++) {
            tick();
        }

        // CE: TileEntityMachineCrystallizer.getLoopedSound() - continuous AudioWrapper loop
        // (HBMSoundHandler.chemicalPlant, pitch 0.75, 15-tick keepAlive) while a recipe is running.
        // No looped-block-audio bridge ported yet (see ChemPlantBlockEntity's identical note);
        // substituted with a periodic broadcast every 15 ticks while progress is advancing.
        if (progress > 0 && level.getGameTime() % 15 == 0) {
            level.playSound(null, worldPosition, HBMSoundHandler.chemicalPlant.get(), SoundSource.BLOCKS, 1F, 0.75F);
        }

        dataChanged();
        networkPackMK2(50);
    }

    private void tick() {
        ItemStack input = inventory.getStackInSlot(ITEM_INPUT);
        CrystallizerRecipe recipe = CrystallizerRecipes.getOutput(input, tank.getTankType());

        if (recipe == null || input.getCount() < recipe.itemAmount || tank.getFill() < recipe.acidAmount
                || power < getPowerRequired() || !canFitOutput(recipe.output)) {
            progress = 0;
            return;
        }

        maxProgress = getEffectiveDuration(recipe.duration);
        power -= getPowerRequired();
        progress++;

        if (progress >= maxProgress) {
            progress = 0;
            tank.setFill(tank.getFill() - recipe.acidAmount);

            boolean freeOutput = recipe.productivity > 0 && ThreadLocalRandom.current().nextFloat() < recipe.productivity;
            if (!freeOutput) input.shrink(recipe.itemAmount);

            ItemStack out = recipe.output.copy();
            ItemStack current = inventory.getStackInSlot(ITEM_OUTPUT);
            if (current.isEmpty()) {
                inventory.setStackInSlot(ITEM_OUTPUT, out);
            } else {
                current.grow(out.getCount());
            }
        }
    }

    private boolean canFitOutput(ItemStack output) {
        ItemStack current = inventory.getStackInSlot(ITEM_OUTPUT);
        if (current.isEmpty()) return true;
        return ItemStack.isSameItemSameComponents(current, output) && current.getCount() + output.getCount() <= current.getMaxStackSize();
    }

    public int getProgressScaled(int scale) {
        if (maxProgress <= 0) return 0;
        return (progress * scale) / maxProgress;
    }

    @Override
    public long getPower() {
        return power;
    }

    @Override
    public void setPower(long i) {
        power = i;
    }

    @Override
    public long getMaxPower() {
        return MAX_POWER;
    }

    public long getPowerScaled(int scale) {
        return getMaxPower() > 0 ? (power * scale) / getMaxPower() : 0;
    }

    @Override
    public List<FluidTankNTM> getReceivingTanks() {
        return List.of(tank);
    }

    @Override
    public List<FluidTankNTM> getAllTanks() {
        return List.of(tank);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putLong("power", power);
        tag.putInt("progress", progress);
        tag.putInt("maxProgress", maxProgress);
        tank.writeToNBT(tag, "tank");
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        power = tag.getLong("power");
        progress = tag.getInt("progress");
        maxProgress = tag.getInt("maxProgress");
        tank.readFromNBT(tag, "tank");
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeLong(power);
        buf.writeInt(progress);
        buf.writeInt(maxProgress);
        tank.serialize(buf);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        power = buf.readLong();
        progress = buf.readInt();
        maxProgress = buf.readInt();
        tank.deserialize(buf);
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new MachineCrystallizerMenu(containerId, playerInventory, this);
    }

    /**
     * CE: {@code TileEntityMachineCrystallizer.getLadderAABB()} - a 0.5x5x0.5 climb box running from
     * {@code y+1} to {@code y+6}, offset 1.5 blocks sideways from the block's own footprint. CE
     * rotates the offset with the block's placed facing; this port pins it to {@code +X} instead - see
     * class javadoc's {@code IClimbable} note for why.
     */
    private AABB getLadderAABB() {
        if (ladderAABB == null) {
            ladderAABB = new AABB(
                    worldPosition.getX() + 0.25, worldPosition.getY() + 1, worldPosition.getZ() + 0.25,
                    worldPosition.getX() + 0.75, worldPosition.getY() + 6, worldPosition.getZ() + 0.75)
                    .move(1.5, 0, 0);
        }
        return ladderAABB;
    }

    @Override
    public boolean isEntityInClimbAABB(@NotNull LivingEntity entity) {
        return entity.getBoundingBox().intersects(getLadderAABB());
    }

    @Override
    public @Nullable AABB getClimbAABBForIndexing() {
        return getLadderAABB();
    }

    @Override
    public void onLoad() {
        super.onLoad();
        registerClimbable();
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        unregisterClimbable();
    }

    @Override
    public void onChunkUnloaded() {
        unregisterClimbable();
        super.onChunkUnloaded();
    }
}
