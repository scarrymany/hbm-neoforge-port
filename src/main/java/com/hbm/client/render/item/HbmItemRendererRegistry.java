package com.hbm.client.render.item;

import com.hbm.main.MainRegistry;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

/**
 * The client-side wiring point that makes an {@link HbmItemBEWLR} subclass actually draw for a
 * given {@link Item} - registers an {@link IClientItemExtensions} instance per item via
 * {@link RegisterClientExtensionsEvent#registerItem(IClientItemExtensions, Item...)}, the
 * confirmed-real 1.21.1 replacement for CE's {@code item.setTileEntityItemStackRenderer(...)}
 * binding call ({@code upstream/hbm-ce/.../main/client/NTMClientRegistry.java:93-107}, per
 * {@code docs/phase5/renderer_framework_and_obj_models.md}'s "Item-in-hand/GUI rendering parity"
 * section).
 *
 * <h2>Why a separate wrapper object instead of {@link HbmItemBEWLR} implementing
 * {@code IClientItemExtensions} itself</h2>
 * A {@link BlockEntityWithoutLevelRenderer} <i>could</i> implement {@code IClientItemExtensions}
 * directly (nothing stops one class implementing both) and return {@code this} from
 * {@code getCustomRenderer()} - but that is <b>not</b> the shape confirmed by
 * {@code upstream/neo-edition}'s own real, compiling registration code
 * ({@code com.hbm.items.weapon.sedna.factory.GunFactoryClient.registerGunItemRenderer}, read in
 * full): it keeps the renderer object (extends {@code ItemRenderWeaponBase}) and the
 * registration-hook object (an anonymous {@code IClientItemExtensions}) separate, with the
 * anonymous object's three overrides ({@code getCustomRenderer}/{@code applyForgeHandTransform}/
 * {@code getArmPose}) all just forwarding onto the one shared renderer instance. This class keeps
 * that exact separation - {@link #register} below builds the same shape of anonymous wrapper,
 * forwarding onto {@link HbmItemBEWLR#applyForgeHandTransform}/{@link HbmItemBEWLR#getArmPose}/
 * {@link HbmItemBEWLR#captureHoldingEntity} - rather than having the framework's own base class
 * additionally implement a second, unrelated interface. Practically equivalent either way; this
 * shape was chosen to stay a closer, more literal mirror of the one real confirmed 1.21.1
 * reference implementation available, on the theory that a future reader diffing this port
 * against that reference benefits from the structural parallel more than from the one-line
 * simplification the alternative shape would have saved.
 *
 * <h2>Registration entry point for future gun renderers (Content-wave task {@code c6})</h2>
 * This class's own {@link #onRegisterClientExtensions} handler is deliberately left with an empty
 * body at this task's scope (f9, framework-only - no concrete gun renderers exist yet to
 * register). Per this port's ground rules ("NeoForge allows multiple listeners for the same
 * event"), whichever future package writes concrete gun renderers should add its <i>own</i>,
 * separately-named {@code @EventBusSubscriber}/{@code @SubscribeEvent} class subscribing to the
 * same {@link RegisterClientExtensionsEvent} (mirroring Neo Edition's own
 * {@code GunFactoryClient.init(RegisterClientExtensionsEvent)} entry point, itself called from
 * client setup - see that class for the confirmed per-gun call shape,
 * {@code registerGunItemRenderer(event, new ItemRenderSPAS12(), NtmItems.GUN_SPAS12.get())} one
 * line per gun) and call {@link #register} once per gun item from inside it - there is no need to
 * add anything to <i>this</i> file, and per this port's own ground rules this file should not be
 * directly edited by a different task's agent anyway (it is this task's own new file, not one of
 * the prohibited shared aggregator files, but the same "add your own class instead of editing
 * someone else's" spirit applies for exactly the reason stated in this task's own instructions).
 * See {@link ExamplePlaceholderBEWLR} in this package for a minimal, compiling reference showing
 * the whole pipeline's types line up end-to-end (not itself registered anywhere - see that
 * class's own javadoc for why).
 *
 * <h2>Event bus - a real discrepancy against this port's own ground rules, flagged rather than
 * silently resolved either way</h2>
 * This port's ground rules state {@code RegisterClientExtensionsEvent} is "same event family as
 * [armor rendering], bus=MOD" and this class follows that instruction below
 * ({@code bus = EventBusSubscriber.Bus.MOD}). <b>However</b>, direct inspection of
 * {@code upstream/neo-edition}'s own real, compiling registration code found it declared on the
 * <i>default</i> bus instead: {@code NuclearTechModClient} (which contains a real
 * {@code @SubscribeEvent public static void onRegisterClientExtensions(RegisterClientExtensionsEvent event)}
 * method, alongside a real {@code @SubscribeEvent public static void onClientSetup(FMLClientSetupEvent event)}
 * method in the very same class) is annotated only {@code @EventBusSubscriber(value = Dist.CLIENT)}
 * - no {@code bus =} parameter anywhere, confirmed by a whole-repo grep
 * (@{@code grep -rln "bus\s*=\s*EventBusSubscriber.Bus.MOD"} across all of
 * {@code upstream/neo-edition/src/main/java} returns zero matches, in any file). Also notable: its
 * own {@code onClientSetup} body separately, manually constructs a <i>second</i>
 * {@code RegisterClientExtensionsEvent} instance via a {@code @Mixin(RegisterClientExtensionsEvent.class)}
 * {@code @Invoker("<init>")} accessor and calls {@code GunFactoryClient.init(...)} against that
 * manually-built instance directly, bypassing the framework's own event dispatch entirely for the
 * actual gun registrations - a strong hint that whoever wrote Neo Edition's client bootstrap
 * either hit the exact "silent no-op without explicit {@code bus=MOD}" bug this port's own ground
 * rules name (and worked around it with a manual-construction hack rather than fixing the
 * annotation), or had some other reason not stated in the code to avoid relying on the event's
 * normal firing. Per this task's ground rules (Neo Edition is an API-shape reference only, "has
 * known bugs and incompleteness of its own - do not blindly copy it"), this class follows this
 * port's own hard-won, already-proven-correct convention (explicit {@code bus = Bus.MOD}, per the
 * real fixed bug in this port's own commit history breaking "packets, GUIs, keybinds, datagen
 * mod-wide") rather than copying Neo Edition's un-annotated, likely-non-functional shape. Flagging
 * this explicitly for whoever first gets a real launchable client in this sandbox to verify -
 * this class's own {@code bus=MOD} choice could not be confirmed against a running game either.
 */
@EventBusSubscriber(modid = MainRegistry.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class HbmItemRendererRegistry {

    private HbmItemRendererRegistry() {
    }

    /**
     * Registers {@code renderer} as the custom item renderer for every item in {@code items},
     * wiring {@code getCustomRenderer}/{@code applyForgeHandTransform}/{@code getArmPose} to
     * forward onto {@code renderer}'s own overridable methods of the same shape. Call this once
     * per gun/OBJ-modeled item from inside your own {@code @SubscribeEvent} handler for
     * {@link RegisterClientExtensionsEvent} - see this class's own javadoc for why the handler
     * lives in your own class, not this one.
     *
     * @param event    the live event this must be called from inside (the SDK's own contract -
     *                 {@code registerItem} is only valid during this event's dispatch).
     * @param renderer one renderer instance, reused for every item passed here and for every
     *                 stack of every one of them - matches CE's/Neo Edition's own "one renderer
     *                 instance per gun type" convention (per
     *                 {@code docs/phase5/weapon_gun_rendering_animloader.md}'s confirmed API
     *                 shapes: "one instance per gun type, reused").
     * @param items    one or more {@link Item}s that should render via {@code renderer}.
     */
    public static void register(RegisterClientExtensionsEvent event, HbmItemBEWLR renderer, Item... items) {
        event.registerItem(new IClientItemExtensions() {

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return renderer;
            }

            @Override
            public boolean applyForgeHandTransform(PoseStack poseStack, LocalPlayer player, HumanoidArm arm,
                                                     ItemStack itemInHand, float partialTick, float equipProcess,
                                                     float swingProcess) {
                return renderer.applyForgeHandTransform(poseStack, player, arm, itemInHand, partialTick,
                        equipProcess, swingProcess);
            }

            @Override
            public HumanoidModel.ArmPose getArmPose(LivingEntity living, InteractionHand hand, ItemStack itemStack) {
                renderer.captureHoldingEntity(living);
                HumanoidModel.ArmPose pose = renderer.getArmPose(living, hand, itemStack);
                return pose != null ? pose : IClientItemExtensions.super.getArmPose(living, hand, itemStack);
            }
        }, items);
    }

    /**
     * Intentionally empty at this task's scope - see this class's own javadoc, "Registration
     * entry point for future gun renderers". Kept as a real, present, correctly-bus-annotated
     * {@code @SubscribeEvent} method (rather than omitted entirely) so this class demonstrably
     * compiles against the real event type and so a future reader has one obvious, confirmed-
     * working example of the annotation shape to copy into their own sibling class.
     */
    @SubscribeEvent
    public static void onRegisterClientExtensions(RegisterClientExtensionsEvent event) {
        // No concrete OBJ-model item renderers exist yet to register - see ExamplePlaceholderBEWLR
        // in this package for a compiling reference implementation, deliberately NOT registered
        // here (its placeholder .obj/.png resources do not exist on disk - see that class's own
        // javadoc for why registering it for real would be unsafe).
    }
}
