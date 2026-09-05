package com.hbm.hazard;

import com.hbm.blocks.MaterialBlockGenerator;
import com.hbm.blocks.bomb.NukeCasingBlocks;
import com.hbm.blocks.generic.GenericCrateBlocks;
import com.hbm.blocks.generic.WastelandVirusBlocks;
import com.hbm.config.GeneralConfig;
import com.hbm.hazard.transformer.HazardTransformerForgeFluid;
import com.hbm.hazard.transformer.HazardTransformerPostCustom;
import com.hbm.hazard.transformer.HazardTransformerRadiationContainer;
import com.hbm.hazard.transformer.HazardTransformerRadiationME;
import com.hbm.hazard.transformer.HazardTransformerRadiationNBT;
import com.hbm.hazard.modifier.HazardModifierFuelRadiation;
import com.hbm.hazard.modifier.HazardModifierRBMKRadiation;
import com.hbm.hazard.modifier.HazardModifierRTGRadiation;
import com.hbm.hazard.modifier.HazardModifierSellafield;
import com.hbm.hazard.type.HazardTypeAsbestos;
import com.hbm.hazard.type.HazardTypeBlinding;
import com.hbm.hazard.type.HazardTypeCoal;
import com.hbm.hazard.type.HazardTypeCold;
import com.hbm.hazard.type.HazardTypeContaminating;
import com.hbm.hazard.type.HazardTypeDangerousDrop;
import com.hbm.hazard.type.HazardTypeDigamma;
import com.hbm.hazard.type.HazardTypeExplosive;
import com.hbm.hazard.type.HazardTypeHot;
import com.hbm.hazard.type.HazardTypeHydroactive;
import com.hbm.hazard.type.HazardTypeRadiation;
import com.hbm.hazard.type.HazardTypeToxic;
import com.hbm.hazard.type.HazardTypeUnstable;
import com.hbm.hazard.type.IHazardType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.material.Mats;
import com.hbm.inventory.material.MaterialShapes;
import com.hbm.items.BilletPowderItems;
import com.hbm.items.IngotNuggetItems;
import com.hbm.items.PlateCrystalWasteItems;
import com.hbm.items.bomb.NukeCasingItems;
import com.hbm.items.machine.Phase11ProcessItems;
import com.hbm.items.machine.ItemBreedingRod;
import com.hbm.items.machine.ItemPWRFuel;
import com.hbm.items.machine.ItemPileRodMK2;
import com.hbm.items.machine.ItemWatzPellet;
import com.hbm.items.machine.ItemZirnoxRod;
import com.hbm.items.machine.ItemZirnoxRodDepleted;
import com.hbm.items.machine.MachineItems;
import com.hbm.items.special.ItemWasteLong;
import com.hbm.items.special.ItemWasteShort;
import com.hbm.items.special.CellFluidHazardModifier;
import com.hbm.items.special.ScatteredMilitaryItems;
import com.hbm.items.special.SpecialItems;
import com.hbm.items.weapon.grenade.GrenadeItems;
import com.hbm.main.MainRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

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
        // CE HazardRegistry.java:171-187 EXPLOSIVE + COAL. Skip dustTinyLignite
        // (powder_lignite_tiny not registered — do not invent).
        HazardSystem.register(Items.GUNPOWDER, new HazardData().addEntry(EXPLOSIVE, 1F));
        HazardSystem.register(Blocks.TNT, new HazardData().addEntry(EXPLOSIVE, 4F));
        HazardSystem.register(Items.PUMPKIN_PIE, new HazardData().addEntry(EXPLOSIVE, 1F));

        HazardSystem.register(Phase11ProcessItems.BALL_DYNAMITE.get(), new HazardData().addEntry(EXPLOSIVE, 2F));
        HazardSystem.register(GrenadeItems.STICK_DYNAMITE.get(), new HazardData().addEntry(EXPLOSIVE, 1F));
        HazardSystem.register(ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "stick_tnt"),
                new HazardData().addEntry(EXPLOSIVE, 1.5F));
        HazardSystem.register(ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "stick_semtex"),
                new HazardData().addEntry(EXPLOSIVE, 2.5F));
        HazardSystem.register(ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "stick_c4"),
                new HazardData().addEntry(EXPLOSIVE, 2.5F));

        HazardSystem.register(Phase11ProcessItems.CORDITE.get(), new HazardData().addEntry(EXPLOSIVE, 2F));
        HazardSystem.register(ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "ballistite"),
                new HazardData().addEntry(EXPLOSIVE, 1F));

        HazardSystem.register(BilletPowderItems.POWDER_COAL.get(), new HazardData().addEntry(COAL, powder));
        HazardSystem.register(BilletPowderItems.POWDER_COAL_TINY.get(), new HazardData().addEntry(COAL, powder_tiny));
        HazardSystem.register(BilletPowderItems.POWDER_LIGNITE.get(), new HazardData().addEntry(COAL, powder));

        // CE :189 insert_polonium / :198 lamp_demon / :208-209 solid_fuel_presto_bf* —
        // unregistered, skip invent. demon_core already bound below.
        // CE :200-207 cell TRITIUM/SAS3 + cell_balefire + eggs + solid_fuel_bf.
        // cell is one item; per-fluid via CellFluidHazardModifier (CE ItemStack meta).
        HazardSystem.register(SpecialItems.CELL.get(), new HazardData()
                .addEntry(new HazardEntry(RADIATION, 0.001F)
                        .addMod(new CellFluidHazardModifier(Fluids.TRITIUM.getID(), 0.001F)))
                .addEntry(new HazardEntry(RADIATION, sas3)
                        .addMod(new CellFluidHazardModifier(Fluids.SAS3.getID(), sas3)))
                .addEntry(new HazardEntry(BLINDING, 60F)
                        .addMod(new CellFluidHazardModifier(Fluids.SAS3.getID(), 60F))));
        HazardSystem.register(ScatteredMilitaryItems.CELL_BALEFIRE.get(),
                new HazardData().addEntry(RADIATION, 50F));
        HazardSystem.register(NukeCasingItems.EGG_BALEFIRE_SHARD.get(),
                new HazardData().addEntry(RADIATION, bf * nugget));
        HazardSystem.register(NukeCasingItems.EGG_BALEFIRE.get(),
                new HazardData().addEntry(RADIATION, bf * ingot));
        HazardSystem.register(Phase11ProcessItems.SOLID_FUEL_BF.get(),
                new HazardData().addEntry(RADIATION, 1000F));

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

        // CE HazardRegistry.java:220 scrap_nuclear; :236-241 sellafield meta 0–5.
        // :243 ore_sellafield_radgem (registered). LEVEL via HazardModifierSellafield.
        HazardSystem.register(ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "scrap_nuclear"),
                new HazardData().addEntry(RADIATION, 1F));
        HazardSystem.register(WastelandVirusBlocks.SELLAFIELD.get(),
                new HazardData().addEntry(new HazardEntry(RADIATION, 0.5F).addMod(new HazardModifierSellafield())));
        HazardSystem.register(ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "ore_sellafield_radgem"),
                new HazardData().addEntry(RADIATION, 25F));

        // CE HazardRegistry.java:221-234 remaining waste / barrel / corium.
        // Skip trinitite item (not registered). Skip nuclear_waste_vitrified_tiny (not registered).
        // vitrified_barrel has no CE HazardRegistry row — do not invent.
        HazardSystem.register(WastelandVirusBlocks.BLOCK_TRINITITE.get(),
                new HazardData().addEntry(RADIATION, trn * block));
        HazardSystem.register(NukeCasingItems.NUCLEAR_WASTE.get(),
                new HazardData().addEntry(RADIATION, wst * ingot).addEntry(CONTAMINATING, wst * powder));
        HazardSystem.register(GenericCrateBlocks.YELLOW_BARREL.get(),
                new HazardData().addEntry(RADIATION, wst * ingot * 10));
        HazardSystem.register(Phase11ProcessItems.NUCLEAR_WASTE_TINY.get(),
                new HazardData().addEntry(RADIATION, wst * nugget).addEntry(CONTAMINATING, wst * powder_tiny));
        HazardSystem.register(Phase11ProcessItems.NUCLEAR_WASTE_VITRIFIED.get(),
                new HazardData().addEntry(RADIATION, wstv * ingot).addEntry(CONTAMINATING, wstv * powder));
        HazardSystem.register(WastelandVirusBlocks.BLOCK_WASTE.get(),
                new HazardData().addEntry(RADIATION, wst * block));
        HazardSystem.register(ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "block_waste_painted"),
                new HazardData().addEntry(RADIATION, wst * block));
        HazardSystem.register(ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "block_waste_vitrified"),
                new HazardData().addEntry(RADIATION, wstv * block));
        HazardSystem.register(ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "ancient_scrap"),
                new HazardData().addEntry(RADIATION, 150F));
        HazardSystem.register(ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "block_corium"),
                new HazardData().addEntry(RADIATION, 150F));
        HazardSystem.register(ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "block_corium_cobble"),
                new HazardData().addEntry(RADIATION, 150F));

        // CE HazardRegistry.java:298-304 debris_*. Skip debris_fuel (not registered).
        HazardSystem.register(ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "debris_graphite"),
                new HazardData().addEntry(RADIATION, 70F).addEntry(HOT, 5F));
        HazardSystem.register(ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "debris_metal"),
                new HazardData().addEntry(RADIATION, 5F));
        HazardSystem.register(ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "debris_concrete"),
                new HazardData().addEntry(RADIATION, 30F));
        HazardSystem.register(ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "debris_exchanger"),
                new HazardData().addEntry(RADIATION, 25F));
        HazardSystem.register(ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "debris_shrapnel"),
                new HazardData().addEntry(RADIATION, 2.5F));
        HazardSystem.register(ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "debris_element"),
                new HazardData().addEntry(RADIATION, 100F));

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
        // CE HazardRegistry.java:491 / :494. Skip :492/:493 fallout item vs ModBlocks.fallout
        // (same BlockItem in this port — do not invent a merged number).
        HazardSystem.register(ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "block_yellowcake"),
                new HazardData().addEntry(RADIATION, yc * block * powder_mult));
        HazardSystem.register(ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "block_fallout"),
                new HazardData().addEntry(RADIATION, yc * block * powder_mult));
        HazardSystem.register(BilletPowderItems.POWDER_CAESIUM.get(),
                new HazardData().addEntry(HYDROACTIVE, 1F).addEntry(HOT, 3F));
        HazardSystem.register(BilletPowderItems.POWDER_COLTAN_ORE.get(), new HazardData().addEntry(ASBESTOS, 3F));
        // CE HazardRegistry.java:497-498
        HazardSystem.register(ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "brick_asbestos"),
                new HazardData().addEntry(ASBESTOS, 1F));
        HazardSystem.register(ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "tile_lab_broken"),
                new HazardData().addEntry(ASBESTOS, 1F));

        // CE HazardRegistry.java:506-513 nuke parts.
        HazardSystem.register(NukeCasingItems.BOY_PROPELLANT.get(),
                new HazardData().addEntry(EXPLOSIVE, 2F));
        HazardSystem.register(NukeCasingItems.GADGET_CORE.get(),
                new HazardData().addEntry(RADIATION, pu239 * nugget * 10));
        HazardSystem.register(NukeCasingItems.BOY_TARGET.get(),
                new HazardData().addEntry(RADIATION, u235 * ingot * 2));
        HazardSystem.register(NukeCasingItems.BOY_BULLET.get(),
                new HazardData().addEntry(RADIATION, u235 * ingot));
        HazardSystem.register(SpecialItems.MAN_CORE.get(),
                new HazardData().addEntry(RADIATION, pu239 * nugget * 10));
        HazardSystem.register(NukeCasingItems.MIKE_CORE.get(),
                new HazardData().addEntry(RADIATION, u238 * nugget * 10));
        HazardSystem.register(NukeCasingItems.TSAR_CORE.get(),
                new HazardData().addEntry(RADIATION, pu239 * nugget * 15));

        // CE HazardRegistry.java:515-521 FLEIJA / solinium / nuke_fstbmb.
        // Skip :522-523 holotape DIGAMMA (meta split — do not invent merge).
        HazardSystem.register(NukeCasingItems.FLEIJA_PROPELLANT.get(),
                new HazardData().addEntry(RADIATION, 15F).addEntry(EXPLOSIVE, 8F).addEntry(BLINDING, 50F));
        HazardSystem.register(NukeCasingItems.FLEIJA_CORE.get(),
                new HazardData().addEntry(RADIATION, 10F));
        HazardSystem.register(Phase11ProcessItems.SOLINIUM_PROPELLANT.get(),
                new HazardData().addEntry(EXPLOSIVE, 10F));
        HazardSystem.register(NukeCasingItems.SOLINIUM_CORE.get(),
                new HazardData().addEntry(RADIATION, sa327 * nugget * 8).addEntry(BLINDING, 45F));
        HazardSystem.register(NukeCasingBlocks.NUKE_FSTBMB.get(),
                new HazardData().addEntry(DIGAMMA, 0.01F));

        // items_ingot_nugget area (docs/phase1/moditems_generative.md section 3,
        // hazard_bindings_plan.md Pattern A/B): only the ingot_/nugget_ fields CE's own
        // HazardRegistry.registerItems() actually binds (upstream hbm-ce HazardRegistry.java
        // lines 306-346) - every other ingot_/nugget_ field (plain metal ingots like
        // ingot_uranium/ingot_plutonium/nugget_polonium, and the ItemUnstable/ItemHot/
        // ItemSchraranium/ItemFuel/food-backed fields) carries no hazard data in CE at all; do not
        // invent radiation for them.
        HazardSystem.register(IngotNuggetItems.NUGGET_URANIUM_FUEL.get(), new HazardData().addEntry(RADIATION, uf * nugget));
        HazardSystem.register(IngotNuggetItems.INGOT_URANIUM_FUEL.get(), new HazardData().addEntry(RADIATION, uf * ingot));
        // CE HazardRegistry.java:309
        HazardSystem.register(ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "block_uranium_fuel"),
                new HazardData().addEntry(RADIATION, uf * block));

        HazardSystem.register(IngotNuggetItems.NUGGET_PLUTONIUM_FUEL.get(), new HazardData().addEntry(RADIATION, puf * nugget));
        HazardSystem.register(IngotNuggetItems.INGOT_PLUTONIUM_FUEL.get(), new HazardData().addEntry(RADIATION, puf * ingot));
        // CE :315
        HazardSystem.register(ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "block_plutonium_fuel"),
                new HazardData().addEntry(RADIATION, puf * block));

        HazardSystem.register(IngotNuggetItems.NUGGET_THORIUM_FUEL.get(), new HazardData().addEntry(RADIATION, thf * nugget));
        HazardSystem.register(IngotNuggetItems.INGOT_THORIUM_FUEL.get(), new HazardData().addEntry(RADIATION, thf * ingot));
        // CE :320
        HazardSystem.register(ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "block_thorium_fuel"),
                new HazardData().addEntry(RADIATION, thf * block));

        HazardSystem.register(IngotNuggetItems.NUGGET_NEPTUNIUM_FUEL.get(), new HazardData().addEntry(RADIATION, npf * nugget));
        HazardSystem.register(IngotNuggetItems.INGOT_NEPTUNIUM_FUEL.get(), new HazardData().addEntry(RADIATION, npf * ingot));

        HazardSystem.register(IngotNuggetItems.NUGGET_MOX_FUEL.get(), new HazardData().addEntry(RADIATION, mox * nugget));
        HazardSystem.register(IngotNuggetItems.INGOT_MOX_FUEL.get(), new HazardData().addEntry(RADIATION, mox * ingot));
        // CE :329
        HazardSystem.register(ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "block_mox_fuel"),
                new HazardData().addEntry(RADIATION, mox * block));

        HazardSystem.register(IngotNuggetItems.NUGGET_AMERICIUM_FUEL.get(), new HazardData().addEntry(RADIATION, amf * nugget));
        HazardSystem.register(IngotNuggetItems.INGOT_AMERICIUM_FUEL.get(), new HazardData().addEntry(RADIATION, amf * ingot));

        HazardSystem.register(IngotNuggetItems.NUGGET_SCHRABIDIUM_FUEL.get(),
                new HazardData().addEntry(RADIATION, saf * nugget).addEntry(BLINDING, 5F * nugget));
        HazardSystem.register(IngotNuggetItems.INGOT_SCHRABIDIUM_FUEL.get(),
                new HazardData().addEntry(RADIATION, saf * ingot).addEntry(BLINDING, 5F * ingot));
        // CE :338
        HazardSystem.register(ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "block_schrabidium_fuel"),
                new HazardData().addEntry(RADIATION, saf * block).addEntry(BLINDING, 5F * block));

        HazardSystem.register(IngotNuggetItems.NUGGET_HES.get(), new HazardData().addEntry(RADIATION, saf * nugget));
        HazardSystem.register(IngotNuggetItems.INGOT_HES.get(), new HazardData().addEntry(RADIATION, saf * ingot));

        HazardSystem.register(IngotNuggetItems.NUGGET_LES.get(), new HazardData().addEntry(RADIATION, saf * nugget));
        HazardSystem.register(IngotNuggetItems.INGOT_LES.get(), new HazardData().addEntry(RADIATION, saf * ingot));

        // modblocks_storage_family: material storage-block hazard bindings
        // (docs/phase1/modblocks_generative.md section 1b/6.3; com.hbm.blocks.MaterialBlockGenerator).
        // CE's own registerItems() never actually binds these plain (non-fuel) storage blocks to any
        // HazardData at all - only the *_fuel/_trinitite/_waste block variants get an entry above
        // (block_uranium_fuel = uf * block, block_trinitite = trn * block, etc.); block_uranium,
        // block_thorium, block_plutonium, block_u233/235/238, block_pu238/239/240, block_schrabidium
        // and friends are BlockHazard-typed in CE but carry no registered radiation whatsoever
        // (confirmed by grep against upstream hbm-ce HazardRegistry.java - no block_uranium/
        // block_thorium/block_plutonium/block_u2../block_pu2../block_neptunium/block_polonium/
        // block_schrabidium/block_schraranium/block_schrabidate/block_solinium/block_ra226/
        // block_magnetized_tungsten hit anywhere in that file). Rather than leave every one of this
        // port's 21 BlockHazard material blocks silently inert, this section applies CE's own
        // established "unit isotope rate * shape multiplier" formula (the exact shape already used
        // for the *_fuel blocks just above, and for rods/pellets/RTGs elsewhere in this file) to the
        // base per-isotope rate constants for the plain block shape.
        HazardSystem.register(MaterialBlockGenerator.get(Mats.MAT_URANIUM).get(), new HazardData().addEntry(RADIATION, u * block));
        HazardSystem.register(MaterialBlockGenerator.get(Mats.MAT_U233).get(), new HazardData().addEntry(RADIATION, u233 * block));
        HazardSystem.register(MaterialBlockGenerator.get(Mats.MAT_U235).get(), new HazardData().addEntry(RADIATION, u235 * block));
        HazardSystem.register(MaterialBlockGenerator.get(Mats.MAT_U238).get(), new HazardData().addEntry(RADIATION, u238 * block));
        HazardSystem.register(MaterialBlockGenerator.get(Mats.MAT_THORIUM).get(), new HazardData().addEntry(RADIATION, th232 * block));
        HazardSystem.register(MaterialBlockGenerator.get(Mats.MAT_PLUTONIUM).get(), new HazardData().addEntry(RADIATION, pu * block));
        HazardSystem.register(MaterialBlockGenerator.get(Mats.MAT_RGP).get(), new HazardData().addEntry(RADIATION, purg * block));
        HazardSystem.register(MaterialBlockGenerator.get(Mats.MAT_PU238).get(), new HazardData().addEntry(RADIATION, pu238 * block));
        HazardSystem.register(MaterialBlockGenerator.get(Mats.MAT_PU239).get(), new HazardData().addEntry(RADIATION, pu239 * block));
        HazardSystem.register(MaterialBlockGenerator.get(Mats.MAT_PU240).get(), new HazardData().addEntry(RADIATION, pu240 * block));
        HazardSystem.register(MaterialBlockGenerator.get(Mats.MAT_PU241).get(), new HazardData().addEntry(RADIATION, pu241 * block));
        HazardSystem.register(MaterialBlockGenerator.get(Mats.MAT_RGA).get(), new HazardData().addEntry(RADIATION, amrg * block));
        HazardSystem.register(MaterialBlockGenerator.get(Mats.MAT_AM241).get(), new HazardData().addEntry(RADIATION, am241 * block));
        HazardSystem.register(MaterialBlockGenerator.get(Mats.MAT_AM242).get(), new HazardData().addEntry(RADIATION, am242 * block));
        HazardSystem.register(MaterialBlockGenerator.get(Mats.MAT_NEPTUNIUM).get(), new HazardData().addEntry(RADIATION, np237 * block));
        HazardSystem.register(MaterialBlockGenerator.get(Mats.MAT_POLONIUM).get(), new HazardData().addEntry(RADIATION, po210 * block));
        HazardSystem.register(MaterialBlockGenerator.get(Mats.MAT_RADIUM).get(), new HazardData().addEntry(RADIATION, ra226 * block));
        HazardSystem.register(MaterialBlockGenerator.get(Mats.MAT_SCHRABIDIUM).get(), new HazardData().addEntry(RADIATION, sa326 * block));
        HazardSystem.register(MaterialBlockGenerator.get(Mats.MAT_SOLINIUM).get(), new HazardData().addEntry(RADIATION, sa327 * block));
        // schrabidate/schraranium have no dedicated CE isotope-rate constant of their own (CE never
        // assigns block_schrabidate/block_schraranium a radiation level either); reusing sa326
        // (schrabidium's own rate - CE gives both the identical 5.0F/300.0F hardness/resistance,
        // marking them as the same super-heavy tier) is the closest same-family analogue rather than
        // inventing an unrelated number.
        HazardSystem.register(MaterialBlockGenerator.get(Mats.MAT_SCHRABIDATE).get(), new HazardData().addEntry(RADIATION, sa326 * block));
        HazardSystem.register(MaterialBlockGenerator.get(Mats.MAT_SCHRARANIUM).get(), new HazardData().addEntry(RADIATION, sa326 * block));
        // Magnetized tungsten has no isotope constant of its own in CE, and CE itself never registers
        // a radiation level for block_magnetized_tungsten either; reusing u238 (a depleted-uranium-
        // adjacent trace level) is a deliberately conservative placeholder, not a CE-sourced number.
        HazardSystem.register(MaterialBlockGenerator.get(Mats.MAT_MAGTUNG).get(), new HazardData().addEntry(RADIATION, u238 * block));

        // BlockOutgas (asbestos) / BlockHydroreactive (lithium) storage blocks. CE never binds
        // block_asbestos to the ASBESTOS hazard type or block_lithium to HYDROACTIVE either - both
        // blocks' actual CE behavior (gas emission, water-contact explosion) is hardcoded directly in
        // the block class rather than routed through HazardSystem - but every other asbestos-bearing
        // item in this file gets an ASBESTOS entry (brick_asbestos/tile_lab_broken = 1F,
        // powder_coltan_ore = 3F) and every hydroactive item gets a HYDROACTIVE entry
        // (powder_caesium = 1F), so these storage blocks get a matching entry for consistency,
        // reusing the same 1F baseline level.
        HazardSystem.register(MaterialBlockGenerator.get(Mats.MAT_ASBESTOS).get(), new HazardData().addEntry(ASBESTOS, 1F));
        HazardSystem.register(MaterialBlockGenerator.get(Mats.MAT_LITHIUM).get(), new HazardData().addEntry(HYDROACTIVE, 1F));

        // hazard_wiring_complete: items_special demon core + nuclear waste
        // (docs/phase1/hazard_bindings_plan.md Pattern F for the demon core; CE HazardRegistry.java
        // demon_core_open/demon_core_closed + nuclear_waste_long/nuclear_waste_short bindings).
        //
        // demon_core_open: RADIATION 5F plus a HazardTypeDangerousDrop that swaps the dropped item
        // back to the closed core on ground contact, mirroring CE's EntityItem/onGround transition
        // with the confirmed 1.21 renames (EntityItem -> ItemEntity, .world -> .level(),
        // .isRemote -> .isClientSide(), world.spawnEntity(...) -> level().addFreshEntity(...); see
        // docs/phase1/DIGEST_REMAINDER.md's hazard_bindings_plan.md section). CE also re-spawns a
        // dropped screwdriver alongside the closed core on this transition; this port has not
        // registered ItemTooling/screwdriver yet (no such item exists anywhere in this codebase), so
        // that half of the drop behavior is intentionally left out here rather than referencing a
        // nonexistent item - flagged as a follow-up once the tooling items land.
        HazardSystem.register(SpecialItems.DEMON_CORE_OPEN.get(), new HazardData()
                .addEntry(RADIATION, 5F)
                .addEntry(new HazardEntry(new HazardTypeDangerousDrop((entityItem, level) -> {
                    if (!entityItem.level().isClientSide() && entityItem.onGround()) {
                        entityItem.setItem(new ItemStack(SpecialItems.DEMON_CORE_CLOSED.get()));
                    }
                }))));
        HazardSystem.register(SpecialItems.DEMON_CORE_CLOSED.get(), new HazardData().addEntry(RADIATION, 100_000F));

        // nuclear_waste_long_* (5 WasteClass variants) / nuclear_waste_short_* (8 WasteClass
        // variants): CE binds nuclear_waste_long flat to RADIATION 5F and nuclear_waste_short flat to
        // RADIATION 30F + HOT 5F with no per-material variation; applied identically across every
        // WasteClass this port flattened CE's single metadata-multi field into.
        for (final ItemWasteLong.WasteClass wasteClass : ItemWasteLong.WasteClass.VALUES) {
            HazardSystem.register(SpecialItems.nuclearWasteLong(wasteClass).get(), new HazardData().addEntry(RADIATION, 5F));
        }
        for (final ItemWasteShort.WasteClass wasteClass : ItemWasteShort.WasteClass.VALUES) {
            HazardSystem.register(SpecialItems.nuclearWasteShort(wasteClass).get(),
                    new HazardData().addEntry(RADIATION, 30F).addEntry(HOT, 5F));
        }

        // hazard_wiring_complete: items_machine fuel/rod/pellet family
        // (docs/phase1/hazard_bindings_plan.md Patterns D/E; CE HazardRegistry.java
        // registerBreedingRodRadiation/registerOtherFuel/registerPWRFuel/registerRTGPellet/
        // registerRBMKPellet call sites plus the plain EnumPileRod/EnumWatzType tables). The
        // parametric helper methods mirroring CE's private helpers are declared further below, after
        // this method.

        // ItemBreedingRod: rod_/rod_dual_/rod_quad_, 14 of BreedingRodType's 17 values - CE never
        // calls registerBreedingRodRadiation for LITHIUM, CO or LEAD anywhere in registerItems().
        registerBreedingRodRadiation(ItemBreedingRod.BreedingRodType.TRITIUM, 0.001F);
        registerBreedingRodRadiation(ItemBreedingRod.BreedingRodType.CO60, co60);
        registerBreedingRodRadiation(ItemBreedingRod.BreedingRodType.RA226, ra226);
        registerBreedingRodRadiation(ItemBreedingRod.BreedingRodType.AC227, ac227);
        registerBreedingRodRadiation(ItemBreedingRod.BreedingRodType.TH232, th232);
        registerBreedingRodRadiation(ItemBreedingRod.BreedingRodType.THF, thf);
        registerBreedingRodRadiation(ItemBreedingRod.BreedingRodType.U235, u235);
        registerBreedingRodRadiation(ItemBreedingRod.BreedingRodType.NP237, np237);
        registerBreedingRodRadiation(ItemBreedingRod.BreedingRodType.U238, u238);
        registerBreedingRodRadiation(ItemBreedingRod.BreedingRodType.PU238, pu238);
        registerBreedingRodRadiation(ItemBreedingRod.BreedingRodType.PU239, pu239);
        registerBreedingRodRadiation(ItemBreedingRod.BreedingRodType.RGP, purg);
        registerBreedingRodRadiation(ItemBreedingRod.BreedingRodType.WASTE, wst);
        registerBreedingRodRadiation(ItemBreedingRod.BreedingRodType.URANIUM, u);

        // ItemZirnoxRod: all 11 EnumZirnoxType variants (CE's registerOtherFuel calls - depletion
        // curve toward the listed waste target via HazardModifierFuelRadiation).
        registerOtherFuel(MachineItems.ZIRNOX_RODS.get(ItemZirnoxRod.EnumZirnoxType.NATURAL_URANIUM_FUEL).get(), u * rod_dual, wst * rod_dual * 11.5F, false);
        registerOtherFuel(MachineItems.ZIRNOX_RODS.get(ItemZirnoxRod.EnumZirnoxType.URANIUM_FUEL).get(), uf * rod_dual, wst * rod_dual * 10F, false);
        registerOtherFuel(MachineItems.ZIRNOX_RODS.get(ItemZirnoxRod.EnumZirnoxType.TH232_FUEL).get(), th232 * rod_dual, thf * rod_dual, false);
        registerOtherFuel(MachineItems.ZIRNOX_RODS.get(ItemZirnoxRod.EnumZirnoxType.THORIUM_FUEL).get(), thf * rod_dual, wst * rod_dual * 7.5F, false);
        registerOtherFuel(MachineItems.ZIRNOX_RODS.get(ItemZirnoxRod.EnumZirnoxType.MOX_FUEL).get(), mox * rod_dual, wst * rod_dual * 10F, false);
        registerOtherFuel(MachineItems.ZIRNOX_RODS.get(ItemZirnoxRod.EnumZirnoxType.PLUTONIUM_FUEL).get(), puf * rod_dual, wst * rod_dual * 12.5F, false);
        registerOtherFuel(MachineItems.ZIRNOX_RODS.get(ItemZirnoxRod.EnumZirnoxType.U233_FUEL).get(), u233 * rod_dual, wst * rod_dual * 10F, false);
        registerOtherFuel(MachineItems.ZIRNOX_RODS.get(ItemZirnoxRod.EnumZirnoxType.U235_FUEL).get(), u235 * rod_dual, wst * rod_dual * 11F, false);
        registerOtherFuel(MachineItems.ZIRNOX_RODS.get(ItemZirnoxRod.EnumZirnoxType.LES_FUEL).get(), saf * rod_dual, wst * rod_dual * 15F, false);
        registerOtherFuel(MachineItems.ZIRNOX_RODS.get(ItemZirnoxRod.EnumZirnoxType.LITHIUM_FUEL).get(), 0F, 0.001F * rod_dual, false);
        registerOtherFuel(MachineItems.ZIRNOX_RODS.get(ItemZirnoxRod.EnumZirnoxType.ZFB_MOX_FUEL).get(), mox * rod_dual, wst * rod_dual * 5F, false);

        // ItemZirnoxRodDepleted: all 9 EnumZirnoxTypeDepleted variants, plain RADIATION (already
        // fully depleted, no modifier chain needed) except LES_FUEL which also carries CE's BLINDING.
        HazardSystem.register(MachineItems.ZIRNOX_RODS_DEPLETED.get(ItemZirnoxRodDepleted.EnumZirnoxTypeDepleted.NATURAL_URANIUM_FUEL).get(),
                new HazardData().addEntry(RADIATION, wst * rod_dual * 11.5F));
        HazardSystem.register(MachineItems.ZIRNOX_RODS_DEPLETED.get(ItemZirnoxRodDepleted.EnumZirnoxTypeDepleted.URANIUM_FUEL).get(),
                new HazardData().addEntry(RADIATION, wst * rod_dual * 10F));
        HazardSystem.register(MachineItems.ZIRNOX_RODS_DEPLETED.get(ItemZirnoxRodDepleted.EnumZirnoxTypeDepleted.THORIUM_FUEL).get(),
                new HazardData().addEntry(RADIATION, wst * rod_dual * 7.5F));
        HazardSystem.register(MachineItems.ZIRNOX_RODS_DEPLETED.get(ItemZirnoxRodDepleted.EnumZirnoxTypeDepleted.MOX_FUEL).get(),
                new HazardData().addEntry(RADIATION, wst * rod_dual * 10F));
        HazardSystem.register(MachineItems.ZIRNOX_RODS_DEPLETED.get(ItemZirnoxRodDepleted.EnumZirnoxTypeDepleted.PLUTONIUM_FUEL).get(),
                new HazardData().addEntry(RADIATION, wst * rod_dual * 12.5F));
        HazardSystem.register(MachineItems.ZIRNOX_RODS_DEPLETED.get(ItemZirnoxRodDepleted.EnumZirnoxTypeDepleted.U233_FUEL).get(),
                new HazardData().addEntry(RADIATION, wst * rod_dual * 10F));
        HazardSystem.register(MachineItems.ZIRNOX_RODS_DEPLETED.get(ItemZirnoxRodDepleted.EnumZirnoxTypeDepleted.U235_FUEL).get(),
                new HazardData().addEntry(RADIATION, wst * rod_dual * 11F));
        HazardSystem.register(MachineItems.ZIRNOX_RODS_DEPLETED.get(ItemZirnoxRodDepleted.EnumZirnoxTypeDepleted.LES_FUEL).get(),
                new HazardData().addEntry(RADIATION, wst * rod_dual * 15F).addEntry(BLINDING, 20F));
        HazardSystem.register(MachineItems.ZIRNOX_RODS_DEPLETED.get(ItemZirnoxRodDepleted.EnumZirnoxTypeDepleted.ZFB_MOX_FUEL).get(),
                new HazardData().addEntry(RADIATION, wst * rod_dual * 5F));

        // ItemPWRFuel: all 15 EnumPWRFuel values (CE's registerPWRFuel calls). Only the fresh
        // pwr_fuel_* item exists in this port so far - MachineItems.registerPwrFuel()'s own comment
        // notes pwr_fuel_hot/pwr_fuel_depleted are separate CE byproduct-marker items sharing this
        // same enum, out of that class's scope and not yet registered here - so only the fresh item's
        // base RADIATION level (CE's un-multiplied "baseRad" argument) is bound; the *10 hot/depleted
        // tiers are a follow-up once those items exist.
        HazardSystem.register(MachineItems.PWR_FUEL.get(ItemPWRFuel.EnumPWRFuel.MEU).get(), new HazardData().addEntry(RADIATION, uf * billet * 2));
        HazardSystem.register(MachineItems.PWR_FUEL.get(ItemPWRFuel.EnumPWRFuel.HEU233).get(), new HazardData().addEntry(RADIATION, u233 * billet * 2));
        HazardSystem.register(MachineItems.PWR_FUEL.get(ItemPWRFuel.EnumPWRFuel.HEU235).get(), new HazardData().addEntry(RADIATION, u235 * billet * 2));
        HazardSystem.register(MachineItems.PWR_FUEL.get(ItemPWRFuel.EnumPWRFuel.MEN).get(), new HazardData().addEntry(RADIATION, npf * billet * 2));
        HazardSystem.register(MachineItems.PWR_FUEL.get(ItemPWRFuel.EnumPWRFuel.HEN237).get(), new HazardData().addEntry(RADIATION, np237 * billet * 2));
        HazardSystem.register(MachineItems.PWR_FUEL.get(ItemPWRFuel.EnumPWRFuel.MOX).get(), new HazardData().addEntry(RADIATION, mox * billet * 2));
        HazardSystem.register(MachineItems.PWR_FUEL.get(ItemPWRFuel.EnumPWRFuel.MEP).get(), new HazardData().addEntry(RADIATION, purg * billet * 2));
        HazardSystem.register(MachineItems.PWR_FUEL.get(ItemPWRFuel.EnumPWRFuel.HEP239).get(), new HazardData().addEntry(RADIATION, pu239 * billet * 2));
        HazardSystem.register(MachineItems.PWR_FUEL.get(ItemPWRFuel.EnumPWRFuel.HEP241).get(), new HazardData().addEntry(RADIATION, pu241 * billet * 2));
        HazardSystem.register(MachineItems.PWR_FUEL.get(ItemPWRFuel.EnumPWRFuel.MEA).get(), new HazardData().addEntry(RADIATION, amrg * billet * 2));
        HazardSystem.register(MachineItems.PWR_FUEL.get(ItemPWRFuel.EnumPWRFuel.HEA242).get(), new HazardData().addEntry(RADIATION, am242 * billet * 2));
        HazardSystem.register(MachineItems.PWR_FUEL.get(ItemPWRFuel.EnumPWRFuel.HES326).get(), new HazardData().addEntry(RADIATION, sa326 * billet * 2));
        HazardSystem.register(MachineItems.PWR_FUEL.get(ItemPWRFuel.EnumPWRFuel.HES327).get(), new HazardData().addEntry(RADIATION, sa327 * billet * 2));
        HazardSystem.register(MachineItems.PWR_FUEL.get(ItemPWRFuel.EnumPWRFuel.BFB_AM_MIX).get(), new HazardData().addEntry(RADIATION, amrg * billet));
        HazardSystem.register(MachineItems.PWR_FUEL.get(ItemPWRFuel.EnumPWRFuel.BFB_PU241).get(), new HazardData().addEntry(RADIATION, pu241 * billet));

        // ItemPileRodMK2: 8 of EnumPileRod's 9 values - CE binds exactly these 8 (to its own
        // ModItems.pile_rod field, a CE naming quirk for what is actually the MK2 rod); ZR is never
        // bound anywhere in CE's registerItems(). Plain RADIATION calls, no modifier chain.
        HazardSystem.register(MachineItems.PILE_RODS_MK2.get(ItemPileRodMK2.EnumPileRod.RA226BE).get(), new HazardData().addEntry(RADIATION, rabe * billet * 3));
        HazardSystem.register(MachineItems.PILE_RODS_MK2.get(ItemPileRodMK2.EnumPileRod.PO210BE).get(), new HazardData().addEntry(RADIATION, pobe * billet * 3));
        HazardSystem.register(MachineItems.PILE_RODS_MK2.get(ItemPileRodMK2.EnumPileRod.NU).get(), new HazardData().addEntry(RADIATION, u * billet * 3));
        HazardSystem.register(MachineItems.PILE_RODS_MK2.get(ItemPileRodMK2.EnumPileRod.PU239).get(), new HazardData().addEntry(RADIATION, pu239 * billet * 3));
        HazardSystem.register(MachineItems.PILE_RODS_MK2.get(ItemPileRodMK2.EnumPileRod.RGP).get(), new HazardData().addEntry(RADIATION, purg * billet * 3));
        HazardSystem.register(MachineItems.PILE_RODS_MK2.get(ItemPileRodMK2.EnumPileRod.WASTE).get(), new HazardData().addEntry(RADIATION, wst * billet * 3));
        HazardSystem.register(MachineItems.PILE_RODS_MK2.get(ItemPileRodMK2.EnumPileRod.THORIUM).get(), new HazardData().addEntry(RADIATION, th232 * billet * 3));
        HazardSystem.register(MachineItems.PILE_RODS_MK2.get(ItemPileRodMK2.EnumPileRod.THORIUM_FUEL).get(), new HazardData().addEntry(RADIATION, thf * billet * 3));

        // ItemRTGPellet: 10 variants (CE's registerRTGPellet calls). pellet_rtg_depleted's Neptunium
        // byproduct variant (a different, plain Item class - RTG_DEPLETED in MachineItems, private and
        // not part of this table) and the port-only PELLET_RTG_BALEFIRE addition (no CE hazard entry
        // exists for it) are both outside CE's own registerRTGPellet table and are skipped.
        registerRTGPellet(MachineItems.PELLET_RTG.get(), pu238 * rtg, 0F, 3F);
        registerRTGPellet(MachineItems.PELLET_RTG_RADIUM.get(), ra226 * rtg, 0F);
        registerRTGPellet(MachineItems.PELLET_RTG_WEAK.get(), (pu238 + (u238 * 2)) * billet, 0F);
        registerRTGPellet(MachineItems.PELLET_RTG_STRONTIUM.get(), sr90 * rtg, 0F);
        registerRTGPellet(MachineItems.PELLET_RTG_COBALT.get(), co60 * rtg, 0F);
        registerRTGPellet(MachineItems.PELLET_RTG_ACTINIUM.get(), ac227 * rtg, 0F);
        registerRTGPellet(MachineItems.PELLET_RTG_POLONIUM.get(), po210 * rtg, 0F, 3F);
        registerRTGPellet(MachineItems.PELLET_RTG_LEAD.get(), pb209 * rtg, 0F, 7F, 50F);
        registerRTGPellet(MachineItems.PELLET_RTG_GOLD.get(), au198 * rtg, 0F, 5F);
        registerRTGPellet(MachineItems.PELLET_RTG_AMERICIUM.get(), am241 * rtg, 0F);

        // ItemWatzPellet: 10 of EnumWatzType's 12 values (CE binds exactly these 10 fresh pellets to
        // RADIATION; LEAD and BORON are never bound anywhere in CE's registerItems()).
        HazardSystem.register(MachineItems.WATZ_PELLET.get(ItemWatzPellet.EnumWatzType.SCHRABIDIUM).get(), new HazardData().addEntry(RADIATION, sa326 * ingot * 4));
        HazardSystem.register(MachineItems.WATZ_PELLET.get(ItemWatzPellet.EnumWatzType.HES).get(), new HazardData().addEntry(RADIATION, saf * ingot * 4));
        HazardSystem.register(MachineItems.WATZ_PELLET.get(ItemWatzPellet.EnumWatzType.MES).get(), new HazardData().addEntry(RADIATION, saf * ingot * 4));
        HazardSystem.register(MachineItems.WATZ_PELLET.get(ItemWatzPellet.EnumWatzType.LES).get(), new HazardData().addEntry(RADIATION, saf * ingot * 4));
        HazardSystem.register(MachineItems.WATZ_PELLET.get(ItemWatzPellet.EnumWatzType.HEN).get(), new HazardData().addEntry(RADIATION, np237 * ingot * 4));
        HazardSystem.register(MachineItems.WATZ_PELLET.get(ItemWatzPellet.EnumWatzType.MEU).get(), new HazardData().addEntry(RADIATION, uf * ingot * 4));
        HazardSystem.register(MachineItems.WATZ_PELLET.get(ItemWatzPellet.EnumWatzType.MEP).get(), new HazardData().addEntry(RADIATION, purg * ingot * 4));
        HazardSystem.register(MachineItems.WATZ_PELLET.get(ItemWatzPellet.EnumWatzType.DU).get(), new HazardData().addEntry(RADIATION, u238 * ingot * 4));
        HazardSystem.register(MachineItems.WATZ_PELLET.get(ItemWatzPellet.EnumWatzType.NQD).get(), new HazardData().addEntry(RADIATION, u235 * ingot * 4));
        HazardSystem.register(MachineItems.WATZ_PELLET.get(ItemWatzPellet.EnumWatzType.NQR).get(), new HazardData().addEntry(RADIATION, pu239 * ingot * 4));

        // ItemRBMKPellet: all 32 fresh pellet ids MachineItems.registerRbmkPellets() actually
        // registers. No accessor map exists for these (they're registered by bare id string through
        // MachineItems' private reg() helper with no field/map capturing the result), so each is
        // bound here by ResourceLocation the same way HazardSystem.register(ResourceLocation, ...) is
        // documented to resolve - immediately, against the already-registered item, safe to call from
        // common setup. ItemRBMKRod itself is out of scope (docs/phase1/items_machine.md: deferred
        // alongside the RBMK tile-entity package it imports from), matching CE's registerRBMKRod
        // calls not being ported here.
        registerRBMKPellet("rbmk_pellet_ueu", u * billet, wst * billet * 20F);
        registerRBMKPellet("rbmk_pellet_meu", uf * billet, wst * billet * 21.5F);
        registerRBMKPellet("rbmk_pellet_heu233", u233 * billet, wst * billet * 31F);
        registerRBMKPellet("rbmk_pellet_heu235", u235 * billet, wst * billet * 30F);
        registerRBMKPellet("rbmk_pellet_uzh", uzh * billet, wst * billet * 20F);
        registerRBMKPellet("rbmk_pellet_thmeu", thf * billet, wst * billet * 17.5F);
        registerRBMKPellet("rbmk_pellet_lep", puf * billet, wst * billet * 25F);
        registerRBMKPellet("rbmk_pellet_mep", purg * billet, wst * billet * 30F);
        registerRBMKPellet("rbmk_pellet_hep239", pu239 * billet, wst * billet * 32.5F);
        registerRBMKPellet("rbmk_pellet_hep241", pu241 * billet, wst * billet * 35F);
        registerRBMKPellet("rbmk_pellet_lea", amf * billet, wst * billet * 26F);
        registerRBMKPellet("rbmk_pellet_mea", amrg * billet, wst * billet * 30.5F);
        registerRBMKPellet("rbmk_pellet_hea241", am241 * billet, wst * billet * 33.5F);
        registerRBMKPellet("rbmk_pellet_hea242", am242 * billet, wst * billet * 34F);
        registerRBMKPellet("rbmk_pellet_men", npf * billet, wst * billet * 22.5F);
        registerRBMKPellet("rbmk_pellet_hen", np237 * billet, wst * billet * 30F);
        registerRBMKPellet("rbmk_pellet_mox", mox * billet, wst * billet * 25.5F);
        registerRBMKPellet("rbmk_pellet_les", saf * billet, wst * billet * 24.5F);
        registerRBMKPellet("rbmk_pellet_mes", saf * billet, wst * billet * 30F);
        registerRBMKPellet("rbmk_pellet_hes", saf * billet, wst * billet * 50F);
        registerRBMKPellet("rbmk_pellet_leaus", 0F, wst * billet * 37.5F);
        registerRBMKPellet("rbmk_pellet_heaus", 0F, wst * billet * 32.5F);
        registerRBMKPellet("rbmk_pellet_po210be", pobe * billet, pobe * billet * 0.1F, true);
        registerRBMKPellet("rbmk_pellet_ra226be", rabe * billet, rabe * billet * 0.4F, true);
        registerRBMKPellet("rbmk_pellet_pu238be", pube * billet, wst * 1.5F);
        registerRBMKPellet("rbmk_pellet_balefire_gold", au198 * billet, bf * billet * 0.5F, true);
        registerRBMKPellet("rbmk_pellet_flashlead", pb209 * 1.25F * billet, pb209 * nugget * 0.05F, true);
        registerRBMKPellet("rbmk_pellet_balefire", bf * billet, bf * billet * 100F, true);
        registerRBMKPellet("rbmk_pellet_zfb_bismuth", pu241 * billet * 0.1F, wst * billet * 5F);
        registerRBMKPellet("rbmk_pellet_zfb_pu241", pu239 * billet * 0.1F, wst * billet * 7.5F);
        registerRBMKPellet("rbmk_pellet_zfb_am_mix", pu241 * billet * 0.1F, wst * billet * 10F);
        registerRBMKPellet("rbmk_pellet_drx", bf * billet, bf * billet * 100F, true, 0F, 1F / 24F);

        // docs/phase3/scattered_military_items.md: CE's ItemUnstable pocket-nuke decay/detonation
        // timer, generalized here via the already-real HazardTypeUnstable parametric strategy
        // (com.hbm.hazard.type.HazardTypeUnstable, confirmed already built and already wired to the
        // per-stack HazardComponents.UNSTABLE_DECAY_TIMER component, but not yet bound to any item)
        // rather than a bespoke ItemUnstable Item subclass - the "level" argument to addEntry is the
        // explosion radius, the HazardTypeUnstable constructor argument is the decay-tick timer.
        // Exact (radius, timer) pairs confirmed against CE's real ModItems.java `new
        // ItemUnstable(radius, timer, name)` call sites (only 3 exist in all of CE): ingot_u238m2
        // (350, 200), ingot_electronium (30, 6000), nugget_u238m2 (60, 2000). The inert
        // ELEMENTS/ARSENIC/VAULT reskins (ingot_u238m2_elements/_arsenic/_vault) are deliberately not
        // bound here - CE's own ItemUnstable short-circuits all behavior for those metas, and
        // post-flattening they are already separate plain-Item registry entries with no ticking
        // behavior of their own (see IngotNuggetItems).
        HazardSystem.register(IngotNuggetItems.INGOT_U238M2.get(),
                new HazardData().addEntry(new HazardTypeUnstable(200), 350));
        HazardSystem.register(IngotNuggetItems.INGOT_ELECTRONIUM.get(),
                new HazardData().addEntry(new HazardTypeUnstable(6000), 30));
        HazardSystem.register(IngotNuggetItems.NUGGET_U238M2.get(),
                new HazardData().addEntry(new HazardTypeUnstable(2000), 60));
    }

    // ==================== items_machine hazard-wiring helpers (CE Pattern E, ported near-verbatim; Item
    // parameters take the already-resolved concrete item via the items area's own DeferredItem<Item>.get()
    // accessors instead of CE's meta-variant overloads) ====================

    private static void registerBreedingRodRadiation(final ItemBreedingRod.BreedingRodType type, final float base) {
        HazardSystem.register(MachineItems.BREEDING_ROD_SINGLE.get(type).get(), new HazardData().addEntry(RADIATION, base));
        HazardSystem.register(MachineItems.BREEDING_ROD_DUAL.get(type).get(), new HazardData().addEntry(RADIATION, base * rod_dual));
        HazardSystem.register(MachineItems.BREEDING_ROD_QUAD.get(type).get(), new HazardData().addEntry(RADIATION, base * rod_quad));
    }

    private static void registerOtherFuel(final Item fuel, final float base, final float target, final boolean blinding) {
        final HazardData data = new HazardData();
        data.addEntry(new HazardEntry(RADIATION, base).addMod(new HazardModifierFuelRadiation(target)));
        if (blinding) data.addEntry(BLINDING, 20F);
        HazardSystem.register(fuel, data);
    }

    private static void registerRTGPellet(final Item pellet, final float base, final float target) {
        registerRTGPellet(pellet, base, target, 0F, 0F);
    }

    private static void registerRTGPellet(final Item pellet, final float base, final float target, final float hot) {
        registerRTGPellet(pellet, base, target, hot, 0F);
    }

    private static void registerRTGPellet(final Item pellet, final float base, final float target, final float hot, final float blinding) {
        final HazardData data = new HazardData();
        data.addEntry(new HazardEntry(RADIATION, base).addMod(new HazardModifierRTGRadiation(target)));
        if (hot > 0) data.addEntry(new HazardEntry(HOT, hot));
        if (blinding > 0) data.addEntry(new HazardEntry(BLINDING, blinding));
        HazardSystem.register(pellet, data);
    }

    private static void registerRBMKPellet(final String name, final float base, final float dep) {
        registerRBMKPellet(name, base, dep, false, 0F, 0F);
    }

    private static void registerRBMKPellet(final String name, final float base, final float dep, final boolean linear) {
        registerRBMKPellet(name, base, dep, linear, 0F, 0F);
    }

    private static void registerRBMKPellet(final String name, final float base, final float dep, final boolean linear, final float blinding, final float digamma) {
        final HazardData data = new HazardData();
        data.addEntry(new HazardEntry(RADIATION, base).addMod(new HazardModifierRBMKRadiation(dep, linear)));
        if (blinding > 0) data.addEntry(new HazardEntry(BLINDING, blinding));
        if (digamma > 0) data.addEntry(new HazardEntry(DIGAMMA, digamma));
        HazardSystem.register(ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, name), data);
    }

    /**
     * Phase 1: contaminating-drop overlay for ore dust tags, run after ore tags are registered. See CE's
     * {@code HazardRegistry.registerContaminatingDrops()}.
     */
    public static void registerContaminatingDrops() {
        // hazard_wiring_complete: registerContaminatingDrops gap-fill
        // (docs/phase1/hazard_bindings_plan.md Pattern C; CE HazardRegistry.java
        // registerContaminatingDrops(), lines 643-670). CE's version layers a CONTAMINATING entry
        // onto already-registered ore-dict dust hazard data via oreMap.computeIfPresent - this port
        // has no pre-existing dust RADIATION registration to layer onto (registerItems() above never
        // touches any MaterialShapes.DUST tag), so each dust tag below is registered directly instead
        // (direct Pattern C, no oreMap pre-population needed): a fresh HazardData carrying just the
        // CONTAMINATING entry, keyed off the material's own c:dusts/<material> convention tag via
        // MaterialShapes.DUST.commonTag(...).
        //
        // Only materials that (a) already have DUST in their Mats.java autogen set and (b) have a
        // real matching NTMMaterial constant in this port are bound here:
        //   th232 -> MAT_THORIUM, u -> MAT_URANIUM, pu -> MAT_PLUTONIUM, np237 -> MAT_NEPTUNIUM,
        //   po210 -> MAT_POLONIUM, sa326 -> MAT_SCHRABIDIUM, sb -> MAT_SCHRABIDATE (CE's "SBD"
        //   DictFrame is literally named "Schrabidate" in OreDictManager.java - a distinct material
        //   from schrabidium/MAT_SCHRABIDIUM despite the "sb" constant name, not strontium as an
        //   earlier planning pass guessed), co60 -> MAT_CO60, au198 -> MAT_AU198,
        //   ra226 -> MAT_RADIUM, pb209 -> MAT_PB209, and MAT_TCALLOY at CE's 0.07F * powder rate.
        // Skipped, not invented: CE's TS.dust() (120F * powder) has no matching Mats.java material
        // identified for this pass. CE's AC227/SR90/I131/XE135/CS137/AT209 dust (+ dustTiny) entries
        // have no matching NTMMaterial constant in this port's Mats.java at all - actinium,
        // strontium-90, iodine-131, xenon-135, caesium-137 and astatine-209 are not modeled as
        // materials here yet - left for whichever items area eventually adds them, rather than
        // inventing new Mats.java constants from this hazard-registry pass.
        HazardSystem.register(MaterialShapes.DUST.commonTag(Mats.MAT_TCALLOY), new HazardData().addEntry(CONTAMINATING, 0.07F * powder));
        HazardSystem.register(MaterialShapes.DUST.commonTag(Mats.MAT_THORIUM), new HazardData().addEntry(CONTAMINATING, th232 * powder));
        HazardSystem.register(MaterialShapes.DUST.commonTag(Mats.MAT_URANIUM), new HazardData().addEntry(CONTAMINATING, u * powder));
        HazardSystem.register(MaterialShapes.DUST.commonTag(Mats.MAT_PLUTONIUM), new HazardData().addEntry(CONTAMINATING, pu * powder));
        HazardSystem.register(MaterialShapes.DUST.commonTag(Mats.MAT_NEPTUNIUM), new HazardData().addEntry(CONTAMINATING, np237 * powder));
        HazardSystem.register(MaterialShapes.DUST.commonTag(Mats.MAT_POLONIUM), new HazardData().addEntry(CONTAMINATING, po210 * powder));
        HazardSystem.register(MaterialShapes.DUST.commonTag(Mats.MAT_SCHRABIDIUM), new HazardData().addEntry(CONTAMINATING, sa326 * powder));
        HazardSystem.register(MaterialShapes.DUST.commonTag(Mats.MAT_SCHRABIDATE), new HazardData().addEntry(CONTAMINATING, sb * powder));
        HazardSystem.register(MaterialShapes.DUST.commonTag(Mats.MAT_CO60), new HazardData().addEntry(CONTAMINATING, co60 * powder));
        HazardSystem.register(MaterialShapes.DUST.commonTag(Mats.MAT_AU198), new HazardData().addEntry(CONTAMINATING, au198 * powder));
        HazardSystem.register(MaterialShapes.DUST.commonTag(Mats.MAT_RADIUM), new HazardData().addEntry(CONTAMINATING, ra226 * powder));
        HazardSystem.register(MaterialShapes.DUST.commonTag(Mats.MAT_PB209), new HazardData().addEntry(CONTAMINATING, pb209 * powder));
    }

    /**
     * Wires the hazard transformer chain. Call once during mod common setup, after {@link HazardComponents} has been
     * registered on the mod event bus.
     */
    public static void registerTrafos() {
        HazardSystem.trafos.add(new HazardTransformerRadiationNBT());
        HazardSystem.trafos.add(new HazardTransformerForgeFluid());

        if (!(GeneralConfig.enableLBSM() && GeneralConfig.LBSM_SAFE_CRATES.get())) HazardSystem.trafos.add(new HazardTransformerRadiationContainer());
        if (!(GeneralConfig.enableLBSM() && GeneralConfig.LBSM_SAFE_ME_DRIVES.get())) HazardSystem.trafos.add(new HazardTransformerRadiationME());
        // Keep the custom post transformer last
        HazardSystem.trafos.add(new HazardTransformerPostCustom());
    }
}
