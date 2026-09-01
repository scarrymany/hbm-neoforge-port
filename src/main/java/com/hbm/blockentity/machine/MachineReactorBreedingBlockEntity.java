package com.hbm.blockentity.machine;

import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.MachineBaseBlockEntity;
import com.hbm.blockentity.machine.dummyable.ReactorResearchBlockEntity;
import com.hbm.blocks.BlockDummyable;
import com.hbm.blocks.machine.dummyable.DummyableProcessBlocks;
import com.hbm.inventory.container.machine.MachineReactorBreedingMenu;
import com.hbm.inventory.recipes.machine.BreederRecipe;
import com.hbm.inventory.recipes.machine.BreederRecipes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * Ported from CE's {@code TileEntityMachineReactorBreeding} (regname of its block:
 * {@code machine_reactor_breeding}, read in full alongside {@code MachineReactorBreeding} and
 * {@code com.hbm.inventory.recipes.BreederRecipes}). A small 2-slot machine (input rod in slot 0,
 * transmuted rod out slot 1) driven entirely by an external "flux" number and a fixed
 * input-&gt;output lookup table - see {@link BreederRecipe} for that table's JSON-ported shape.
 *
 * Flux comes from a live {@code reactor_research} core
 * ({@link ReactorResearchBlockEntity#totalFlux}) on the four horizontal neighbours, matching
 * CE {@code TileEntityMachineReactorBreeding.getInteractions} :92-111.
 */
public class MachineReactorBreedingBlockEntity extends MachineBaseBlockEntity implements ITickableBE, MenuProvider {

    private static final int[] SLOTS_IO = {0, 1};

    public int flux;
    public float progress;

    public MachineReactorBreedingBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, 2, false, false);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.reactorBreeding");
    }

    @Override
    public void updateEntity() {
        if (level == null || level.isClientSide) return;

        this.flux = 0;
        getInteractions();

        Optional<BreederRecipe> recipe = findRecipe();

        if (canProcess(recipe)) {
            BreederRecipe rec = recipe.get();
            progress += 0.0025F * ((float) this.flux / rec.getFlux());

            if (this.progress >= 1.0F) {
                this.progress = 0F;
                processItem(rec);
                this.setChanged();
            }
        } else {
            this.progress = 0.0F;
        }

        dataChanged();
        networkPackNT(20);
    }

    /** CE {@code TileEntityMachineReactorBreeding.getInteractions} :92-111. */
    private void getInteractions() {
        if (level == null) return;
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            BlockPos neighbor = worldPosition.relative(dir);
            if (level.getBlockState(neighbor).getBlock() != DummyableProcessBlocks.REACTOR_RESEARCH.get()) continue;
            BlockPos core = ((BlockDummyable) DummyableProcessBlocks.REACTOR_RESEARCH.get()).findCore(level, neighbor);
            if (core != null && level.getBlockEntity(core) instanceof ReactorResearchBlockEntity reactor) {
                this.flux += reactor.totalFlux;
            }
        }
    }

    private Optional<BreederRecipe> findRecipe() {
        if (level == null) return Optional.empty();
        ItemStack input = inventory.getStackInSlot(0);
        if (input.isEmpty()) return Optional.empty();
        return level.getRecipeManager()
                .getRecipeFor(BreederRecipes.BREEDER_TYPE.get(), new SingleRecipeInput(input), level)
                .map(RecipeHolder::value);
    }

    private boolean canProcess(Optional<BreederRecipe> recipe) {
        if (level == null || recipe.isEmpty()) return false;
        BreederRecipe rec = recipe.get();
        if (this.flux < rec.getFlux()) return false;

        ItemStack existingOutput = inventory.getStackInSlot(1);
        if (existingOutput.isEmpty()) return true;

        ItemStack recipeOutput = rec.getResultItem(level.registryAccess());
        return ItemStack.isSameItemSameComponents(existingOutput, recipeOutput) && existingOutput.getCount() < existingOutput.getMaxStackSize();
    }

    private void processItem(BreederRecipe rec) {
        if (level == null) return;
        ItemStack output = rec.getResultItem(level.registryAccess()).copy();
        ItemStack existing = inventory.getStackInSlot(1);

        if (existing.isEmpty()) {
            inventory.setStackInSlot(1, output);
        } else if (ItemStack.isSameItemSameComponents(existing, output)) {
            existing.grow(output.getCount());
        }

        ItemStack input = inventory.getStackInSlot(0);
        input.shrink(1);
        if (input.isEmpty()) inventory.setStackInSlot(0, ItemStack.EMPTY);
    }

    public int getProgressScaled(int scale) {
        return (int) (this.progress * scale);
    }

    @Override
    public int[] getAccessibleSlotsFromSide(Direction side) {
        return SLOTS_IO;
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        return slot == 0;
    }

    @Override
    public boolean canExtractItem(int slot, ItemStack itemStack, int amount) {
        return slot == 1;
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new MachineReactorBreedingMenu(containerId, playerInventory, this);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("flux", flux);
        tag.putFloat("progress", progress);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        flux = tag.getInt("flux");
        progress = tag.getFloat("progress");
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeInt(flux);
        buf.writeFloat(progress);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        flux = buf.readInt();
        progress = buf.readFloat();
    }
}
