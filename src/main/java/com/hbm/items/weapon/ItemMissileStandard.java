package com.hbm.items.weapon;

import com.hbm.items.ItemBase;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * Ported from CE's {@code com.hbm.items.weapon.ItemMissileStandard} (104 lines, read in full) -
 * the 28-preset item class. Each of the 28 real {@code missile_*} items registered in {@link
 * MissileItems} is one instance of this class, 1:1-paired (via {@code getMissileItemForInfo()})
 * with one concrete {@code EntityMissileTierN}/{@code EntityMissileAntiBallistic}/{@code
 * EntityMissileShuttle}/{@code EntityMissileStealth} Java subclass in {@code com.hbm.entity.missile}.
 * <p>
 * {@link MissileFormFactor}/{@link MissileTier}/{@link MissileFuel} are a completely separate enum
 * family from {@link ItemMissile}'s {@code PartSize}/{@code WarheadType}/{@code FuelType} - see
 * {@code docs/phase3/missile_framework.md}'s Open questions for why these must never be conflated.
 */
public class ItemMissileStandard extends ItemBase {

    public final MissileFormFactor formFactor;
    public final MissileTier tier;
    public final MissileFuel fuel;

    public int fuelCap;
    public boolean launchable = true;

    public ItemMissileStandard(Properties properties, MissileFormFactor form, MissileTier tier) {
        this(properties, form, tier, form.defaultFuel);
    }

    public ItemMissileStandard(Properties properties, MissileFormFactor form, MissileTier tier, MissileFuel fuel) {
        super(properties.stacksTo(1));
        this.formFactor = form;
        this.tier = tier;
        this.fuel = fuel;
        this.fuelCap = fuel.defaultCap;
    }

    public ItemMissileStandard notLaunchable() {
        this.launchable = false;
        return this;
    }

    public ItemMissileStandard setFuelCap(int fuelCap) {
        this.fuelCap = fuelCap;
        return this;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> list, TooltipFlag flag) {
        list.add(Component.literal(this.tier.display).withStyle(ChatFormatting.ITALIC));

        if (!this.launchable) {
            list.add(Component.literal("Not launchable!").withStyle(ChatFormatting.RED));
        } else {
            list.add(Component.literal("Fuel: " + this.fuel.display));
            if (this.fuelCap > 0) {
                list.add(Component.literal("Fuel capacity: " + this.fuelCap + "mB"));
            }
            super.appendHoverText(stack, context, list, flag);
        }
    }

    public enum MissileFormFactor {
        ABM(MissileFuel.SOLID),
        MICRO(MissileFuel.SOLID),
        V2(MissileFuel.ETHANOL_PEROXIDE),
        STRONG(MissileFuel.KEROSENE_PEROXIDE),
        HUGE(MissileFuel.KEROSENE_LOXY),
        ATLAS(MissileFuel.JETFUEL_LOXY),
        OTHER(MissileFuel.KEROSENE_PEROXIDE);

        public final MissileFuel defaultFuel;

        MissileFormFactor(MissileFuel defaultFuel) {
            this.defaultFuel = defaultFuel;
        }
    }

    public enum MissileTier {
        TIER0("Tier 0"),
        TIER1("Tier 1"),
        TIER2("Tier 2"),
        TIER3("Tier 3"),
        TIER4("Tier 4");

        public final String display;

        MissileTier(String display) {
            this.display = display;
        }
    }

    public enum MissileFuel {
        SOLID(ChatFormatting.GOLD + "Solid Fuel (pre-fueled)", 0),
        ETHANOL_PEROXIDE(ChatFormatting.AQUA + "Ethanol / Hydrogen Peroxide", 4_000),
        KEROSENE_PEROXIDE(ChatFormatting.BLUE + "Kerosene / Hydrogen Peroxide", 8_000),
        KEROSENE_LOXY(ChatFormatting.LIGHT_PURPLE + "Kerosene / Liquid Oxygen", 12_000),
        JETFUEL_LOXY(ChatFormatting.RED + "Jet Fuel / Liquid Oxygen", 16_000);

        public final String display;
        public final int defaultCap;

        MissileFuel(String display, int defaultCap) {
            this.display = display;
            this.defaultCap = defaultCap;
        }
    }
}
