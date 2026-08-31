package com.hbm.client.render.armor;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Function;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.Model;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

import com.hbm.items.armor.PoweredArmorItems;
import com.hbm.items.gear.JetpackItems;
import com.hbm.main.MainRegistry;

/**
 * Central "one labeled call per armor item/set" registration point for CE's <b>"Path A"</b> custom
 * armor rendering (a standalone-worn piece swapping in its own {@link
 * com.hbm.client.render.armor.ArmorModelBase} in place of the vanilla body shape) - see {@link
 * ArmorModelBase}'s class javadoc for the full confirmed-API-shape citation trail
 * ({@code IClientItemExtensions#getGenericArmorModel}, {@code
 * docs/phase5/armor_humanoidmodel_rendering.md}). Mirrors {@code com.hbm.client.render.
 * ClientEntityRenderers}' own "every entity gets one labeled {@code EntityRenderers.register} call,
 * a future Content-wave agent finds-and-replaces exactly one line" structure, applied to armor
 * items instead of entity types.
 *
 * <h2>Registration mechanism - confirmed real, not the task brief's guessed event package</h2>
 * Fires from {@link net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent}
 * (note the real package: {@code net.neoforged.neoforge.client.extensions.common}, <b>not</b>
 * {@code net.neoforged.neoforge.client.event} - the latter does not exist for this event; confirmed
 * by two independent real, compiling import sites in {@code upstream/neo-edition}: {@code
 * items/weapon/sedna/factory/GunFactoryClient.java} and {@code main/ClientProxy.java}, both of which
 * also confirm the exact {@code event.registerItem(IClientItemExtensions, Item...)} varargs call
 * shape this class's {@link #registerArmorModel} helper uses below). This class self-registers via
 * {@code @EventBusSubscriber(bus = Bus.MOD)} on itself, rather than requiring a manual wiring line
 * in {@code com.hbm.main.ClientModRegistry} - the same self-registering pattern that class's own
 * javadoc already documents for {@code com.hbm.handler.HbmKeybinds}, and the pattern this task's
 * own brief explicitly prefers "if it fits the API shape better."
 *
 * <p><b>{@code bus = Bus.MOD} is required, and Neo Edition's own reference tree is live proof of
 * what happens without it</b> (this port's ground rules' recurring-bug-pattern 5, re-confirmed
 * concretely by this task's own research rather than just cited abstractly): {@code
 * RegisterClientExtensionsEvent} is a mod-bus event, but Neo Edition's {@code
 * main/NuclearTechModClient.java} annotates its whole class {@code @EventBusSubscriber(value =
 * Dist.CLIENT)} - <b>no {@code bus =}</b>, defaulting to {@code Bus.GAME} - so its own {@code
 * onRegisterClientExtensions(RegisterClientExtensionsEvent)} {@code @SubscribeEvent} method can
 * never actually fire via the bus. Neo Edition works around its own bug with a Mixin invoker
 * method ({@code util/mixins/invokers/RegisterClientExtensionsEventInvoker.java}) that manually
 * constructs a {@code RegisterClientExtensionsEvent} instance and calls {@code
 * proxy.registerClientExtensions(event)} directly from inside {@code
 * FMLClientSetupEvent.enqueueWork} instead of fixing the annotation. This port has no
 * Mixin infrastructure at all (confirmed absent by {@code
 * docs/phase5/armor_humanoidmodel_rendering.md} and re-confirmed by this task's own grep) and needs
 * none here: setting {@code bus = EventBusSubscriber.Bus.MOD} correctly below is sufficient for the
 * event to fire normally, exactly like every other mod-bus {@code Register*Event} this port's
 * {@code ClientModRegistry}/{@code ClientEntityRenderers}/particle-provider classes already handle
 * correctly. Not copying Neo Edition's Mixin workaround here is a deliberate choice, not an
 * oversight - see this port's ground rules on Neo Edition being cross-checked for API shape only,
 * never blindly copied.
 *
 * <h2>Why this is not pre-populated with one stub call per already-registered armor item</h2>
 * Unlike {@code ClientEntityRenderers} (where <i>omitting</i> a registration call is a guaranteed
 * client crash the moment the entity spawns, per that class's own javadoc), omitting a {@code
 * getGenericArmorModel} override for an armor item is completely safe: {@code
 * IClientItemExtensions}'s default implementation (inherited, never overridden here) returns {@code
 * original} unchanged, so the item simply renders with its plain vanilla per-material armor texture
 * layer - correct, harmless default behavior, not a landmine. This task's own brief is explicit
 * that authoring every armor piece's real geometry/texture is <b>not</b> this task's job (zero
 * {@code .obj}/PNG armor assets are ported into this port yet - a separate, already-flagged Content
 * wave (c7) gap), so this class intentionally registers only the one concrete, fully-wired {@link
 * #registerHev example} rather than ~66 empty placeholder stubs that would just be noise for a
 * later pass to delete. A future Content-wave agent adds one new {@code registerXyz(event)} call
 * inside {@link #registerAll} plus one new private static method below it, following {@link
 * #registerHev}'s exact shape - same "find one clean insertion point, do not touch any other set's
 * lines" ergonomics {@code ClientEntityRenderers} already established for entities.
 *
 * <p><b>Update (task {@code c7-armor-model-rendering})</b>: exactly that future pass has now
 * happened - every remaining {@code PoweredArmorItems}/{@code JetpackItems} set named by {@code
 * docs/phase5/armor_humanoidmodel_rendering.md} finding 3's bucket (a) (OBJ-driven) census is
 * registered below via {@link ObjArmorModel} (a generic, data-driven leaf extracted from what was
 * originally {@link #registerHev}'s one-off body - see that class's own javadoc for the full
 * design rationale, including its documented single-texture-per-slot simplification). Bucket (c)
 * sets ({@code ArmorLiquidator}; CE has no matching {@code ModelArmorLiquidator} at all) correctly
 * get <b>no</b> registration call here - they render via the plain vanilla per-material armor
 * texture layer already, exactly as this class's original javadoc predicted for any un-registered
 * item. Bucket (b) hand-modeled leaves ({@code gas_mask}/{@code gas_mask_m65}/{@code _mono}/{@code
 * _olde}, {@code hazmat_helmet_red}/{@code _grey}, {@code nossy_hat}) are wired through their own
 * item classes' existing {@code initializeClient} hook instead of through this registry (see
 * {@link com.hbm.items.gear.ArmorGasMask}/{@link com.hbm.items.gear.ArmorHazmatMask}/{@link
 * com.hbm.items.armor.ArmorHat}) - those classes already had a per-item client-model hook from
 * Phase 3 (or, for {@code ArmorHat}, from {@link com.hbm.items.gear.ArmorModel}), so registering
 * the <i>same</i> item a second time here via {@code event.registerItem(...)} would risk a
 * double-registration conflict for no benefit; this registry's external mechanism is reserved for
 * the ~66 {@code PoweredArmorItems}/{@code JetpackItems} leaves that have no such per-item hook of
 * their own (confirmed by this task's own grep: no {@code ArmorFSB}/{@code ArmorFSBPowered}/{@code
 * ArmorFSBFueled}/{@code ItemArmorMod} class anywhere in this tree overrides {@code
 * initializeClient}).
 */
@EventBusSubscriber(modid = MainRegistry.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class ArmorRenderRegistry {

    private ArmorRenderRegistry() {
    }

    @SubscribeEvent
    public static void onRegisterClientExtensions(RegisterClientExtensionsEvent event) {
        registerAll(event);
    }

    /**
     * Add one new {@code registerXyz(event);} line here per future armor set, plus a matching
     * private static method below (see {@link #registerHev} for the template) - do not fold
     * multiple sets into one method or loop over a computed list; this mirrors {@code
     * ClientEntityRenderers#registerAll}'s own "one labeled call per item/set" convention exactly,
     * so a Content-wave diff touches exactly the lines for the set it is adding.
     */
    public static void registerAll(RegisterClientExtensionsEvent event) {
        registerHev(event);
        registerAjr(event);
        registerAjro(event);
        registerBismuth(event);
        registerDns(event);
        registerSteamsuit(event);
        registerDieselsuit(event);
        registerT51(event);
        registerTaurun(event);
        registerTrenchmaster(event);
        registerRpa(event);
        registerNcrpa(event);
        registerFau(event);
        registerEnvsuit(event);
        registerBj(event);
        registerBjJetpack(event);
        registerJetpacks(event);
    }

    /**
     * CE's HEV power-armor set (4 pieces) - real geometry via {@link HevArmorModel}, a thin fixed-
     * configuration subclass of {@link ObjArmorModel} (see that class's own javadoc).
     */
    private static void registerHev(RegisterClientExtensionsEvent event) {
        registerArmorModel(event, HevArmorModel::new,
                PoweredArmorItems.HEV_HELMET.get(),
                PoweredArmorItems.HEV_PLATE.get(),
                PoweredArmorItems.HEV_LEGS.get(),
                PoweredArmorItems.HEV_BOOTS.get());
    }

    /**
     * CE's AJR power-armor set (4 pieces) - {@code render/model/ModelArmorAJR.java} (51 lines, read
     * in full), {@code models/armor/ajr.obj} (CE's own {@code ResourceManager} references {@code
     * "AJR.obj"}; the real on-disk file this port's directory listing confirms is lowercase {@code
     * ajr.obj} - CE's own case mismatch never mattered on Windows, but does on a case-sensitive
     * filesystem, so the lowercase name is used here).
     */
    private static void registerAjr(RegisterClientExtensionsEvent event) {
        ResourceLocation obj = rl("models/armor/ajr.obj");
        Map<EquipmentSlot, ObjArmorModel.SlotRecipe> recipes = Map.of(
                EquipmentSlot.HEAD, ObjArmorModel.SlotRecipe.of(rl("textures/armor/ajr_helmet.png"), "Head"),
                EquipmentSlot.CHEST, ObjArmorModel.SlotRecipe.of(rl("textures/armor/ajr_chest.png"), "Body", "LeftArm", "RightArm"),
                EquipmentSlot.LEGS, ObjArmorModel.SlotRecipe.of(rl("textures/armor/ajr_leg.png"), "LeftLeg", "RightLeg"),
                EquipmentSlot.FEET, ObjArmorModel.SlotRecipe.of(rl("textures/armor/ajr_leg.png"), "LeftBoot", "RightBoot"));
        registerArmorModel(event, slot -> new ObjArmorModel(slot, obj, recipes),
                PoweredArmorItems.AJR_HELMET.get(), PoweredArmorItems.AJR_PLATE.get(),
                PoweredArmorItems.AJR_LEGS.get(), PoweredArmorItems.AJR_BOOTS.get());
    }

    /**
     * CE's AJRO power-armor set (4 pieces) - {@code render/model/ModelArmorAJRO.java} (52 lines,
     * read in full): shares AJR's {@code models/armor/ajr.obj} mesh (confirmed - CE's own
     * constructor uses {@code ResourceManager.armor_ajr} for AJRO too) with its own separate
     * {@code ajro_*} textures.
     */
    private static void registerAjro(RegisterClientExtensionsEvent event) {
        ResourceLocation obj = rl("models/armor/ajr.obj");
        Map<EquipmentSlot, ObjArmorModel.SlotRecipe> recipes = Map.of(
                EquipmentSlot.HEAD, ObjArmorModel.SlotRecipe.of(rl("textures/armor/ajro_helmet.png"), "Head"),
                EquipmentSlot.CHEST, ObjArmorModel.SlotRecipe.of(rl("textures/armor/ajro_chest.png"), "Body", "LeftArm", "RightArm"),
                EquipmentSlot.LEGS, ObjArmorModel.SlotRecipe.of(rl("textures/armor/ajro_leg.png"), "LeftLeg", "RightLeg"),
                EquipmentSlot.FEET, ObjArmorModel.SlotRecipe.of(rl("textures/armor/ajro_leg.png"), "LeftBoot", "RightBoot"));
        registerArmorModel(event, slot -> new ObjArmorModel(slot, obj, recipes),
                PoweredArmorItems.AJRO_HELMET.get(), PoweredArmorItems.AJRO_PLATE.get(),
                PoweredArmorItems.AJRO_LEGS.get(), PoweredArmorItems.AJRO_BOOTS.get());
    }

    /**
     * CE's Bismuth armor set (4 pieces) - {@code render/model/ModelArmorBismuth.java} (59 lines,
     * read in full): {@code models/armor/bismuth.obj}, a single shared texture for every slot
     * ({@code ResourceManager.armor_bismuth_tex}, {@code "textures/armor/bismuth.png"} - CE binds
     * it once outside the {@code switch}, unlike every other set here).
     */
    private static void registerBismuth(RegisterClientExtensionsEvent event) {
        ResourceLocation obj = rl("models/armor/bismuth.obj");
        ResourceLocation tex = rl("textures/armor/bismuth.png");
        Map<EquipmentSlot, ObjArmorModel.SlotRecipe> recipes = Map.of(
                EquipmentSlot.HEAD, ObjArmorModel.SlotRecipe.of(tex, "Head"),
                EquipmentSlot.CHEST, ObjArmorModel.SlotRecipe.of(tex, "Body", "LeftArm", "RightArm"),
                EquipmentSlot.LEGS, ObjArmorModel.SlotRecipe.of(tex, "LeftLeg", "RightLeg"),
                EquipmentSlot.FEET, ObjArmorModel.SlotRecipe.of(tex, "LeftFoot", "RightFoot"));
        registerArmorModel(event, slot -> new ObjArmorModel(slot, obj, recipes),
                PoweredArmorItems.BISMUTH_HELMET.get(), PoweredArmorItems.BISMUTH_PLATE.get(),
                PoweredArmorItems.BISMUTH_LEGS.get(), PoweredArmorItems.BISMUTH_BOOTS.get());
    }

    /**
     * CE's DNT/"dns" power-armor set (4 pieces, this port's registry ids {@code dns_*} - see
     * {@code PoweredArmorItems}' own javadoc for the id-vs-class-name note) - {@code
     * render/model/ModelArmorDNT.java} (51 lines, read in full): {@code models/armor/dnt.obj},
     * {@code dnt_*} textures.
     */
    private static void registerDns(RegisterClientExtensionsEvent event) {
        ResourceLocation obj = rl("models/armor/dnt.obj");
        Map<EquipmentSlot, ObjArmorModel.SlotRecipe> recipes = Map.of(
                EquipmentSlot.HEAD, ObjArmorModel.SlotRecipe.of(rl("textures/armor/dnt_helmet.png"), "Head"),
                EquipmentSlot.CHEST, ObjArmorModel.SlotRecipe.of(rl("textures/armor/dnt_chest.png"), "Body", "LeftArm", "RightArm"),
                EquipmentSlot.LEGS, ObjArmorModel.SlotRecipe.of(rl("textures/armor/dnt_leg.png"), "LeftLeg", "RightLeg"),
                EquipmentSlot.FEET, ObjArmorModel.SlotRecipe.of(rl("textures/armor/dnt_leg.png"), "LeftBoot", "RightBoot"));
        registerArmorModel(event, slot -> new ObjArmorModel(slot, obj, recipes),
                PoweredArmorItems.DNS_HELMET.get(), PoweredArmorItems.DNS_PLATE.get(),
                PoweredArmorItems.DNS_LEGS.get(), PoweredArmorItems.DNS_BOOTS.get());
    }

    /**
     * CE's Desh/"steamsuit" armor set (4 pieces) - {@code render/model/ModelArmorDesh.java} (52
     * lines, read in full): {@code models/armor/steamsuit.obj}, {@code steamsuit_*} textures.
     */
    private static void registerSteamsuit(RegisterClientExtensionsEvent event) {
        ResourceLocation obj = rl("models/armor/steamsuit.obj");
        Map<EquipmentSlot, ObjArmorModel.SlotRecipe> recipes = Map.of(
                EquipmentSlot.HEAD, ObjArmorModel.SlotRecipe.of(rl("textures/armor/steamsuit_helmet.png"), "Head"),
                EquipmentSlot.CHEST, ObjArmorModel.SlotRecipe.of(rl("textures/armor/steamsuit_chest.png"), "Body", "LeftArm", "RightArm"),
                EquipmentSlot.LEGS, ObjArmorModel.SlotRecipe.of(rl("textures/armor/steamsuit_leg.png"), "LeftLeg", "RightLeg"),
                EquipmentSlot.FEET, ObjArmorModel.SlotRecipe.of(rl("textures/armor/steamsuit_leg.png"), "LeftBoot", "RightBoot"));
        registerArmorModel(event, slot -> new ObjArmorModel(slot, obj, recipes),
                PoweredArmorItems.STEAMSUIT_HELMET.get(), PoweredArmorItems.STEAMSUIT_PLATE.get(),
                PoweredArmorItems.STEAMSUIT_LEGS.get(), PoweredArmorItems.STEAMSUIT_BOOTS.get());
    }

    /**
     * CE's Diesel/"dieselsuit" armor set (4 pieces) - {@code render/model/ModelArmorDiesel.java}
     * (64 lines, read in full): {@code ResourceManager.armor_dieselsuit} resolves to {@code
     * models/armor/bnuuy.obj} in CE's real, current source (confirmed by direct read - not {@code
     * dieselsuit.obj}, which does not exist; the Dieselsuit reuses the joke "bnuuy" mesh file,
     * resolving {@code docs/phase5/armor_humanoidmodel_rendering.md} open question 4 definitively),
     * with its own real {@code bnuuy_*} textures ({@code dieselsuit_helmet} etc. all point at
     * {@code textures/armor/bnuuy_*.png} in CE's {@code ResourceManager}).
     */
    private static void registerDieselsuit(RegisterClientExtensionsEvent event) {
        ResourceLocation obj = rl("models/armor/bnuuy.obj");
        Map<EquipmentSlot, ObjArmorModel.SlotRecipe> recipes = Map.of(
                EquipmentSlot.HEAD, ObjArmorModel.SlotRecipe.of(rl("textures/armor/bnuuy_helmet.png"), "Head"),
                EquipmentSlot.CHEST, ObjArmorModel.SlotRecipe.of(rl("textures/armor/bnuuy_chest.png"), "Body", "LeftArm", "RightArm"),
                EquipmentSlot.LEGS, ObjArmorModel.SlotRecipe.of(rl("textures/armor/bnuuy_leg.png"), "LeftLeg", "RightLeg"),
                EquipmentSlot.FEET, ObjArmorModel.SlotRecipe.of(rl("textures/armor/bnuuy_leg.png"), "LeftBoot", "RightBoot"));
        registerArmorModel(event, slot -> new ObjArmorModel(slot, obj, recipes),
                PoweredArmorItems.DIESELSUIT_HELMET.get(), PoweredArmorItems.DIESELSUIT_PLATE.get(),
                PoweredArmorItems.DIESELSUIT_LEGS.get(), PoweredArmorItems.DIESELSUIT_BOOTS.get());
    }

    /**
     * CE's T51 power-armor set (4 pieces) - {@code render/model/ModelArmorT51.java} (53 lines, read
     * in full): {@code models/armor/t51.obj}, {@code t51_*} textures. Group names are {@code
     * "Helmet"}/{@code "Chest"} here (not {@code "Head"}/{@code "Body"}) - CE's own literal choice
     * for this set, transcribed exactly.
     */
    private static void registerT51(RegisterClientExtensionsEvent event) {
        ResourceLocation obj = rl("models/armor/t51.obj");
        Map<EquipmentSlot, ObjArmorModel.SlotRecipe> recipes = Map.of(
                EquipmentSlot.HEAD, ObjArmorModel.SlotRecipe.of(rl("textures/armor/t51_helmet.png"), "Helmet"),
                EquipmentSlot.CHEST, ObjArmorModel.SlotRecipe.of(rl("textures/armor/t51_chest.png"), "Chest", "LeftArm", "RightArm"),
                EquipmentSlot.LEGS, ObjArmorModel.SlotRecipe.of(rl("textures/armor/t51_leg.png"), "LeftLeg", "RightLeg"),
                EquipmentSlot.FEET, ObjArmorModel.SlotRecipe.of(rl("textures/armor/t51_leg.png"), "LeftBoot", "RightBoot"));
        registerArmorModel(event, slot -> new ObjArmorModel(slot, obj, recipes),
                PoweredArmorItems.T51_HELMET.get(), PoweredArmorItems.T51_PLATE.get(),
                PoweredArmorItems.T51_LEGS.get(), PoweredArmorItems.T51_BOOTS.get());
    }

    /**
     * CE's Taurun armor set (4 pieces) - {@code render/model/ModelArmorTaurun.java} (62 lines, read
     * in full): {@code models/armor/taurun.obj}, {@code taurun_*} textures, {@code "Helmet"}/{@code
     * "Chest"} group names (same convention as T51).
     */
    private static void registerTaurun(RegisterClientExtensionsEvent event) {
        ResourceLocation obj = rl("models/armor/taurun.obj");
        Map<EquipmentSlot, ObjArmorModel.SlotRecipe> recipes = Map.of(
                EquipmentSlot.HEAD, ObjArmorModel.SlotRecipe.of(rl("textures/armor/taurun_helmet.png"), "Helmet"),
                EquipmentSlot.CHEST, ObjArmorModel.SlotRecipe.of(rl("textures/armor/taurun_chest.png"), "Chest", "LeftArm", "RightArm"),
                EquipmentSlot.LEGS, ObjArmorModel.SlotRecipe.of(rl("textures/armor/taurun_leg.png"), "LeftLeg", "RightLeg"),
                EquipmentSlot.FEET, ObjArmorModel.SlotRecipe.of(rl("textures/armor/taurun_leg.png"), "LeftBoot", "RightBoot"));
        registerArmorModel(event, slot -> new ObjArmorModel(slot, obj, recipes),
                PoweredArmorItems.TAURUN_HELMET.get(), PoweredArmorItems.TAURUN_PLATE.get(),
                PoweredArmorItems.TAURUN_LEGS.get(), PoweredArmorItems.TAURUN_BOOTS.get());
    }

    /**
     * CE's Trenchmaster armor set (4 pieces) - {@code render/model/ModelArmorTrenchmaster.java} (81
     * lines, read in full): {@code models/armor/trenchmaster.obj}, {@code trenchmaster_*} textures,
     * {@code "Helmet"}/{@code "Chest"} group names, plus a full-bright {@code "Light"} glow group on
     * the helmet slot (CE: alpha-blended draw + {@code disableLighting()}/full lightmap - see {@link
     * ObjArmorModel.SlotRecipe}'s own javadoc for how the glow-part convention maps this).
     */
    private static void registerTrenchmaster(RegisterClientExtensionsEvent event) {
        ResourceLocation obj = rl("models/armor/trenchmaster.obj");
        Map<EquipmentSlot, ObjArmorModel.SlotRecipe> recipes = Map.of(
                EquipmentSlot.HEAD, ObjArmorModel.SlotRecipe.of(rl("textures/armor/trenchmaster_helmet.png"), "Helmet").withGlow("Light"),
                EquipmentSlot.CHEST, ObjArmorModel.SlotRecipe.of(rl("textures/armor/trenchmaster_chest.png"), "Chest", "LeftArm", "RightArm"),
                EquipmentSlot.LEGS, ObjArmorModel.SlotRecipe.of(rl("textures/armor/trenchmaster_leg.png"), "LeftLeg", "RightLeg"),
                EquipmentSlot.FEET, ObjArmorModel.SlotRecipe.of(rl("textures/armor/trenchmaster_leg.png"), "LeftBoot", "RightBoot"));
        registerArmorModel(event, slot -> new ObjArmorModel(slot, obj, recipes),
                PoweredArmorItems.TRENCHMASTER_HELMET.get(), PoweredArmorItems.TRENCHMASTER_PLATE.get(),
                PoweredArmorItems.TRENCHMASTER_LEGS.get(), PoweredArmorItems.TRENCHMASTER_BOOTS.get());
    }

    /**
     * CE's RPA armor set (4 pieces) - {@code render/model/ModelArmorRPA.java} (84 lines, read in
     * full): {@code ResourceManager.armor_remnant} resolves to {@code models/armor/remnant.obj}
     * (not {@code rpa.obj}), {@code rpa_*} textures, plus a full-bright {@code "Glow"} group on the
     * chest slot (CE's own {@code this.body.copyTo(this.glow)} + fullbright draw). CE's rotating
     * {@code "Fan"} group is rendered here as an ordinary (non-spinning) extra chest part - the
     * per-frame Y-axis spin ({@code System.currentTimeMillis()}-driven) is a real, documented
     * simplification cut, not an oversight.
     */
    private static void registerRpa(RegisterClientExtensionsEvent event) {
        ResourceLocation obj = rl("models/armor/remnant.obj");
        Map<EquipmentSlot, ObjArmorModel.SlotRecipe> recipes = Map.of(
                EquipmentSlot.HEAD, ObjArmorModel.SlotRecipe.of(rl("textures/armor/rpa_helmet.png"), "Head"),
                EquipmentSlot.CHEST, ObjArmorModel.SlotRecipe.of(rl("textures/armor/rpa_chest.png"), "Body", "Fan", "LeftArm", "RightArm").withGlow("Glow"),
                EquipmentSlot.LEGS, ObjArmorModel.SlotRecipe.of(rl("textures/armor/rpa_leg.png"), "LeftLeg", "RightLeg"),
                EquipmentSlot.FEET, ObjArmorModel.SlotRecipe.of(rl("textures/armor/rpa_leg.png"), "LeftBoot", "RightBoot"));
        registerArmorModel(event, slot -> new ObjArmorModel(slot, obj, recipes),
                PoweredArmorItems.RPA_HELMET.get(), PoweredArmorItems.RPA_PLATE.get(),
                PoweredArmorItems.RPA_LEGS.get(), PoweredArmorItems.RPA_BOOTS.get());
    }

    /**
     * CE's NCRPA armor set (4 pieces) - {@code render/model/ModelArmorNCRPA.java} (72 lines, read
     * in full): {@code models/armor/ncrpa.obj}, {@code ncrpa_*} textures, {@code "Helmet"}/{@code
     * "Chest"} group names, plus a full-bright {@code "Eyes"} glow group on the helmet slot.
     */
    private static void registerNcrpa(RegisterClientExtensionsEvent event) {
        ResourceLocation obj = rl("models/armor/ncrpa.obj");
        Map<EquipmentSlot, ObjArmorModel.SlotRecipe> recipes = Map.of(
                EquipmentSlot.HEAD, ObjArmorModel.SlotRecipe.of(rl("textures/armor/ncrpa_helmet.png"), "Helmet").withGlow("Eyes"),
                EquipmentSlot.CHEST, ObjArmorModel.SlotRecipe.of(rl("textures/armor/ncrpa_chest.png"), "Chest", "LeftArm", "RightArm"),
                EquipmentSlot.LEGS, ObjArmorModel.SlotRecipe.of(rl("textures/armor/ncrpa_leg.png"), "LeftLeg", "RightLeg"),
                EquipmentSlot.FEET, ObjArmorModel.SlotRecipe.of(rl("textures/armor/ncrpa_leg.png"), "LeftBoot", "RightBoot"));
        registerArmorModel(event, slot -> new ObjArmorModel(slot, obj, recipes),
                PoweredArmorItems.NCRPA_HELMET.get(), PoweredArmorItems.NCRPA_PLATE.get(),
                PoweredArmorItems.NCRPA_LEGS.get(), PoweredArmorItems.NCRPA_BOOTS.get());
    }

    /**
     * CE's Digamma/"fau" armor set (4 pieces) - {@code render/model/ModelArmorDigamma.java} (63
     * lines, read in full): {@code models/armor/fau.obj}, {@code fau_*} textures, plus a {@code
     * "Cassette"} extra chest group. CE draws {@code Cassette} through its <i>own</i> separate
     * {@code fau_cassette.png} file with additive-ish blending - both dropped here as a documented
     * simplification (this class's single-texture-per-slot design, see {@link ObjArmorModel}'s
     * javadoc): the cassette geometry still draws, sharing the chest texture/normal blend instead.
     */
    private static void registerFau(RegisterClientExtensionsEvent event) {
        ResourceLocation obj = rl("models/armor/fau.obj");
        Map<EquipmentSlot, ObjArmorModel.SlotRecipe> recipes = Map.of(
                EquipmentSlot.HEAD, ObjArmorModel.SlotRecipe.of(rl("textures/armor/fau_helmet.png"), "Head"),
                EquipmentSlot.CHEST, ObjArmorModel.SlotRecipe.of(rl("textures/armor/fau_chest.png"), "Body", "Cassette", "LeftArm", "RightArm"),
                EquipmentSlot.LEGS, ObjArmorModel.SlotRecipe.of(rl("textures/armor/fau_leg.png"), "LeftLeg", "RightLeg"),
                EquipmentSlot.FEET, ObjArmorModel.SlotRecipe.of(rl("textures/armor/fau_leg.png"), "LeftBoot", "RightBoot"));
        registerArmorModel(event, slot -> new ObjArmorModel(slot, obj, recipes),
                PoweredArmorItems.FAU_HELMET.get(), PoweredArmorItems.FAU_PLATE.get(),
                PoweredArmorItems.FAU_LEGS.get(), PoweredArmorItems.FAU_BOOTS.get());
    }

    /**
     * CE's Envsuit armor set (4 pieces) - {@code render/model/ModelArmorEnvsuit.java} (101 lines,
     * read in full): {@code models/armor/envsuit.obj}, {@code envsuit_*} textures, {@code
     * "Helmet"}/{@code "Chest"} group names, {@code "LeftFoot"}/{@code "RightFoot"} (not {@code
     * Boot}) feet group names, plus a full-bright {@code "Lamps"} glow group on the helmet slot.
     */
    private static void registerEnvsuit(RegisterClientExtensionsEvent event) {
        ResourceLocation obj = rl("models/armor/envsuit.obj");
        Map<EquipmentSlot, ObjArmorModel.SlotRecipe> recipes = Map.of(
                EquipmentSlot.HEAD, ObjArmorModel.SlotRecipe.of(rl("textures/armor/envsuit_helmet.png"), "Helmet").withGlow("Lamps"),
                EquipmentSlot.CHEST, ObjArmorModel.SlotRecipe.of(rl("textures/armor/envsuit_chest.png"), "Chest", "LeftArm", "RightArm"),
                EquipmentSlot.LEGS, ObjArmorModel.SlotRecipe.of(rl("textures/armor/envsuit_leg.png"), "LeftLeg", "RightLeg"),
                EquipmentSlot.FEET, ObjArmorModel.SlotRecipe.of(rl("textures/armor/envsuit_leg.png"), "LeftFoot", "RightFoot"));
        registerArmorModel(event, slot -> new ObjArmorModel(slot, obj, recipes),
                PoweredArmorItems.ENVSUIT_HELMET.get(), PoweredArmorItems.ENVSUIT_PLATE.get(),
                PoweredArmorItems.ENVSUIT_LEGS.get(), PoweredArmorItems.ENVSUIT_BOOTS.get());
    }

    /**
     * CE's BJ armor set (4 plain pieces - the jetpack-chest variant is {@link #registerBjJetpack}
     * below, a distinct item) - {@code render/model/ModelArmorBJ.java} (72 lines, read in full):
     * {@code models/armor/bj.obj} (CE's own {@code ResourceManager} references {@code "BJ.obj"};
     * the real on-disk file is lowercase {@code bj.obj} - same case-correction as {@link
     * #registerAjr}), {@code bj_eyepatch}/{@code bj_leg}/{@code bj_chest}/{@code bj_arm} textures,
     * {@code "LeftFoot"}/{@code "RightFoot"} feet group names.
     */
    private static void registerBj(RegisterClientExtensionsEvent event) {
        ResourceLocation obj = rl("models/armor/bj.obj");
        Map<EquipmentSlot, ObjArmorModel.SlotRecipe> recipes = Map.of(
                EquipmentSlot.HEAD, ObjArmorModel.SlotRecipe.of(rl("textures/armor/bj_eyepatch.png"), "Head"),
                EquipmentSlot.CHEST, ObjArmorModel.SlotRecipe.of(rl("textures/armor/bj_chest.png"), "Body", "LeftArm", "RightArm"),
                EquipmentSlot.LEGS, ObjArmorModel.SlotRecipe.of(rl("textures/armor/bj_leg.png"), "LeftLeg", "RightLeg"),
                EquipmentSlot.FEET, ObjArmorModel.SlotRecipe.of(rl("textures/armor/bj_leg.png"), "LeftFoot", "RightFoot"));
        registerArmorModel(event, slot -> new ObjArmorModel(slot, obj, recipes),
                PoweredArmorItems.BJ_HELMET.get(), PoweredArmorItems.BJ_PLATE.get(),
                PoweredArmorItems.BJ_LEGS.get(), PoweredArmorItems.BJ_BOOTS.get());
    }

    /**
     * CE's {@code bj_plate_jetpack} ({@link com.hbm.items.armor.ArmorBJJetpack}) - a single distinct
     * chestplate item (not part of the plain BJ set above), CE's {@code type == 5} branch in {@code
     * ModelArmorBJ#renderArmor}: the same {@code Body}/{@code LeftArm}/{@code RightArm} groups plus
     * an extra {@code "Jetpack"} sub-mesh (CE binds it through its own separate {@code
     * bj_jetpack.png} - dropped here as the same documented single-texture-per-slot simplification
     * as {@link #registerFau}'s cassette; the jetpack geometry still draws, sharing {@code
     * bj_chest.png} instead).
     */
    private static void registerBjJetpack(RegisterClientExtensionsEvent event) {
        ResourceLocation obj = rl("models/armor/bj.obj");
        Map<EquipmentSlot, ObjArmorModel.SlotRecipe> recipes = Map.of(
                EquipmentSlot.CHEST, ObjArmorModel.SlotRecipe.of(rl("textures/armor/bj_chest.png"), "Body", "Jetpack", "LeftArm", "RightArm"));
        registerArmorModel(event, slot -> new ObjArmorModel(slot, obj, recipes),
                PoweredArmorItems.BJ_PLATE_JETPACK.get());
    }

    /**
     * The 5 standalone-wearable jetpack items ({@code com.hbm.items.gear.JetpackItems}) - CE's
     * hand-modeled {@code ModelJetPack} (see {@link JetpackWornModel}'s own class javadoc, including
     * why this registration currently has no live caller in-game: standalone jetpack wear itself is
     * blocked on a separate, already-named {@code Equippable} gap, not on anything in this class).
     */
    private static void registerJetpacks(RegisterClientExtensionsEvent event) {
        registerArmorModel(event, JetpackWornModel::new,
                JetpackItems.JETPACK_FLY.get(), JetpackItems.JETPACK_BREAK.get(),
                JetpackItems.JETPACK_VECTOR.get(), JetpackItems.JETPACK_BOOST.get(),
                JetpackItems.JETPACK_GLIDER.get());
    }

    private static ResourceLocation rl(String path) {
        return ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, path);
    }

    /**
     * Shared registration helper every {@code registerXyz} method above should go through: builds
     * <b>one</b> {@link IClientItemExtensions} instance shared across every {@code item} passed in
     * (confirmed real, compiling multi-item-one-instance shape: {@code upstream/neo-edition}'s
     * {@code ClientProxy.registerItemRenderer}, e.g. its 8-item {@code RenderBarrelItem}
     * registration), which lazily builds and caches one {@code modelFactory}-produced {@link
     * ArmorModelBase} per {@link EquipmentSlot} the first time that slot is asked for (a helmet's
     * model is never rebuilt on every frame, matching CE's own {@code if (model == null) this.model
     * = new ModelX();} caching idiom and Neo Edition's {@code ArmorNo9}'s identical {@code private
     * ModelNo9 replacement;} field), and calls {@link ArmorModelBase#getPropertiesFrom} to re-sync
     * live pose data every call before returning the cached instance.
     *
     * @param modelFactory builds a fresh {@link ArmorModelBase} for one {@link EquipmentSlot} - only
     *                      invoked once per distinct slot actually requested at runtime (i.e. only
     *                      for the slots {@code items} are actually equipped into).
     * @param items         every item that should share this one {@link IClientItemExtensions}
     *                      instance (typically a set's helmet/chestplate/leggings/boots, one entry
     *                      each - CE's own {@code cloneStats} sibling-item pattern groups pieces the
     *                      same way).
     */
    private static void registerArmorModel(RegisterClientExtensionsEvent event,
                                            Function<EquipmentSlot, ? extends ArmorModelBase> modelFactory,
                                            Item... items) {
        event.registerItem(new IClientItemExtensions() {
            private final Map<EquipmentSlot, ArmorModelBase> cache = new EnumMap<>(EquipmentSlot.class);

            @Override
            public Model getGenericArmorModel(LivingEntity livingEntity, ItemStack itemStack,
                                               EquipmentSlot slot, HumanoidModel<?> original) {
                ArmorModelBase replacement = cache.computeIfAbsent(slot, modelFactory);
                // No cast needed: HumanoidModel<T extends LivingEntity> means the unbounded
                // `original` parameter's type HumanoidModel<?> is already identical to
                // HumanoidModel<? extends LivingEntity> - confirmed by Neo Edition's own compiling
                // ArmorNo9.getGenericArmorModel passing `original` straight into a
                // HumanoidModel<? extends LivingEntity>-typed constructor parameter with zero cast.
                replacement.getPropertiesFrom(original, livingEntity);
                return replacement;
            }
        }, items);
    }
}
