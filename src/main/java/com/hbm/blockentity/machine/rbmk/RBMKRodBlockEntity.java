package com.hbm.blockentity.machine.rbmk;

import com.hbm.api.rbmk.IRBMKFluxReceiver;
import com.hbm.api.redstoneoverradio.IRORValueProvider;
import com.hbm.api.rbmk.IRBMKLoadable;
import com.hbm.api.rbmk.RBMKDials;
import com.hbm.blocks.machine.rbmk.RBMKBaseBlock;
import com.hbm.blocks.machine.rbmk.RBMKRodBlock;
import com.hbm.handler.neutron.NeutronStream;
import com.hbm.handler.neutron.RBMKNeutronHandler;
import com.hbm.handler.radiation.ChunkRadiationManager;
import com.hbm.items.machine.ItemRBMKRod;
import com.hbm.items.machine.rbmk.RBMKRods;
import com.hbm.saveddata.satellites.SatelliteRayScan;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Fuel rod channel - the flux-receive/burn/spread cycle, ported from CE's
 * {@code TileEntityRBMKRod} (579 lines, read in full). This is the centerpiece of the whole RBMK
 * package. Implements the real, landed {@link IRBMKFluxReceiver}/{@link IRBMKLoadable} contracts from
 * the sibling {@code rbmk_core_logic} package's {@code com.hbm.api.rbmk} (reconciled against the real
 * classes, not a forward-referenced guess).
 * <p>
 * <b>Not ported</b>: OpenComputers. ROR: CE {@code TileEntityRBMKRod.java:554-577}.
 * SatelliteRayScan Exact CE {@code TileEntityRBMKRod.java:132-133}.
 * {@code ChunkRadiationManager} un-lidded irradiation (Phase 4) is now wired in {@link #updateEntity}.
 */
public class RBMKRodBlockEntity extends RBMKSlottedBlockEntity
        implements IRBMKFluxReceiver, IRBMKLoadable, IRORValueProvider {

    public double fluxFastRatio;
    public double fluxQuantity;
    public double lastFluxQuantity;
    public double lastFluxRatio;
    public boolean hasRod;
    public int rodColor = 0;

    public RBMKRodBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        this(type, pos, state, 1);
    }

    protected RBMKRodBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, int slots) {
        super(type, pos, state, slots);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.rbmkRod");
    }

    @Override
    public boolean isModerated() {
        return getBlockState().getBlock() instanceof RBMKRodBlock rod && rod.moderated;
    }

    // implements IRBMKFluxReceiver.receiveFlux(NeutronStream) - com.hbm.api.rbmk.IRBMKFluxReceiver
    @Override
    public void receiveFlux(NeutronStream stream) {
        double fastFlux = this.fluxQuantity * this.fluxFastRatio;
        double fastFluxIn = stream.fluxQuantity * stream.fluxRatio;

        this.fluxQuantity += stream.fluxQuantity;
        this.fluxFastRatio = this.fluxQuantity <= 0 ? 0 : (fastFlux + fastFluxIn) / this.fluxQuantity;
    }

    // implements IRBMKFluxReceiver.canReceiveFlux() - CE: TileEntityRBMKRod#hasRod, per that interface's own javadoc
    @Override
    public boolean canReceiveFlux() {
        return hasRod;
    }

    @Override
    public double getLastFluxQuantity() {
        return lastFluxQuantity;
    }

    public boolean coldEnoughForAutoloader() {
        ItemStack stack = inventory.getStackInSlot(0);
        if (!stack.isEmpty() && stack.getItem() instanceof ItemRBMKRod) {
            return ItemRBMKRod.getHullHeat(stack) <= 1_000;
        }
        return true;
    }

    public boolean coldEnoughForManual() {
        ItemStack stack = inventory.getStackInSlot(0);
        if (!stack.isEmpty() && stack.getItem() instanceof ItemRBMKRod) {
            return ItemRBMKRod.getHullHeat(stack) <= 200;
        }
        return true;
    }

    @Override
    public void updateEntity() {
        if (!(level instanceof ServerLevel serverLevel)) return;

        ItemStack stack = inventory.getStackInSlot(0).copy();
        if (stack.getItem() instanceof ItemRBMKRod rod) {
            // Exact CE TileEntityRBMKRod.java:132-133 — before burn zeroes fluxQuantity
            if (this.fluxQuantity > 0 && level.getGameTime() % 200 == 0) {
                SatelliteRayScan.reportEvent(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(),
                        SatelliteRayScan.RayEvent.INFO_NUCLEAR, 300);
            }

            this.rodColor = rod.colorTint;

            double fluxIn = fluxFromType(rod.nType);
            double fluxQuantityOut = rod.burn(serverLevel, stack, fluxIn);
            double fluxRatioOut = rod.rType == IRBMKFluxReceiver.NType.SLOW ? 0 : 1;

            rod.updateHeat(serverLevel, stack, 1.0D);
            this.heat += rod.provideHeat(serverLevel, stack, heat, 1.0D);
            inventory.setStackInSlot(0, stack);
            hasRod = true;

            // CE: TileEntityRBMKRod.update() irradiates the surrounding chunk when the rod's lid is
            // off (un-lidded fuel rods leak radiation into the room around them).
            if (!hasLid()) {
                ChunkRadiationManager.proxy.incrementRad(serverLevel, worldPosition,
                        fluxQuantity * 0.05D, fluxQuantity * 10D);
            }

            moveHeat();
            boolean overheated = this.heat > this.maxHeat();
            checkMeltdown(serverLevel);

            if (isRemoved()) return;

            if (overheated) {
                this.lastFluxRatio = 0;
                this.lastFluxQuantity = 0;
                this.fluxQuantity = 0;
                networkPackNT(trackingRange());
                return;
            }

            if (this.heat > 10_000) this.heat = 10_000;
            this.lastFluxQuantity = this.fluxQuantity;
            this.lastFluxRatio = this.fluxFastRatio;

            this.fluxQuantity = 0;
            this.fluxFastRatio = 0;

            spreadFlux(fluxQuantityOut, fluxRatioOut);
        } else {
            this.lastFluxRatio = 0;
            this.lastFluxQuantity = 0;
            this.fluxQuantity = 0;
            this.fluxFastRatio = 0;
            hasRod = false;
            moveHeat();
            checkMeltdown(serverLevel);
        }

        networkPackNT(trackingRange());
    }

    /**
     * SLOW: full efficiency for slow neutrons, half for fast. FAST: full for fast, 30% for slow.
     * ANY: unweighted sum. CE: {@code TileEntityRBMKRod.fluxFromType(NType)}.
     */
    private double fluxFromType(IRBMKFluxReceiver.NType type) {
        double fastFlux = this.fluxQuantity * this.fluxFastRatio;
        double slowFlux = this.fluxQuantity * (1 - this.fluxFastRatio);
        return switch (type) {
            case SLOW -> slowFlux + fastFlux * 0.5;
            case FAST -> fastFlux + slowFlux * 0.3;
            case ANY -> this.fluxQuantity;
        };
    }

    /**
     * Casts the 4 cardinal flux streams from this rod's node - delegates to the sibling
     * {@code rbmk_core_logic} package's own ready-made {@link RBMKNeutronHandler#spreadFlux}
     * (CE: {@code TileEntityRBMKRod.spreadFlux(double, double)}) rather than reimplementing the
     * node-cache lookup/creation here, since that package's real {@code RBMKNeutronNode}/
     * {@code checkNode} cache-eviction logic already assumes it owns that call.
     */
    protected void spreadFlux(double flux, double ratio) {
        RBMKNeutronHandler.spreadFlux(this, flux, ratio);
    }

    @Override
    public void onMelt(int reduce) {
        boolean moderated = isModerated();
        int h = RBMKDials.getColumnHeight((ServerLevel) level);
        reduce = Math.max(1, Math.min(reduce, h));
        if (level.getRandom().nextInt(3) == 0) reduce++;

        ItemStack stack = inventory.getStackInSlot(0);
        boolean corium = stack.getItem() instanceof ItemRBMKRod;
        boolean digamma = corium && RBMKRods.DRX != null && stack.getItem() == RBMKRods.DRX.get();

        inventory.setStackInSlot(0, ItemStack.EMPTY);

        if (corium) {
            for (int i = h; i >= 0; i--) {
                if (i <= h + 1 - reduce) {
                    level.setBlockAndUpdate(worldPosition.above(i), com.hbm.blocks.machine.PWRBlocks.CORIUM_BLOCK.get().defaultBlockState());
                } else {
                    level.setBlockAndUpdate(worldPosition.above(i), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
                }
            }
            int count = 1 + level.getRandom().nextInt(h);
            for (int i = 0; i < count; i++) spawnDebris("FUEL");
        } else {
            this.standardMelt(reduce);
        }

        if (moderated) {
            int count = 2 + level.getRandom().nextInt(2);
            for (int i = 0; i < count; i++) spawnDebris("GRAPHITE");
        }

        spawnDebris("ELEMENT");

        if (digamma) {
            RBMKBaseBlock.digamma = true;
        }
        if (getBlockState().getValue(com.hbm.blocks.BlockDummyable.META) == RBMKBaseBlock.DIR_NORMAL_LID.get3DDataValue() + com.hbm.blocks.BlockDummyable.offset) {
            spawnDebris("LID");
        }
    }

    @Override
    public RBMKNeutronHandler.RBMKType getRBMKType() {
        return RBMKNeutronHandler.RBMKType.ROD;
    }

    @Override
    public RBMKColumn.ColumnType getConsoleType() {
        return RBMKColumn.ColumnType.FUEL;
    }

    @Override
    public RBMKColumn getConsoleData() {
        RBMKColumn.FuelColumn data = (RBMKColumn.FuelColumn) super.getConsoleData();
        ItemStack stack = inventory.getStackInSlot(0);
        if (stack.getItem() instanceof ItemRBMKRod rod) {
            data.enrichment = ItemRBMKRod.getEnrichment(stack);
            data.xenon = ItemRBMKRod.getPoison(stack);
            data.c_heat = ItemRBMKRod.getHullHeat(stack);
            data.c_coreHeat = ItemRBMKRod.getCoreHeat(stack);
            data.c_maxHeat = rod.meltingPoint;
        }
        return data;
    }

    @Override
    public boolean canLoad(ItemStack toLoad) {
        return !toLoad.isEmpty() && inventory.getStackInSlot(0).isEmpty();
    }

    @Override
    public void load(ItemStack toLoad) {
        inventory.setStackInSlot(0, toLoad.copy());
        setChanged();
    }

    @Override
    public boolean canUnload() {
        return !inventory.getStackInSlot(0).isEmpty();
    }

    @Override
    public ItemStack provideNext() {
        return inventory.getStackInSlot(0);
    }

    @Override
    public void unload() {
        inventory.setStackInSlot(0, ItemStack.EMPTY);
        setChanged();
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeDouble(this.lastFluxQuantity);
        buf.writeDouble(this.lastFluxRatio);
        buf.writeBoolean(this.hasRod);
        buf.writeInt(this.rodColor);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        this.lastFluxQuantity = buf.readDouble();
        this.lastFluxRatio = buf.readDouble();
        this.hasRod = buf.readBoolean();
        this.rodColor = buf.readInt();
    }

    @Override
    public String[] getFunctionInfo() {
        // CE :554-563
        return new String[]{
                PREFIX_VALUE + "columnheat",
                PREFIX_VALUE + "rodheat",
                PREFIX_VALUE + "depletion",
                PREFIX_VALUE + "xenon",
                PREFIX_VALUE + "fastflux",
                PREFIX_VALUE + "slowflux",
                PREFIX_VALUE + "flux"
        };
    }

    @Override
    public String provideRORValue(String name) {
        // CE :567-577
        if ((PREFIX_VALUE + "columnheat").equals(name)) return "" + (int) this.heat;
        if (inventory.getStackInSlot(0).getItem() instanceof ItemRBMKRod) {
            if ((PREFIX_VALUE + "rodheat").equals(name)) return "" + (int) ItemRBMKRod.getHullHeat(inventory.getStackInSlot(0));
            if ((PREFIX_VALUE + "depletion").equals(name)) return "" + (int) (100 - ItemRBMKRod.getEnrichment(inventory.getStackInSlot(0)) * 100);
            if ((PREFIX_VALUE + "xenon").equals(name)) return "" + (int) (ItemRBMKRod.getPoison(inventory.getStackInSlot(0)));
        }
        if ((PREFIX_VALUE + "fastflux").equals(name)) return "" + (int) (lastFluxQuantity * lastFluxRatio);
        if ((PREFIX_VALUE + "slowflux").equals(name)) return "" + (int) (lastFluxQuantity * (1 - lastFluxRatio));
        if ((PREFIX_VALUE + "flux").equals(name)) return "" + ((int) (lastFluxQuantity * lastFluxRatio) + (int) (lastFluxQuantity * (1 - lastFluxRatio)));
        return null;
    }
}
