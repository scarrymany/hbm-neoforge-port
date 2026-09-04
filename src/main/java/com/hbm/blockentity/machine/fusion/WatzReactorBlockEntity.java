package com.hbm.blockentity.machine.fusion;

import com.hbm.api.fluidmk2.IFluidStandardTransceiverMK2;
import com.hbm.api.redstoneoverradio.IRORValueProvider;
import com.hbm.blockentity.IPersistentNBT;
import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.MachineBaseBlockEntity;
import com.hbm.blocks.BlockDummyable;
import com.hbm.entity.projectile.EntityShrapnel;
import com.hbm.handler.radiation.ChunkRadiationManager;
import com.hbm.main.AdvancementManager;
import com.hbm.main.MainRegistry;
import com.hbm.inventory.container.machine.fusion.WatzReactorMenu;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.fluid.trait.FT_Heatable;
import com.hbm.inventory.fluid.trait.FT_Heatable.HeatingStep;
import com.hbm.items.machine.ItemWatzPellet;
import com.hbm.items.machine.ItemWatzPellet.EnumWatzType;
import com.hbm.items.machine.MachineItems;
import com.hbm.lib.DirPos;
import com.hbm.lib.HBMSoundHandler;
import com.hbm.util.Compat;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Ported from CE's {@code TileEntityWatz}: a vertically-stacked (3-block-tall segments) liquid/
 * pellet breeder reactor. Each segment ({@code docs/phase2/machine_fusion_watz.md}) is its own
 * {@link com.hbm.blocks.BlockDummyable} multiblock core holding 24 {@link ItemWatzPellet} slots;
 * segments share three {@link FluidTankNTM} pools (coolant cold/hot, {@code WATZ} mud/waste)
 * top-to-bottom every tick, and the reaction math (base flux, burn/absorb functions, heat emission,
 * mud byproduct) is entirely keyed off {@link ItemWatzPellet.EnumWatzType} via
 * {@link ItemWatzPellet#computeBurn}/{@link ItemWatzPellet#computeAbsorb} - CE's own math, just
 * relocated behind those two accessors (see that class's javadoc for why).
 *
 * <h2>Simplifications versus CE (documented, not accidental)</h2>
 * <ul>
 *   <li><b>Multiblock shell</b>: CE validates a hollow lattice of three distinct sub-block types
 *   ({@code watz_element}/{@code watz_cooler}/{@code watz_casing}) per segment. This port's
 *   {@link com.hbm.blocks.machine.fusion.WatzReactorBlock} uses the shared uniform-casing
 *   {@link BlockDummyable} box contract instead (same simplification {@code IcfReactorBlock} makes,
 *   see that class's javadoc) - a real, working per-segment multiblock, just one casing block type
 *   instead of three.</li>
 *   <li><b>Watz Pump / redstone control</b>: CE gates {@code turnedOn} on a dedicated
 *   {@code watz_pump} block directly above the segment plus redstone two blocks further up; no
 *   {@code watz_pump} block exists in this port yet (out of this pass's scope - it is a plain
 *   redstone-relay block, not reactor logic). This reactor instead reads a redstone signal directly
 *   at the position one block above the top segment's own top slice - same "redstone turns the
 *   reactor on" mechanic, without the intermediate pump block. Restoring the pump block is a
 *   follow-up for whoever owns that decorative-block family.</li>
 *   <li><b>Meltdown aftermath</b>: Exact CE {@code TileEntityWatz.java:185-199}/{@code :527-585}
 *   — roof air-clear, {@code EntityShrapnel.setWatz}, {@code watz_element}/{@code watz_cooler}/
 *   {@code watz_casing} rubble, {@code achWatzBoom}, {@code incrementRad 1000}. {@code mud_block}
 *   (unregistered) and RBMKMush AuxParticle stay skipped.</li>
 * </ul>
 */
public class WatzReactorBlockEntity extends MachineBaseBlockEntity
        implements ITickableBE, IFluidStandardTransceiverMK2, IPersistentNBT, MenuProvider,
        IRORValueProvider {

    public static final int SEGMENT_HEIGHT = 3;
    private static final int PELLET_SLOTS = 24;
    private static final double COOLING_FACTOR = 0.2D;
    private static final int[] ACCESSIBLE = buildAccessible();

    private static int[] buildAccessible() {
        int[] slots = new int[PELLET_SLOTS];
        for (int i = 0; i < PELLET_SLOTS; i++) slots[i] = i;
        return slots;
    }

    public final FluidTankNTM[] tanks = new FluidTankNTM[3];
    /** Aggregate pool shared across the whole vertical stack this tick - assigned by the top segment, read-only elsewhere. */
    private FluidTankNTM[] sharedTanks;

    public double heat;
    private double fluxLastBase;
    private double fluxLastReaction;
    public double fluxDisplay;
    public boolean isOn;

    public WatzReactorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, PELLET_SLOTS, true, false);
        tanks[0] = new FluidTankNTM(Fluids.COOLANT, 64_000).withOwner(this);
        tanks[1] = new FluidTankNTM(Fluids.COOLANT_HOT, 64_000).withOwner(this);
        tanks[2] = new FluidTankNTM(Fluids.WATZ, 64_000).withOwner(this);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.watz");
    }

    @Override
    public void updateEntity() {
        if (level == null || level.isClientSide) return;
        if (isLockedByAbove()) return;

        boolean turnedOn = level.hasNeighborSignal(worldPosition.above(SEGMENT_HEIGHT));

        List<WatzReactorBlockEntity> segments = new ArrayList<>();
        segments.add(this);
        for (int y = worldPosition.getY() - SEGMENT_HEIGHT; y >= level.getMinBuildHeight(); y -= SEGMENT_HEIGHT) {
            BlockEntity tile = Compat.getBlockEntityStandard(level, new BlockPos(worldPosition.getX(), y, worldPosition.getZ()));
            if (tile instanceof WatzReactorBlockEntity segment) {
                segments.add(segment);
            } else {
                break;
            }
        }
        subscribeToTop();

        this.sharedTanks = new FluidTankNTM[3];
        for (int i = 0; i < 3; i++) this.sharedTanks[i] = new FluidTankNTM(tanks[i].getTankType(), 0).withOwner(this);

        for (WatzReactorBlockEntity segment : segments) {
            segment.setupCoolant();
            for (int i = 0; i < 3; i++) {
                this.sharedTanks[i].changeTankSize(this.sharedTanks[i].getMaxFill() + segment.tanks[i].getMaxFill());
                this.sharedTanks[i].setFill(this.sharedTanks[i].getFill() + segment.tanks[i].getFill());
            }
        }

        for (int i = segments.size() - 1; i >= 0; i--) {
            segments.get(i).updateCoolant(this.sharedTanks);
        }

        int mudOverflow = this.updateReaction(null, this.sharedTanks, turnedOn);
        for (int i = 1; i < segments.size(); i++) {
            WatzReactorBlockEntity segment = segments.get(i);
            WatzReactorBlockEntity above = segments.get(i - 1);
            mudOverflow += segment.updateReaction(above, this.sharedTanks, turnedOn);
        }

        for (WatzReactorBlockEntity segment : segments) {
            segment.sharedTanks = this.sharedTanks;
            segment.isOn = turnedOn;
            segment.fluxDisplay = segment.fluxLastBase + segment.fluxLastReaction;
            segment.dataChanged();
            segment.networkPackMK2(25);
            segment.heat *= 0.99D;
        }

        for (int i = segments.size() - 1; i >= 0; i--) {
            WatzReactorBlockEntity segment = segments.get(i);
            for (int j = 0; j < 3; j++) {
                int min = Math.min(segment.tanks[j].getMaxFill(), sharedTanks[j].getFill());
                sharedTanks[j].setFill(sharedTanks[j].getFill() - min);
                segment.tanks[j].setFill(min);
            }
        }

        segments.get(segments.size() - 1).sendOutBottom();

        if (sharedTanks[2].getFill() > 0 || mudOverflow > 0) {
            meltdown();
        }
    }

    private boolean isLockedByAbove() {
        return Compat.getBlockEntityStandard(level, worldPosition.above(SEGMENT_HEIGHT)) instanceof WatzReactorBlockEntity;
    }

    private void setupCoolant() {
        tanks[0].setTankType(Fluids.COOLANT);
        tanks[1].setTankType(tanks[0].getTankType().getTrait(FT_Heatable.class).getFirstStep().typeProduced);
    }

    private void updateCoolant(FluidTankNTM[] shared) {
        double heatToUse = this.heat * COOLING_FACTOR;

        FT_Heatable trait = shared[0].getTankType().getTrait(FT_Heatable.class);
        HeatingStep step = trait.getFirstStep();

        int heatCycles = (int) (heatToUse / step.heatReq);
        int coolCycles = shared[0].getFill() / step.amountReq;
        int hotCycles = (shared[1].getMaxFill() - shared[1].getFill()) / step.amountProduced;

        int cycles = Math.min(heatCycles, Math.min(hotCycles, coolCycles));
        this.heat -= (double) cycles * step.heatReq;
        shared[0].setFill(shared[0].getFill() - cycles * step.amountReq);
        shared[1].setFill(shared[1].getFill() + cycles * step.amountProduced);
    }

    private int updateReaction(@Nullable WatzReactorBlockEntity above, FluidTankNTM[] shared, boolean turnedOn) {
        int overflow = 0;

        if (turnedOn) {
            List<ItemStack> pellets = new ArrayList<>();
            for (int i = 0; i < PELLET_SLOTS; i++) {
                ItemStack stack = inventory.getStackInSlot(i);
                if (!stack.isEmpty() && stack.getItem() instanceof ItemWatzPellet pellet && !pellet.isDepleted()) {
                    pellets.add(stack);
                }
            }

            double baseFlux = 0D;
            for (ItemStack stack : pellets) {
                baseFlux += ((ItemWatzPellet) stack.getItem()).getType().passive;
            }

            double inputFlux = baseFlux + fluxLastReaction;
            double addedFlux = 0D;
            double addedHeat = 0D;

            for (ItemStack stack : pellets) {
                EnumWatzType type = ((ItemWatzPellet) stack.getItem()).getType();
                double burn = ItemWatzPellet.computeBurn(stack, heat, inputFlux);
                if (burn != 0D) {
                    ItemWatzPellet.setYield(stack, ItemWatzPellet.getYield(stack) - burn);
                    addedFlux += burn;
                    addedHeat += type.heatEmission * burn;
                    overflow += addMud(shared[2], (int) Math.round(type.mudContent * burn));
                }
            }

            for (ItemStack stack : pellets) {
                EnumWatzType type = ((ItemWatzPellet) stack.getItem()).getType();
                double absorb = ItemWatzPellet.computeAbsorb(stack, baseFlux + fluxLastReaction);
                if (absorb != 0D) {
                    addedHeat += absorb;
                    ItemWatzPellet.setYield(stack, ItemWatzPellet.getYield(stack) - absorb);
                    overflow += addMud(shared[2], (int) Math.round(type.mudContent * absorb));
                }
            }

            this.heat += addedHeat;
            this.fluxLastBase = baseFlux;
            this.fluxLastReaction = addedFlux;
        } else {
            this.fluxLastBase = 0;
            this.fluxLastReaction = 0;
        }

        for (int i = 0; i < PELLET_SLOTS; i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (!stack.isEmpty() && stack.getItem() instanceof ItemWatzPellet pellet && !pellet.isDepleted()
                    && ItemWatzPellet.getEnrichment(stack) <= 0) {
                var depletedItem = MachineItems.WATZ_PELLET_DEPLETED.get(pellet.getType());
                inventory.setStackInSlot(i, depletedItem != null ? new ItemStack(depletedItem.get()) : ItemStack.EMPTY);
            }
        }

        if (above != null) {
            for (int i = 0; i < PELLET_SLOTS; i++) {
                ItemStack stackBottom = inventory.getStackInSlot(i);
                ItemStack stackTop = above.inventory.getStackInSlot(i);

                if (stackBottom.isEmpty() && !stackTop.isEmpty()) {
                    inventory.setStackInSlot(i, stackTop.copy());
                    above.inventory.setStackInSlot(i, ItemStack.EMPTY);
                } else if (!stackBottom.isEmpty() && stackBottom.getItem() instanceof ItemWatzPellet bp && !bp.isDepleted()
                        && !stackTop.isEmpty() && stackTop.getItem() instanceof ItemWatzPellet tp && tp.isDepleted()) {
                    ItemStack buf = stackTop.copy();
                    above.inventory.setStackInSlot(i, stackBottom.copy());
                    inventory.setStackInSlot(i, buf);
                }
            }
        }

        return overflow;
    }

    private static int addMud(FluidTankNTM tank, int amount) {
        if (amount <= 0) return 0;
        int space = tank.getMaxFill() - tank.getFill();
        tank.setFill(tank.getFill() + amount);
        return Math.max(0, amount - space);
    }

    private void subscribeToTop() {
        for (DirPos dp : getConPos()) {
            if (dp.getDir() == Direction.UP) trySubscribe(tanks[0].getTankType(), level, dp);
        }
    }

    private void sendOutBottom() {
        for (DirPos dp : getConPos()) {
            if (dp.getDir() != Direction.DOWN) continue;
            if (tanks[1].getFill() > 0) tryProvide(tanks[1], level, dp);
            if (tanks[2].getFill() > 0) tryProvide(tanks[2], level, dp);
        }
    }

    public DirPos[] getConPos() {
        BlockPos p = worldPosition;
        return new DirPos[]{
                new DirPos(p.getX(), p.getY() + SEGMENT_HEIGHT, p.getZ(), Direction.UP),
                new DirPos(p.getX() + 2, p.getY() + SEGMENT_HEIGHT, p.getZ(), Direction.UP),
                new DirPos(p.getX() - 2, p.getY() + SEGMENT_HEIGHT, p.getZ(), Direction.UP),
                new DirPos(p.getX(), p.getY() + SEGMENT_HEIGHT, p.getZ() + 2, Direction.UP),
                new DirPos(p.getX(), p.getY() + SEGMENT_HEIGHT, p.getZ() - 2, Direction.UP),
                new DirPos(p.getX(), p.getY() - 1, p.getZ(), Direction.DOWN),
                new DirPos(p.getX() + 2, p.getY() - 1, p.getZ(), Direction.DOWN),
                new DirPos(p.getX() - 2, p.getY() - 1, p.getZ(), Direction.DOWN),
                new DirPos(p.getX(), p.getY() - 1, p.getZ() + 2, Direction.DOWN),
                new DirPos(p.getX(), p.getY() - 1, p.getZ() - 2, Direction.DOWN)
        };
    }

    /**
     * Exact CE {@code TileEntityWatz.java:185-199} + {@code disassemble :527-585}.
     * {@code mud_block} writes become air (unregistered). RBMKMush stays skipped.
     */
    private void meltdown() {
        if (level == null) return;
        BlockPos origin = worldPosition;
        for (int x = -3; x <= 3; x++) {
            for (int y = 3; y < 6; y++) {
                for (int z = -3; z <= 3; z++) {
                    level.setBlock(origin.offset(x, y, z), Blocks.AIR.defaultBlockState(), 3);
                }
            }
        }
        disassemble();
        ChunkRadiationManager.proxy.incrementRad(level, origin.above(), 1_000F);
        level.playSound(null, origin.getX() + 0.5, origin.getY() + 2, origin.getZ() + 0.5,
                HBMSoundHandler.rbmk_explosion.get(), SoundSource.BLOCKS, 50.0F, 1.0F);
    }

    private void disassemble() {
        if (level == null) return;
        BlockPos origin = worldPosition;
        int count = 20;
        var rand = level.random;
        for (int i = 0; i < count * 5; i++) {
            EntityShrapnel shrapnel = new EntityShrapnel(level, origin.getX() + 0.5, origin.getY() + 3, origin.getZ() + 0.5);
            double motionY = ((rand.nextFloat() * 0.5) + 0.5) * (1 + (count / (15.0F + rand.nextInt(21)))) + (rand.nextFloat() / 50 * count);
            double motionX = rand.nextGaussian() * 1 * (1 + (count / 100.0F));
            double motionZ = rand.nextGaussian() * 1 * (1 + (count / 100.0F));
            shrapnel.setDeltaMovement(motionX, motionY, motionZ);
            shrapnel.setWatz(true);
            level.addFreshEntity(shrapnel);
        }

        // CE :543-545 mud_block on core column — unregistered, air.
        level.setBlock(origin, Blocks.AIR.defaultBlockState(), 3);
        level.setBlock(origin.above(), Blocks.AIR.defaultBlockState(), 3);
        level.setBlock(origin.above(2), Blocks.AIR.defaultBlockState(), 3);

        Block element = watzBlock("watz_element");
        Block cooler = watzBlock("watz_cooler");
        Block casing = watzBlock("watz_casing");
        setBrokenColumn(0, element, 1, 0);
        setBrokenColumn(0, element, 2, 0);
        setBrokenColumn(0, element, 0, 1);
        setBrokenColumn(0, element, 0, 2);
        setBrokenColumn(0, element, -1, 0);
        setBrokenColumn(0, element, -2, 0);
        setBrokenColumn(0, element, 0, -1);
        setBrokenColumn(0, element, 0, -2);
        setBrokenColumn(0, element, 1, 1);
        setBrokenColumn(0, element, 1, -1);
        setBrokenColumn(0, element, -1, 1);
        setBrokenColumn(0, element, -1, -1);
        setBrokenColumn(0, cooler, 2, 1);
        setBrokenColumn(0, cooler, 2, -1);
        setBrokenColumn(0, cooler, 1, 2);
        setBrokenColumn(0, cooler, -1, 2);
        setBrokenColumn(0, cooler, -2, 1);
        setBrokenColumn(0, cooler, -2, -1);
        setBrokenColumn(0, cooler, 1, -2);
        setBrokenColumn(0, cooler, -1, -2);

        for (int j = -1; j < 2; j++) {
            setBrokenColumn(1, casing, 3, j);
            setBrokenColumn(1, casing, j, 3);
            setBrokenColumn(1, casing, -3, j);
            setBrokenColumn(1, casing, j, -3);
        }
        setBrokenColumn(1, casing, 2, 2);
        setBrokenColumn(1, casing, 2, -2);
        setBrokenColumn(1, casing, -2, 2);
        setBrokenColumn(1, casing, -2, -2);

        double cx = origin.getX() + 0.5;
        double cy = origin.getY() + 0.5;
        double cz = origin.getZ() + 0.5;
        AABB box = new AABB(cx, cy, cz, cx, cy, cz).inflate(50, 50, 50);
        for (Player player : level.getEntitiesOfClass(Player.class, box)) {
            if (player instanceof ServerPlayer serverPlayer) {
                AdvancementManager.grantAchievement(serverPlayer, AdvancementManager.achWatzBoom);
            }
        }
    }

    private void setBrokenColumn(int minHeight, Block block, int x, int z) {
        if (level == null || block == Blocks.AIR) return;
        int height = minHeight + level.random.nextInt(3 - minHeight);
        BlockPos origin = worldPosition;
        for (int i = 0; i < 3; i++) {
            BlockPos p = origin.offset(x, i, z);
            if (i <= height) {
                level.setBlock(p, block.defaultBlockState(), 3);
            } else {
                // CE :596 mud_block — unregistered, air.
                level.setBlock(p, Blocks.AIR.defaultBlockState(), 3);
            }
        }
    }

    private static Block watzBlock(String name) {
        return BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, name));
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        return stack.getItem() instanceof ItemWatzPellet pellet && !pellet.isDepleted();
    }

    @Override
    public int[] getAccessibleSlotsFromSide(Direction side) {
        return ACCESSIBLE;
    }

    @Override
    public boolean canExtractItem(int slot, ItemStack stack, int amount) {
        return !(stack.getItem() instanceof ItemWatzPellet pellet) || pellet.isDepleted();
    }

    @Override
    public List<FluidTankNTM> getAllTanks() {
        return List.of(tanks);
    }

    @Override
    public List<FluidTankNTM> getSendingTanks() {
        return List.of(tanks[1], tanks[2]);
    }

    @Override
    public List<FluidTankNTM> getReceivingTanks() {
        return List.of(tanks[0]);
    }

    @Override
    public boolean canConnect(FluidType type, Direction dir) {
        return dir != null;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        for (int i = 0; i < 3; i++) tanks[i].writeToNBT(tag, "t" + i);
        tag.putDouble("heat", heat);
        tag.putDouble("fluxBase", fluxLastBase);
        tag.putDouble("fluxReaction", fluxLastReaction);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        for (int i = 0; i < 3; i++) tanks[i].readFromNBT(tag, "t" + i);
        this.heat = tag.getDouble("heat");
        this.fluxLastBase = tag.getDouble("fluxBase");
        this.fluxLastReaction = tag.getDouble("fluxReaction");
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeDouble(heat);
        buf.writeBoolean(isOn);
        buf.writeDouble(fluxLastBase + fluxLastReaction);
        for (FluidTankNTM tank : tanks) tank.serialize(buf);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        this.heat = buf.readDouble();
        this.isOn = buf.readBoolean();
        this.fluxDisplay = buf.readDouble();
        for (FluidTankNTM tank : tanks) tank.deserialize(buf);
    }

    @Override
    public void writeNBT(CompoundTag nbt) {
        for (int i = 0; i < 3; i++) tanks[i].writeToNBT(nbt, "t" + i);
        nbt.putDouble("heat", heat);
    }

    @Override
    public void readNBT(CompoundTag nbt) {
        for (int i = 0; i < 3; i++) tanks[i].readFromNBT(nbt, "t" + i);
        this.heat = nbt.getDouble("heat");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new WatzReactorMenu(containerId, playerInventory, this);
    }

    @Override
    public String[] getFunctionInfo() {
        // CE TileEntityWatz.java:703-713
        return new String[]{
                PREFIX_VALUE + "heat",
                PREFIX_VALUE + "flux",
                PREFIX_VALUE + "mud",
                PREFIX_VALUE + "coolant_hot",
                PREFIX_VALUE + "coolant_cold",
        };
    }

    @Override
    public String provideRORValue(String name) {
        // CE :717-723
        if ((PREFIX_VALUE + "heat").equals(name)) return "" + this.heat;
        if ((PREFIX_VALUE + "flux").equals(name)) return "" + (int) (this.fluxLastBase + this.fluxLastReaction);
        if ((PREFIX_VALUE + "mud").equals(name)) return "" + this.tanks[2].getFill();
        if ((PREFIX_VALUE + "coolant_hot").equals(name)) return "" + this.tanks[1].getFill();
        if ((PREFIX_VALUE + "coolant_cold").equals(name)) return "" + this.tanks[0].getFill();
        return null;
    }
}
