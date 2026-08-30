package com.hbm.blockentity.machine.oil;

import com.hbm.api.energymk2.IEnergyReceiverMK2;
import com.hbm.api.fluidmk2.IFluidStandardTransceiverMK2;
import com.hbm.blockentity.IPersistentNBT;
import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.MachineBaseBlockEntity;
import com.hbm.inventory.UpgradeManagerNT;
import com.hbm.inventory.container.machine.oil.MachineOilWellMenu;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.items.machine.ItemMachineUpgrade;
import com.hbm.items.machine.ItemMachineUpgrade.UpgradeType;
import com.hbm.lib.DirPos;
import com.hbm.lib.Library;
import com.hbm.main.MainRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Ported from CE's {@code com.hbm.tileentity.machine.oil.TileEntityOilDrillBase} (334 lines, read in
 * full) - the shared extraction engine for the derrick ({@link MachineOilWellBlockEntity}), pumpjack
 * ({@link MachinePumpjackBlockEntity}) and fracking tower ({@link MachineFrackingTowerBlockEntity}).
 * Per {@code docs/phase2/oil_production_chain.md}, all three concrete extractors are
 * {@link com.hbm.blocks.BlockDummyable} multiblocks built on the now-landed
 * {@code MultiblockHandlerXR}/{@code MachineBaseBlockEntity} foundation.
 *
 * <h2>World-gen dependency - the exact "shell now" boundary</h2>
 * {@link #trySuck(int)}/{@link #doSuck(BlockPos)} scan the world for {@code ore_oil}/
 * {@code ore_oil_empty}/{@code ore_bedrock_oil} blocks exactly like CE - this is a live, working
 * mechanic, not a no-op stub, and needs no future rewrite. What actually gates it is Phase 4 scope
 * per the research report's "Headline finding"/"Deferred scope #6": nothing in this port's world
 * generation ever places {@code ore_oil}/{@code ore_bedrock_oil} in a freshly generated world yet
 * (that is CE's {@code BedrockOilDeposit}/{@code OilBubble}/{@code OilSandBubble}, explicitly Phase 4
 * scope, not implemented here). So today this drill sits idle (creeping down one {@code oil_pipe}
 * layer at a time via {@link #tryDrill(int)}, per CE's own design, until it hits
 * {@link #getDrillDepth()}) on any world with no manually-placed oil ore - <b>the extraction mechanic
 * itself needs zero changes once Phase 4's world-gen (or a future deposit/reservoir
 * {@code SavedData}, should that replace CE's block-scan approach instead) lands</b>; it will simply
 * start finding real deposits. {@code ore_oil}/{@code oil_pipe} are already registered elsewhere in
 * this port (Phase 0/1); {@code ore_oil_empty}/{@code ore_bedrock_oil} are registered by this same
 * pass's {@link com.hbm.blocks.machine.OilChainBlocks} (a cheap, non-Phase-4 gap the research report
 * explicitly calls out as safe to close now - see its Deferred scope #7).
 *
 * <h2>Scope trims vs. CE</h2>
 * <ul>
 *   <li><b>No item-container fluid loading</b> (CE's {@code tanks[0].unloadTank(1,2,inventory)}/
 *   {@code tanks[1].unloadTank(3,4,inventory)}, i.e. draining a tank into an empty canister/gastank
 *   item): the same pre-existing, cross-cutting gap {@link FluidTankNTM}'s own javadoc and
 *   {@code MachineDieselBlockEntity}'s javadoc already document (no {@code FluidContainerRegistry}
 *   equivalent exists in this port yet). Fluid leaves purely over the pipe network. The inventory is
 *   therefore renumbered: slot 0 battery, slots 1-3 upgrades (CE: slot 0 battery, 1-4 canister
 *   in/out pairs, 5-6 upgrades, matching {@code TileEntityOilDrillBase}'s 8-slot layout).</li>
 *   <li><b>No {@code UpgradeManagerNT}-triggered "plug-in" sound effect</b> (CE's
 *   {@code SoundUtil.playUpgradePlugSound} call from a custom {@code setStackInSlot} override) and no
 *   {@code IUpgradeInfoProvider} GUI-tooltip integration - both purely cosmetic, dropped per
 *   {@link UpgradeManagerNT}'s own javadoc precedent.</li>
 *   <li><b>No uranium/asbestos {@code gas_radon_dense}/{@code gas_asbestos} drill side-effect</b>
 *   (CE's {@code onDrill} OreDictionary branch in the two concrete subclasses) - both blocks are
 *   confirmed absent from this port; the research report's own "Open questions" flags this as safe to
 *   drop rather than block the whole TE port on two decorative flavor blocks.</li>
 *   <li><b>No {@code TileEntityProxyCombo} at non-core dummy positions</b> (CE's pumpjack/fracking
 *   tower use one; the derrick already uses {@code null} even in CE). {@code TileEntityProxyCombo} is
 *   not ported (confirmed absent, {@code docs/phase2/oil_production_chain.md} Deferred scope #2) - a
 *   real, separate "shell now" boundary of its own: <b>TODO(phase2-followup)</b> once a proxy-combo
 *   block entity exists, {@code MachinePumpjackBlock}/{@code MachineFrackingTowerBlock}/
 *   {@code MachineRefineryBlock}'s {@code newBlockEntity} can hand it out for {@code meta 6-11}
 *   positions so external pipes can dock on a dummy face, not just the core's own faces - every
 *   extractor's {@link #getConPos()} already only ever targets core-relative absolute positions, so
 *   this omission does not break the primary fluid-output path today, only dummy-face docking
 *   convenience.</li>
 * </ul>
 */
public abstract class OilDrillBaseBlockEntity extends MachineBaseBlockEntity
        implements IEnergyReceiverMK2, IFluidStandardTransceiverMK2, ITickableBE, IPersistentNBT, MenuProvider {

    protected static final int BATTERY_SLOT = 0;
    protected static final int UPGRADE_SLOT_START = 1;
    protected static final int UPGRADE_SLOT_END = 3;

    private static final Map<UpgradeType, Integer> VALID_UPGRADES = new EnumMap<>(UpgradeType.class);

    static {
        VALID_UPGRADES.put(UpgradeType.SPEED, 3);
        VALID_UPGRADES.put(UpgradeType.POWER, 3);
        VALID_UPGRADES.put(UpgradeType.AFTERBURN, 3);
        VALID_UPGRADES.put(UpgradeType.OVERDRIVE, 3);
    }

    private final UpgradeManagerNT upgradeManager = new UpgradeManagerNT(VALID_UPGRADES);
    private final Set<BlockPos> processed = new HashSet<>();

    /** Index 0 = OIL, index 1 = GAS, any further tank (fracking's FRACKSOL) is subclass-appended. */
    public final List<FluidTankNTM> tanks = new ArrayList<>();

    public long power;
    public int indicator = 0;
    protected int speedLevel;
    protected int energyLevel;
    protected int overLevel = 1;

    // Lazily resolved (registry isn't populated yet at BlockEntity class-load time) - see class javadoc.
    private static Block oreOilCache;
    private static Block oreOilEmptyCache;
    private static Block oreBedrockOilCache;
    private static Block oilPipeCache;

    protected OilDrillBaseBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, 4, true, true);
        tanks.add(new FluidTankNTM(Fluids.OIL, 64_000).withOwner(this));
        tanks.add(new FluidTankNTM(Fluids.GAS, 64_000).withOwner(this));
    }

    private static Block resolve(String path) {
        return BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, path));
    }

    protected static Block oreOil() {
        if (oreOilCache == null) oreOilCache = resolve("ore_oil");
        return oreOilCache;
    }

    protected static Block oreOilEmpty() {
        if (oreOilEmptyCache == null) oreOilEmptyCache = resolve("ore_oil_empty");
        return oreOilEmptyCache;
    }

    protected static Block oreBedrockOil() {
        if (oreBedrockOilCache == null) oreBedrockOilCache = resolve("ore_bedrock_oil");
        return oreBedrockOilCache;
    }

    protected static Block oilPipe() {
        if (oilPipeCache == null) oilPipeCache = resolve("oil_pipe");
        return oilPipeCache;
    }

    public FluidTankNTM getOilTank() {
        return tanks.get(0);
    }

    public FluidTankNTM getGasTank() {
        return tanks.get(1);
    }

    public abstract long getMaxPower();

    public abstract int getPowerReq();

    public abstract int getDelay();

    public abstract DirPos[] getConPos();

    /** Called with the block just above/at each dug-through layer, before it's replaced with {@code oil_pipe}. No-op by default (see class javadoc - flavor side-effects dropped). */
    public void onDrill(int y) {
    }

    /** Called when a full deposit ({@code ore_oil}/{@code ore_bedrock_oil}) is actually found and consumed. */
    public abstract void onSuck(BlockPos pos);

    public int getDrillDepth() {
        return 5;
    }

    public boolean canPump() {
        return true;
    }

    public int getPowerReqEff() {
        int req = getPowerReq();
        return (req + (req / 4 * speedLevel) - (req / 4 * energyLevel)) * overLevel;
    }

    public int getDelayEff() {
        int delay = getDelay();
        return Math.max((delay - (delay / 4 * speedLevel) + (delay / 10 * energyLevel)) / overLevel, 1);
    }

    @Override
    public void updateEntity() {
        if (level == null || level.isClientSide) return;

        for (DirPos dp : getConPos()) {
            trySubscribe(level, dp);
            trySubscribeFluids(dp);
        }

        upgradeManager.checkSlots(inventory, UPGRADE_SLOT_START, UPGRADE_SLOT_END);
        speedLevel = Math.min(upgradeManager.getLevel(UpgradeType.SPEED), 3);
        energyLevel = Math.min(upgradeManager.getLevel(UpgradeType.POWER), 3);
        overLevel = Math.min(upgradeManager.getLevel(UpgradeType.OVERDRIVE), 3) + 1;
        int abLevel = Math.min(upgradeManager.getLevel(UpgradeType.AFTERBURN), 3);

        int toBurn = Math.min(getGasTank().getFill(), abLevel * 10);
        if (toBurn > 0) {
            getGasTank().setFill(getGasTank().getFill() - toBurn);
            power = Math.min(getMaxPower(), power + toBurn * 5L);
        }

        power = Library.chargeTEFromItems(inventory, BATTERY_SLOT, power, getMaxPower());

        for (DirPos dp : getConPos()) {
            if (getOilTank().getFill() > 0) tryProvide(getOilTank(), level, dp);
            if (getGasTank().getFill() > 0) tryProvide(getGasTank(), level, dp);
        }

        if (power >= getPowerReqEff() && getOilTank().getFill() < getOilTank().getMaxFill()
                && getGasTank().getFill() < getGasTank().getMaxFill()) {
            power -= getPowerReqEff();

            if (level.getGameTime() % getDelayEff() == 0) {
                indicator = 0;

                for (int y = worldPosition.getY() - 1; y >= getDrillDepth(); y--) {
                    BlockPos p = new BlockPos(worldPosition.getX(), y, worldPosition.getZ());

                    if (level.getBlockState(p).getBlock() != oilPipe()) {
                        if (!trySuck(y)) tryDrill(y);
                        break;
                    }

                    if (y == getDrillDepth()) indicator = 1;
                }
            }
        } else {
            indicator = 2;
        }

        dataChanged();
        networkPackMK2(50);
    }

    /** Subclass hook - the fracking tower additionally subscribes its FRACKSOL input tank here. */
    protected void trySubscribeFluids(DirPos dp) {
    }

    public void tryDrill(int y) {
        BlockPos posD = new BlockPos(worldPosition.getX(), y, worldPosition.getZ());
        BlockState state = level.getBlockState(posD);

        if (state.getBlock().getExplosionResistance() < 1000) {
            onDrill(y);
            level.setBlock(posD, oilPipe().defaultBlockState(), 3);
        } else {
            indicator = 2;
        }
    }

    public boolean trySuck(int y) {
        BlockPos startPos = new BlockPos(worldPosition.getX(), y, worldPosition.getZ());
        Block startBlock = level.getBlockState(startPos).getBlock();
        if (!canSuckBlock(startBlock)) return false;
        if (!canPump()) return true;

        Queue<BlockPos> queue = new ArrayDeque<>();
        processed.clear();
        queue.offer(startPos);
        processed.add(startPos);

        int nodesVisited = 0;
        while (!queue.isEmpty() && nodesVisited < 256) {
            BlockPos currentPos = queue.poll();
            nodesVisited++;
            Block currentBlock = level.getBlockState(currentPos).getBlock();

            if (currentBlock == oreOil() || currentBlock == oreBedrockOil()) {
                doSuck(currentPos);
                return true;
            }
            if (currentBlock != oreOilEmpty()) continue;

            List<Direction> dirs = new ArrayList<>(Arrays.asList(Direction.values()));
            Collections.shuffle(dirs, ThreadLocalRandom.current());
            for (Direction dir : dirs) {
                BlockPos neighborPos = currentPos.relative(dir);
                if (!processed.contains(neighborPos) && canSuckBlock(level.getBlockState(neighborPos).getBlock())) {
                    processed.add(neighborPos);
                    queue.offer(neighborPos);
                }
            }
        }
        return false;
    }

    public boolean canSuckBlock(Block b) {
        return b == oreOil() || b == oreOilEmpty();
    }

    public void doSuck(BlockPos pos) {
        Block b = level.getBlockState(pos).getBlock();
        if (b == oreOil()) onSuck(pos);
    }

    @Override
    public boolean isItemValidForSlot(int i, ItemStack stack) {
        if (i == BATTERY_SLOT) return Library.isBattery(stack);
        if (i >= UPGRADE_SLOT_START && i <= UPGRADE_SLOT_END) {
            return stack.getItem() instanceof ItemMachineUpgrade;
        }
        return false;
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
    public List<FluidTankNTM> getSendingTanks() {
        return List.of(getOilTank(), getGasTank());
    }

    @Override
    public List<FluidTankNTM> getReceivingTanks() {
        return List.of();
    }

    @Override
    public List<FluidTankNTM> getAllTanks() {
        return tanks;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putLong("power", power);
        tag.putInt("indicator", indicator);
        for (int i = 0; i < tanks.size(); i++) tanks.get(i).writeToNBT(tag, "t" + i);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        power = tag.getLong("power");
        indicator = tag.getInt("indicator");
        for (int i = 0; i < tanks.size(); i++) tanks.get(i).readFromNBT(tag, "t" + i);
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeLong(power);
        buf.writeInt(indicator);
        for (FluidTankNTM tank : tanks) tank.serialize(buf);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        power = buf.readLong();
        indicator = buf.readInt();
        for (FluidTankNTM tank : tanks) tank.deserialize(buf);
    }

    @Override
    public void writeNBT(CompoundTag nbt) {
        boolean empty = power == 0;
        for (FluidTankNTM tank : tanks) if (tank.getFill() > 0) empty = false;

        if (!empty) {
            nbt.putLong("power", power);
            for (int i = 0; i < tanks.size(); i++) tanks.get(i).writeToNBT(nbt, "t" + i);
        }
    }

    @Override
    public void readNBT(CompoundTag nbt) {
        power = nbt.getLong("power");
        for (int i = 0; i < tanks.size(); i++) tanks.get(i).readFromNBT(nbt, "t" + i);
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new MachineOilWellMenu(containerId, playerInventory, this);
    }
}
