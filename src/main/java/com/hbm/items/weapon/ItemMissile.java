package com.hbm.items.weapon;

import com.hbm.entity.missile.EntityMissileCustom;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.util.i18n.I18nUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Consumer;

/**
 * Ported from CE's {@code com.hbm.items.weapon.ItemMissile} (416 lines, read in full) - the
 * {@code mp_*} composable-missile-part factory. Every concrete {@code mp_*} item is one
 * {@code ItemMissile} instance configured via exactly one of the 5 {@code make*} builder calls
 * below, exactly as CE's own {@code ModItems} field declarations do.
 * <p>
 * <b>Dropped from the port, deliberately</b>: CE's constructor self-registered into
 * {@code ModItems.ALL_ITEMS} and set translation key/registry name/creative tab directly on the
 * item instance (1.12 construction-as-registration). All three are DeferredRegister/
 * BuildCreativeModeTabContentsEvent concerns now, owned by {@link MissileItems} - this class only
 * carries the fields the *builders* configure (type/top/bottom/rarity/health/mass/attributes/
 * title/author/witty).
 * <p>
 * {@code parts} (CE's {@code HashMap<Integer, ItemMissile>} keyed by {@code hashCode()}) is not
 * ported - CE never actually reads it back anywhere (confirmed by grep), it exists purely as a
 * registration side-effect with no consumer.
 * <p>
 * {@code copy(String)} (CE's cosmetic-reskin-item mechanism behind the 61 {@code .copy(name)}
 * fuselage variants documented in {@code docs/phase3/missile_framework.md}) is not ported as an
 * instance method - in CE, {@code copy} both builds *and registers* a second item, but this port's
 * registration is owned entirely by {@link MissileItems}'s {@code DeferredRegister} calls, not by
 * item-instance construction. A future cosmetic-clone pass creates each reskin the same way every
 * other item here is created (one more {@code MissileItems.register*(...)} call copying the
 * source's {@code type}/{@code top}/{@code bottom}/{@code health}/{@code attributes}/{@code mass}),
 * not via a revived {@code copy()} method on this class.
 * <p>
 * {@link #setRarity}'s CE body pushes {@code this} onto {@code ItemLootCrate.list10}/{@code
 * list15}/{@code listMisc} - the loot-crate roll tables. {@code com.hbm.items.special.ItemLootCrate}
 * has since landed (see {@code docs/phase3/scattered_military_items.md}) and this side effect is
 * wired for real in {@link #setRarity} below; {@link #rarity} itself is still stored and still
 * surfaces in the tooltip regardless.
 */
public class ItemMissile extends Item {

    public PartType type;
    public PartSize top;
    public PartSize bottom;
    public Rarity rarity;
    public float health;
    public int mass = 0;
    protected String title;
    protected String author;
    protected String witty;

    /**
     * CE's own doc comment on the array shape, preserved verbatim (index meanings vary by
     * {@link #type}):
     * <pre>
     * == Chips ==      [0]: inaccuracy
     * == Warheads ==   [0]: type, [1]: strength/radius/cluster count, [2]: weight
     * == Fuselages ==  [0]: fuel type, [1]: tank size
     * == Stability ==  [0]: inaccuracy mod
     * == Thrusters ==  [0]: fuel type, [1]: consumption, [2]: lift strength
     * </pre>
     */
    public Object[] attributes;

    public ItemMissile(Properties properties) {
        super(properties.stacksTo(1));
    }

    public enum PartType {
        CHIP,
        WARHEAD,
        FUSELAGE,
        FINS,
        THRUSTER
    }

    public enum PartSize {
        // for chips
        ANY,
        // for missile tips and thrusters
        NONE,
        // regular sizes, 1.0m, 1.5m and 2.0m
        SIZE_10(1.0),
        SIZE_15(1.5),
        SIZE_20(2.0),
        // space-grade
        SIZE_25(2.5),
        SIZE_30(3.0);

        public final double radius;

        PartSize() {
            this.radius = 0;
        }

        PartSize(double radius) {
            this.radius = radius;
        }
    }

    public enum WarheadType {

        HE,
        INC,
        BUSTER,
        CLUSTER,
        NUCLEAR,
        TX,
        N2,
        BALEFIRE,
        SCHRAB,
        TAINT,
        CLOUD,
        VOLCANO,
        TURBINE,
        MIRV(null, EntityMissileCustom::mirvSplit),
        APOLLO,
        SATELLITE,

        CUSTOM0, CUSTOM1, CUSTOM2, CUSTOM3, CUSTOM4, CUSTOM5, CUSTOM6, CUSTOM7, CUSTOM8, CUSTOM9;

        /** Overrides that type's impact effect. Only runs serverside. */
        public Consumer<EntityMissileCustom> impactCustom = null;
        /** Runs at the beginning of the missile's update cycle, both client and serverside. */
        public Consumer<EntityMissileCustom> updateCustom = null;
        /** Override for the warhead's name in the missile description. */
        public String labelCustom = null;

        WarheadType() {
        }

        WarheadType(Consumer<EntityMissileCustom> onImpact, Consumer<EntityMissileCustom> onUpdate) {
            impactCustom = onImpact;
            updateCustom = onUpdate;
        }
    }

    public enum FuelType {
        ANY, // used by space-grade fuselages
        KEROSENE,
        SOLID,
        HYDROGEN,
        XENON,
        BALEFIRE,
        HYDRAZINE,
        METHALOX,
        KEROLOX, // oxygen rather than peroxide
    }

    public enum Rarity {

        COMMON("rarity.common"),
        UNCOMMON("rarity.uncommon"),
        RARE("rarity.rare"),
        EPIC("rarity.epic"),
        LEGENDARY("rarity.legendary"),
        SEWS_CLOTHES_AND_SUCKS_HORSE_COCK("rarity.strange");

        public final String name;

        Rarity(String name) {
            this.name = name;
        }
    }

    public ItemMissile makeChip(float inaccuracy) {
        this.type = PartType.CHIP;
        this.top = PartSize.ANY;
        this.bottom = PartSize.ANY;
        this.attributes = new Object[]{inaccuracy};
        return this;
    }

    public ItemMissile makeWarhead(WarheadType type, float punch, float weight, PartSize size) {
        this.type = PartType.WARHEAD;
        this.top = PartSize.NONE;
        this.bottom = size;
        this.attributes = new Object[]{type, punch, weight};
        return this;
    }

    public ItemMissile makeFuselage(FuelType type, float fuel, int mass, PartSize top, PartSize bottom) {
        this.type = PartType.FUSELAGE;
        this.top = top;
        this.bottom = bottom;
        this.mass = mass;
        this.attributes = new Object[]{type, fuel};
        return this;
    }

    public ItemMissile makeStability(float inaccuracy, PartSize size) {
        this.type = PartType.FINS;
        this.top = size;
        this.bottom = size;
        this.attributes = new Object[]{inaccuracy};
        return this;
    }

    public ItemMissile makeThruster(FuelType type, float consumption, float lift, PartSize size) {
        this.type = PartType.THRUSTER;
        this.top = size;
        this.bottom = PartSize.NONE;
        this.attributes = new Object[]{type, consumption, lift};
        return this;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> list, TooltipFlag flag) {
        if (title != null) {
            list.add(Component.literal("\"" + title + "\"").withStyle(ChatFormatting.DARK_PURPLE));
        }

        try {
            switch (type) {
                case CHIP -> list.add(bold(I18nUtil.resolveKey("desc.inaccuracy")).append(gray(" " + (Float) attributes[0] * 100 + "%")));
                case WARHEAD -> {
                    list.add(bold(I18nUtil.resolveKey("desc.size")).append(gray(" " + getSize(bottom))));
                    list.add(bold(I18nUtil.resolveKey("desc.type")).append(gray(" " + getWarhead((WarheadType) attributes[0]))));
                    if (attributes[0] != WarheadType.APOLLO && attributes[0] != WarheadType.SATELLITE) {
                        list.add(bold(I18nUtil.resolveKey("desc.strength")).append(Component.literal(" " + attributes[1]).withStyle(ChatFormatting.RED)));
                    }
                    list.add(bold(I18nUtil.resolveKey("desc.weight")).append(gray(" " + attributes[2] + "t")));
                }
                case FUSELAGE -> {
                    list.add(bold(I18nUtil.resolveKey("desc.topsize")).append(gray(" " + getSize(top))));
                    list.add(bold(I18nUtil.resolveKey("desc.botsize")).append(gray(" " + getSize(bottom))));
                    list.add(bold(I18nUtil.resolveKey("desc.fueltype")).append(gray(" " + getFuel((FuelType) attributes[0]))));
                    list.add(bold(I18nUtil.resolveKey("desc.fuelamnt")).append(gray(" " + attributes[1] + "l")));
                    list.add(bold(I18nUtil.resolveKey("desc.mass", mass)));
                }
                case FINS -> {
                    list.add(bold(I18nUtil.resolveKey("desc.size")).append(gray(" " + getSize(top))));
                    list.add(bold(I18nUtil.resolveKey("desc.inaccuracy")).append(gray(" " + (Float) attributes[0] * 100 + "%")));
                }
                case THRUSTER -> {
                    list.add(bold(I18nUtil.resolveKey("desc.size")).append(gray(" " + getSize(top))));
                    list.add(bold(I18nUtil.resolveKey("desc.fuelamnt")).append(gray(" " + getFuel((FuelType) attributes[0]))));
                }
            }
        } catch (Exception ex) {
            list.add(Component.literal("### I AM ERROR ###"));
        }

        if (type != PartType.CHIP) {
            list.add(bold(I18nUtil.resolveKey("desc.health")).append(Component.literal(" " + health + "HP").withStyle(ChatFormatting.GREEN)));
        }

        if (this.rarity != null) {
            list.add(bold(I18nUtil.resolveKey("desc.rarity")).append(gray(" " + I18nUtil.resolveKey(this.rarity.name))));
        }
        if (author != null) {
            list.add(Component.literal("  " + I18nUtil.resolveKey("desc.author") + " " + author).withStyle(ChatFormatting.WHITE));
        }
        if (witty != null) {
            list.add(Component.literal("   \"" + witty + "\"").withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC));
        }
    }

    private static Component bold(String s) {
        return Component.literal(s).withStyle(ChatFormatting.BOLD);
    }

    private static Component gray(String s) {
        return Component.literal(s).withStyle(ChatFormatting.GRAY);
    }

    public String getSize(PartSize size) {
        return switch (size) {
            case ANY -> I18nUtil.resolveKey("desc.any");
            case SIZE_10 -> "§e1.0m";
            case SIZE_15 -> "§61.5m";
            case SIZE_20 -> "§c2.0m";
            default -> I18nUtil.resolveKey("desc.none");
        };
    }

    public String getWarhead(WarheadType type) {
        if (type.labelCustom != null) return type.labelCustom;

        return switch (type) {
            case HE -> ChatFormatting.YELLOW + I18nUtil.resolveKey("warhead.he");
            case INC -> ChatFormatting.GOLD + I18nUtil.resolveKey("warhead.inc");
            case CLUSTER -> ChatFormatting.GRAY + I18nUtil.resolveKey("warhead.cluster");
            case BUSTER -> ChatFormatting.WHITE + I18nUtil.resolveKey("warhead.buster");
            case NUCLEAR -> ChatFormatting.DARK_GREEN + I18nUtil.resolveKey("warhead.nuclear");
            case TX -> ChatFormatting.DARK_PURPLE + I18nUtil.resolveKey("warhead.tx");
            case N2 -> ChatFormatting.RED + I18nUtil.resolveKey("warhead.n2");
            case BALEFIRE -> ChatFormatting.GREEN + I18nUtil.resolveKey("warhead.balefire");
            case SCHRAB -> ChatFormatting.AQUA + I18nUtil.resolveKey("warhead.schrab");
            case TAINT -> ChatFormatting.DARK_PURPLE + I18nUtil.resolveKey("warhead.taint");
            case CLOUD -> ChatFormatting.LIGHT_PURPLE + I18nUtil.resolveKey("warhead.cloud");
            case TURBINE -> (System.currentTimeMillis() % 1000 < 500 ? ChatFormatting.RED : ChatFormatting.LIGHT_PURPLE) + I18nUtil.resolveKey("warhead.turbine");
            case VOLCANO -> ChatFormatting.DARK_RED + I18nUtil.resolveKey("warhead.volcano");
            case MIRV -> ChatFormatting.DARK_PURPLE + I18nUtil.resolveKey("warhead.mirv");
            case APOLLO -> (System.currentTimeMillis() % 1000 < 500 ? ChatFormatting.GOLD : ChatFormatting.RED) + I18nUtil.resolveKey("warhead.capsule");
            case SATELLITE -> (System.currentTimeMillis() % 1000 < 500 ? ChatFormatting.GOLD : ChatFormatting.RED) + I18nUtil.resolveKey("warhead.satellite");
            default -> ChatFormatting.BOLD + I18nUtil.resolveKey("desc.na");
        };
    }

    public String getFuel(FuelType type) {
        return switch (type) {
            case ANY -> ChatFormatting.GRAY + "Any Liquid Fuel";
            case KEROSENE -> ChatFormatting.LIGHT_PURPLE + I18nUtil.resolveKey("fuel.kerosene");
            case METHALOX -> ChatFormatting.YELLOW + "Natural Gas / Oxygen";
            case KEROLOX -> ChatFormatting.LIGHT_PURPLE + "Kerosene / Oxygen";
            case SOLID -> ChatFormatting.GOLD + I18nUtil.resolveKey("fuel.solid");
            case HYDROGEN -> ChatFormatting.DARK_AQUA + I18nUtil.resolveKey("fuel.hydrogen");
            case XENON -> ChatFormatting.DARK_PURPLE + I18nUtil.resolveKey("fuel.xenon");
            case BALEFIRE -> ChatFormatting.GREEN + I18nUtil.resolveKey("fuel.balefire");
            case HYDRAZINE -> ChatFormatting.AQUA + "Hydrazine";
        };
    }

    @Nullable
    public FluidType getFuel() {
        if (!(attributes[0] instanceof FuelType)) return null;
        return switch ((FuelType) attributes[0]) {
            case KEROSENE, KEROLOX -> Fluids.KEROSENE;
            case METHALOX -> Fluids.GAS;
            case HYDROGEN -> Fluids.HYDROGEN;
            case XENON -> Fluids.XENON;
            case BALEFIRE -> Fluids.BALEFIRE;
            case HYDRAZINE -> Fluids.HYDRAZINE;
            case SOLID -> Fluids.NONE; // requires non-fluid fuel
            default -> null;
        };
    }

    @Nullable
    public FluidType getOxidizer() {
        if (!(attributes[0] instanceof FuelType)) return null;
        return switch ((FuelType) attributes[0]) {
            case KEROLOX, HYDROGEN, METHALOX -> Fluids.OXYGEN;
            case KEROSENE, BALEFIRE -> Fluids.PEROXIDE;
            default -> null;
        };
    }

    /**
     * CE: reads {@code attributes[3]}/{@code attributes[4]} - indices {@link #makeThruster} never
     * populates (that builder only ever fills indices 0-2: fuel type, consumption, lift). Every
     * real CE call site of {@code getThrust()}/{@code getISP()} is dead (grep-confirmed: none exist
     * outside this class itself), so this out-of-bounds read is preserved as an inert method rather
     * than "fixed" into reading index 2 - it is simply never invoked.
     */
    public int getThrust() {
        if (type != PartType.THRUSTER) return 0;
        if (attributes.length <= 3 || !(attributes[3] instanceof Integer)) return 0;
        return (Integer) attributes[3];
    }

    public int getISP() {
        if (type != PartType.THRUSTER) return 0;
        if (attributes.length <= 4 || !(attributes[4] instanceof Integer)) return 0;
        return (Integer) attributes[4];
    }

    public float getTankSize() {
        if (type != PartType.FUSELAGE) return 0;
        if (attributes[1] instanceof Integer i) return i;
        if (attributes[1] instanceof Float f) return f;
        return 0;
    }

    public ItemMissile setAuthor(String author) {
        this.author = author;
        return this;
    }

    public ItemMissile setTitle(String title) {
        this.title = title;
        return this;
    }

    public ItemMissile setWittyText(String witty) {
        this.witty = witty;
        return this;
    }

    public ItemMissile setHealth(float health) {
        this.health = health;
        return this;
    }

    /**
     * CE additionally pushes {@code this} onto {@code ItemLootCrate.list10}/{@code list15}/
     * {@code listMisc} here (rarity-weighted loot-crate roll tables), keyed on
     * {@code this.type == FUSELAGE && this.top == SIZE_10/SIZE_15}, else {@code listMisc} (confirmed
     * by direct read of CE's real method body). {@code com.hbm.items.special.ItemLootCrate} has
     * since landed (see {@code docs/phase3/scattered_military_items.md}) - wired for real below.
     * {@link #rarity} itself is still stored and still shows in the tooltip regardless of pool.
     */
    public ItemMissile setRarity(Rarity rarity) {
        this.rarity = rarity;

        if (this.type == PartType.FUSELAGE) {
            if (this.top == PartSize.SIZE_10) {
                com.hbm.items.special.ItemLootCrate.LIST_10.add(this);
            }
            if (this.top == PartSize.SIZE_15) {
                com.hbm.items.special.ItemLootCrate.LIST_15.add(this);
            }
        } else {
            com.hbm.items.special.ItemLootCrate.LIST_MISC.add(this);
        }
        return this;
    }
}
