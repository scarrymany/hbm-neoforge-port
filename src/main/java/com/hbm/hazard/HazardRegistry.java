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

/**
 * Physics-derived level constants and the {@link IHazardType} singletons every {@code HazardData} entry is built
 * from, plus the transformer chain wiring.
 * <p>
 * {@link #registerItems()} and {@link #registerContaminatingDrops()} are intentionally empty. CE's ~460-line bulk
 * item-to-hazard binding table (and the specialized {@code registerRBMKRod}/{@code registerRTGPellet}/... helper
 * methods it uses) bind concrete items from the ~458-item CE catalog, which is Phase 1 content work, not part of
 * this registry-mechanism deliverable. Phase 1 should populate these two methods the same way CE's
 * {@code HazardRegistry} does, using the constants and {@link IHazardType} singletons declared here.
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
