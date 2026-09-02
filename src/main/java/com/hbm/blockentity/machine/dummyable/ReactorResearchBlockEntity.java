package com.hbm.blockentity.machine.dummyable;

import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.MachineBaseBlockEntity;
import com.hbm.blocks.machine.PWRBlocks;
import com.hbm.blocks.machine.dummyable.DummyableProcessBlocks;
import com.hbm.handler.radiation.ChunkRadiationManager;
import com.hbm.inventory.container.machine.dummyable.ReactorResearchMenu;
import com.hbm.items.PlateCrystalWasteItems;
import com.hbm.items.machine.ItemFuelRod;
import com.hbm.items.machine.ItemPlateFuel;
import com.hbm.items.machine.MachineItems;
import com.hbm.items.weapon.WeaponMeleeItems;
import com.hbm.main.MainRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.IdentityHashMap;
import java.util.Map;

/**
 * CE {@code TileEntityReactorResearch}: 12 plate-fuel slots, maxHeat 50000, rod speed 0.04.
 * TODO(CE: TileEntityReactorResearch.java:420-478): OpenComputers callbacks.
 * TODO(CE: TileEntityReactorResearch.java:335-341): MobConfig.enableElementals radMark.
 * TODO(CE: TileEntityReactorResearch.java:232): block_lead / block_desh (not registered).
 */
public class ReactorResearchBlockEntity extends MachineBaseBlockEntity implements ITickableBE, MenuProvider {

    public static final int MAX_HEAT = 50_000;
    public static final double SPEED = 0.04D;
    private static final int[] SLOT_IO = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11};

    public double lastLevel;
    /** CE field {@code level} — renamed: BlockEntity already owns {@code level} (the world). NBT key stays {@code level}. */
    public double rodLevel;
    public double targetLevel;
    public int heat;
    public byte water;
    public int[] slotFlux = new int[12];
    public int totalFlux;

    private static Map<Item, Item> fuelMap;

    public ReactorResearchBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, 12, false, false);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.reactorResearch");
    }

    @Override
    public int[] getAccessibleSlotsFromSide(Direction side) {
        return SLOT_IO;
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        // CE TileEntityReactorResearch.java:86 is `i < 12 && i <= 0` — typo, only slot 0.
        // GUI + TransferStrategy take plate fuel in all 12; keep that.
        return slot >= 0 && slot < 12 && stack.getItem() instanceof ItemPlateFuel;
    }

    @Override
    public boolean canExtractItem(int slot, ItemStack stack, int amount) {
        return slot >= 0 && slot < 12 && fuelMap().containsValue(stack.getItem());
    }

    @Override
    public void updateEntity() {
        rodControl();
        if (level == null || level.isClientSide) return;

        totalFlux = 0;
        if (rodLevel > 0) {
            reaction();
        }

        if (this.heat > 0) {
            water = getWater();
            if (water > 0) {
                this.heat -= (int) (this.heat * 0.07F * water / 12);
            } else {
                this.heat -= 1;
            }
            if (this.heat < 0) this.heat = 0;
        }

        if (this.heat > MAX_HEAT) {
            explode();
            return;
        }

        if (rodLevel > 0 && heat > 0 && !(blocksRad(worldPosition.offset(1, 1, 0))
                && blocksRad(worldPosition.offset(-1, 1, 0))
                && blocksRad(worldPosition.offset(0, 1, 1))
                && blocksRad(worldPosition.offset(0, 1, -1)))) {
            float rad = (float) heat / (float) MAX_HEAT * 50F;
            ChunkRadiationManager.proxy.incrementRad(level, worldPosition, rad, 25000F);
        }

        dataChanged();
        networkPackNT(150);
    }

    public byte getWater() {
        byte count = 0;
        for (Direction dir : Direction.values()) {
            if (dir.getAxis() == Direction.Axis.Y) {
                if (isWater(worldPosition.offset(0, 1 + dir.getStepY() * 2, 0))) count++;
            } else {
                for (int i = 0; i < 3; i++) {
                    if (isWater(worldPosition.offset(dir.getStepX(), i, dir.getStepZ()))) count++;
                }
            }
        }
        return count;
    }

    public boolean isSubmerged() {
        return isWater(worldPosition.offset(1, 1, 0))
                || isWater(worldPosition.offset(0, 1, 1))
                || isWater(worldPosition.offset(-1, 1, 0))
                || isWater(worldPosition.offset(0, 1, -1));
    }

    private boolean isWater(BlockPos pos) {
        return level != null && level.getFluidState(pos).is(FluidTags.WATER);
    }

    private boolean blocksRad(BlockPos pos) {
        if (level == null) return false;
        BlockState state = level.getBlockState(pos);
        if (level.getFluidState(pos).is(FluidTags.WATER) && level.getFluidState(pos).isSource()) {
            return true;
        }
        Block b = state.getBlock();
        if (b == DummyableProcessBlocks.REACTOR_RESEARCH.get() || b == PWRBlocks.REACTOR_BREEDING.get()) {
            return true;
        }
        return b.getExplosionResistance() >= 100;
    }

    private static int[] getNeighboringSlots(int id) {
        return switch (id) {
            case 0 -> new int[]{1, 5};
            case 1 -> new int[]{0, 6};
            case 2 -> new int[]{3, 7};
            case 3 -> new int[]{2, 4, 8};
            case 4 -> new int[]{3, 9};
            case 5 -> new int[]{0, 6, 0xA};
            case 6 -> new int[]{1, 5, 0xB};
            case 7 -> new int[]{2, 8};
            case 8 -> new int[]{3, 7, 9};
            case 9 -> new int[]{4, 8};
            case 10 -> new int[]{5, 0xB};
            case 11 -> new int[]{6, 0xA};
            default -> new int[0];
        };
    }

    private void reaction() {
        for (int i = 0; i < 12; i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (stack.isEmpty()) {
                slotFlux[i] = 0;
                continue;
            }
            if (stack.getItem() instanceof ItemPlateFuel rod) {
                int outFlux = rod.react(stack, slotFlux[i]);
                this.heat += outFlux * 2;
                slotFlux[i] = 0;
                totalFlux += outFlux;
                if (ItemFuelRod.getLifeTime(stack) > rod.getLifeTime()) {
                    Item waste = fuelMap().get(stack.getItem());
                    if (waste != null) inventory.setStackInSlot(i, new ItemStack(waste));
                }
                for (int neighbor : getNeighboringSlots(i)) {
                    slotFlux[neighbor] += (int) (outFlux * rodLevel);
                }
                continue;
            }
            if (stack.getItem() == WeaponMeleeItems.METEORITE_SWORD_BRED.get()) {
                inventory.setStackInSlot(i, new ItemStack(WeaponMeleeItems.METEORITE_SWORD_IRRADIATED.get()));
            }
            slotFlux[i] = 0;
        }
    }

    private void explode() {
        if (level == null) return;
        for (int i = 0; i < inventory.getSlots(); i++) {
            inventory.setStackInSlot(i, ItemStack.EMPTY);
        }
        BlockPos origin = worldPosition;
        level.removeBlock(origin, false);
        for (Direction dir : Direction.values()) {
            if (dir.getAxis() == Direction.Axis.Y) {
                BlockPos p = origin.offset(0, 1 + dir.getStepY() * 2, 0);
                if (isWater(p)) level.setBlock(p, Blocks.AIR.defaultBlockState(), 3);
            } else {
                for (int i = 0; i < 3; i++) {
                    BlockPos p = origin.offset(dir.getStepX(), i, dir.getStepZ());
                    if (isWater(p)) level.setBlock(p, Blocks.AIR.defaultBlockState(), 3);
                }
            }
        }
        level.explode(null, origin.getX(), origin.getY(), origin.getZ(), 18.0F, true, Level.ExplosionInteraction.TNT);
        Block deco = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "deco_steel"));
        if (deco != Blocks.AIR) {
            level.setBlock(origin, deco.defaultBlockState(), 3);
            level.setBlock(origin.above(2), deco.defaultBlockState(), 3);
        }
        level.setBlock(origin.above(), PWRBlocks.CORIUM_BLOCK.get().defaultBlockState(), 3);
        ChunkRadiationManager.proxy.incrementRad(level, origin, 50F, 15000F);
    }

    public void setTarget(double target) {
        this.targetLevel = target;
    }

    private void rodControl() {
        if (level == null) return;
        if (level.isClientSide) {
            this.lastLevel = this.rodLevel;
            return;
        }
        if (this.rodLevel < targetLevel) {
            this.rodLevel += SPEED;
            if (this.rodLevel >= targetLevel) this.rodLevel = targetLevel;
        }
        if (this.rodLevel > targetLevel) {
            this.rodLevel -= SPEED;
            if (this.rodLevel <= targetLevel) this.rodLevel = targetLevel;
        }
    }

    public int[] getDisplayData() {
        return new int[]{
                this.totalFlux,
                (int) Math.round(this.heat * 0.00002 * 980 + 20)
        };
    }

    private static Map<Item, Item> fuelMap() {
        if (fuelMap == null) {
            fuelMap = new IdentityHashMap<>();
            fuelMap.put(MachineItems.PLATE_FUEL_U233.get(), PlateCrystalWasteItems.WASTE_PLATE_U233_HOT.get());
            fuelMap.put(MachineItems.PLATE_FUEL_U235.get(), PlateCrystalWasteItems.WASTE_PLATE_U235_HOT.get());
            fuelMap.put(MachineItems.PLATE_FUEL_MOX.get(), PlateCrystalWasteItems.WASTE_PLATE_MOX_HOT.get());
            fuelMap.put(MachineItems.PLATE_FUEL_PU239.get(), PlateCrystalWasteItems.WASTE_PLATE_PU239_HOT.get());
            fuelMap.put(MachineItems.PLATE_FUEL_SA326.get(), PlateCrystalWasteItems.WASTE_PLATE_SA326_HOT.get());
            fuelMap.put(MachineItems.PLATE_FUEL_RA226BE.get(), PlateCrystalWasteItems.WASTE_PLATE_RA226BE_HOT.get());
            fuelMap.put(MachineItems.PLATE_FUEL_PU238BE.get(), PlateCrystalWasteItems.WASTE_PLATE_PU238BE_HOT.get());
        }
        return fuelMap;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("heat", heat);
        tag.putByte("water", water);
        tag.putDouble("level", this.rodLevel);
        tag.putDouble("targetLevel", targetLevel);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        heat = tag.getInt("heat");
        water = tag.getByte("water");
        this.rodLevel = tag.getDouble("level");
        targetLevel = tag.getDouble("targetLevel");
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeInt(heat);
        buf.writeByte(water);
        buf.writeDouble(this.rodLevel);
        buf.writeDouble(targetLevel);
        buf.writeVarIntArray(slotFlux);
        buf.writeInt(totalFlux);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        heat = buf.readInt();
        water = buf.readByte();
        this.rodLevel = buf.readDouble();
        targetLevel = buf.readDouble();
        slotFlux = buf.readVarIntArray();
        totalFlux = buf.readInt();
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new ReactorResearchMenu(id, inv, this);
    }
}
