package com.hbm.blockentity.machine.dummyable;

import com.hbm.api.block.IDrillInteraction;
import com.hbm.api.block.IMiningDrill;
import com.hbm.api.energymk2.IEnergyReceiverMK2;
import com.hbm.api.fluidmk2.IFluidStandardSenderMK2;
import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.MachineBaseBlockEntity;
import com.hbm.blocks.gas.BlockGasBase;
import com.hbm.inventory.container.machine.dummyable.MiningLaserMenu;
import com.hbm.inventory.UpgradeManagerNT;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.recipes.CrystallizerRecipes;
import com.hbm.inventory.recipes.ProcessingRecipes;
import com.hbm.inventory.recipes.chem.CentrifugeRecipes;
import com.hbm.items.machine.ItemMachineUpgrade;
import com.hbm.items.machine.ItemMachineUpgrade.UpgradeType;
import com.hbm.lib.DirPos;
import com.hbm.lib.HBMSoundHandler;
import com.hbm.lib.Library;
import com.hbm.main.MainRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

/**
 * CE {@code TileEntityMachineMiningLaser} (673 lines). Silk touch is not in CE.
 * CE laser does not increment pollution (full-file read).
 * Exclusive processors: crystallizer/centrifuge Java tables + shredder JSON + vanilla smelting.
 * Nullifier uses CE {@code ItemMachineUpgrade.scrapItems}.
 * TODO(CE: RenderLaserMiner.java:18): TESR beam. Do not invent.
 */
public class MachineMiningLaserBlockEntity extends MachineBaseBlockEntity
        implements IEnergyReceiverMK2, IFluidStandardSenderMK2, IMiningDrill, ITickableBE, MenuProvider {

    public static final long MAX_POWER = 100_000_000L;
    public static final int CONSUMPTION = 10_000;

    /** CE {@code TileEntityMachineMiningLaser.getValidUpgrades} — SPEED/POWER/EFFECT 12, FORTUNE 3, OVERDRIVE 9. SCREAM is identity-checked separately. */
    private final UpgradeManagerNT upgradeManager = new UpgradeManagerNT(Map.of(
            UpgradeType.SPEED, 12,
            UpgradeType.POWER, 12,
            UpgradeType.EFFECT, 12,
            UpgradeType.FORTUNE, 3,
            UpgradeType.OVERDRIVE, 9
    ));

    public final FluidTankNTM tank;
    public long power;
    public boolean isOn;
    public boolean redstonePowered;
    public int targetX;
    public int targetY;
    public int targetZ;
    public int lastTargetX;
    public int lastTargetY;
    public int lastTargetZ;
    public boolean beam;
    private double breakProgress;
    private double clientBreakProgress;

    public MachineMiningLaserBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, 30, true, true);
        this.tank = new FluidTankNTM(Fluids.OIL, 64_000).withOwner(this);
    }

    @Override
    protected ItemStackHandler getNewInventory(int scount, int slotlimit) {
        return new ItemStackHandler(scount) {
            @Override
            protected void onContentsChanged(int slot) {
                super.onContentsChanged(slot);
                setChanged();
            }

            @Override
            public int getSlotLimit(int slot) {
                return slotlimit;
            }

            @Override
            public void setStackInSlot(int slot, @NotNull ItemStack stack) {
                super.setStackInSlot(slot, stack);
                if (level != null && !stack.isEmpty() && slot >= 1 && slot <= 8
                        && stack.getItem() instanceof ItemMachineUpgrade) {
                    level.playSound(null, worldPosition.getX() + 0.5, worldPosition.getY() + 1.5, worldPosition.getZ() + 0.5,
                            HBMSoundHandler.upgradePlug.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
                }
            }
        };
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.miningLaser");
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot == 0) return Library.isBattery(stack);
        if (slot >= 1 && slot <= 8) return stack.getItem() instanceof ItemMachineUpgrade;
        return false;
    }

    @Override
    public boolean canExtractItem(int slot, ItemStack stack, int amount) {
        return slot >= 9 && slot <= 29;
    }

    @Override
    public int[] getAccessibleSlotsFromSide(Direction side) {
        int[] slots = new int[21];
        for (int i = 0; i < 21; i++) {
            slots[i] = i + 9;
        }
        return slots;
    }

    @Override
    public void updateEntity() {
        if (level == null || level.isClientSide) return;

        trySubscribe(level, worldPosition.above(2), Direction.UP);
        tryProvide(tank, level, worldPosition.east(2), Direction.EAST);
        tryProvide(tank, level, worldPosition.west(2), Direction.WEST);
        tryProvide(tank, level, worldPosition.south(2), Direction.SOUTH);
        tryProvide(tank, level, worldPosition.north(2), Direction.NORTH);

        power = Library.chargeTEFromItems(inventory, 0, power, MAX_POWER);

        if (lastTargetX != targetX || lastTargetY != targetY || lastTargetZ != targetZ) {
            breakProgress = 0;
        }
        lastTargetX = targetX;
        lastTargetY = targetY;
        lastTargetZ = targetZ;

        boolean prevRedstone = this.redstonePowered;
        this.redstonePowered = isMultiblockRedstonePowered();
        if (prevRedstone != this.redstonePowered) {
            setChanged();
        }

        if (isOn && !redstonePowered) {
            upgradeManager.checkSlots(inventory, 1, 8);
            int cycles = 1 + upgradeManager.getLevel(UpgradeType.OVERDRIVE);
            int speed = 1 + Math.min(upgradeManager.getLevel(UpgradeType.SPEED), 12);
            int range = 1 + Math.min(upgradeManager.getLevel(UpgradeType.EFFECT) * 2, 24);
            int fortune = Math.min(upgradeManager.getLevel(UpgradeType.FORTUNE), 3);
            int consumption = CONSUMPTION
                    - (CONSUMPTION * Math.min(upgradeManager.getLevel(UpgradeType.POWER), 12) / 16)
                    + (CONSUMPTION * Math.min(upgradeManager.getLevel(UpgradeType.SPEED), 12) / 16);

            if (hasUpgradeType(UpgradeType.SCREAM)) {
                cycles *= 4;
                speed *= 4;
                consumption *= 20;
            }

            for (int i = 0; i < cycles; i++) {
                if (power < consumption) {
                    beam = false;
                    break;
                }
                power -= consumption;

                if (targetY <= 0) {
                    targetY = worldPosition.getY() - 2;
                }

                scan(range);

                BlockPos target = new BlockPos(targetX, targetY, targetZ);
                BlockState block = level.getBlockState(target);

                if (!block.getFluidState().isEmpty()) {
                    level.removeBlock(target, false);
                    buildDam();
                    continue;
                }
                if (beam && canBreak(block, targetX, targetY, targetZ)) {
                    breakProgress += getBreakSpeed(speed);
                    clientBreakProgress = Math.min(breakProgress, 1);
                    if (breakProgress < 1) {
                        level.destroyBlockProgress(-1, target, (int) Math.floor(breakProgress * 10));
                    } else {
                        breakBlock(fortune);
                        buildDam();
                    }
                }
            }
            if (beam && hasUpgradeType(UpgradeType.SCREAM)) {
                level.playSound(null, targetX + 0.5, targetY + 0.5, targetZ + 0.5,
                        HBMSoundHandler.screm.get(), SoundSource.BLOCKS, 20.0F, 1.0F);
            }
        } else {
            targetY = worldPosition.getY() - 2;
            beam = false;
        }

        tryFillContainer(worldPosition.getX() + 2, worldPosition.getY(), worldPosition.getZ());
        tryFillContainer(worldPosition.getX() - 2, worldPosition.getY(), worldPosition.getZ());
        tryFillContainer(worldPosition.getX(), worldPosition.getY(), worldPosition.getZ() + 2);
        tryFillContainer(worldPosition.getX(), worldPosition.getY(), worldPosition.getZ() - 2);

        dataChanged();
        networkPackMK2(250);
    }

    public void toggleOn() {
        isOn = !isOn;
        setChanged();
    }

    private void buildDam() {
        placeBags(new BlockPos(targetX + 1, targetY, targetZ));
        placeBags(new BlockPos(targetX - 1, targetY, targetZ));
        placeBags(new BlockPos(targetX, targetY, targetZ + 1));
        placeBags(new BlockPos(targetX, targetY, targetZ - 1));
    }

    private void placeBags(BlockPos wallPos) {
        BlockState state = level.getBlockState(wallPos);
        if (state.canBeReplaced() && !state.getFluidState().isEmpty()) {
            Block bags = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "sandbags"));
            if (bags != Blocks.AIR) {
                level.setBlock(wallPos, bags.defaultBlockState(), 3);
            }
        }
    }

    private void tryFillContainer(int x, int y, int z) {
        IItemHandler h = level.getCapability(Capabilities.ItemHandler.BLOCK, new BlockPos(x, y, z), null);
        if (h == null) return;
        for (int i = 9; i <= 29; i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (stack.isEmpty()) continue;
            int prev = stack.getCount();
            ItemStack leftover = insertRange(h, 0, h.getSlots() - 1, stack.copy());
            inventory.setStackInSlot(i, leftover);
            if (leftover.isEmpty() || leftover.getCount() < prev) return;
        }
    }

    private void breakBlock(int fortune) {
        BlockPos pos = new BlockPos(targetX, targetY, targetZ);
        BlockState state = level.getBlockState(pos);
        boolean normal = true;
        boolean doesBreak = true;

        ItemStack asItem = new ItemStack(state.getBlock());
        if (!asItem.isEmpty()) {
            if (hasUpgradeItem("upgrade_crystallizer")) {
                CrystallizerRecipes.CrystallizerRecipe result = CrystallizerRecipes.getOutput(asItem, Fluids.PEROXIDE);
                if (result == null) result = CrystallizerRecipes.getOutput(asItem, Fluids.SULFURIC_ACID);
                if (result != null) {
                    level.addFreshEntity(new ItemEntity(level, targetX + 0.5, targetY + 0.5, targetZ + 0.5, result.output.copy()));
                    normal = false;
                }
            } else if (hasUpgradeItem("upgrade_centrifuge")) {
                ItemStack[] result = CentrifugeRecipes.getOutput(asItem);
                if (result != null) {
                    for (ItemStack sta : result) {
                        if (sta != null && !sta.isEmpty()) {
                            level.addFreshEntity(new ItemEntity(level, targetX + 0.5, targetY + 0.5, targetZ + 0.5, sta.copy()));
                            normal = false;
                        }
                    }
                }
            } else if (hasUpgradeItem("upgrade_shredder")) {
                ItemStack result = shredderResult(asItem);
                Item scrap = hbmItem("scrap");
                if (!result.isEmpty() && result.getItem() != scrap) {
                    level.addFreshEntity(new ItemEntity(level, targetX + 0.5, targetY + 0.5, targetZ + 0.5, result.copy()));
                    normal = false;
                }
            } else if (hasUpgradeItem("upgrade_smelter")) {
                ItemStack result = smeltResult(asItem);
                if (!result.isEmpty()) {
                    level.addFreshEntity(new ItemEntity(level, targetX + 0.5, targetY + 0.5, targetZ + 0.5, result.copy()));
                    normal = false;
                }
            }
        }

        if (normal && state.getBlock() instanceof IDrillInteraction in) {
            doesBreak = in.canBreak(level, targetX, targetY, targetZ, state, this);
            if (doesBreak) {
                ItemStack drop = in.extractResource(level, targetX, targetY, targetZ, state, this);
                if (!drop.isEmpty()) {
                    level.addFreshEntity(new ItemEntity(level, targetX + 0.5, targetY + 0.5, targetZ + 0.5, drop.copy()));
                }
            }
        }

        if (doesBreak) {
            if (normal) {
                dropWithFortune(state, pos, fortune);
            }
            level.destroyBlock(pos, false);
        }

        suckDrops();
        breakProgress = 0;
    }

    private void dropWithFortune(BlockState state, BlockPos pos, int fortune) {
        if (!(level instanceof ServerLevel sl)) return;
        ItemStack tool = new ItemStack(Items.DIAMOND_PICKAXE);
        if (fortune > 0) {
            sl.registryAccess().holder(Enchantments.FORTUNE)
                    .ifPresent(holder -> tool.enchant(holder, fortune));
        }
        for (ItemStack drop : Block.getDrops(state, sl, pos, null, null, tool)) {
            Block.popResource(level, pos, drop);
        }
    }

    private void suckDrops() {
        int rangeHor = 3;
        int rangeVer = 1;
        AABB box = new AABB(
                targetX + 0.5 - rangeHor, targetY + 0.5 - rangeVer, targetZ + 0.5 - rangeHor,
                targetX + 0.5 + rangeHor, targetY + 0.5 + rangeVer, targetZ + 0.5 + rangeHor);
        Item oreOil = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "ore_oil"));
        boolean nullifier = hasUpgradeItem("upgrade_nullifier");

        for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, box)) {
            if (nullifier && !item.getItem().isEmpty() && ItemMachineUpgrade.isScrapItem(item.getItem().getItem())) {
                item.discard();
                continue;
            }
            if (!item.getItem().isEmpty() && item.getItem().is(oreOil)) {
                tank.setTankType(Fluids.OIL);
                tank.setFill(Math.min(tank.getMaxFill(), tank.getFill() + 500));
                item.discard();
                continue;
            }
            ItemStack leftover = insertRange(inventory, 9, 29, item.getItem().copy());
            if (leftover.isEmpty()) {
                item.discard();
            } else {
                item.setItem(leftover.copy());
            }
        }

        AABB burn = new AABB(
                targetX + 0.5 - 1, targetY + 0.5 - 1, targetZ + 0.5 - 1,
                targetX + 0.5 + 1, targetY + 0.5 + 1, targetZ + 0.5 + 1);
        for (LivingEntity mob : level.getEntitiesOfClass(LivingEntity.class, burn)) {
            mob.igniteForSeconds(5);
        }
    }

    private double getBreakSpeed(int speed) {
        BlockPos pos = new BlockPos(targetX, targetY, targetZ);
        float hardness = level.getBlockState(pos).getDestroySpeed(level, pos) * 15 / speed;
        if (hardness == 0) return 1;
        return 1 / hardness;
    }

    public void scan(int range) {
        for (int x = -range; x <= range; x++) {
            for (int z = -range; z <= range; z++) {
                BlockState state = level.getBlockState(new BlockPos(x + worldPosition.getX(), targetY, z + worldPosition.getZ()));
                if (!state.getFluidState().isEmpty()) continue;
                if (canBreak(state, x + worldPosition.getX(), targetY, z + worldPosition.getZ())) {
                    targetX = x + worldPosition.getX();
                    targetZ = z + worldPosition.getZ();
                    beam = true;
                    return;
                }
            }
        }
        beam = false;
        targetY--;
    }

    private boolean canBreak(BlockState state, int x, int y, int z) {
        Block b = state.getBlock();
        if (state.isAir()) return false;
        if (b == Blocks.BEDROCK) return false;
        if (b instanceof BlockGasBase) return false;
        float hardness = state.getDestroySpeed(level, new BlockPos(x, y, z));
        if (hardness < 0 || hardness > 3_500_000) return false;
        return state.getFluidState().isEmpty();
    }

    public int getRange() {
        int range = 1;
        for (int i = 1; i < 9; i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (stack.isEmpty()) continue;
            Item item = stack.getItem();
            if (item == hbmItem("upgrade_effect_1")) range += 2;
            else if (item == hbmItem("upgrade_effect_2")) range += 4;
            else if (item == hbmItem("upgrade_effect_3")) range += 6;
        }
        return Math.min(range, 25);
    }

    public int getWidth() {
        return 1 + getRange() * 2;
    }

    public int getPowerScaled(int i) {
        return (int) ((power * i) / MAX_POWER);
    }

    public int getProgressScaled(int i) {
        return (int) (clientBreakProgress * i);
    }

    private ItemStack shredderResult(ItemStack stack) {
        if (level == null || stack.isEmpty()) return ItemStack.EMPTY;
        return level.getRecipeManager()
                .getRecipeFor(ProcessingRecipes.SHREDDER_TYPE.get(), new SingleRecipeInput(stack), level)
                .map(RecipeHolder::value)
                .map(recipe -> recipe.getResultItem(level.registryAccess()).copy())
                .orElse(ItemStack.EMPTY);
    }

    private ItemStack smeltResult(ItemStack stack) {
        if (level == null || stack.isEmpty()) return ItemStack.EMPTY;
        return level.getRecipeManager()
                .getRecipeFor(RecipeType.SMELTING, new SingleRecipeInput(stack), level)
                .map(holder -> holder.value().assemble(new SingleRecipeInput(stack), level.registryAccess()))
                .orElse(ItemStack.EMPTY);
    }

    private boolean hasUpgradeItem(String path) {
        Item want = hbmItem(path);
        if (want == Items.AIR) return false;
        for (int i = 1; i <= 8; i++) {
            if (inventory.getStackInSlot(i).is(want)) return true;
        }
        return false;
    }

    private boolean hasUpgradeType(UpgradeType type) {
        for (int i = 1; i <= 8; i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (stack.getItem() instanceof ItemMachineUpgrade u && u.getType() == type) {
                return true;
            }
        }
        return false;
    }

    private static Item hbmItem(String path) {
        return BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, path));
    }

    private static ItemStack insertRange(IItemHandler inv, int start, int end, ItemStack stack) {
        ItemStack remain = stack;
        for (int i = start; i <= end && !remain.isEmpty(); i++) {
            remain = inv.insertItem(i, remain, false);
        }
        return remain;
    }

    public DirPos[] getConPos() {
        return new DirPos[]{
                new DirPos(worldPosition.getX(), worldPosition.getY() + 2, worldPosition.getZ(), Direction.UP),
                new DirPos(worldPosition.getX() + 2, worldPosition.getY(), worldPosition.getZ(), Direction.EAST),
                new DirPos(worldPosition.getX() - 2, worldPosition.getY(), worldPosition.getZ(), Direction.WEST),
                new DirPos(worldPosition.getX(), worldPosition.getY(), worldPosition.getZ() + 2, Direction.SOUTH),
                new DirPos(worldPosition.getX(), worldPosition.getY(), worldPosition.getZ() - 2, Direction.NORTH)
        };
    }

    private boolean isMultiblockRedstonePowered() {
        for (DirPos conPos : getConPos()) {
            if (level.hasNeighborSignal(conPos.getPos().relative(conPos.getDir().getOpposite()))) {
                return true;
            }
        }
        return false;
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
    public DrillType getDrillTier() {
        return DrillType.HITECH;
    }

    @Override
    public int getDrillRating() {
        return 100;
    }

    @Override
    public @NotNull List<FluidTankNTM> getSendingTanks() {
        return List.of(tank);
    }

    @Override
    public @NotNull List<FluidTankNTM> getAllTanks() {
        return List.of(tank);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tank.writeToNBT(tag, "oil");
        tag.putBoolean("isOn", isOn);
        tag.putLong("power", power);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        tank.readFromNBT(tag, "oil");
        isOn = tag.getBoolean("isOn");
        power = tag.getLong("power");
        redstonePowered = false;
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeLong(power);
        buf.writeInt(lastTargetX);
        buf.writeInt(lastTargetY);
        buf.writeInt(lastTargetZ);
        buf.writeInt(targetX);
        buf.writeInt(targetY);
        buf.writeInt(targetZ);
        buf.writeBoolean(beam);
        buf.writeBoolean(isOn);
        buf.writeBoolean(redstonePowered);
        buf.writeDouble(clientBreakProgress);
        tank.serialize(buf);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        power = buf.readLong();
        lastTargetX = buf.readInt();
        lastTargetY = buf.readInt();
        lastTargetZ = buf.readInt();
        targetX = buf.readInt();
        targetY = buf.readInt();
        targetZ = buf.readInt();
        beam = buf.readBoolean();
        isOn = buf.readBoolean();
        redstonePowered = buf.readBoolean();
        breakProgress = buf.readDouble();
        clientBreakProgress = breakProgress;
        tank.deserialize(buf);
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new MiningLaserMenu(id, inv, this);
    }
}
