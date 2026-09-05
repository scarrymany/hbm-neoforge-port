package com.hbm.inventory.recipes.loader;

import com.hbm.inventory.RecipesCommon;
import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.recipes.loader.GenericRecipes.IOutput;
import com.hbm.items.machine.ItemFluidIcon;
import com.hbm.main.MainRegistry;
import com.hbm.util.BobMathUtil;
import com.hbm.util.i18n.I18nUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Minimal compile-time stand-in for CE's {@code com.hbm.inventory.recipes.loader.GenericRecipe}.
 * <p>
 * CE's real {@code GenericRecipe} ({@code upstream/hbm-ce/.../recipes/loader/GenericRecipe.java},
 * ~200 lines) is a full machine-recipe description: N {@code AStack} inputs, one fluid input, N
 * chance-weighted {@code IOutput} outputs, one fluid output, duration/power, plus this
 * pool/localization metadata — built by 9 of CE's ~60 recipe classes ({@code AssemblyMachineRecipes},
 * {@code ChemicalPlantRecipes}, {@code FusionRecipes}, etc — see
 * {@code docs/phase2/items_tool_machine_coupling_and_recipe_system.md} Part B). Porting that whole
 * shape now would mean also porting {@code IOutput}/{@code ChanceOutput}/{@code ChanceOutputMulti},
 * {@code FluidStack}'s still-missing {@code Codec}, {@code GeneralConfig} and {@code I18nUtil} —
 * none of which any machine in this port actually needs yet, since no {@code GenericRecipe}-shaped
 * machine package has landed. Per that report's own design (point 3 under "Key design/API
 * decisions": blueprint pools become a JSON-{@code Recipe<?>}-derived index going forward, not
 * authored via this class) and this task's explicit scope limit ("do NOT port CE's actual ~60
 * machine recipe classes' data in this pass"), this class only carries the slice
 * {@code ItemBlueprints}/{@code ItemBlueprintFolder} actually read today: a stable internal name,
 * the pool/localization-name fields those two items' tooltip needs, and a NeoForge-native
 * {@link Component}-returning {@link #getLocalizedName()} (CE's version returned a plain
 * {@code String}; these two items' tooltip is a {@code List<Component>}).
 * <p>
 * Whoever ports a real "{@code GenericRecipe}-shaped" machine (the 9 named above) should extend or
 * replace this with the real input/output/duration/power fields at that time — this class is
 * intentionally not that; see {@link GenericRecipes} for the same scope note.
 */
public class GenericRecipe {

    protected final String name;
    protected String nameWrapper;
    protected boolean customLocalization;
    protected ItemStack icon = ItemStack.EMPTY;
    private String[] pools;
    public RecipesCommon.AStack[] inputItem;
    public FluidStack[] inputFluid;
    public IOutput[] outputItem;
    public FluidStack[] outputFluid;
    public int duration;
    public long power;
    public String autoSwitchGroup;

    public GenericRecipe(String name) {
        this.name = name;
    }

    public String getInternalName() {
        return name;
    }

    public boolean isPooled() {
        return pools != null;
    }

    public String[] getPools() {
        return pools;
    }

    public boolean isPartOfPool(String lookingFor) {
        if (!isPooled()) return false;
        for (String pool : pools) if (pool.equals(lookingFor)) return true;
        return false;
    }

    public GenericRecipe setNamed() {
        this.customLocalization = true;
        return this;
    }

    public GenericRecipe setNameWrapper(String wrapper) {
        this.nameWrapper = wrapper;
        return this;
    }

    public GenericRecipe setIcon(ItemStack icon) {
        this.icon = icon;
        return this;
    }

    public GenericRecipe setIcon(Item item) {
        return setIcon(new ItemStack(item));
    }

    public GenericRecipe setIcon(Block block) {
        return setIcon(new ItemStack(block));
    }

    /** Registers this recipe under one or more blueprint pools — see {@link GenericRecipes#addToPool}. */
    public GenericRecipe setDuration(int duration) {
        this.duration = duration;
        return this;
    }

    public GenericRecipe setPower(long power) {
        this.power = power;
        return this;
    }

    public GenericRecipe inputFluids(FluidStack... input) {
        this.inputFluid = input;
        return this;
    }

    public GenericRecipe outputFluids(FluidStack... output) {
        this.outputFluid = output;
        return this;
    }

    public GenericRecipe outputItems(ItemStack... output) {
        this.outputItem = new IOutput[output.length];
        for (int i = 0; i < output.length; i++) {
            this.outputItem[i] = new GenericRecipes.ChanceOutput(output[i]);
        }
        return this;
    }

    public GenericRecipe setPools(String... pools) {
        this.pools = pools;
        for (String pool : pools) GenericRecipes.addToPool(pool, this);
        return this;
    }

    /** CE {@code GenericRecipe.getIcon}: first output item, else fluid icon, else {@code nothing}. */
    public ItemStack getIcon() {
        if (icon == null || icon.isEmpty()) {
            if (outputItem != null && outputItem.length > 0) {
                ItemStack single = outputItem[0].getSingle();
                if (single != null && !single.isEmpty()) icon = single.copy();
            }
            if ((icon == null || icon.isEmpty()) && outputFluid != null && outputFluid.length > 0) {
                Item fluidIcon = BuiltInRegistries.ITEM.get(
                        ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "fluid_icon"));
                icon = ItemFluidIcon.make(fluidIcon, outputFluid[0]);
            }
            if (icon == null || icon.isEmpty()) {
                icon = new ItemStack(BuiltInRegistries.ITEM.get(
                        ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "nothing")));
            }
        }
        return icon;
    }

    public List<Component> print() {
        List<Component> list = new ArrayList<>();
        list.add(getLocalizedName().copy().withStyle(ChatFormatting.YELLOW));
        if (Screen.hasShiftDown()) {
            list.add(Component.literal("Internal: " + getInternalName()).withStyle(ChatFormatting.DARK_GRAY));
        }
        if (duration > 0) {
            list.add(Component.literal(I18nUtil.resolveKey("gui.recipe.duration") + ": " + (duration / 20D) + "s")
                    .withStyle(ChatFormatting.RED));
        }
        if (power > 0) {
            list.add(Component.literal(I18nUtil.resolveKey("gui.recipe.consumption") + ": "
                    + BobMathUtil.getShortNumber(power) + "HE/t").withStyle(ChatFormatting.RED));
        }
        list.add(Component.literal(I18nUtil.resolveKey("gui.recipe.input") + ":").withStyle(ChatFormatting.BOLD));
        if (inputItem != null) {
            for (RecipesCommon.AStack stack : inputItem) {
                ItemStack display = stack.extractForCyclingDisplay(20);
                list.add(Component.literal("  " + display.getCount() + "x " + display.getHoverName().getString())
                        .withStyle(ChatFormatting.GRAY));
            }
        }
        if (inputFluid != null) {
            for (FluidStack fluid : inputFluid) {
                String pressure = fluid.pressure == 0 ? ""
                        : " " + I18nUtil.resolveKey("gui.recipe.atPressure") + " " + fluid.pressure + " PU";
                list.add(Component.literal("  " + fluid.fill + "mB " + fluid.type.getLocalizedName().getString() + pressure)
                        .withStyle(ChatFormatting.BLUE));
            }
        }
        list.add(Component.literal(I18nUtil.resolveKey("gui.recipe.output") + ":").withStyle(ChatFormatting.BOLD));
        if (outputItem != null) {
            for (IOutput output : outputItem) {
                ItemStack single = output.getSingle();
                if (single == null || single.isEmpty()) continue;
                list.add(Component.literal("  " + single.getCount() + "x " + single.getHoverName().getString()));
            }
        }
        if (outputFluid != null) {
            for (FluidStack fluid : outputFluid) {
                String pressure = fluid.pressure == 0 ? ""
                        : " " + I18nUtil.resolveKey("gui.recipe.atPressure") + " " + fluid.pressure + " PU";
                list.add(Component.literal("  " + fluid.fill + "mB " + fluid.type.getLocalizedName().getString() + pressure)
                        .withStyle(ChatFormatting.BLUE));
            }
        }
        return list;
    }

    /** CE {@code GenericRecipe.matchesSearch} — localized-name substring. */
    public boolean matchesSearch(String substring) {
        return getLocalizedName().getString().toLowerCase(Locale.US)
                .contains(substring.toLowerCase(Locale.US));
    }

    /**
     * @return this recipe's display name: {@code name} resolved as a translation key when
     * {@link #setNamed()} was called, otherwise the icon's own hover name (falling back to a
     * literal of the internal name if no icon was set), optionally wrapped via
     * {@link #setNameWrapper(String)}.
     */
    public Component getLocalizedName() {
        Component base = customLocalization
                ? Component.translatable(name)
                : (getIcon().isEmpty() ? Component.literal(name) : getIcon().getHoverName());
        return nameWrapper != null ? Component.translatable(nameWrapper, base) : base;
    }
}
