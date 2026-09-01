package com.hbm.items.gear;

import com.hbm.items.ModItems;
import com.hbm.main.MaterialRegistry;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;

/**
 * CE {@code ModItems.java:494-533} regular ArmorFSB sets whose models/lang already lived in-tree
 * but were never registered — the I/O hole blocking t51/ajr/hev/steamsuit/bj/fau/schrabidium crafts
 * ({@code ArmorRecipes.java:31-58/:183-191}).
 *
 * <p>{@code steel_plate}/{@code titanium_plate} are the CE armor chestpieces ({@code ModItems.java:495/:499}).
 * {@code MaterialItemGenerator} does not emit {@link com.hbm.inventory.material.MaterialShapes#PLATE}
 * (hand plates stay {@code plate_steel}/{@code plate_titanium}), so these ids are free.
 *
 * <p>No creative tab: CE {@code ArmorFSB} ctor never calls {@code setCreativeTab} and none of these
 * declaration sites override it (same as asbestos/schrabidium in {@link SpecialArmorItems}).
 *
 * <p>Robes reuse {@link MaterialRegistry#aMatSteel} for stats (CE does too) — worn texture is the
 * material layer ({@code steel}), not CE's {@code robes_1.png} override. VFX last.
 *
 * <p>Literal {@code ITEMS.register("id"} so {@code extract_all_ids} sees every piece (no helper-blind).
 */
public final class BasicArmorItems {

    private BasicArmorItems() {
    }

    public static void registerAll() {
        // class-load static fields
    }

    private static Item.Properties props(Holder<ArmorMaterial> material, ArmorItem.Type type) {
        return new Item.Properties().stacksTo(1)
                .durability(type.getDurability(MaterialRegistry.getDurabilityMultiplier(material)));
    }

    // steel — CE :494-497
    public static final DeferredItem<Item> STEEL_HELMET = ModItems.ITEMS.register("steel_helmet",
            () -> new ArmorFSB(MaterialRegistry.aMatSteel, ArmorItem.Type.HELMET, props(MaterialRegistry.aMatSteel, ArmorItem.Type.HELMET)));
    public static final DeferredItem<Item> STEEL_PLATE = ModItems.ITEMS.register("steel_plate",
            () -> new ArmorFSB(MaterialRegistry.aMatSteel, ArmorItem.Type.CHESTPLATE, props(MaterialRegistry.aMatSteel, ArmorItem.Type.CHESTPLATE)));
    public static final DeferredItem<Item> STEEL_LEGS = ModItems.ITEMS.register("steel_legs",
            () -> new ArmorFSB(MaterialRegistry.aMatSteel, ArmorItem.Type.LEGGINGS, props(MaterialRegistry.aMatSteel, ArmorItem.Type.LEGGINGS)));
    public static final DeferredItem<Item> STEEL_BOOTS = ModItems.ITEMS.register("steel_boots",
            () -> new ArmorFSB(MaterialRegistry.aMatSteel, ArmorItem.Type.BOOTS, props(MaterialRegistry.aMatSteel, ArmorItem.Type.BOOTS)));

    // titanium — CE :498-501
    public static final DeferredItem<Item> TITANIUM_HELMET = ModItems.ITEMS.register("titanium_helmet",
            () -> new ArmorFSB(MaterialRegistry.aMatTitan, ArmorItem.Type.HELMET, props(MaterialRegistry.aMatTitan, ArmorItem.Type.HELMET)));
    public static final DeferredItem<Item> TITANIUM_PLATE = ModItems.ITEMS.register("titanium_plate",
            () -> new ArmorFSB(MaterialRegistry.aMatTitan, ArmorItem.Type.CHESTPLATE, props(MaterialRegistry.aMatTitan, ArmorItem.Type.CHESTPLATE)));
    public static final DeferredItem<Item> TITANIUM_LEGS = ModItems.ITEMS.register("titanium_legs",
            () -> new ArmorFSB(MaterialRegistry.aMatTitan, ArmorItem.Type.LEGGINGS, props(MaterialRegistry.aMatTitan, ArmorItem.Type.LEGGINGS)));
    public static final DeferredItem<Item> TITANIUM_BOOTS = ModItems.ITEMS.register("titanium_boots",
            () -> new ArmorFSB(MaterialRegistry.aMatTitan, ArmorItem.Type.BOOTS, props(MaterialRegistry.aMatTitan, ArmorItem.Type.BOOTS)));

    // cobalt — CE :506-509
    public static final DeferredItem<Item> COBALT_HELMET = ModItems.ITEMS.register("cobalt_helmet",
            () -> new ArmorFSB(MaterialRegistry.aMatCobalt, ArmorItem.Type.HELMET, props(MaterialRegistry.aMatCobalt, ArmorItem.Type.HELMET)));
    public static final DeferredItem<Item> COBALT_PLATE = ModItems.ITEMS.register("cobalt_plate",
            () -> new ArmorFSB(MaterialRegistry.aMatCobalt, ArmorItem.Type.CHESTPLATE, props(MaterialRegistry.aMatCobalt, ArmorItem.Type.CHESTPLATE)));
    public static final DeferredItem<Item> COBALT_LEGS = ModItems.ITEMS.register("cobalt_legs",
            () -> new ArmorFSB(MaterialRegistry.aMatCobalt, ArmorItem.Type.LEGGINGS, props(MaterialRegistry.aMatCobalt, ArmorItem.Type.LEGGINGS)));
    public static final DeferredItem<Item> COBALT_BOOTS = ModItems.ITEMS.register("cobalt_boots",
            () -> new ArmorFSB(MaterialRegistry.aMatCobalt, ArmorItem.Type.BOOTS, props(MaterialRegistry.aMatCobalt, ArmorItem.Type.BOOTS)));

    // starmetal — CE :514-517. STAR autogen is CASTPLATE not PLATE.
    public static final DeferredItem<Item> STARMETAL_HELMET = ModItems.ITEMS.register("starmetal_helmet",
            () -> new ArmorFSB(MaterialRegistry.aMatStarmetal, ArmorItem.Type.HELMET, props(MaterialRegistry.aMatStarmetal, ArmorItem.Type.HELMET)));
    public static final DeferredItem<Item> STARMETAL_PLATE = ModItems.ITEMS.register("starmetal_plate",
            () -> new ArmorFSB(MaterialRegistry.aMatStarmetal, ArmorItem.Type.CHESTPLATE, props(MaterialRegistry.aMatStarmetal, ArmorItem.Type.CHESTPLATE)));
    public static final DeferredItem<Item> STARMETAL_LEGS = ModItems.ITEMS.register("starmetal_legs",
            () -> new ArmorFSB(MaterialRegistry.aMatStarmetal, ArmorItem.Type.LEGGINGS, props(MaterialRegistry.aMatStarmetal, ArmorItem.Type.LEGGINGS)));
    public static final DeferredItem<Item> STARMETAL_BOOTS = ModItems.ITEMS.register("starmetal_boots",
            () -> new ArmorFSB(MaterialRegistry.aMatStarmetal, ArmorItem.Type.BOOTS, props(MaterialRegistry.aMatStarmetal, ArmorItem.Type.BOOTS)));

    // security — CE :510-513
    public static final DeferredItem<Item> SECURITY_HELMET = ModItems.ITEMS.register("security_helmet",
            () -> new ArmorFSB(MaterialRegistry.aMatSecurity, ArmorItem.Type.HELMET, props(MaterialRegistry.aMatSecurity, ArmorItem.Type.HELMET)));
    public static final DeferredItem<Item> SECURITY_PLATE = ModItems.ITEMS.register("security_plate",
            () -> new ArmorFSB(MaterialRegistry.aMatSecurity, ArmorItem.Type.CHESTPLATE, props(MaterialRegistry.aMatSecurity, ArmorItem.Type.CHESTPLATE)));
    public static final DeferredItem<Item> SECURITY_LEGS = ModItems.ITEMS.register("security_legs",
            () -> new ArmorFSB(MaterialRegistry.aMatSecurity, ArmorItem.Type.LEGGINGS, props(MaterialRegistry.aMatSecurity, ArmorItem.Type.LEGGINGS)));
    public static final DeferredItem<Item> SECURITY_BOOTS = ModItems.ITEMS.register("security_boots",
            () -> new ArmorFSB(MaterialRegistry.aMatSecurity, ArmorItem.Type.BOOTS, props(MaterialRegistry.aMatSecurity, ArmorItem.Type.BOOTS)));

    // robes — CE :518-521, aMatSteel + robes texture override (VFX last)
    public static final DeferredItem<Item> ROBES_HELMET = ModItems.ITEMS.register("robes_helmet",
            () -> new ArmorFSB(MaterialRegistry.aMatSteel, ArmorItem.Type.HELMET, props(MaterialRegistry.aMatSteel, ArmorItem.Type.HELMET)));
    public static final DeferredItem<Item> ROBES_PLATE = ModItems.ITEMS.register("robes_plate",
            () -> new ArmorFSB(MaterialRegistry.aMatSteel, ArmorItem.Type.CHESTPLATE, props(MaterialRegistry.aMatSteel, ArmorItem.Type.CHESTPLATE)));
    public static final DeferredItem<Item> ROBES_LEGS = ModItems.ITEMS.register("robes_legs",
            () -> new ArmorFSB(MaterialRegistry.aMatSteel, ArmorItem.Type.LEGGINGS, props(MaterialRegistry.aMatSteel, ArmorItem.Type.LEGGINGS)));
    public static final DeferredItem<Item> ROBES_BOOTS = ModItems.ITEMS.register("robes_boots",
            () -> new ArmorFSB(MaterialRegistry.aMatSteel, ArmorItem.Type.BOOTS, props(MaterialRegistry.aMatSteel, ArmorItem.Type.BOOTS)));

    // zirconium legs only — CE :522
    public static final DeferredItem<Item> ZIRCONIUM_LEGS = ModItems.ITEMS.register("zirconium_legs",
            () -> new ArmorFSB(MaterialRegistry.aMatZirconium, ArmorItem.Type.LEGGINGS, props(MaterialRegistry.aMatZirconium, ArmorItem.Type.LEGGINGS)));

    // dnt — CE :523-526. MAT_DNT registry is dineutronium, so dnt_plate is free.
    public static final DeferredItem<Item> DNT_HELMET = ModItems.ITEMS.register("dnt_helmet",
            () -> new ArmorFSB(MaterialRegistry.aMatDNT, ArmorItem.Type.HELMET, props(MaterialRegistry.aMatDNT, ArmorItem.Type.HELMET)));
    public static final DeferredItem<Item> DNT_PLATE = ModItems.ITEMS.register("dnt_plate",
            () -> new ArmorFSB(MaterialRegistry.aMatDNT, ArmorItem.Type.CHESTPLATE, props(MaterialRegistry.aMatDNT, ArmorItem.Type.CHESTPLATE)));
    public static final DeferredItem<Item> DNT_LEGS = ModItems.ITEMS.register("dnt_legs",
            () -> new ArmorFSB(MaterialRegistry.aMatDNT, ArmorItem.Type.LEGGINGS, props(MaterialRegistry.aMatDNT, ArmorItem.Type.LEGGINGS)));
    public static final DeferredItem<Item> DNT_BOOTS = ModItems.ITEMS.register("dnt_boots",
            () -> new ArmorFSB(MaterialRegistry.aMatDNT, ArmorItem.Type.BOOTS, props(MaterialRegistry.aMatDNT, ArmorItem.Type.BOOTS)));

    // cmb — CE :527-533. MAT_CMB registry is cmbsteel, so cmb_plate is free.
    public static final DeferredItem<Item> CMB_HELMET = ModItems.ITEMS.register("cmb_helmet",
            () -> cmbEffects(new ArmorFSB(MaterialRegistry.aMatCMB, ArmorItem.Type.HELMET, props(MaterialRegistry.aMatCMB, ArmorItem.Type.HELMET))));
    public static final DeferredItem<Item> CMB_PLATE = ModItems.ITEMS.register("cmb_plate",
            () -> cmbEffects(new ArmorFSB(MaterialRegistry.aMatCMB, ArmorItem.Type.CHESTPLATE, props(MaterialRegistry.aMatCMB, ArmorItem.Type.CHESTPLATE))));
    public static final DeferredItem<Item> CMB_LEGS = ModItems.ITEMS.register("cmb_legs",
            () -> cmbEffects(new ArmorFSB(MaterialRegistry.aMatCMB, ArmorItem.Type.LEGGINGS, props(MaterialRegistry.aMatCMB, ArmorItem.Type.LEGGINGS))));
    public static final DeferredItem<Item> CMB_BOOTS = ModItems.ITEMS.register("cmb_boots",
            () -> cmbEffects(new ArmorFSB(MaterialRegistry.aMatCMB, ArmorItem.Type.BOOTS, props(MaterialRegistry.aMatCMB, ArmorItem.Type.BOOTS))));

    private static ArmorFSB cmbEffects(ArmorFSB armor) {
        // CE ModItems.java:528-530 Speed II / Haste / Strength, 30-tick refresh.
        armor.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 30, 2, false, true));
        armor.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 30, 0, false, true));
        armor.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 30, 0, false, true));
        return armor;
    }
}
