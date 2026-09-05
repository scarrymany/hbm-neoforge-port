package com.hbm.blockentity.machine;

import com.hbm.api.energymk2.IEnergyReceiverMK2;
import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.MachineBaseBlockEntity;
import com.hbm.blocks.generic.BlockSellafield;
import com.hbm.blocks.generic.WastelandVirusBlocks;
import com.hbm.inventory.container.machine.MachineShredderMenu;
import com.hbm.inventory.recipes.HbmSimpleRecipe;
import com.hbm.inventory.recipes.ProcessingRecipes;
import com.hbm.items.machine.ItemBlades;
import com.hbm.lib.Library;
import com.hbm.main.MainRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * Ported from CE's {@code com.hbm.tileentity.machine.TileEntityMachineShredder} (335 lines, read in
 * full) - see {@code docs/phase2/machines_shredder_assembler_crystallizer_mixer.md}'s per-machine
 * detail for the full slot/power/recipe breakdown this class implements.
 * <p>
 * <b>Slots</b> (30 total, unchanged from CE): 0-8 input, 9-26 output (take-only via
 * {@link #canExtractItem}), 27/28 left/right {@link ItemBlades}, 29 battery.
 * <p>
 * <b>Recipe lookup</b>: {@link ProcessingRecipes#SHREDDER_TYPE} ({@link HbmSimpleRecipe}, JSON-backed -
 * see that class's own javadoc), replacing CE's hardcoded {@code ShredderRecipes} HashMap.
 * {@code ModItems.scrap} is registered; the explicit {@code scrap → dust} row is JSON
 * ({@code ShredderRecipes.java:208}). Miss-fallback emits scrap Exact CE
 * {@code ShredderRecipes.java:103-115}.
 * OreDict {@code registerPost} auto-dust is 1.12-integration, not ported —
 * TODO(CE: ShredderRecipes.java:119-201).
 *         {@code dustLapis} members other than {@code powder_lapis} —
 * TODO(CE: ShredderRecipes.java:246).
 * Old {@code ItemBedrockOre} wildcard (not {@code bedrock_ore_new_*}) —
 * TODO(CE: ShredderRecipes.java:348).
 * Sellafield LEVEL 0–5 yields Exact CE {@code ShredderRecipes.java:352-357}
 * ({@code 1/2/3/5/7/15} {@code scrap_nuclear}).
 * Bobbleheads (block not registered) — TODO(CE: ShredderRecipes.java:400-402).
 * GC/AR moon-turf (commented out in CE) — TODO(CE: ShredderRecipes.java:412-423).
 */
public class MachineShredderBlockEntity extends MachineBaseBlockEntity
        implements IEnergyReceiverMK2, ITickableBE, MenuProvider {

    public static final long MAX_POWER = 10_000L;
    public static final int PROCESSING_SPEED = 60;

    private static final int INPUT_START = 0;
    private static final int INPUT_END = 8;
    private static final int OUTPUT_START = 9;
    private static final int OUTPUT_END = 26;
    private static final int BLADE_LEFT = 27;
    private static final int BLADE_RIGHT = 28;
    private static final int BATTERY_SLOT = 29;

    private static final int[] ALL_SLOTS = new int[30];

    static {
        for (int i = 0; i < ALL_SLOTS.length; i++) ALL_SLOTS[i] = i;
    }

    private long power;
    private int progress;

    public MachineShredderBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, 30, false, true);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.machineShredder");
    }

    @Override
    public int[] getAccessibleSlotsFromSide(Direction side) {
        return ALL_SLOTS;
    }

    private Optional<HbmSimpleRecipe> recipeFor(ItemStack stack) {
        if (level == null || stack.isEmpty()) return Optional.empty();
        return level.getRecipeManager()
                .getRecipeFor(ProcessingRecipes.SHREDDER_TYPE.get(), new SingleRecipeInput(stack), level)
                .map(RecipeHolder::value);
    }

    /** CE {@code ShredderRecipes.java:352-357} meta 0–5. */
    private static final int[] SELLAFIELD_SCRAP = {1, 2, 3, 5, 7, 15};

    /** CE {@code ShredderRecipes.getShredderResult} — miss / empty → {@code scrap}. */
    private ItemStack shredderResult(ItemStack stack) {
        if (stack.isEmpty()) return scrapStack();
        if (stack.is(WastelandVirusBlocks.SELLAFIELD.get().asItem())) {
            int meta = BlockSellafield.itemLevel(stack);
            int count = SELLAFIELD_SCRAP[Mth.clamp(meta, 0, SELLAFIELD_SCRAP.length - 1)];
            Item scrapNuc = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "scrap_nuclear"));
            return scrapNuc == Items.AIR ? scrapStack() : new ItemStack(scrapNuc, count);
        }
        Optional<HbmSimpleRecipe> recipe = recipeFor(stack);
        if (recipe.isPresent()) {
            ItemStack out = recipe.get().getResultItem(level.registryAccess());
            if (!out.isEmpty()) return out.copy();
        }
        return scrapStack();
    }

    private static ItemStack scrapStack() {
        Item scrap = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "scrap"));
        return scrap == Items.AIR ? ItemStack.EMPTY : new ItemStack(scrap);
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot >= INPUT_START && slot <= INPUT_END) {
            // CE TileEntityMachineShredder.java:78 — getShredderResult is never null
            return !(stack.getItem() instanceof ItemBlades);
        }
        if (slot == BATTERY_SLOT) return Library.isBattery(stack);
        return (slot == BLADE_LEFT || slot == BLADE_RIGHT) && stack.getItem() instanceof ItemBlades;
    }

    @Override
    public boolean canInsertItem(int slot, ItemStack itemStack) {
        if (slot != BLADE_LEFT && slot != BLADE_RIGHT && itemStack.getItem() instanceof ItemBlades) return false;
        if (slot != BATTERY_SLOT && Library.isDischargeableBattery(itemStack)) return false;
        return isItemValidForSlot(slot, itemStack);
    }

    @Override
    public boolean canExtractItem(int slot, ItemStack itemStack, int amount) {
        if (slot >= OUTPUT_START && slot <= OUTPUT_END) return true;
        if (slot == BLADE_LEFT || slot == BLADE_RIGHT) {
            return itemStack.getMaxDamage() > 0 && itemStack.getDamageValue() == itemStack.getMaxDamage();
        }
        return false;
    }

    public boolean hasPower() {
        return power > 0;
    }

    public boolean isProcessing() {
        return progress > 0;
    }

    public long getPower() {
        return power;
    }

    public long getMaxPower() {
        return MAX_POWER;
    }

    @Override
    public void setPower(long i) {
        power = i;
    }

    public int getProgress() {
        return progress;
    }

    public int getProgressScaled(int scale) {
        return (progress * scale) / PROCESSING_SPEED;
    }

    /** Gear wear tier of the given blade slot: 0 (no blade), 1 (fresh-to-half), 2 (half-to-worn), 3 (fully worn, needs replacing) - matches CE's {@code getGearLeft}/{@code getGearRight} exactly. */
    private int gearOf(int bladeSlot) {
        ItemStack blade = inventory.getStackInSlot(bladeSlot);
        if (blade.isEmpty() || !(blade.getItem() instanceof ItemBlades)) return 0;
        if (blade.getMaxDamage() == 0) return 1;
        if (blade.getDamageValue() < blade.getMaxDamage() / 2) return 1;
        if (blade.getDamageValue() != blade.getMaxDamage()) return 2;
        return 3;
    }

    public boolean canProcess() {
        int left = gearOf(BLADE_LEFT);
        int right = gearOf(BLADE_RIGHT);
        if (left <= 0 || left >= 3 || right <= 0 || right >= 3) return false;

        for (int i = INPUT_START; i <= INPUT_END; i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (!stack.isEmpty() && hasSpace(stack)) return true;
        }
        return false;
    }

    public boolean hasSpace(ItemStack stack) {
        ItemStack result = shredderResult(stack);
        if (result.isEmpty()) return false;

        int spaceLeft = 0;
        for (int i = OUTPUT_START; i <= OUTPUT_END; i++) {
            ItemStack slotStack = inventory.getStackInSlot(i);
            if (slotStack.isEmpty()) {
                spaceLeft += result.getMaxStackSize();
            } else if (ItemStack.isSameItemSameComponents(slotStack, result)) {
                spaceLeft += slotStack.getMaxStackSize() - slotStack.getCount();
            }
        }
        return spaceLeft >= result.getCount();
    }

    public void processItem() {
        for (int inSlot = INPUT_START; inSlot <= INPUT_END; inSlot++) {
            ItemStack inp = inventory.getStackInSlot(inSlot);
            if (inp.isEmpty() || !hasSpace(inp)) continue;

            ItemStack outp = shredderResult(inp);
            int itemsLeft = outp.getCount();

            for (int outSlot = OUTPUT_START; outSlot <= OUTPUT_END && itemsLeft > 0; outSlot++) {
                ItemStack slotStack = inventory.getStackInSlot(outSlot);
                if (!slotStack.isEmpty() && ItemStack.isSameItemSameComponents(slotStack, outp)) {
                    int space = slotStack.getMaxStackSize() - slotStack.getCount();
                    if (space > 0) {
                        int amount = Math.min(itemsLeft, space);
                        slotStack.grow(amount);
                        itemsLeft -= amount;
                    }
                }
            }
            for (int outSlot = OUTPUT_START; outSlot <= OUTPUT_END && itemsLeft > 0; outSlot++) {
                if (inventory.getStackInSlot(outSlot).isEmpty()) {
                    int amount = Math.min(itemsLeft, outp.getMaxStackSize());
                    ItemStack newStack = outp.copy();
                    newStack.setCount(amount);
                    inventory.setStackInSlot(outSlot, newStack);
                    itemsLeft -= amount;
                }
            }

            inp.shrink(1);
            if (inp.isEmpty()) inventory.setStackInSlot(inSlot, ItemStack.EMPTY);
        }
    }

    @Override
    public void updateEntity() {
        if (level == null || level.isClientSide) return;

        if (hasPower() && canProcess()) {
            progress++;
            power -= 5;

            if (progress == PROCESSING_SPEED) {
                for (int bladeSlot = BLADE_LEFT; bladeSlot <= BLADE_RIGHT; bladeSlot++) {
                    ItemStack blade = inventory.getStackInSlot(bladeSlot);
                    if (blade.getMaxDamage() > 0) blade.setDamageValue(blade.getDamageValue() + 1);
                }
                progress = 0;
                processItem();
            }
        } else {
            progress = 0;
        }

        power = Library.chargeTEFromItems(inventory, BATTERY_SLOT, power, MAX_POWER);

        dataChanged();
        networkPackMK2(15);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putLong("powerTime", power);
        tag.putInt("progress", progress);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        power = tag.getLong("powerTime");
        progress = tag.getInt("progress");
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeLong(power);
        buf.writeInt(progress);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        power = buf.readLong();
        progress = buf.readInt();
    }

    public long getPowerScaled(int scale) {
        return MAX_POWER > 0 ? (power * scale) / MAX_POWER : 0;
    }

    public int getDiFurnaceProgressScaled(int scale) {
        return PROCESSING_SPEED > 0 ? (progress * scale) / PROCESSING_SPEED : 0;
    }

    public int getGearLeft() {
        ItemStack blade = inventory.getStackInSlot(BLADE_LEFT);
        if (blade.isEmpty() || !(blade.getItem() instanceof ItemBlades)) return 0;
        if (blade.getMaxDamage() == 0) return 1;
        int damage = blade.getDamageValue();
        if (damage < blade.getMaxDamage() / 2) return 1;
        if (damage != blade.getMaxDamage()) return 2;
        return 3;
    }

    public int getGearRight() {
        ItemStack blade = inventory.getStackInSlot(BLADE_RIGHT);
        if (blade.isEmpty() || !(blade.getItem() instanceof ItemBlades)) return 0;
        if (blade.getMaxDamage() == 0) return 1;
        int damage = blade.getDamageValue();
        if (damage < blade.getMaxDamage() / 2) return 1;
        if (damage != blade.getMaxDamage()) return 2;
        return 3;
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new MachineShredderMenu(containerId, playerInventory, this);
    }
}
