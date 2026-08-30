package com.hbm.items.machine;

import com.hbm.inventory.material.MaterialShapes;
import com.hbm.inventory.material.Mats;
import com.hbm.inventory.material.NTMMaterial;
import com.hbm.items.ItemBase;
import com.hbm.main.MainRegistry;
import com.hbm.util.i18n.I18nUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Blocks;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Foundry mold selector. CE modeled ~29 mold ids (including hand-authored multi-material molds
 * producing bespoke tool/casing items) as metadata on one registry entry; this port keeps a single
 * {@code mold} item with the selected mold as a data component ({@link MachineDataComponents#MOLD_ID}),
 * per the porting plan's recommendation to decouple mold selection from material entirely - the
 * (Phase 2) foundry tile entity picks the material at insert time, exactly as CE's own
 * {@code getOutput(NTMMaterial)} does.
 * <p>
 * Only the material-shape-driven molds (CE's {@code MoldShape}/{@code MoldBlock}, resolved purely
 * against {@link MaterialShapes}/{@link com.hbm.inventory.material.Mats}, ~20 entries) are ported.
 * CE's {@code MoldMulti}/{@code MoldSingle} variants (blade, blades, stamp, c9, c50, pipes molds)
 * hardcode output stacks from specific items in {@code items.tool}/{@code items.special}/
 * {@code items.weapon} that are outside this area's scope and not necessarily registered yet; they
 * are deliberately left out of this pass rather than guessed at, and can be added as additional
 * {@link MoldEntry} instances once those items exist.
 */
public class ItemMold extends ItemBase {

    /** CE's {@code blockOverrides}: BLOCK-shape molds for materials whose "block" is a vanilla block, not an autogen item. */
    private static final Map<NTMMaterial, ItemStack> BLOCK_OVERRIDES = Map.of(
            Mats.MAT_STONE, new ItemStack(Blocks.STONE),
            Mats.MAT_OBSIDIAN, new ItemStack(Blocks.OBSIDIAN));

    public static final List<MoldEntry> MOLDS = buildMolds();

    public ItemMold(Properties properties) {
        super(properties);
    }

    private static List<MoldEntry> buildMolds() {
        List<MoldEntry> molds = new ArrayList<>();
        molds.add(new MoldEntry(0, "nugget", false, MaterialShapes.NUGGET, 1));
        molds.add(new MoldEntry(1, "billet", false, MaterialShapes.BILLET, 1));
        molds.add(new MoldEntry(2, "ingot", false, MaterialShapes.INGOT, 1));
        molds.add(new MoldEntry(3, "plate", false, MaterialShapes.PLATE, 1));
        molds.add(new MoldEntry(4, "wire", false, MaterialShapes.WIRE, 8));
        molds.add(new MoldEntry(8, "shell", false, MaterialShapes.SHELL, 1));
        molds.add(new MoldEntry(9, "pipe", false, MaterialShapes.PIPE, 1));
        molds.add(new MoldEntry(10, "ingots", true, MaterialShapes.INGOT, 9));
        molds.add(new MoldEntry(11, "plates", true, MaterialShapes.PLATE, 9));
        molds.add(new MoldEntry(12, "block", true, MaterialShapes.BLOCK, 1));
        molds.add(new MoldEntry(15, "plates_cast", true, MaterialShapes.CASTPLATE, 3));
        molds.add(new MoldEntry(19, "plate_cast", false, MaterialShapes.CASTPLATE, 1));
        molds.add(new MoldEntry(20, "wire_dense", false, MaterialShapes.DENSEWIRE, 1));
        molds.add(new MoldEntry(21, "wires_dense", true, MaterialShapes.DENSEWIRE, 9));
        molds.add(new MoldEntry(22, "barrel_light", false, MaterialShapes.LIGHTBARREL, 1));
        molds.add(new MoldEntry(23, "barrel_heavy", false, MaterialShapes.HEAVYBARREL, 1));
        molds.add(new MoldEntry(24, "receiver_light", false, MaterialShapes.LIGHTRECEIVER, 1));
        molds.add(new MoldEntry(25, "receiver_heavy", false, MaterialShapes.HEAVYRECEIVER, 1));
        molds.add(new MoldEntry(26, "mechanism", false, MaterialShapes.MECHANISM, 1));
        molds.add(new MoldEntry(27, "stock", false, MaterialShapes.STOCK, 1));
        molds.add(new MoldEntry(28, "grip", false, MaterialShapes.GRIP, 1));
        return List.copyOf(molds);
    }

    public static int getMoldId(ItemStack stack) {
        return stack.getOrDefault(MachineDataComponents.MOLD_ID.get(), 0);
    }

    public static void setMoldId(ItemStack stack, int moldId) {
        stack.set(MachineDataComponents.MOLD_ID.get(), moldId);
    }

    public static MoldEntry getMold(ItemStack stack) {
        int id = getMoldId(stack);
        for (MoldEntry mold : MOLDS) {
            if (mold.id == id) return mold;
        }
        return MOLDS.get(0);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        MoldEntry mold = getMold(stack);
        tooltip.add(Component.literal(mold.getTitle()).withStyle(ChatFormatting.YELLOW));
        tooltip.add(Component.literal(mold.large ? "Foundry Basin" : "Foundry Mold").withStyle(mold.large ? ChatFormatting.RED : ChatFormatting.GOLD));
    }

    public record MoldEntry(int id, String name, boolean large, MaterialShapes shape, int amount) {

        /** Resolves the cast product for the given material, matching CE's {@code getOutput(NTMMaterial)}. */
        public ItemStack getOutput(NTMMaterial mat) {
            ItemStack override = BLOCK_OVERRIDES.get(mat);
            if (override != null) return override.copy();

            ResourceLocation registryName = ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, shape.buildRegistryName(mat));
            return BuiltInRegistries.ITEM.getOptional(registryName)
                    .map(item -> new ItemStack(item, amount))
                    .orElse(ItemStack.EMPTY);
        }

        public int getCost() {
            return shape.q(amount);
        }

        public String getTitle() {
            return I18nUtil.resolveKey("shape." + shape.registryName) + " x" + amount;
        }
    }
}
