package com.hbm.items.gear;

import com.hbm.config.PotionConfig;
import com.hbm.handler.ArmorUtil;
import com.hbm.items.armor.IArmorDisableModel;
import com.hbm.items.tool.ToolItems;
import com.hbm.lib.HBMSoundHandler;
import com.hbm.util.ArmorRegistry;
import com.hbm.util.ArmorRegistry.HazardClass;
import com.hbm.util.ContaminationUtil;
import com.hbm.util.Tuple;
import com.hbm.util.i18n.I18nUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Ported from CE's {@code com.hbm.items.gear.ArmorFSB} (540 lines) - the "full suit bonus" fluent
 * builder base every non-trivial CE armor set extends, directly or via {@code ArmorFSBPowered}/
 * {@code ArmorFSBFueled} ({@code com.hbm.items.armor}). "FSB" stands for <b>full suit bonus</b>, not
 * "force shield belt" - this class has no shield/damage-absorb logic of any kind. That is a
 * completely separate, already-ported mechanic living on
 * {@link com.hbm.capability.HbmPlayerAttachment#getShield()}/{@code #setShield(float)}, wired by
 * {@code com.hbm.handler.ArmorDamageHandler} (see that class).
 *
 * <p>What this class actually does, all confirmed from CE's real source and ported with no
 * behavior change except where explicitly noted:
 * <ul>
 *     <li>A fluent fluent-builder set of {@code set*}/{@code enable*}/{@code add*} methods a
 *     concrete leaf item's constructor chains to configure its full-set bonus.</li>
 *     <li>{@link #hasFSBArmor}/{@link #hasFSBArmorHelmet}/{@link #hasFSBArmorIgnoreCharge} - the
 *     "is this player wearing 4 matching, powered-on pieces of this exact material" check every
 *     other piece of this framework (potion tick, hazard/rad-resist checks, damage/attack dispatch,
 *     part-hiding) is built on.</li>
 *     <li>{@link #handleAttack}/{@link #handleHurt} - empty base hooks overridden by leaf classes,
 *     dispatched centrally by {@code com.hbm.handler.ArmorDamageHandler} from
 *     {@link LivingIncomingDamageEvent}/{@link LivingDamageEvent.Pre} respectively (the confirmed
 *     real 1.21.1 replacements for CE's {@code LivingAttackEvent}/{@code LivingHurtEvent} - see
 *     {@code docs/phase3/armor_equippable_framework.md} Key design decision #2).</li>
 *     <li>{@link #handleTick}/{@link #handleJump}/{@link #handleFall} - static dispatch hooks for
 *     the full-suit potion effects, jump sound, and hard-landing knockback+fall sound. <b>Not</b>
 *     wired to any NeoForge event by this package - {@code com.hbm.handler.ArmorDamageHandler}'s
 *     confirmed event-dispatch scope (per this package's task brief) covers only damage/attack, not
 *     per-tick/jump/fall player events; wiring these three belongs to whichever later package first
 *     needs a general player-tick/fall listener. The methods exist now so that wiring is a one-line
 *     call, matching CE's own shape exactly.</li>
 *     <li>{@link #disablesPart} ({@link IArmorDisableModel}) - body-part hiding for custom-modeled
 *     pieces, consumed by the (Phase 5) player render layer.</li>
 *     <li>{@link #setHazardClass} - self-registration into the already-ported
 *     {@link ArmorRegistry}.</li>
 *     <li>{@link #setRadResist} - stubbed with a documented forward-reference TODO: CE's real body
 *     registers into {@code com.hbm.handler.HazmatRegistry}, which does not exist anywhere in this
 *     port yet (a separate, not-yet-ported per-item radiation-resistance table - see this package's
 *     task brief item 2 and {@code docs/phase3/armor_equippable_framework.md} finding #4). The
 *     {@link #radResist} field itself is still stored so a future port of {@code HazmatRegistry} has
 *     the value to register.</li>
 * </ul>
 *
 * <p>{@link #onArmorTick}'s geiger-tick sound cue (Phase 4) is now wired against
 * {@link ContaminationUtil#getActualPlayerRads} - it only ever needed Phase 3's own
 * {@code HbmLivingProps}-tracked accumulated-dose value, not the chunk-radiation simulation itself
 * (unlike {@code ItemGeigerCounter}'s ambient click, this cue was never blocked on
 * {@code ChunkRadiationManager}).
 *
 * <p><b>Simplified relative to CE</b> (documented, not silently dropped): {@code handleTick}'s
 * footstep-sound cadence (CE's {@code steppy} helper, keyed off 1.12-only
 * {@code EntityLivingBase#nextStepDistance}/{@code #distanceWalkedOnStepModified} fields with no
 * confirmed 1.21 Mojang-mapped equivalent found in this pass) and its {@code the_NCR}/
 * {@code Barnaby99_x} cosmetic easter egg are left out; the mechanically important half of that
 * method (full-suit potion effects) is ported in full. {@link #handleFall}'s hard-landing knockback
 * uses {@code level.damageSources().playerAttack(player)} - CE's original bypasses the victim's
 * armor via {@code DamageSource#setDamageBypassesArmor()}, a per-instance flag with no 1.21
 * equivalent (bypass-armor is now a data-driven {@code DamageType} tag); this is a minor documented
 * behavior deviation (victims' armor now mitigates the knockback damage), not an invented mechanic.
 */
public class ArmorFSB extends ArmorItem implements IArmorDisableModel {

    /**
     * Kept as our own field (rather than relying on an inherited getter) so
     * {@link #hasFSBArmor}'s material-identity check does not depend on guessing
     * {@link ArmorItem}'s exact accessor name for its material holder - this port could not verify
     * that name against real compiled 1.21.1 classes in this sandbox (see this area's structured
     * report).
     */
    private final Holder<ArmorMaterial> materialHolder;

    public final List<MobEffectInstance> effects = new ArrayList<>();
    public boolean noHelmet = false;
    public boolean vats = false;
    public boolean thermal = false;
    public boolean geigerSound = false;
    public boolean customGeiger = false;
    public boolean hardLanding = false;
    public int dashCount = 0;
    public int stepSize = 0;
    public SoundEvent step;
    public SoundEvent jump;
    public SoundEvent fall;
    public double radResist = 0;

    private final Set<EnumPlayerPart> hidden = EnumSet.noneOf(EnumPlayerPart.class);
    private boolean needsFullSet = false;

    public ArmorFSB(Holder<ArmorMaterial> material, Type type, Item.Properties properties) {
        super(material, type, properties);
        this.materialHolder = material;
    }

    /** Maps this port's 4 {@link Type} constants onto the matching {@link EquipmentSlot}, without
     * relying on an unconfirmed inherited accessor - see {@link #materialHolder}'s javadoc for why. */
    private static EquipmentSlot slotForType(Type type) {
        return switch (type) {
            case HELMET -> EquipmentSlot.HEAD;
            case CHESTPLATE -> EquipmentSlot.CHEST;
            case LEGGINGS -> EquipmentSlot.LEGS;
            case BOOTS -> EquipmentSlot.FEET;
            default -> EquipmentSlot.CHEST;
        };
    }

    public static boolean hasFSBArmor(Player player) {
        if (player == null) return false;

        ItemStack plate = player.getItemBySlot(EquipmentSlot.CHEST);

        if (!plate.isEmpty() && plate.getItem() instanceof ArmorFSB chestplate) {

            boolean skipHelmet = chestplate.noHelmet;

            for (EquipmentSlot slot : EquipmentSlot.values()) {
                if (!slot.isArmor()) continue;
                if (skipHelmet && slot == EquipmentSlot.HEAD) continue;

                ItemStack armor = player.getItemBySlot(slot);

                if (armor.isEmpty() || !(armor.getItem() instanceof ArmorFSB armorFSB)) return false;
                if (armorFSB.materialHolder != chestplate.materialHolder) return false;
                if (!armorFSB.isArmorEnabled(armor)) return false;
            }
            return true;
        }

        return false;
    }

    public static boolean hasFSBArmorHelmet(Player player) {
        ItemStack plate = player.getItemBySlot(EquipmentSlot.CHEST);

        if (!plate.isEmpty() && plate.getItem() instanceof ArmorFSB chestplate) {
            return !chestplate.noHelmet && hasFSBArmor(player);
        }
        return false;
    }

    public static boolean hasFSBArmorIgnoreCharge(Player player) {
        if (player == null) return false;

        ItemStack plate = player.getItemBySlot(EquipmentSlot.CHEST);

        if (!plate.isEmpty() && plate.getItem() instanceof ArmorFSB chestplate) {
            boolean skipHelmet = chestplate.noHelmet;

            for (EquipmentSlot slot : EquipmentSlot.values()) {
                if (!slot.isArmor()) continue;
                if (skipHelmet && slot == EquipmentSlot.HEAD) continue;

                ItemStack armor = player.getItemBySlot(slot);

                if (armor.isEmpty() || !(armor.getItem() instanceof ArmorFSB armorFSB)) return false;
                if (armorFSB.materialHolder != chestplate.materialHolder) return false;
            }
            return true;
        }

        return false;
    }

    public ArmorFSB setHides(EnumPlayerPart... parts) {
        hidden.addAll(List.of(parts));
        return this;
    }

    public ArmorFSB setFullSetForHide() {
        needsFullSet = true;
        return this;
    }

    @Override
    public boolean disablesPart(Player player, ItemStack stack, EnumPlayerPart part) {
        return hidden.contains(part) && (!needsFullSet || hasFSBArmorIgnoreCharge(player));
    }

    /**
     * Dispatched by {@code com.hbm.handler.ArmorDamageHandler} from {@link LivingIncomingDamageEvent}
     * (CE: {@code ArmorFSB#handleAttack(LivingAttackEvent)}, called from
     * {@code ModEventHandler#onEntityAttacked}). No-op base; overridden by leaves. CE's
     * {@code LivingAttackEvent} does not exist under that name in real NeoForge 1.21.1 (confirmed
     * absent from the whole {@code neoforged/NeoForge} source tree) - {@code LivingIncomingDamageEvent}
     * is its actual, confirmed-real successor (same "cancel before any damage math runs" semantics).
     */
    public void handleAttack(LivingIncomingDamageEvent event) {
    }

    /**
     * Dispatched by {@code com.hbm.handler.ArmorDamageHandler} from {@link LivingDamageEvent.Pre}
     * (CE: {@code ArmorFSB#handleHurt(LivingHurtEvent)}, called from
     * {@code ModEventHandler#onEntityHurt}). No-op base; overridden by leaves.
     */
    public void handleHurt(LivingDamageEvent.Pre event) {
    }

    /**
     * CE: {@code ArmorFSB#handleTick(TickEvent.PlayerTickEvent)} - applies the full-suit potion
     * effect list every tick a matching set is worn. See the class javadoc's "Simplified relative
     * to CE" note for what is intentionally left out of this port (the footstep-sound cadence).
     * Not wired to any event by this package - see the class javadoc.
     */
    public static void handleTick(Player player) {
        if (!hasFSBArmor(player)) return;

        ItemStack plate = player.getItemBySlot(EquipmentSlot.CHEST);
        ArmorFSB chestplate = (ArmorFSB) plate.getItem();

        for (MobEffectInstance effect : chestplate.effects) {
            // Norwood (CE comment): no particles while the effect is armor-granted.
            player.addEffect(new MobEffectInstance(effect.getEffect(), effect.getDuration(), effect.getAmplifier(), effect.isAmbient(), false));
        }
    }

    /** CE: {@code ArmorFSB#handleJump(EntityPlayer)}. Not wired to any event by this package - see the class javadoc. */
    public static void handleJump(Player player) {
        if (hasFSBArmor(player)) {
            ArmorFSB chestplate = (ArmorFSB) player.getItemBySlot(EquipmentSlot.CHEST).getItem();

            if (chestplate.jump != null) {
                player.playSound(chestplate.jump, 1.0F, 1.0F);
            }
        }
    }

    /** CE: {@code ArmorFSB#handleFall(EntityPlayer)}. Not wired to any event by this package - see the class javadoc. */
    public static void handleFall(Player player) {
        if (hasFSBArmor(player)) {
            ArmorFSB chestplate = (ArmorFSB) player.getItemBySlot(EquipmentSlot.CHEST).getItem();

            if (chestplate.hardLanding && player.fallDistance > 10) {

                List<Entity> entities = player.level().getEntities(player, player.getBoundingBox().inflate(3, 0, 3));

                for (Entity e : entities) {
                    if (e instanceof ItemEntity) continue;

                    Vec3 vec = new Vec3(player.getX() - e.getX(), 0, player.getZ() - e.getZ());

                    if (vec.length() < 3) {
                        double intensity = 3 - vec.length();
                        e.setDeltaMovement(e.getDeltaMovement().add(vec.x * intensity * -2, 0.1D * intensity, vec.z * intensity * -2));
                        // CE bypasses the victim's armor here (DamageSource#setDamageBypassesArmor) -
                        // see class javadoc's "Simplified relative to CE" note.
                        e.hurt(player.level().damageSources().playerAttack(player), (float) (intensity * 10));
                    }
                }
            }

            if (chestplate.fall != null && player.fallDistance > 0.25) {
                player.playSound(chestplate.fall, 1.0F, 1.0F);
            }
        }
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);
        if (!(entity instanceof Player player)) return;
        if (player.getItemBySlot(slotForType(this.getType())) != stack) return;
        onArmorTick(level, player, stack);
    }

    /**
     * CE: {@code ArmorFSB#onArmorTick(World, EntityPlayer, ItemStack)}. Kept as its own overridable
     * method (rather than inlined into {@link #inventoryTick}) so {@code ArmorFSBPowered}/
     * {@code ArmorFSBFueled} can override just the per-tick body, matching CE's own override shape
     * exactly (CE's {@code ArmorFSBPowered.onArmorTick} does not call {@code super}, while
     * {@code ArmorFSBFueled.onArmorTick} does - both preserved as-is).
     */
    protected void onArmorTick(Level level, Player player, ItemStack stack) {
        if (this.getType() != Type.CHESTPLATE) return;
        if (!hasFSBArmor(player) || !this.geigerSound) return;
        if (carriesGeigerOrDosimeter(player)) return;
        if (level.getGameTime() % 5 != 0) return;

        double rads = ContaminationUtil.getActualPlayerRads(player);
        if (rads <= 1e-5) return;

        List<Integer> tiers = new ArrayList<>();
        if (rads < 1) tiers.add(0);
        if (rads < 5) tiers.add(0);
        if (rads < 10) tiers.add(1);
        if (rads > 5 && rads < 15) tiers.add(2);
        if (rads > 10 && rads < 20) tiers.add(3);
        if (rads > 15 && rads < 25) tiers.add(4);
        if (rads > 20 && rads < 30) tiers.add(5);
        if (rads > 25) tiers.add(6);

        int tier = tiers.get(level.getRandom().nextInt(tiers.size()));
        if (tier > 0) {
            SoundEvent[] geigerSounds = HBMSoundHandler.geigerSounds();
            level.playSound(null, player.getX(), player.getY(), player.getZ(), geigerSounds[tier - 1], SoundSource.PLAYERS, 1.0F, 1.0F);
        }
    }

    /**
     * CE: {@code InventoryUtil.hasItem(entity, ModItems.geiger_counter) ||
     * InventoryUtil.hasItem(entity, ModItems.dosimeter)} - skip the armor's own geiger cue while the
     * player is already carrying either detector item (which plays its own ambient click), checking
     * main inventory, armor, and offhand exactly as CE's {@code InventoryUtil.hasItem} does. No
     * standalone {@code InventoryUtil} class exists in this port (a separate, much larger CE utility
     * class not carried over wholesale) - reimplemented locally as the one method this call site needs.
     */
    private static boolean carriesGeigerOrDosimeter(Player player) {
        Item geiger = ToolItems.GEIGER_COUNTER.get();
        Item dosimeter = ToolItems.DOSIMETER.get();
        for (ItemStack stack : player.getInventory().items) {
            if (stack.getItem() == geiger || stack.getItem() == dosimeter) return true;
        }
        for (ItemStack stack : player.getInventory().armor) {
            if (stack.getItem() == geiger || stack.getItem() == dosimeter) return true;
        }
        for (ItemStack stack : player.getInventory().offhand) {
            if (stack.getItem() == geiger || stack.getItem() == dosimeter) return true;
        }
        return false;
    }

    public boolean isArmorEnabled(ItemStack stack) {
        return true;
    }

    public ArmorFSB enableThermalSight(boolean thermal) {
        this.thermal = thermal;
        return this;
    }

    public ArmorFSB setHasGeigerSound(boolean geiger) {
        this.geigerSound = geiger;
        return this;
    }

    public ArmorFSB setHasCustomGeiger(boolean geiger) {
        this.customGeiger = geiger;
        return this;
    }

    public ArmorFSB setHasHardLanding(boolean hardLanding) {
        this.hardLanding = hardLanding;
        return this;
    }

    public ArmorFSB setDashCount(int dashCount) {
        this.dashCount = dashCount;
        return this;
    }

    public ArmorFSB setStepSize(int stepSize) {
        this.stepSize = stepSize;
        return this;
    }

    public ArmorFSB setStep(SoundEvent step) {
        this.step = step;
        return this;
    }

    public ArmorFSB setJump(SoundEvent jump) {
        this.jump = jump;
        return this;
    }

    public ArmorFSB setFall(SoundEvent fall) {
        this.fall = fall;
        return this;
    }

    public ArmorFSB addEffect(MobEffectInstance effect) {
        // MobEffects.JUMP is Mojang's real (legacy-internal-name) field for the Jump Boost effect -
        // confirmed against this port's own already-established usage (items.food.ItemEnergy/
        // ItemPill/FoodItems), not the display-name-shaped MobEffects.JUMP_BOOST, which does not
        // exist under Mojang mappings.
        if (!PotionConfig.DO_JUMP_BOOST.get() && effect.getEffect() == MobEffects.JUMP) return this;
        effects.add(effect);
        return this;
    }

    public ArmorFSB setNoHelmet(boolean noHelmet) {
        this.noHelmet = noHelmet;
        return this;
    }

    public ArmorFSB enableVATS(boolean vats) {
        this.vats = vats;
        return this;
    }

    public ArmorFSB setHazardClass(HazardClass... classes) {
        ArmorUtil.external.add(new Tuple.Pair<>(this, classes));
        return this;
    }

    /**
     * CE: {@code ArmorFSB#setRadResist(double)}. TODO(HazmatRegistry): CE's real body also
     * registers {@code fullSet * HazmatRegistry.<slot>} into {@code HazmatRegistry.external}; that
     * class does not exist anywhere in this port yet (a separate, not-yet-ported per-item
     * radiation-resistance table - see class javadoc). {@link #radResist} is still stored so a
     * future port of that registry has the value.
     */
    public ArmorFSB setRadResist(double fullSet) {
        this.radResist = fullSet;
        return this;
    }

    public ArmorFSB cloneStats(ArmorFSB original) {
        // Lists aren't modified after instantiation, so there's no need to dereference.
        this.effects.addAll(original.effects);
        this.noHelmet = original.noHelmet;
        this.vats = original.vats;
        this.thermal = original.thermal;
        this.geigerSound = original.geigerSound;
        this.customGeiger = original.customGeiger;
        this.hardLanding = original.hardLanding;
        this.dashCount = original.dashCount;
        this.stepSize = original.stepSize;
        this.step = original.step;
        this.jump = original.jump;
        this.fall = original.fall;
        this.setRadResist(original.radResist);
        return this;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> components, TooltipFlag flag) {
        List<Component> toAdd = new ArrayList<>();

        if (!effects.isEmpty()) {
            List<String> names = new ArrayList<>();
            for (MobEffectInstance effect : effects) {
                names.add(Component.translatable(effect.getEffect().value().getDescriptionId()).getString());
            }
            toAdd.add(Component.literal(String.join(", ", names)).withStyle(ChatFormatting.AQUA));
        }

        if (geigerSound) toAdd.add(Component.literal("  " + I18nUtil.resolveKey("armor.geigerSound")).withStyle(ChatFormatting.GOLD));
        if (customGeiger) toAdd.add(Component.literal("  " + I18nUtil.resolveKey("armor.geigerHUD")).withStyle(ChatFormatting.GOLD));
        if (vats) toAdd.add(Component.literal("  " + I18nUtil.resolveKey("armor.vats")).withStyle(ChatFormatting.RED));
        if (thermal) toAdd.add(Component.literal("  " + I18nUtil.resolveKey("armor.thermal")).withStyle(ChatFormatting.RED));
        if (hardLanding) toAdd.add(Component.literal("  " + I18nUtil.resolveKey("armor.hardLanding")).withStyle(ChatFormatting.RED));
        if (stepSize != 0) toAdd.add(Component.literal("  " + I18nUtil.resolveKey("armor.stepSize", stepSize)).withStyle(ChatFormatting.BLUE));
        if (dashCount > 0) toAdd.add(Component.literal("  " + I18nUtil.resolveKey("armor.dash", dashCount)).withStyle(ChatFormatting.AQUA));

        if (!toAdd.isEmpty()) {
            components.add(Component.literal(I18nUtil.resolveKey("armor.fullSetBonus")).withStyle(ChatFormatting.GOLD));
            components.addAll(toAdd);
        }
    }
}
