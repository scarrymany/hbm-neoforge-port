package com.hbm.blockentity.machine.dummyable;

import com.hbm.api.fluidmk2.IFluidStandardTransceiverMK2;
import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.MachineBaseBlockEntity;
import com.hbm.inventory.container.machine.dummyable.HephaestusMenu;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.fluid.trait.FT_Heatable;
import com.hbm.inventory.fluid.trait.FT_Heatable.HeatingStep;
import com.hbm.inventory.fluid.trait.FT_Heatable.HeatingType;
import com.hbm.inventory.recipes.HeatRecipes;
import com.hbm.items.machine.IItemFluidIdentifier;
import com.hbm.lib.DirPos;
import com.hbm.main.MainRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * CE {@code TileEntityMachineHephaestus.java}:132-182 — lava/magma/fissure heat +
 * {@code FT_Heatable} HEATEXCHANGER. Named table {@link HeatRecipes} (already counted).
 * CE is overlay-only; this port adds a live ID+tank+heat menu.
 */
public class MachineHephaestusBlockEntity extends MachineBaseBlockEntity
        implements IFluidStandardTransceiverMK2, ITickableBE, MenuProvider {

    public final FluidTankNTM input;
    public final FluidTankNTM output;
    public int bufferedHeat;
    private final int[] heat = new int[10];
    private long fissureScanTime;

    public MachineHephaestusBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, 1, true, false);
        this.input = new FluidTankNTM(Fluids.OIL, 24_000).withOwner(this);
        this.output = new FluidTankNTM(Fluids.HOTOIL, 24_000).withOwner(this);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.machineHephaestus");
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        return slot == 0 && stack.getItem() instanceof IItemFluidIdentifier;
    }

    @Override
    public void updateEntity() {
        if (level == null || level.isClientSide) return;

        ItemStack id = inventory.getStackInSlot(0);
        if (!id.isEmpty() && id.getItem() instanceof IItemFluidIdentifier ident) {
            input.setTankType(ident.getType(level, worldPosition, id));
        }

        setupTanks();

        int height = (int) (level.getGameTime() % 10);
        int range = 7;
        int fromY = worldPosition.getY() - 1 - height;
        heat[height] = 0;
        if (fromY >= level.getMinBuildHeight()) {
            for (int ox = -range; ox <= range; ox++) {
                for (int oz = -range; oz <= range; oz++) {
                    heat[height] += heatFromBlock(worldPosition.getX() + ox, fromY, worldPosition.getZ() + oz);
                }
            }
        }

        heatFluid();
        bufferedHeat = getTotalHeat();

        if (level.getGameTime() % 20 == 0 && input.getTankType() != Fluids.NONE) {
            for (DirPos pos : getConPos()) {
                trySubscribe(input.getTankType(), level, pos);
            }
        }
        if (output.getFill() > 0) {
            for (DirPos pos : getConPos()) {
                tryProvide(output, level, pos);
            }
        }
        dataChanged();
        networkPackMK2(150);
    }

    private void setupTanks() {
        FluidType type = input.getTankType();
        if (type.hasTrait(FT_Heatable.class)) {
            FT_Heatable trait = type.getTrait(FT_Heatable.class);
            if (trait.getEfficiency(HeatingType.HEATEXCHANGER) > 0) {
                output.setTankType(trait.getFirstStep().typeProduced);
                return;
            }
        }
        HeatRecipes.HeatRecipe boil = HeatRecipes.getBoilRecipe(type);
        if (boil != null) {
            output.setTankType(boil.output.type);
            return;
        }
        input.setTankType(Fluids.NONE);
        output.setTankType(Fluids.NONE);
    }

    private void heatFluid() {
        FluidType type = input.getTankType();
        if (type.hasTrait(FT_Heatable.class)) {
            FT_Heatable trait = type.getTrait(FT_Heatable.class);
            int h = getTotalHeat();
            HeatingStep step = trait.getFirstStep();
            int inputOps = input.getFill() / Math.max(1, step.amountReq);
            int outputOps = (output.getMaxFill() - output.getFill()) / Math.max(1, step.amountProduced);
            int heatOps = h / Math.max(1, step.heatReq);
            int ops = Math.min(Math.min(inputOps, outputOps), heatOps);
            if (ops > 0) {
                input.setFill(input.getFill() - step.amountReq * ops);
                output.setFill(output.getFill() + step.amountProduced * ops);
                setChanged();
            }
            return;
        }
        HeatRecipes.HeatRecipe boil = HeatRecipes.getBoilRecipe(type);
        if (boil != null) {
            int h = getTotalHeat();
            int inCost = Math.max(1, boil.input.fill);
            int outGain = Math.max(1, boil.output.fill);
            int heatCost = Math.max(1, boil.heat);
            int inputOps = input.getFill() / inCost;
            int outputOps = (output.getMaxFill() - output.getFill()) / outGain;
            int heatOps = h / heatCost;
            int ops = Math.min(Math.min(inputOps, outputOps), heatOps);
            if (ops > 0) {
                input.setFill(input.getFill() - inCost * ops);
                output.setFill(output.getFill() + outGain * ops);
                setChanged();
            }
        }
    }

    private int heatFromBlock(int x, int y, int z) {
        Block b = level.getBlockState(new BlockPos(x, y, z)).getBlock();
        if (b == Blocks.LAVA || b == Blocks.MAGMA_BLOCK) return 5;
        Block volcano = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "ore_volcano"));
        if (volcano != Blocks.AIR && b == volcano) {
            fissureScanTime = level.getGameTime();
            return 300;
        }
        return 0;
    }

    public int getTotalHeat() {
        boolean fissure = level != null && level.getGameTime() - fissureScanTime < 20;
        int total = 0;
        for (int h : heat) total += h;
        return fissure ? total * 3 : total;
    }

    public DirPos[] getConPos() {
        int x = worldPosition.getX();
        int y = worldPosition.getY();
        int z = worldPosition.getZ();
        return new DirPos[]{
                new DirPos(x + 2, y, z, Direction.EAST),
                new DirPos(x - 2, y, z, Direction.WEST),
                new DirPos(x, y, z + 2, Direction.SOUTH),
                new DirPos(x, y, z - 2, Direction.NORTH),
                new DirPos(x + 2, y + 11, z, Direction.EAST),
                new DirPos(x - 2, y + 11, z, Direction.WEST),
                new DirPos(x, y + 11, z + 2, Direction.SOUTH),
                new DirPos(x, y + 11, z - 2, Direction.NORTH),
        };
    }

    @Override
    public @NotNull List<FluidTankNTM> getReceivingTanks() {
        return List.of(input);
    }

    @Override
    public @NotNull List<FluidTankNTM> getSendingTanks() {
        return List.of(output);
    }

    @Override
    public @NotNull List<FluidTankNTM> getAllTanks() {
        return List.of(input, output);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        input.writeToNBT(tag, "t0");
        output.writeToNBT(tag, "t1");
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        input.readFromNBT(tag, "t0");
        output.readFromNBT(tag, "t1");
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeInt(bufferedHeat);
        input.serialize(buf);
        output.serialize(buf);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        bufferedHeat = buf.readInt();
        input.deserialize(buf);
        output.deserialize(buf);
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new HephaestusMenu(id, inv, this);
    }
}
