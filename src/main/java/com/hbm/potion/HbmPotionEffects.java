package com.hbm.potion;

import com.hbm.main.MainRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * NeoForge {@link MobEffect} registry for CE's {@code com.hbm.potion.HbmPotion} - CE's single
 * {@code Potion} subclass with 12 {@code this == X} - dispatched singletons, split here into one
 * small {@link MobEffect} class per active effect (plus one shared no-op class for the 4
 * marker-only fields) per docs/phase4/hbm_potion_system.md's Key design/API decisions section.
 * <p>
 * {@code MobEffect} is a built-in registry exactly like {@code Item}/{@code Block}/
 * {@link net.minecraft.sounds.SoundEvent} (not a datapack registry like {@code DamageType}) -
 * confirmed twice independently: Neo Edition's own {@code com.hbm.lib.ModEffect} (a different
 * project, real compiling 1.21.1 NeoForge source) and this port's own already-merged
 * {@link com.hbm.lib.HBMSoundHandler} (a different registry, identical
 * {@code DeferredRegister.create(BuiltInRegistries.X, MainRegistry.MODID)} shape). Call
 * {@link #register(IEventBus)} once from {@link MainRegistry}'s constructor, following the exact
 * {@code HBMSoundHandler.register(modEventBus);} precedent already in that constructor's call list.
 * <p>
 * <b>Note (f1-sound-registry-dedup):</b> a previously-duplicate {@code com.hbm.sound.ModSounds}
 * {@code SoundEvent} registry (159 ids colliding with {@link com.hbm.lib.HBMSoundHandler}'s own)
 * has been deleted; {@link com.hbm.lib.HBMSoundHandler} is now the sole {@code SoundEvent} registry
 * in this port.
 * <p>
 * Every color literal below is CE's own real {@code preinit()} hex/decimal value (independently
 * decimal-to-hex verified in the research report) - <b>do not copy Neo Edition's own several wrong
 * hex values</b> (its {@code mutation}/{@code radx}/{@code radaway}/{@code phosphorus} colors all
 * diverge from CE's real ones). Categories follow CE's own strict binary {@code isBad} flag
 * ({@code true} -> {@link MobEffectCategory#HARMFUL}, {@code false} -> {@link
 * MobEffectCategory#BENEFICIAL}) - CE has no {@code NEUTRAL} equivalent, so none is used here even
 * though Neo Edition invents one for {@code potionsickness}. {@code death} is CE-flagged
 * {@code isBad=false} (a purely cosmetic joke effect, see {@link NoopEffect}'s javadoc) and is
 * therefore {@link MobEffectCategory#BENEFICIAL} here too, unlike Neo Edition's mismatched
 * {@code HARMFUL} mapping.
 */
public final class HbmPotionEffects {

    private HbmPotionEffects() {
    }

    public static final DeferredRegister<MobEffect> MOB_EFFECTS =
            DeferredRegister.create(BuiltInRegistries.MOB_EFFECT, MainRegistry.MODID);

    public static final DeferredHolder<MobEffect, TaintEffect> TAINT =
            MOB_EFFECTS.register("taint", () -> new TaintEffect(MobEffectCategory.HARMFUL, 0x800080));
    public static final DeferredHolder<MobEffect, RadiationEffect> RADIATION =
            MOB_EFFECTS.register("radiation", () -> new RadiationEffect(MobEffectCategory.HARMFUL, 0x84C128));
    public static final DeferredHolder<MobEffect, BangEffect> BANG =
            MOB_EFFECTS.register("bang", () -> new BangEffect(MobEffectCategory.HARMFUL, 0x111111));
    public static final DeferredHolder<MobEffect, NoopEffect> MUTATION =
            MOB_EFFECTS.register("mutation", () -> new NoopEffect(MobEffectCategory.BENEFICIAL, 0xFF8132));
    public static final DeferredHolder<MobEffect, NoopEffect> RADX =
            MOB_EFFECTS.register("radx", () -> new NoopEffect(MobEffectCategory.BENEFICIAL, 0x225900));
    public static final DeferredHolder<MobEffect, LeadEffect> LEAD =
            MOB_EFFECTS.register("lead", () -> new LeadEffect(MobEffectCategory.HARMFUL, 0x767682));
    public static final DeferredHolder<MobEffect, RadawayEffect> RADAWAY =
            MOB_EFFECTS.register("radaway", () -> new RadawayEffect(MobEffectCategory.BENEFICIAL, 0xFFE400));
    public static final DeferredHolder<MobEffect, TelekinesisEffect> TELEKINESIS =
            MOB_EFFECTS.register("telekinesis", () -> new TelekinesisEffect(MobEffectCategory.HARMFUL, 0x00F3FF));
    public static final DeferredHolder<MobEffect, PhosphorusEffect> PHOSPHORUS =
            MOB_EFFECTS.register("phosphorus", () -> new PhosphorusEffect(MobEffectCategory.HARMFUL, 0xFF3A00));
    public static final DeferredHolder<MobEffect, NoopEffect> STABILITY =
            MOB_EFFECTS.register("stability", () -> new NoopEffect(MobEffectCategory.BENEFICIAL, 0xD0D0D0));
    public static final DeferredHolder<MobEffect, PotionSicknessEffect> POTIONSICKNESS =
            MOB_EFFECTS.register("potionsickness", () -> new PotionSicknessEffect(MobEffectCategory.BENEFICIAL, 0xFF8080));
    public static final DeferredHolder<MobEffect, NoopEffect> DEATH =
            MOB_EFFECTS.register("death", () -> new NoopEffect(MobEffectCategory.BENEFICIAL, 0x111111));

    public static void register(IEventBus modEventBus) {
        MOB_EFFECTS.register(modEventBus);
    }

    /**
     * Single shared stand-in for CE's {@code CompatibilityConfig.isWarDim(World)}, which is not
     * ported in this port yet (keyed by CE's now-meaningless integer Forge dimension id - see
     * docs/phase4/hbm_potion_system.md's Deferred scope). Gates 3 of the 8 active effects here
     * ({@link TaintEffect}'s dropped trail-spread branch is additionally blocked on
     * {@code BlockTaint} not existing at all, see that class's own TODO; {@link BangEffect}'s lethal
     * branch; {@link PhosphorusEffect}'s fire-setting branch).
     * <p>
     * <b>Stubbed to {@code true}, not {@code false}</b> - CE's real default has
     * {@code peaceDimensionsIsWhitelist=true} with an <i>empty</i> {@code peaceDimensions} set, so
     * {@code isWarDim} evaluates to {@code true} for literally every dimension until a server
     * operator opts one out. This is the opposite of this port's usual "stub forward references to
     * the inert/no-op direction" convention - stubbing {@code false} here would silently disable
     * real CE-default behavior (taint trails/the bang lethal payload/phosphorus fire) in every
     * world, not just newly-configured ones. Kept as a single package-visible method (rather than a
     * private helper duplicated in each of the 3 call sites) so there is exactly one place to swap
     * in the real {@code CompatibilityConfig.isWarDim} call once that config lands.
     */
    static boolean isWarDim(Level level) {
        return true;
    }
}
