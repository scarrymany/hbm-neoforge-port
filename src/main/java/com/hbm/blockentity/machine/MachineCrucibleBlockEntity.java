package com.hbm.blockentity.machine;

import com.hbm.api.block.ICrucibleAcceptor;
import com.hbm.api.tile.IHeatSource;
import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.MachineBaseBlockEntity;
import com.hbm.blocks.BlockDummyable;
import com.hbm.config.ServerConfig;
import com.hbm.handler.pollution.PollutionHandler;
import com.hbm.interfaces.IControlReceiver;
import com.hbm.inventory.container.machine.MachineCrucibleMenu;
import com.hbm.inventory.material.MaterialShapes;
import com.hbm.inventory.material.Mats;
import com.hbm.inventory.material.NTMMaterial;
import com.hbm.inventory.recipes.CrucibleRecipe;
import com.hbm.inventory.recipes.CrucibleRecipes;
import com.hbm.items.machine.ItemScraps;
import com.hbm.main.MainRegistry;
import com.hbm.util.CrucibleUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Ported from CE's {@code com.hbm.tileentity.machine.TileEntityCrucible} (675 lines, read in full)
 * - see {@code docs/phase7/crucible_core.md} for the full mechanics walkthrough this implements.
 * Follows this port's established machine block-entity convention ({@code MachineBaseBlockEntity} +
 * {@link ITickableBE} + {@link MenuProvider}, matching {@code MachineShredderBlockEntity}/
 * {@code MachineMixerBlockEntity}), plus {@link ICrucibleAcceptor} (a crucible can theoretically be
 * poured into from above, matching CE) and {@link IControlReceiver} (the GUI's recipe-picker sends a
 * recipe-name string, matching {@code RBMKConsoleBlockEntity}/{@code LaunchPadRustedBlockEntity}'s
 * established {@code IControlReceiver} implementer shape).
 * <p>
 * <b>Inventory</b>: 10 slots, matching CE exactly - slot 0 is a genuine dead slot (never exposed by
 * {@link #getAccessibleSlotsFromSide} nor addressed by {@link com.hbm.inventory.container.machine.MachineCrucibleMenu}'s
 * 9 GUI slots, which map to slots 1-9, exactly like CE's {@code ContainerCrucible}). No fluid/energy
 * capability wrapper (CE's crucible has neither).
 * <p>
 * <b>Heat</b>: pulled from an {@link IHeatSource} directly below the core each tick, scaled by
 * {@link #DIFFUSION}; passively decays otherwise. This port's {@code IHeatSource} interface already
 * exists verbatim, but <b>zero implementers exist anywhere in this port yet</b> (no heater/firebox
 * block has been ported) - so a placed Crucible compiles and is structurally complete/correct, but
 * sits permanently at {@code heat == 0} (fully inert) until a future phase ports at least one heat
 * source block. This is a real, documented, out-of-scope-for-this-task gap, not a bug.
 * <p>
 * <b>Smelting input</b> ({@link #isItemSmeltable}/{@link #trySmelt}): calls
 * {@link Mats#getSmeltingMaterialsFromItem(ItemStack)} unchanged - already fully implemented and
 * wired in this port's {@code Mats.java}, but {@code Mats.materialEntries}/{@code materialOreEntries}
 * are still empty pending the separate {@code crucible-matdistribution} task, so this call correctly
 * (by design) returns an empty list for every item today, meaning nothing is smeltable yet. This is
 * the intended degradation path, not a defect - see {@code Mats.java}'s own javadoc.
 * <p>
 * <b>Pouring</b> ({@link CrucibleUtil}): CE's particle-effect/network-packet plumbing on a successful
 * pour ({@code AuxParticlePacketNT}/{@code HbmEffectNT.Foundry}) is not ported - none of those
 * classes exist anywhere in this port (a separate, unported particle-engine system) and inventing
 * them is out of this task's scope. The pour routing itself (raytrace, {@link ICrucibleAcceptor}
 * lookup, partial-pour negotiation, safe spill) is ported in full via {@link CrucibleUtil}.
 */
public class MachineCrucibleBlockEntity extends MachineBaseBlockEntity
        implements ITickableBE, MenuProvider, ICrucibleAcceptor, IControlReceiver {

    /** CE: {@code TileEntityCrucible.recipeZCapacity}/{@code wasteZCapacity}. */
    public static final int RECIPE_Z_CAPACITY = MaterialShapes.BLOCK.q(16);
    public static final int WASTE_Z_CAPACITY = MaterialShapes.BLOCK.q(16);
    /** CE: {@code processTime} - total accumulated heat-scaled "progress" needed to smelt one item. */
    public static final int PROCESS_TIME = 20_000;
    /** CE: {@code diffusion} - fraction of the {@link IHeatSource}'s available heat delta pulled per tick. */
    public static final double DIFFUSION = 0.25D;
    public static final int MAX_HEAT = 100_000;
    /** CE: {@code MaterialShapes.NUGGET.q(3)} - per-tick pour rate cap. */
    private static final int POUR_RATE = MaterialShapes.NUGGET.q(3);

    private static final int INPUT_START = 1;
    private static final int INPUT_END = 9;
    private static final int[] ACCESSIBLE_SLOTS = {1, 2, 3, 4, 5, 6, 7, 8, 9};

    public int heat;
    public int progress;
    public String recipe = "";

    public List<Mats.MaterialStack> recipeStack = new ArrayList<>();
    public List<Mats.MaterialStack> wasteStack = new ArrayList<>();

    public MachineCrucibleBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, 10, false, false);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.machineCrucible");
    }

    @Override
    public int[] getAccessibleSlotsFromSide(Direction side) {
        return ACCESSIBLE_SLOTS;
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        return slot >= INPUT_START && slot <= INPUT_END && isItemSmeltable(stack);
    }

    // ==================== accessors (Menu/Screen) ====================

    public int getHeat() {
        return heat;
    }

    public int getProgress() {
        return progress;
    }

    public String getRecipeName() {
        return recipe;
    }

    public List<Mats.MaterialStack> getRecipeStack() {
        return recipeStack;
    }

    public List<Mats.MaterialStack> getWasteStack() {
        return wasteStack;
    }

    public CrucibleRecipe getLoadedRecipe() {
        return CrucibleRecipes.getRecipe(recipe);
    }

    // ==================== tick loop ====================

    @Override
    public void updateEntity() {
        if (level == null || level.isClientSide) return;

        tryPullHeat();

        if (level.getGameTime() % 5 == 0) {
            collectNearbyItems();
        }

        applyOverflowDamage();

        if (!trySmelt()) {
            progress = 0;
        }
        tryRecipe();

        pourWaste();
        pourRecipe();

        recipeStack.removeIf(s -> s.amount <= 0);
        wasteStack.removeIf(s -> s.amount <= 0);

        dataChanged();
        networkPackMK2(25);
    }

    /** CE: {@code tryPullHeat()}. */
    private void tryPullHeat() {
        if (heat >= MAX_HEAT) return;

        if (level.getBlockEntity(worldPosition.below()) instanceof IHeatSource source) {
            int diff = source.getHeatStored() - heat;

            if (diff != 0) {
                diff = Math.min(diff, MAX_HEAT - heat);

                if (diff > 0) {
                    diff = (int) Math.ceil(diff * DIFFUSION);
                    source.useUpHeat(diff);
                    heat = Math.min(heat + diff, MAX_HEAT);
                    return;
                }
            }
        }

        heat = Math.max(heat - Math.max(heat / 1000, 1), 0);
    }

    /** CE: {@code update()}'s "collect items" block - sucks smeltable {@link ItemEntity} stacks resting on the crucible into slots 1-9. */
    private void collectNearbyItems() {
        AABB box = new AABB(worldPosition.getX() - 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() - 0.5,
                worldPosition.getX() + 1.5, worldPosition.getY() + 1, worldPosition.getZ() + 1.5);

        for (ItemEntity itemEntity : level.getEntitiesOfClass(ItemEntity.class, box)) {
            if (!itemEntity.isAlive()) continue;
            ItemStack stack = itemEntity.getItem();
            if (!isItemSmeltable(stack)) continue;

            for (int slot = INPUT_START; slot <= INPUT_END; slot++) {
                if (!inventory.getStackInSlot(slot).isEmpty()) continue;

                if (stack.getCount() == 1) {
                    inventory.setStackInSlot(slot, stack.copy());
                    itemEntity.discard();
                } else {
                    ItemStack single = stack.copy();
                    single.setCount(1);
                    inventory.setStackInSlot(slot, single);
                    stack.shrink(1);
                }
                setChanged();
                break;
            }
        }
    }

    /** CE: {@code update()}'s lava-column overflow-hazard block. */
    private void applyOverflowDamage() {
        int totalCap = RECIPE_Z_CAPACITY + WASTE_Z_CAPACITY;
        int totalMass = 0;
        for (Mats.MaterialStack stack : recipeStack) totalMass += stack.amount;
        for (Mats.MaterialStack stack : wasteStack) totalMass += stack.amount;

        double columnHeight = ((double) totalMass / (double) totalCap) * 0.875D;
        if (columnHeight <= 0) return;

        AABB box = new AABB(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5,
                worldPosition.getX() + 0.5, worldPosition.getY() + 0.5 + columnHeight, worldPosition.getZ() + 0.5)
                .inflate(1, 0, 1);

        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, box)) {
            entity.hurt(level.damageSources().lava(), 5F);
            entity.setSecondsOnFire(5);
        }
    }

    /** CE: {@code trySmelt()}. */
    private boolean trySmelt() {
        if (heat < MAX_HEAT / 2) return false;

        int slot = getFirstSmeltableSlot();
        if (slot == -1) return false;

        int delta = heat - (MAX_HEAT / 2);
        delta = (int) (delta * 0.05);

        progress += delta;
        heat -= delta;

        if (progress >= PROCESS_TIME) {
            progress = 0;

            List<Mats.MaterialStack> materials = Mats.getSmeltingMaterialsFromItem(inventory.getStackInSlot(slot));
            CrucibleRecipe loaded = getLoadedRecipe();

            for (Mats.MaterialStack material : materials) {
                boolean recipeMaterial = loaded != null
                        && (getQuantaFromType(loaded.input(), material.material) > 0 || getQuantaFromType(loaded.output(), material.material) > 0);

                if ((loaded == null && !ServerConfig.LEGACY_CRUCIBLE_RULES.get()) || recipeMaterial) {
                    addToStack(recipeStack, material);
                } else {
                    addToStack(wasteStack, material);
                }
            }

            inventory.getStackInSlot(slot).shrink(1);
        }

        return true;
    }

    /** CE: {@code tryRecipe()} - fires once every {@code recipe.frequency} ticks, a steady-state throughput rate, not a one-shot craft. */
    private void tryRecipe() {
        CrucibleRecipe loaded = getLoadedRecipe();
        if (loaded == null) return;
        if (level.getGameTime() % loaded.frequency() > 0) return;

        for (Mats.MaterialStack stack : loaded.input()) {
            if (getQuantaFromType(recipeStack, stack.material) < stack.amount) return;
        }

        for (Mats.MaterialStack stack : recipeStack) {
            stack.amount -= getQuantaFromType(loaded.input(), stack.material);
        }

        outer:
        for (Mats.MaterialStack out : loaded.output()) {
            for (Mats.MaterialStack stack : recipeStack) {
                if (stack.material == out.material) {
                    stack.amount += out.amount;
                    continue outer;
                }
            }
            recipeStack.add(out.copy());
        }
    }

    private int getFirstSmeltableSlot() {
        for (int i = INPUT_START; i <= INPUT_END; i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (!stack.isEmpty() && isItemSmeltable(stack)) return i;
        }
        return -1;
    }

    /** CE: {@code isItemSmeltable(ItemStack)}. */
    public boolean isItemSmeltable(ItemStack stack) {
        List<Mats.MaterialStack> materials = Mats.getSmeltingMaterialsFromItem(stack);
        if (materials.isEmpty()) return false;

        CrucibleRecipe loaded = getLoadedRecipe();
        boolean matchesRecipe = loaded == null;

        int recipeContent = loaded != null ? loaded.getInputAmount() : 0;
        int recipeAmount = getQuantaFromType(recipeStack, null);
        int wasteAmount = getQuantaFromType(wasteStack, null);

        for (Mats.MaterialStack mat : materials) {
            int recipeInputRequired = loaded != null ? getQuantaFromType(loaded.input(), mat.material) : 0;

            if (loaded != null && getQuantaFromType(loaded.output(), mat.material) > 0) {
                recipeAmount += mat.amount;
                matchesRecipe = true;
                continue;
            }

            if (recipeInputRequired == 0) {
                if (loaded == null && !ServerConfig.LEGACY_CRUCIBLE_RULES.get()) {
                    recipeAmount += mat.amount;
                } else {
                    wasteAmount += mat.amount;
                }
            } else {
                int matMaximum = recipeContent == 0 ? 0 : recipeInputRequired * RECIPE_Z_CAPACITY / recipeContent;
                int amountStored = getQuantaFromType(recipeStack, mat.material);

                matchesRecipe = true;
                recipeAmount += mat.amount;

                if (amountStored + mat.amount > matMaximum) return false;
            }
        }

        return recipeAmount <= RECIPE_Z_CAPACITY && wasteAmount <= WASTE_Z_CAPACITY && matchesRecipe;
    }

    private void addToStack(List<Mats.MaterialStack> stack, Mats.MaterialStack matStack) {
        for (Mats.MaterialStack mat : stack) {
            if (mat.material == matStack.material) {
                mat.amount += matStack.amount;
                return;
            }
        }
        stack.add(matStack.copy());
    }

    /* "Arrays and Lists don't have a common ancestor" - CE's own comment, ported verbatim. */
    private int getQuantaFromType(Mats.MaterialStack[] stacks, NTMMaterial mat) {
        for (Mats.MaterialStack stack : stacks) {
            if (mat == null || stack.material == mat) {
                return stack.amount;
            }
        }
        return 0;
    }

    private int getQuantaFromType(List<Mats.MaterialStack> stacks, NTMMaterial mat) {
        int sum = 0;
        for (Mats.MaterialStack stack : stacks) {
            if (stack.material == mat) {
                return stack.amount;
            }
            if (mat == null) {
                sum += stack.amount;
            }
        }
        return sum;
    }

    /** The core's own placement facing, decoded the same way CE does ({@code ForgeDirection.getOrientation(meta - BlockDummyable.offset)}). Only ever called on the core position (meta 12-15). */
    private Direction facingDirection() {
        return Direction.from3DDataValue(getBlockState().getValue(BlockDummyable.META) - BlockDummyable.offset);
    }

    /** CE: {@code update()}'s waste-stack pour block - pours straight down opposite the block's facing. */
    private void pourWaste() {
        if (wasteStack.isEmpty()) return;

        Direction dir = facingDirection().getOpposite();
        double sx = worldPosition.getX() + 0.5D + dir.getStepX() * 1.875D;
        double sy = worldPosition.getY() + 0.25D;
        double sz = worldPosition.getZ() + 0.5D + dir.getStepZ() * 1.875D;

        CrucibleUtil.pourFullStack(level, sx, sy, sz, 6, true, wasteStack, POUR_RATE);
        PollutionHandler.incrementPollution(level, worldPosition, PollutionHandler.PollutionType.SOOT, PollutionHandler.SOOT_PER_SECOND / 20F);
    }

    /** CE: {@code update()}'s recipe-stack pour block - pours down the block's own facing, filtered to the loaded recipe's outputs (if any). */
    private void pourRecipe() {
        if (recipeStack.isEmpty()) return;

        CrucibleRecipe loaded = getLoadedRecipe();
        List<Mats.MaterialStack> toCast;
        if (loaded == null) {
            toCast = recipeStack;
        } else {
            toCast = new ArrayList<>();
            outer:
            for (Mats.MaterialStack stack : recipeStack) {
                for (Mats.MaterialStack output : loaded.output()) {
                    if (stack.material == output.material) {
                        toCast.add(stack);
                        continue outer;
                    }
                }
            }
        }

        Direction dir = facingDirection();
        double sx = worldPosition.getX() + 0.5D + dir.getStepX() * 1.875D;
        double sy = worldPosition.getY() + 0.25D;
        double sz = worldPosition.getZ() + 0.5D + dir.getStepZ() * 1.875D;

        CrucibleUtil.pourFullStack(level, sx, sy, sz, 6, true, toCast, POUR_RATE);
        PollutionHandler.incrementPollution(level, worldPosition, PollutionHandler.PollutionType.SOOT, PollutionHandler.SOOT_PER_SECOND / 20F);
    }

    // ==================== shovel-scoop / break dump ====================

    /**
     * CE: {@code MachineCrucible.onBlockActivated}'s shovel-scoop branch - dumps both stacks into the
     * player's inventory (or drops as {@link ItemEntity} on overflow) as {@code scraps_<material>}
     * stacks. Returns {@code false} (no-op) if both stacks were already empty.
     */
    public boolean scoopOut(Player player) {
        if (level == null || level.isClientSide) return false;
        if (recipeStack.isEmpty() && wasteStack.isEmpty()) return false;

        List<ItemStack> scraps = new ArrayList<>(toScraps(recipeStack));
        scraps.addAll(toScraps(wasteStack));

        for (ItemStack scrap : scraps) {
            if (!player.getInventory().add(scrap)) {
                level.addFreshEntity(new ItemEntity(level, player.getX(), player.getY(), player.getZ(), scrap));
            }
        }

        recipeStack.clear();
        wasteStack.clear();
        setChanged();
        dataChanged();
        return true;
    }

    /** CE: {@code MachineCrucible.breakBlock} - same scrap conversion as {@link #scoopOut}, but always drops at the block position (called from {@code MachineCrucibleBlock#onRemove}). */
    public void dropAllAsScraps() {
        if (level == null) return;

        List<ItemStack> scraps = new ArrayList<>(toScraps(recipeStack));
        scraps.addAll(toScraps(wasteStack));

        for (ItemStack scrap : scraps) {
            level.addFreshEntity(new ItemEntity(level, worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5, scrap));
        }

        recipeStack.clear();
        wasteStack.clear();
    }

    /** CE: {@code ItemScraps.create(new Mats.MaterialStack(...))} - this port has no single-item-per-metadata scrap equivalent, so the specific {@code scraps_<material>} item is resolved first, matching {@code ItemMold.MoldEntry.getOutput}'s established lazy-lookup pattern. Silently skips a material with no registered scrap item (should not happen for anything that ever reaches these stacks - see class javadoc). */
    private static List<ItemStack> toScraps(List<Mats.MaterialStack> stacks) {
        List<ItemStack> result = new ArrayList<>();
        for (Mats.MaterialStack stack : stacks) {
            if (stack.amount <= 0) continue;

            ResourceLocation id = ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "scraps_" + stack.material.getRegistryName());
            BuiltInRegistries.ITEM.getOptional(id).ifPresent(item -> result.add(ItemScraps.create(new ItemStack(item), stack.amount, false)));
        }
        return result;
    }

    // ==================== ICrucibleAcceptor ====================

    /** CE: {@code TileEntityCrucible.canAcceptPartialPour} - lets an external source pour into this crucible from above. */
    @Override
    public boolean canAcceptPartialPour(Level level, BlockPos pos, double dX, double dY, double dZ, Direction side, Mats.MaterialStack stack) {
        CrucibleRecipe loaded = getLoadedRecipe();
        if (loaded == null) {
            return getQuantaFromType(wasteStack, null) < WASTE_Z_CAPACITY;
        }

        int recipeContent = loaded.getInputAmount();
        int recipeInputRequired = getQuantaFromType(loaded.input(), stack.material);
        int matMaximum = recipeContent == 0 ? 0 : recipeInputRequired * RECIPE_Z_CAPACITY / recipeContent;
        int amountStored = getQuantaFromType(recipeStack, stack.material);

        return amountStored < matMaximum && getQuantaFromType(recipeStack, null) < RECIPE_Z_CAPACITY;
    }

    /**
     * CE: {@code TileEntityCrucible.pour}. Note: CE's own overflow check here compares
     * {@code recipeInputRequired + stack.amount <= matMaximum} rather than
     * {@code amountStored + stack.amount} (the check {@link #canAcceptPartialPour} itself uses just
     * above) - looks like a real CE asymmetry/bug, ported verbatim rather than silently "corrected"
     * per this task's ground rule to port CE's actual behavior, not invented balance.
     */
    @Override
    public Mats.MaterialStack pour(Level level, BlockPos pos, double dX, double dY, double dZ, Direction side, Mats.MaterialStack stack) {
        CrucibleRecipe loaded = getLoadedRecipe();

        if (loaded == null) {
            int amount = getQuantaFromType(wasteStack, null);
            if (amount + stack.amount <= WASTE_Z_CAPACITY) {
                addToStack(wasteStack, stack.copy());
                return null;
            }
            int toAdd = WASTE_Z_CAPACITY - amount;
            addToStack(wasteStack, new Mats.MaterialStack(stack.material, toAdd));
            return new Mats.MaterialStack(stack.material, stack.amount - toAdd);
        }

        int recipeContent = loaded.getInputAmount();
        int recipeInputRequired = getQuantaFromType(loaded.input(), stack.material);
        int matMaximum = recipeContent == 0 ? 0 : recipeInputRequired * RECIPE_Z_CAPACITY / recipeContent;

        if (recipeInputRequired + stack.amount <= matMaximum) {
            addToStack(recipeStack, stack.copy());
            return null;
        }

        int toAdd = matMaximum - stack.amount;
        toAdd = Math.min(toAdd, RECIPE_Z_CAPACITY - getQuantaFromType(recipeStack, null));
        addToStack(recipeStack, new Mats.MaterialStack(stack.material, toAdd));
        return new Mats.MaterialStack(stack.material, stack.amount - toAdd);
    }

    @Override
    public boolean canAcceptPartialFlow(Level level, BlockPos pos, Direction side, Mats.MaterialStack stack) {
        return false;
    }

    @Override
    public Mats.MaterialStack flow(Level level, BlockPos pos, Direction side, Mats.MaterialStack stack) {
        return null;
    }

    // ==================== IControlReceiver ====================

    @Override
    public boolean hasPermission(Player player) {
        return isUseableByPlayer(player);
    }

    /**
     * Recipe-name selection from the GUI's recipe-cycle zone. A minimal, Crucible-specific control
     * shape (a single {@code "recipe"} string key) rather than CE's generic
     * {@code GUIScreenRecipeSelector} index/selection contract - see
     * {@code docs/phase7/crucible_core.md}'s "Recommended shape" #7 for why the narrower,
     * machine-specific picker was chosen over porting the shared 331-line generic widget.
     */
    @Override
    public void receiveControl(CompoundTag data) {
        if (data.contains("recipe")) {
            this.recipe = data.getString("recipe");
            setChanged();
            dataChanged();
        }
    }

    // ==================== NBT / network ====================

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putString("recipe", recipe);
        tag.putInt("heat", heat);
        tag.putInt("progress", progress);
        tag.putIntArray("rec", toIntArray(recipeStack));
        tag.putIntArray("was", toIntArray(wasteStack));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        recipe = tag.getString("recipe");
        heat = tag.getInt("heat");
        progress = tag.getInt("progress");
        recipeStack = fromIntArray(tag.getIntArray("rec"));
        wasteStack = fromIntArray(tag.getIntArray("was"));
    }

    private static int[] toIntArray(List<Mats.MaterialStack> stacks) {
        int[] arr = new int[stacks.size() * 2];
        for (int i = 0; i < stacks.size(); i++) {
            Mats.MaterialStack s = stacks.get(i);
            arr[i * 2] = s.material.id;
            arr[i * 2 + 1] = s.amount;
        }
        return arr;
    }

    private static List<Mats.MaterialStack> fromIntArray(int[] arr) {
        List<Mats.MaterialStack> list = new ArrayList<>(arr.length / 2);
        for (int i = 0; i < arr.length / 2; i++) {
            NTMMaterial mat = Mats.matById.get(arr[i * 2]);
            if (mat != null) list.add(new Mats.MaterialStack(mat, arr[i * 2 + 1]));
        }
        return list;
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeInt(progress);
        buf.writeInt(heat);
        buf.writeUtf(recipe);
        writeStackList(buf, recipeStack);
        writeStackList(buf, wasteStack);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        progress = buf.readInt();
        heat = buf.readInt();
        recipe = buf.readUtf();
        recipeStack = readStackList(buf);
        wasteStack = readStackList(buf);
    }

    private static void writeStackList(RegistryFriendlyByteBuf buf, List<Mats.MaterialStack> stacks) {
        buf.writeShort(stacks.size());
        for (Mats.MaterialStack s : stacks) {
            buf.writeInt(s.material == null ? -1 : s.material.id);
            buf.writeInt(s.amount);
        }
    }

    private static List<Mats.MaterialStack> readStackList(RegistryFriendlyByteBuf buf) {
        int len = buf.readShort() & 0xFFFF;
        List<Mats.MaterialStack> list = new ArrayList<>(len);
        for (int i = 0; i < len; i++) {
            int id = buf.readInt();
            int amount = buf.readInt();
            if (id >= 0) {
                NTMMaterial mat = Mats.matById.get(id);
                if (mat != null) list.add(new Mats.MaterialStack(mat, amount));
            }
        }
        return list;
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new MachineCrucibleMenu(containerId, playerInventory, this);
    }
}
