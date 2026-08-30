package com.hbm.hazard;

import com.hbm.config.GeneralConfig;
import com.hbm.hazard.transformer.HazardTransformerForgeFluid;
import com.hbm.hazard.transformer.HazardTransformerPostCustom;
import com.hbm.hazard.transformer.HazardTransformerRadiationContainer;
import com.hbm.hazard.transformer.HazardTransformerRadiationME;
import com.hbm.hazard.transformer.HazardTransformerRadiationNBT;
import com.hbm.hazard.type.HazardTypeAsbestos;
import com.hbm.hazard.type.HazardTypeBlinding;
import com.hbm.hazard.type.HazardTypeCoal;
import com.hbm.hazard.type.HazardTypeCold;
import com.hbm.hazard.type.HazardTypeContaminating;
import com.hbm.hazard.type.HazardTypeDigamma;
import com.hbm.hazard.type.HazardTypeExplosive;
import com.hbm.hazard.type.HazardTypeHot;
import com.hbm.hazard.type.HazardTypeHydroactive;
import com.hbm.hazard.type.HazardTypeRadiation;
import com.hbm.hazard.type.HazardTypeToxic;
import com.hbm.hazard.type.IHazardType;
import com.hbm.items.BilletPowderItems;
import com.hbm.items.IngotNuggetItems;
import com.hbm.items.PlateCrystalWasteItems;

/**
 * Physics-derived level constants and the {@link IHazardType} singletons every {@code HazardData} entry is built
 * from, plus the transformer chain wiring.
 * <p>
 * {@link #registerItems()} and {@link #registerContaminatingDrops()} started out empty and are populated
 * incrementally, one Phase 1 content area at a time, as each area's items land. CE's ~460-line bulk item-to-hazard
 * binding table (and the specialized {@code registerRBMKRod}/{@code registerRTGPellet}/... helper methods it uses)
 * binds concrete items from the ~458-item CE catalog; every area should append its own slice of that table here,
 * using the constants and {@link IHazardType} singletons declared below, rather than replacing another area's calls.
 */
@SuppressWarnings("unused") // constant table is consumed by Phase 1's bulk registration, not all of it by this file
public class HazardRegistry {

    //CO60		             5a		β−	030.00Rad/s	Spicy
    //SR90		            29a		β−	015.00Rad/s Spicy
    //TC99		       211,000a		β−	002.75Rad/s	Spicy
    //I181		           192h		β−	150.00Rad/s	2 much spice :(
    //XE135		             9h		β−	aaaaaaaaaaaaaaaa
    //CS137		            30a		β−	020.00Rad/s	Spicy
    //AU198		            64h		β−	500.00Rad/s	2 much spice :(
    //PB209		             3h		β−	10,000.00Rad/s mama mia my face is melting off
    //AT209		             5h		β+	like 7.5k or sth idk bruv
    //PO210		           138d		α	075.00Rad/s	Spicy
    //RA226		         1,600a		α	007.50Rad/s
    //AC227		            22a		β−	030.00Rad/s Spicy
    //TH232		14,000,000,000a		α	000.10Rad/s
    //U233		       160,000a		α	005.00Rad/s
    //U235		   700,000,000a		α	001.00Rad/s
    //U238		 4,500,000,000a		α	000.25Rad/s
    //NP237		     2,100,000a		α	002.50Rad/s
    //PU238		            88a		α	010.00Rad/s	Spicy
    //PU239		        24,000a		α	005.00Rad/s
    //PU240		         6,600a		α	007.50Rad/s
    //PU241		            14a		β−	025.00Rad/s	Spicy
    //AM241		           432a		α	008.50Rad/s
    //AM242		           141a		β−	009.50Rad/s

    //simplified groups for ReC compat
    public static final float gen_S = 10_000F;
    public static final float gen_H = 2_000F;
    public static final float gen_10D = 100F;
    public static final float gen_100D = 80F;
    public static final float gen_1Y = 50F;
    public static final float gen_10Y = 30F;
    public static final float gen_100Y = 10F;
    public static final float gen_1K = 7.5F;
    public static final float gen_10K = 6.25F;
    public static final float gen_100K = 5F;
    public static final float gen_1M = 2.5F;
    public static final float gen_10M = 1.5F;
    public static final float gen_100M = 1F;
    public static final float gen_1B = 0.5F;
    public static final float gen_10B = 0.1F;

    public static final float co60 = 30.0F;
    public static final float sr90 = 15.0F;
    public static final float tc99 = 2.75F;
    public static final float i131 = 150.0F;
    public static final float xe135 = 1250.0F;
    public static final float cs137 = 20.0F;
    public static final float au198 = 500.0F;
    public static final float pb209 = 10000.0F;
    public static final float at209 = 7500.0F;
    public static final float po210 = 75.0F;
    public static final float ra226 = 7.5F;
    public static final float ac227 = 30.0F;
    public static final float th232 = 0.1F;
    public static final float thf = 1.75F;
    public static final float u = 0.35F;
    public static final float u233 = 5.0F;
    public static final float u235 = 1.0F;
    public static final float u238 = 0.25F;
    public static final float uf = 0.5F;
    public static final float uzh = 0.125F;
    public static final float np237 = 2.5F;
    public static final float npf = 1.5F;
    public static final float pu = 7.5F;
    public static final float purg = 6.25F;
    public static final float pu238 = 10.0F;
    public static final float pu239 = 5.0F;
    public static final float pu240 = 7.5F;
    public static final float pu241 = 25.0F;
    public static final float puf = 4.25F;
    public static final float am241 = 8.5F;
    public static final float am242 = 9.5F;
    public static final float amrg = 9.0F;
    public static final float amf = 4.75F;
    public static final float mox = 2.5F;
    public static final float sa326 = 15.0F;
    public static final float sa327 = 17.5F;
    public static final float saf = 5.85F;
    public static final float sas3 = 5F;
    public static final float gh336 = 5.0F;
    public static final float mud = 1.0F;
    public static final float radsource_mult = 3.0F;
    public static final float pobe = po210 * radsource_mult;
    public static final float rabe = ra226 * radsource_mult;
    public static final float pube = pu238 * radsource_mult;
    public static final float zfb_bi = u235 * 0.35F;
    public static final float zfb_pu241 = pu241 * 0.5F;
    public static final float zfb_am_mix = amrg * 0.5F;
    public static final float bf = 300_000.0F;
    public static final float bfb = 500_000.0F;

    public static final float sr = sa326 * 0.1F;
    public static final float sb = sa326 * 0.1F;
    public static final float trx = 25.0F;
    public static final float trn = 0.1F;
    public static final float wst = 15.0F;
    public static final float wstv = 7.5F;
    public static final float yc = u;
    public static final float fo = 10F;

    public static final float nugget = 0.1F;
    public static final float ingot = 1.0F;
    public static final float gem = 1.0F;
    public static final float plate = ingot;
    public static final float plateCast = plate * 3;
    public static final float powder_mult = 3.0F;
    public static final float powder = ingot * powder_mult;
    public static final float powder_tiny = nugget * powder_mult;
    public static final float ore = ingot;
    public static final float block = 10.0F;
    public static final float crystal = block;
    public static final float billet = 0.5F;
    public static final float rtg = billet * 3;
    public static final float rod = 0.5F;
    public static final float rod_dual = rod * 2;
    public static final float rod_quad = rod * 4;
    public static final float rod_rbmk = rod * 8;

    public static final IHazardType RADIATION = new HazardTypeRadiation();
    public static final IHazardType CONTAMINATING = new HazardTypeContaminating();
    public static final IHazardType DIGAMMA = new HazardTypeDigamma();
    public static final IHazardType HOT = new HazardTypeHot();
    public static final IHazardType BLINDING = new HazardTypeBlinding();
    public static final IHazardType ASBESTOS = new HazardTypeAsbestos();
    public static final IHazardType COAL = new HazardTypeCoal();
    public static final IHazardType HYDROACTIVE = new HazardTypeHydroactive();
    public static final IHazardType EXPLOSIVE = new HazardTypeExplosive();
    public static final IHazardType TOXIC = new HazardTypeToxic();
    public static final IHazardType COLD = new HazardTypeCold();

    /**
     * Phase 1: full item-to-hazard bulk registration table. See CE's {@code HazardRegistry.registerItems()} for the
     * ~458-item source data; port it using {@link HazardSystem#register(Object, HazardData)} and the constants and
     * {@link IHazardType} singletons above.
     */
    public static void registerItems() {
        // items_plate_crystal_waste area (docs/phase1/moditems_generative.md section 3,
        // docs/phase1/hazard_bindings_plan.md Pattern A/D): waste_/crystal_/gem_ hazard bindings.
        // Formulas ported verbatim from CE's HazardRegistry.registerOtherWasteContaminating/
        // registerOtherWaste/registerRadSourceWaste helpers (CE HazardRegistry.java:613-639) - not
        // reproduced as shared private helpers here to avoid colliding with another Phase 1 area's
        // own same-named helper in this shared file; each call is written out in full instead.

        // waste_* families with CONTAMINATING (registerOtherWasteContaminating, CE:272-280)
        HazardSystem.register(PlateCrystalWasteItems.WASTE_NATURAL_URANIUM.get(),
                new HazardData().addEntry(RADIATION, wst * billet * 11.5F * 0.075F).addEntry(CONTAMINATING, 15));
        HazardSystem.register(PlateCrystalWasteItems.WASTE_NATURAL_URANIUM_HOT.get(),
                new HazardData().addEntry(RADIATION, wst * billet * 11.5F).addEntry(HOT, 5F).addEntry(CONTAMINATING, 15));

        HazardSystem.register(PlateCrystalWasteItems.WASTE_URANIUM.get(),
                new HazardData().addEntry(RADIATION, wst * billet * 10F * 0.075F).addEntry(CONTAMINATING, 15));
        HazardSystem.register(PlateCrystalWasteItems.WASTE_URANIUM_HOT.get(),
                new HazardData().addEntry(RADIATION, wst * billet * 10F).addEntry(HOT, 5F).addEntry(CONTAMINATING, 15));

        HazardSystem.register(PlateCrystalWasteItems.WASTE_THORIUM.get(),
                new HazardData().addEntry(RADIATION, wst * billet * 7.5F * 0.075F).addEntry(CONTAMINATING, 10));
        HazardSystem.register(PlateCrystalWasteItems.WASTE_THORIUM_HOT.get(),
                new HazardData().addEntry(RADIATION, wst * billet * 7.5F).addEntry(HOT, 5F).addEntry(CONTAMINATING, 10));

        HazardSystem.register(PlateCrystalWasteItems.WASTE_MOX.get(),
                new HazardData().addEntry(RADIATION, wst * billet * 10F * 0.075F).addEntry(CONTAMINATING, 15));
        HazardSystem.register(PlateCrystalWasteItems.WASTE_MOX_HOT.get(),
                new HazardData().addEntry(RADIATION, wst * billet * 10F).addEntry(HOT, 5F).addEntry(CONTAMINATING, 15));

        HazardSystem.register(PlateCrystalWasteItems.WASTE_PLUTONIUM.get(),
                new HazardData().addEntry(RADIATION, wst * billet * 12.5F * 0.075F).addEntry(CONTAMINATING, 15));
        HazardSystem.register(PlateCrystalWasteItems.WASTE_PLUTONIUM_HOT.get(),
                new HazardData().addEntry(RADIATION, wst * billet * 12.5F).addEntry(HOT, 5F).addEntry(CONTAMINATING, 15));

        HazardSystem.register(PlateCrystalWasteItems.WASTE_U233.get(),
                new HazardData().addEntry(RADIATION, wst * billet * 10F * 0.075F).addEntry(CONTAMINATING, 15));
        HazardSystem.register(PlateCrystalWasteItems.WASTE_U233_HOT.get(),
                new HazardData().addEntry(RADIATION, wst * billet * 10F).addEntry(HOT, 5F).addEntry(CONTAMINATING, 15));

        HazardSystem.register(PlateCrystalWasteItems.WASTE_U235.get(),
                new HazardData().addEntry(RADIATION, wst * billet * 11F * 0.075F).addEntry(CONTAMINATING, 15));
        HazardSystem.register(PlateCrystalWasteItems.WASTE_U235_HOT.get(),
                new HazardData().addEntry(RADIATION, wst * billet * 11F).addEntry(HOT, 5F).addEntry(CONTAMINATING, 15));

        HazardSystem.register(PlateCrystalWasteItems.WASTE_SCHRABIDIUM.get(),
                new HazardData().addEntry(RADIATION, wst * billet * 15F * 0.075F).addEntry(CONTAMINATING, 40));
        HazardSystem.register(PlateCrystalWasteItems.WASTE_SCHRABIDIUM_HOT.get(),
                new HazardData().addEntry(RADIATION, wst * billet * 15F).addEntry(HOT, 5F).addEntry(CONTAMINATING, 40));

        HazardSystem.register(PlateCrystalWasteItems.WASTE_ZFB_MOX.get(),
                new HazardData().addEntry(RADIATION, wst * billet * 5F * 0.075F).addEntry(CONTAMINATING, 10));
        HazardSystem.register(PlateCrystalWasteItems.WASTE_ZFB_MOX_HOT.get(),
                new HazardData().addEntry(RADIATION, wst * billet * 5F).addEntry(HOT, 5F).addEntry(CONTAMINATING, 10));

        // waste_plate_* without CONTAMINATING (registerOtherWaste, CE:290-294)
        HazardSystem.register(PlateCrystalWasteItems.WASTE_PLATE_U233.get(),
                new HazardData().addEntry(RADIATION, wst * ingot * 13F * 0.075F));
        HazardSystem.register(PlateCrystalWasteItems.WASTE_PLATE_U233_HOT.get(),
                new HazardData().addEntry(RADIATION, wst * ingot * 13F).addEntry(HOT, 5F));

        HazardSystem.register(PlateCrystalWasteItems.WASTE_PLATE_U235.get(),
                new HazardData().addEntry(RADIATION, wst * ingot * 10F * 0.075F));
        HazardSystem.register(PlateCrystalWasteItems.WASTE_PLATE_U235_HOT.get(),
                new HazardData().addEntry(RADIATION, wst * ingot * 10F).addEntry(HOT, 5F));

        HazardSystem.register(PlateCrystalWasteItems.WASTE_PLATE_MOX.get(),
                new HazardData().addEntry(RADIATION, wst * ingot * 16F * 0.075F));
        HazardSystem.register(PlateCrystalWasteItems.WASTE_PLATE_MOX_HOT.get(),
                new HazardData().addEntry(RADIATION, wst * ingot * 16F).addEntry(HOT, 5F));

        HazardSystem.register(PlateCrystalWasteItems.WASTE_PLATE_PU239.get(),
                new HazardData().addEntry(RADIATION, wst * ingot * 13.5F * 0.075F));
        HazardSystem.register(PlateCrystalWasteItems.WASTE_PLATE_PU239_HOT.get(),
                new HazardData().addEntry(RADIATION, wst * ingot * 13.5F).addEntry(HOT, 5F));

        HazardSystem.register(PlateCrystalWasteItems.WASTE_PLATE_SA326.get(),
                new HazardData().addEntry(RADIATION, wst * ingot * 10F * 0.075F));
        HazardSystem.register(PlateCrystalWasteItems.WASTE_PLATE_SA326_HOT.get(),
                new HazardData().addEntry(RADIATION, wst * ingot * 10F).addEntry(HOT, 5F));

        // waste_plate_ra226be/pu238be: radioisotope-source waste, no 0.075x scaling on the cooled
        // variant (registerRadSourceWaste, CE:295-296)
        HazardSystem.register(PlateCrystalWasteItems.WASTE_PLATE_RA226BE.get(),
                new HazardData().addEntry(RADIATION, pobe * nugget * 3));
        HazardSystem.register(PlateCrystalWasteItems.WASTE_PLATE_RA226BE_HOT.get(),
                new HazardData().addEntry(RADIATION, pobe * nugget * 3).addEntry(HOT, 5F));

        HazardSystem.register(PlateCrystalWasteItems.WASTE_PLATE_PU238BE.get(),
                new HazardData().addEntry(RADIATION, pube * nugget * 1));
        HazardSystem.register(PlateCrystalWasteItems.WASTE_PLATE_PU238BE_HOT.get(),
                new HazardData().addEntry(RADIATION, pube * nugget * 1).addEntry(HOT, 5F));

        // crystal_/gem_ direct bindings (CE HazardRegistry.java:502-503, 244)
        HazardSystem.register(PlateCrystalWasteItems.CRYSTAL_PHOSPHORUS.get(),
                new HazardData().addEntry(HOT, 2F * crystal));
        HazardSystem.register(PlateCrystalWasteItems.CRYSTAL_TRIXITE.get(),
                new HazardData().addEntry(RADIATION, trx * crystal));
        HazardSystem.register(PlateCrystalWasteItems.GEM_RAD.get(),
                new HazardData().addEntry(RADIATION, 25F));

        // items_billet_powder area (docs/phase1/moditems_generative.md section 3,
        // hazard_bindings_plan.md Pattern A/B): only the billet_/powder_ fields CE's own
        // HazardRegistry.registerItems() actually binds (upstream hbm-ce HazardRegistry.java
        // lines 225, 307-352, 203, 490-499) - the great majority of billet_/powder_ fields (plain
        // metal billets like billet_uranium/billet_plutonium, most powders) carry no hazard data
        // in CE at all; do not invent radiation for them.

        // billet_* fuel-shape family (CE:307-352)
        HazardSystem.register(BilletPowderItems.BILLET_URANIUM_FUEL.get(), new HazardData().addEntry(RADIATION, uf * billet));
        HazardSystem.register(BilletPowderItems.BILLET_UZH.get(), new HazardData().addEntry(RADIATION, uzh * billet));
        HazardSystem.register(BilletPowderItems.BILLET_PLUTONIUM_FUEL.get(), new HazardData().addEntry(RADIATION, puf * billet));
        HazardSystem.register(BilletPowderItems.BILLET_THORIUM_FUEL.get(), new HazardData().addEntry(RADIATION, thf * billet));
        HazardSystem.register(BilletPowderItems.BILLET_NEPTUNIUM_FUEL.get(), new HazardData().addEntry(RADIATION, npf * billet));
        HazardSystem.register(BilletPowderItems.BILLET_MOX_FUEL.get(), new HazardData().addEntry(RADIATION, mox * billet));
        HazardSystem.register(BilletPowderItems.BILLET_AMERICIUM_FUEL.get(), new HazardData().addEntry(RADIATION, amf * billet));
        HazardSystem.register(BilletPowderItems.BILLET_SCHRABIDIUM_FUEL.get(),
                new HazardData().addEntry(RADIATION, saf * billet).addEntry(BLINDING, 5F * billet));
        HazardSystem.register(BilletPowderItems.BILLET_HES.get(), new HazardData().addEntry(RADIATION, saf * billet));
        HazardSystem.register(BilletPowderItems.BILLET_LES.get(), new HazardData().addEntry(RADIATION, saf * billet));
        HazardSystem.register(BilletPowderItems.BILLET_BALEFIRE_GOLD.get(), new HazardData().addEntry(RADIATION, au198 * billet));
        HazardSystem.register(BilletPowderItems.BILLET_FLASHLEAD.get(),
                new HazardData().addEntry(RADIATION, pb209 * 1.25F * billet).addEntry(HOT, 7F));
        HazardSystem.register(BilletPowderItems.BILLET_PO210BE.get(), new HazardData().addEntry(RADIATION, pobe * billet));
        HazardSystem.register(BilletPowderItems.BILLET_RA226BE.get(), new HazardData().addEntry(RADIATION, rabe * billet));
        HazardSystem.register(BilletPowderItems.BILLET_PU238BE.get(), new HazardData().addEntry(RADIATION, pube * billet));

        // billet_nuclear_waste (CE:225)
        HazardSystem.register(BilletPowderItems.BILLET_NUCLEAR_WASTE.get(),
                new HazardData().addEntry(RADIATION, wst * billet).addEntry(CONTAMINATING, wst * billet));

        // powder_* direct bindings (CE:203, 490, 495, 499). powder_balefire's two CE calls
        // (registerItems RADIATION 500F + registerContaminatingDrops CONTAMINATING bf * powder)
        // are merged into one HazardData here since both target the same item and neither
        // overrides - equivalent end state, one registration instead of two.
        HazardSystem.register(BilletPowderItems.POWDER_BALEFIRE.get(),
                new HazardData().addEntry(RADIATION, 500F).addEntry(CONTAMINATING, bf * powder));
        HazardSystem.register(BilletPowderItems.POWDER_YELLOWCAKE.get(),
                new HazardData().addEntry(RADIATION, yc * powder).addEntry(CONTAMINATING, yc * powder));
        HazardSystem.register(BilletPowderItems.POWDER_CAESIUM.get(),
                new HazardData().addEntry(HYDROACTIVE, 1F).addEntry(HOT, 3F));
        HazardSystem.register(BilletPowderItems.POWDER_COLTAN_ORE.get(), new HazardData().addEntry(ASBESTOS, 3F));

        // items_ingot_nugget area (docs/phase1/moditems_generative.md section 3,
        // hazard_bindings_plan.md Pattern A/B): only the ingot_/nugget_ fields CE's own
        // HazardRegistry.registerItems() actually binds (upstream hbm-ce HazardRegistry.java
        // lines 306-346) - every other ingot_/nugget_ field (plain metal ingots like
        // ingot_uranium/ingot_plutonium/nugget_polonium, and the ItemUnstable/ItemHot/
        // ItemSchraranium/ItemFuel/food-backed fields) carries no hazard data in CE at all; do not
        // invent radiation for them.
        HazardSystem.register(IngotNuggetItems.NUGGET_URANIUM_FUEL.get(), new HazardData().addEntry(RADIATION, uf * nugget));
        HazardSystem.register(IngotNuggetItems.INGOT_URANIUM_FUEL.get(), new HazardData().addEntry(RADIATION, uf * ingot));

        HazardSystem.register(IngotNuggetItems.NUGGET_PLUTONIUM_FUEL.get(), new HazardData().addEntry(RADIATION, puf * nugget));
        HazardSystem.register(IngotNuggetItems.INGOT_PLUTONIUM_FUEL.get(), new HazardData().addEntry(RADIATION, puf * ingot));

        HazardSystem.register(IngotNuggetItems.NUGGET_THORIUM_FUEL.get(), new HazardData().addEntry(RADIATION, thf * nugget));
        HazardSystem.register(IngotNuggetItems.INGOT_THORIUM_FUEL.get(), new HazardData().addEntry(RADIATION, thf * ingot));

        HazardSystem.register(IngotNuggetItems.NUGGET_NEPTUNIUM_FUEL.get(), new HazardData().addEntry(RADIATION, npf * nugget));
        HazardSystem.register(IngotNuggetItems.INGOT_NEPTUNIUM_FUEL.get(), new HazardData().addEntry(RADIATION, npf * ingot));

        HazardSystem.register(IngotNuggetItems.NUGGET_MOX_FUEL.get(), new HazardData().addEntry(RADIATION, mox * nugget));
        HazardSystem.register(IngotNuggetItems.INGOT_MOX_FUEL.get(), new HazardData().addEntry(RADIATION, mox * ingot));

        HazardSystem.register(IngotNuggetItems.NUGGET_AMERICIUM_FUEL.get(), new HazardData().addEntry(RADIATION, amf * nugget));
        HazardSystem.register(IngotNuggetItems.INGOT_AMERICIUM_FUEL.get(), new HazardData().addEntry(RADIATION, amf * ingot));

        HazardSystem.register(IngotNuggetItems.NUGGET_SCHRABIDIUM_FUEL.get(),
                new HazardData().addEntry(RADIATION, saf * nugget).addEntry(BLINDING, 5F * nugget));
        HazardSystem.register(IngotNuggetItems.INGOT_SCHRABIDIUM_FUEL.get(),
                new HazardData().addEntry(RADIATION, saf * ingot).addEntry(BLINDING, 5F * ingot));

        HazardSystem.register(IngotNuggetItems.NUGGET_HES.get(), new HazardData().addEntry(RADIATION, saf * nugget));
        HazardSystem.register(IngotNuggetItems.INGOT_HES.get(), new HazardData().addEntry(RADIATION, saf * ingot));

        HazardSystem.register(IngotNuggetItems.NUGGET_LES.get(), new HazardData().addEntry(RADIATION, saf * nugget));
        HazardSystem.register(IngotNuggetItems.INGOT_LES.get(), new HazardData().addEntry(RADIATION, saf * ingot));
    }

    /**
     * Phase 1: contaminating-drop overlay for ore dust tags, run after ore tags are registered. See CE's
     * {@code HazardRegistry.registerContaminatingDrops()}.
     */
    public static void registerContaminatingDrops() {
    }

    /**
     * Wires the hazard transformer chain. Call once during mod common setup, after {@link HazardComponents} has been
     * registered on the mod event bus.
     */
    public static void registerTrafos() {
        HazardSystem.trafos.add(new HazardTransformerRadiationNBT());
        HazardSystem.trafos.add(new HazardTransformerForgeFluid());

        if (!(GeneralConfig.enableLBSM && GeneralConfig.enableLBSMSafeCrates)) HazardSystem.trafos.add(new HazardTransformerRadiationContainer());
        if (!(GeneralConfig.enableLBSM && GeneralConfig.enableLBSMSafeMEDrives)) HazardSystem.trafos.add(new HazardTransformerRadiationME());
        // Keep the custom post transformer last
        HazardSystem.trafos.add(new HazardTransformerPostCustom());
    }
}
