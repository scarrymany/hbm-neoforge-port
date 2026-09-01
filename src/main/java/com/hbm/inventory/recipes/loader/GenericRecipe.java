package com.hbm.inventory.recipes.loader;

import com.hbm.inventory.RecipesCommon;
import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.recipes.loader.GenericRecipes.IOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

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

    /**
     * @return this recipe's display name: {@code name} resolved as a translation key when
     * {@link #setNamed()} was called, otherwise the icon's own hover name (falling back to a
     * literal of the internal name if no icon was set), optionally wrapped via
     * {@link #setNameWrapper(String)}.
     */
    public Component getLocalizedName() {
        Component base = customLocalization
                ? Component.translatable(name)
                : (icon.isEmpty() ? Component.literal(name) : icon.getHoverName());
        return nameWrapper != null ? Component.translatable(nameWrapper, base) : base;
    }
}
