package com.hbm.util;

import com.hbm.capability.HbmLivingAttachment;
import com.hbm.capability.HbmLivingProps;
import com.hbm.config.CompatibilityConfig;
import com.hbm.config.GeneralConfig;
import com.hbm.damage.ModDamageTypes;
import com.hbm.entity.mob.EntityQuackos;
import com.hbm.handler.ArmorUtil;
import com.hbm.handler.HazmatRegistry;
import com.hbm.handler.radiation.ChunkRadiationManager;
import com.hbm.hazard.HazardSystem;
import com.hbm.hazard.type.HazardTypeRadiation;
import com.hbm.interfaces.IRadiationImmune;
import com.hbm.items.HbmDataComponents;
import com.hbm.items.gear.ArmorEuphemium;
import com.hbm.lib.Library;
import com.hbm.potion.HbmPotionEffects;
import com.hbm.util.ArmorRegistry.HazardClass;
import com.hbm.util.i18n.I18nUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.MushroomCow;
import net.minecraft.world.entity.animal.Ocelot;
import net.minecraft.world.entity.animal.horse.SkeletonHorse;
import net.minecraft.world.entity.animal.horse.ZombieHorse;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.decoration.HangingEntity;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Direct port of CE's {@code com.hbm.util.ContaminationUtil} against {@link HbmLivingProps}/
 * {@link HbmLivingAttachment}. Already a live, uncommented dependency of
 * {@code hazard.type.HazardTypeRadiation} ({@link #contaminate}) and
 * {@code hazard.type.HazardTypeDigamma} ({@link #applyDigammaData}).
 *
 * <p>{@link HazardType}/{@link ContaminationType} are ported verbatim as nested enums (matching
 * CE's nesting exactly, since both are already imported by name as
 * {@code ContaminationUtil.HazardType}/{@code ContaminationUtil.ContaminationType} from the two
 * hazard-type call sites above).
 */
public final class ContaminationUtil {

    private ContaminationUtil() {
    }

    /**
     * Calculates how much radiation can be applied to this entity by way of resistance.
     *
     * <p>CE returns {@code 0D} immediately when {@code entity.isPotionActive(HbmPotion.mutation)};
     * this port's {@link com.hbm.potion.HbmPotionEffects#MUTATION} equivalent.
     *
     * <p>CE also reads a per-entity {@code "hbmradmultiplier"} scratch float off the entity's own
     * synced data ({@code EntityLivingBase#getEntityData()}) here as an extra multiplier; nothing
     * in this port (or, as far as this port's sources show, in CE itself) ever writes that key, so
     * that branch is a dead no-op in practice and is not reproduced.
     */
    public static double calculateRadiationMod(LivingEntity entity) {
        if (entity.hasEffect(HbmPotionEffects.MUTATION)) return 0D;

        double koeff = 10.0D;

        double hazmatResistance = HazmatRegistry.getResistance(entity);

        return Math.pow(koeff, -(getConfigEntityRadResistance(entity) + hazmatResistance));
    }

    public static void printGeigerData(Player player) {
        double rawRadMod = calculateRadiationMod(player);
        double eRad = HbmLivingProps.getRadiation(player);
        // CE: ChunkRadiationManager.proxy.getRadiation(player.world, player.getPosition()) - ambient
        // chunk radiation at the player's own position (Phase 4, com.hbm.handler.radiation).
        double rads = ChunkRadiationManager.proxy.getRadiation(player.level(), player.blockPosition());
        double env = getPlayerRads(player);
        double res = (1.0D - rawRadMod) * 100.0D;
        double resKoeff = HazmatRegistry.getResistance(player) * 100.0D;
        double rec = env * rawRadMod;

        String eRadS = formatMagnitude(eRad, 3);
        String radsS = formatMagnitude(rads, 3);
        String envS = formatMagnitude(env, 3);
        String recS = formatMagnitude(rec, 3);
        String resS = formatMagnitude(res, 6);
        String resKoeffS = formatMagnitude(resKoeff, 2);

        ChatFormatting chunkPrefix = getPrefixFromRad(rads);
        ChatFormatting envPrefix = getPrefixFromRad(env);
        ChatFormatting recPrefix = getPrefixFromRad(rec);

        ChatFormatting radPrefix;
        if (eRad < 200) radPrefix = ChatFormatting.GREEN;
        else if (eRad < 400) radPrefix = ChatFormatting.YELLOW;
        else if (eRad < 600) radPrefix = ChatFormatting.GOLD;
        else if (eRad < 800) radPrefix = ChatFormatting.RED;
        else if (eRad < 1000) radPrefix = ChatFormatting.DARK_RED;
        else radPrefix = ChatFormatting.DARK_GRAY;

        ChatFormatting resPrefix = resKoeff > 0 ? ChatFormatting.GREEN : ChatFormatting.WHITE;

        player.sendSystemMessage(Component.literal("===== ☢ ")
                .append(Component.translatable("geiger.title"))
                .append(Component.literal(" ☢ ====="))
                .withStyle(ChatFormatting.GOLD));
        player.sendSystemMessage(Component.translatable("geiger.chunkRad")
                .append(Component.literal(" " + radsS + " RAD/s").withStyle(chunkPrefix))
                .withStyle(ChatFormatting.YELLOW));
        player.sendSystemMessage(Component.translatable("geiger.envRad")
                .append(Component.literal(" " + envS + " RAD/s").withStyle(envPrefix))
                .withStyle(ChatFormatting.YELLOW));
        player.sendSystemMessage(Component.translatable("geiger.recievedRad")
                .append(Component.literal(" " + recS + " RAD/s").withStyle(recPrefix))
                .withStyle(ChatFormatting.YELLOW));
        player.sendSystemMessage(Component.translatable("geiger.playerRad")
                .append(Component.literal(" " + eRadS + " RAD").withStyle(radPrefix))
                .withStyle(ChatFormatting.YELLOW));
        player.sendSystemMessage(Component.translatable("geiger.playerRes")
                .append(Component.literal(" " + resS + "% (" + resKoeffS + ")").withStyle(resPrefix))
                .withStyle(ChatFormatting.YELLOW));
    }

    public static void printDosimeterData(Player player) {
        double rads = getActualPlayerRads(player);
        boolean limit = false;

        if (rads > 3.6D) {
            rads = 3.6D;
            limit = true;
        }
        rads = ((int) (1000D * rads)) / 1000D;
        ChatFormatting radsPrefix = getPrefixFromRad(rads);

        player.sendSystemMessage(Component.literal("===== ☢ ")
                .append(Component.translatable("dosimeter.title"))
                .append(Component.literal(" ☢ ====="))
                .withStyle(ChatFormatting.GOLD));
        player.sendSystemMessage(Component.translatable("geiger.recievedRad")
                .append(Component.literal(" " + (limit ? ">" : "") + rads + " RAD/s").withStyle(radsPrefix))
                .withStyle(ChatFormatting.YELLOW));
    }

    public static void printDiagnosticData(Player player) {
        double digamma = ((int) (HbmLivingProps.getDigamma(player) * 1000)) / 1000D;
        double halflife = ((int) ((1D - Math.pow(0.5, digamma)) * 10000)) / 100D;

        player.sendSystemMessage(Component.literal("===== Ϝ ")
                .append(Component.translatable("digamma.title"))
                .append(Component.literal(" Ϝ ====="))
                .withStyle(ChatFormatting.DARK_PURPLE));
        player.sendSystemMessage(Component.translatable("digamma.playerDigamma")
                .append(Component.literal(" " + digamma + " DRX").withStyle(ChatFormatting.RED))
                .withStyle(ChatFormatting.LIGHT_PURPLE));
        player.sendSystemMessage(Component.translatable("digamma.playerHealth")
                .append(Component.literal(String.format(" %6.2f", halflife) + "%").withStyle(getTextColorFromPercent(halflife / 100D)))
                .withStyle(ChatFormatting.LIGHT_PURPLE));
    }

    public static void printLungDiagnosticData(Player player) {
        float playerAsbestos = 100F - ((int) (10000F * HbmLivingProps.getAsbestos(player) / HbmLivingAttachment.MAX_ASBESTOS)) / 100F;
        float playerBlacklung = 100F - ((int) (10000F * HbmLivingProps.getBlackLung(player) / HbmLivingAttachment.MAX_BLACKLUNG)) / 100F;
        float playerTotal = playerAsbestos * playerBlacklung / 100F;
        int contagion = HbmLivingProps.getContagion(player);

        player.sendSystemMessage(Component.literal("===== L ")
                .append(Component.translatable("lung_scanner.title"))
                .append(Component.literal(" L ====="))
                .withStyle(ChatFormatting.WHITE));
        player.sendSystemMessage(Component.translatable("lung_scanner.player_asbestos_health")
                .append(Component.literal(String.format(" %6.2f", playerAsbestos) + " %").withStyle(getTextColorLung(playerAsbestos / 100D)))
                .withStyle(ChatFormatting.WHITE));
        player.sendSystemMessage(Component.translatable("lung_scanner.player_coal_health")
                .append(Component.literal(String.format(" %6.2f", playerBlacklung) + " %").withStyle(getTextColorLung(playerBlacklung / 100D)))
                .withStyle(ChatFormatting.DARK_GRAY));
        player.sendSystemMessage(Component.translatable("lung_scanner.player_total_health")
                .append(Component.literal(String.format(" %6.2f", playerTotal) + " %").withStyle(getTextColorLung(playerTotal / 100D)))
                .withStyle(ChatFormatting.GRAY));
        player.sendSystemMessage(Component.translatable("lung_scanner.player_mku")
                .append(Component.translatable(contagion > 0 ? "lung_scanner.pos" : "lung_scanner.neg"))
                .withStyle(ChatFormatting.GRAY));
        if (contagion > 0) {
            player.sendSystemMessage(Component.translatable("lung_scanner.player_mku_duration")
                    .append(Component.literal(" " + ticksToDateString(contagion, 72000)).withStyle(ChatFormatting.RED))
                    .withStyle(ChatFormatting.GRAY));
        }
    }

    public static double getActualPlayerRads(LivingEntity entity) {
        return getPlayerRads(entity) * calculateRadiationMod(entity);
    }

    public static double getPlayerRads(LivingEntity entity) {
        double rads = HbmLivingProps.getRadBuf(entity);
        if (entity instanceof Player) rads = rads + HbmLivingProps.getNeutron(entity) * 20;
        return rads;
    }

    public static double getNoNeutronPlayerRads(LivingEntity entity) {
        return HbmLivingProps.getRadBuf(entity) * calculateRadiationMod(entity);
    }

    public static boolean isRadItem(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        return HazardSystem.getRawRadsFromStack(stack) > 0;
    }

    public static float getNeutronRads(ItemStack stack) {
        if (stack != null && !stack.isEmpty() && !isRadItem(stack)) {
            Float activation = stack.get(HbmDataComponents.NEUTRON_ACTIVATION.get());
            if (activation != null) {
                return activation * stack.getCount();
            }
        }
        return 0F;
    }

    /** CE's {@code player}/{@code flagIn} parameters are unused in the original method too. */
    public static void addNeutronRadInfo(ItemStack stack, Player player, List<Component> list, TooltipFlag flagIn) {
        if (HazardSystem.getRawRadsFromStack(stack) > 0) return;

        float activationRads = getNeutronRads(stack);
        if (activationRads > 0) {
            list.add(Component.literal("[" + I18nUtil.resolveKey("trait.radioactive") + "]").withStyle(ChatFormatting.GREEN));
            float stackRad = activationRads / stack.getCount();
            list.add(Component.literal(" " + Library.roundFloat((float) HazardTypeRadiation.getNewValue(stackRad), 3)
                    + HazardTypeRadiation.getSuffix(stackRad) + " RAD/s").withStyle(ChatFormatting.YELLOW));

            if (stack.getCount() > 1) {
                list.add(Component.literal(" " + I18nUtil.resolveKey("desc.stack") + " "
                        + Library.roundFloat((float) HazardTypeRadiation.getNewValue(activationRads), 3)
                        + HazardTypeRadiation.getSuffix(activationRads) + " RAD/s").withStyle(ChatFormatting.YELLOW));
            }
        }
    }

    public static boolean neutronActivateInventory(Player player, float rad, float decay) {
        boolean changed = false;
        List<ItemStack> main = player.getInventory().items;
        int selected = player.getInventory().selected;

        for (int slotI = 0; slotI < main.size(); slotI++) {
            if (slotI != selected && neutronActivateItem(main.get(slotI), rad, decay)) {
                changed = true;
            }
        }

        for (ItemStack armorStack : player.getInventory().armor) {
            if (neutronActivateItem(armorStack, rad, decay)) {
                changed = true;
            }
        }

        return changed;
    }

    public static boolean neutronActivateItem(ItemStack stack, float rad, float decay) {
        if (stack == null || stack.isEmpty() || stack.getCount() != 1 || isRadItem(stack)) return false;

        float prevActivation = stack.getOrDefault(HbmDataComponents.NEUTRON_ACTIVATION.get(), 0F);
        float newActivation = prevActivation * decay + (rad / stack.getCount());

        if (newActivation < 0.0001F) {
            if (prevActivation > 0) {
                stack.remove(HbmDataComponents.NEUTRON_ACTIVATION.get());
                return true;
            }
        } else if (Math.abs(newActivation - prevActivation) > 1e-6) {
            stack.set(HbmDataComponents.NEUTRON_ACTIVATION.get(), newActivation);
            return true;
        }
        return false;
    }

    public static boolean isContaminated(ItemStack stack) {
        return stack.has(HbmDataComponents.NEUTRON_ACTIVATION.get());
    }

    public static ChatFormatting getPrefixFromRad(double rads) {
        if (rads == 0) return ChatFormatting.GREEN;
        if (rads < 1) return ChatFormatting.YELLOW;
        if (rads < 10) return ChatFormatting.GOLD;
        if (rads < 100) return ChatFormatting.RED;
        if (rads < 1000) return ChatFormatting.DARK_RED;
        return ChatFormatting.DARK_GRAY;
    }

    public static ChatFormatting getTextColorFromPercent(double percent) {
        if (percent < 0.5) return ChatFormatting.GREEN;
        if (percent < 0.6) return ChatFormatting.YELLOW;
        if (percent < 0.7) return ChatFormatting.GOLD;
        if (percent < 0.8) return ChatFormatting.RED;
        if (percent < 0.9) return ChatFormatting.DARK_RED;
        return ChatFormatting.DARK_GRAY;
    }

    public static ChatFormatting getTextColorLung(double percent) {
        if (percent > 0.9) return ChatFormatting.GREEN;
        if (percent > 0.75) return ChatFormatting.YELLOW;
        if (percent > 0.5) return ChatFormatting.GOLD;
        if (percent > 0.25) return ChatFormatting.RED;
        if (percent > 0.1) return ChatFormatting.DARK_RED;
        return ChatFormatting.DARK_GRAY;
    }

    public static double getRads(Entity e) {
        if (e instanceof IRadiationImmune) return 0.0D;
        if (e instanceof LivingEntity living) return HbmLivingProps.getRadiation(living);
        return 0.0D;
    }

    public static float getConfigEntityRadResistance(Entity e) {
        float totalResistanceValue = 0.0F;
        if (!(e instanceof Player)) {
            ResourceLocation entityPath = BuiltInRegistries.ENTITY_TYPE.getKey(e.getType());
            if (entityPath != null) {
                Float resistanceMod = CompatibilityConfig.mobModRadresistance().get(entityPath.getNamespace());
                Float resistanceMob = CompatibilityConfig.mobRadresistance().get(entityPath.toString());
                if (resistanceMod != null) totalResistanceValue += resistanceMod;
                if (resistanceMob != null) totalResistanceValue += resistanceMob;
            }
        }
        return totalResistanceValue;
    }

    public static boolean checkConfigEntityImmunity(Entity e) {
        if (!(e instanceof Player)) {
            ResourceLocation entityPath = BuiltInRegistries.ENTITY_TYPE.getKey(e.getType());
            if (entityPath != null) {
                if (CompatibilityConfig.mobModRadimmune().contains(entityPath.getNamespace())) {
                    return true;
                }
                return CompatibilityConfig.mobRadimmune().contains(entityPath.toString());
            }
        }
        return false;
    }

    /**
     * CE's hardcoded {@code immuneEntities} class array mixed vanilla mobs with two HBM-custom
     * ones ({@code EntityCreeperNuclear}, {@code EntityQuackos}) - ported with the vanilla classes
     * plus the {@link IRadiationImmune} extensibility-point interface (Phase 0's whole reason to
     * define it rather than hardcode a list). {@code EntityCreeperNuclear} (Phase 4,
     * {@code docs/phase4/entities_creeper_variants.md}) now {@code implements IRadiationImmune}
     * directly - a deliberate, behavior-preserving departure from CE's own hardcoded-array shape,
     * recommended by that area's research report - so the {@code instanceof IRadiationImmune} check
     * below already covers it with no further change needed here. {@link EntityQuackos} (Phase 4,
     * {@code docs/phase4/entities_bosses.md}) is now real too - added below as an explicit
     * {@code instanceof} check (matching CE's own hardcoded-array treatment for this one mob, not the
     * {@link IRadiationImmune} interface {@code EntityCreeperNuclear} uses, since CE itself never gave
     * {@code EntityQuackos} that marker interface either).
     */
    public static boolean isRadImmune(Entity e) {
        if (e instanceof LivingEntity living && living.hasEffect(HbmPotionEffects.MUTATION)) {
            return true;
        }

        if (e instanceof Zombie || e instanceof Skeleton || e instanceof MushroomCow
                || e instanceof Ocelot || e instanceof IRadiationImmune
                || e instanceof ZombieHorse || e instanceof SkeletonHorse || e instanceof ArmorStand
                || e instanceof EntityQuackos) {
            return true;
        }

        return checkConfigEntityImmunity(e);
    }

    /// ASBESTOS ///

    public static void applyAsbestos(Entity e, int i, int dmg) {
        applyAsbestos(e, i, dmg, 1);
    }

    public static void applyAsbestos(Entity e, int i, int dmg, int chance) {
        if (!GeneralConfig.ENABLE_ASBESTOS_DUST.get()) return;
        if (!(e instanceof LivingEntity entity)) return;
        if (entity instanceof Player player && (player.isCreative() || player.isSpectator())) return;
        if (e instanceof Player && e.tickCount < 200) return;

        if (ArmorRegistry.hasProtection(entity, EquipmentSlot.HEAD, HazardClass.PARTICLE_FINE)) {
            if (chance > 1) {
                if (entity.getRandom().nextInt(chance) == 0) {
                    ArmorUtil.damageGasMaskFilter(entity, 1);
                }
            } else {
                ArmorUtil.damageGasMaskFilter(entity, dmg);
            }
        } else {
            HbmLivingProps.incrementAsbestos(entity, i);
        }
    }

    /// COAL ///

    public static void applyCoal(Entity e, int i, int dmg, int chance) {
        if (!GeneralConfig.ENABLE_COAL_DUST.get()) return;
        if (!(e instanceof LivingEntity entity)) return;
        if (entity instanceof Player player && (player.isCreative() || player.isSpectator())) return;
        if (e instanceof Player && e.tickCount < 200) return;

        if (ArmorRegistry.hasProtection(entity, EquipmentSlot.HEAD, HazardClass.PARTICLE_COARSE)) {
            if (chance > 1) {
                if (entity.getRandom().nextInt(chance) == 0) {
                    ArmorUtil.damageGasMaskFilter(entity, 1);
                }
            } else {
                ArmorUtil.damageGasMaskFilter(entity, dmg);
            }
        } else {
            HbmLivingProps.incrementBlackLung(entity, i);
        }
    }

    /// DIGAMMA ///

    public static void applyDigammaData(Entity e, double f) {
        if (!(e instanceof LivingEntity entity)) return;

        if (e instanceof Ocelot) return;
        if (e instanceof EntityQuackos) return;

        if (entity instanceof Player player && (player.isCreative() || player.isSpectator())) return;
        if (e instanceof Player && e.tickCount < 200) return;

        if (entity.hasEffect(HbmPotionEffects.STABILITY)) return;

        if (!(entity instanceof Player digammaPlayer && ArmorUtil.checkForDigamma(digammaPlayer))) {
            HbmLivingProps.incrementDigamma(entity, f);
        }
    }

    public static double getDigamma(Entity e) {
        if (!(e instanceof LivingEntity entity)) return 0.0D;
        return HbmLivingProps.getDigamma(entity);
    }

    public static void radiate(Level level, double x, double y, double z, double range, float rad3d) {
        radiate(level, x, y, z, range, rad3d, 0, 0, 0, 0);
    }

    public static void radiate(Level level, double x, double y, double z, double range, float rad3d, float dig3d, float fire3d) {
        radiate(level, x, y, z, range, rad3d, dig3d, fire3d, 0, 0);
    }

    public static void radiate(Level level, double x, double y, double z, double range, float rad3d, float dig3d, float fire3d, float blast3d) {
        radiate(level, x, y, z, range, rad3d, dig3d, fire3d, blast3d, range);
    }

    /**
     * The 3D radiation/digamma/fire/blast AoE helper. Mechanically portable now (only needs
     * {@link LivingEntity}, {@link ModDamageTypes#NUCLEAR_BLAST}/{@link ModDamageTypes#BLAST} -
     * both already registered - and {@link #contaminate}), but its real callers are Phase 3
     * nuclear-explosion content that doesn't exist yet ({@code EntityNukeExplosionMK5},
     * {@code EntityMIRV}, grenade fillings, ...) - see this area's research report's Deferred
     * scope. Ported now regardless since the method body itself has no missing dependency.
     */
    public static void radiate(Level level, double x, double y, double z, double range, float rad3d, float dig3d, float fire3d, float blast3d, double blastRange) {
        List<Entity> entities = level.getEntitiesOfClass(Entity.class,
                new AABB(x - range, y - range, z - range, x + range, y + range, z + range));
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (Entity e : entities) {
            if (isExplosionExempt(e)) continue;

            Vec3 vec = new Vec3(e.getX() - x, (e.getY() + e.getEyeHeight()) - y, e.getZ() - z);
            double len = vec.length();

            if (len > range) continue;
            vec = vec.normalize();
            double dmgLen = Math.max(len, range * 0.05D);

            float res = 0;

            for (int i = 1; i < len; i++) {
                int ix = (int) Math.floor(x + vec.x * i);
                int iy = (int) Math.floor(y + vec.y * i);
                int iz = (int) Math.floor(z + vec.z * i);
                res += level.getBlockState(pos.set(ix, iy, iz)).getBlock().getExplosionResistance();
            }

            boolean isLiving = e instanceof LivingEntity;

            if (res < 1) res = 1;

            if (isLiving && rad3d > 0) {
                float eRads = rad3d;
                eRads /= (float) (dmgLen * dmgLen * Math.sqrt(res));
                contaminate((LivingEntity) e, HazardType.RADIATION, ContaminationType.CREATIVE, eRads);
            }

            if (isLiving && dig3d > 0) {
                float eDig = dig3d;
                eDig /= (float) (dmgLen * dmgLen * dmgLen);
                contaminate((LivingEntity) e, HazardType.DIGAMMA, ContaminationType.DIGAMMA, eDig);
            }

            if (fire3d > 0.025) {
                float fireDmg = fire3d;
                fireDmg /= (float) (dmgLen * dmgLen * res * res);
                if (fireDmg > 0.025) {
                    // CE swaps a held "marshmallow" item for "marshmallow_roasted" here at
                    // fireDmg > 0.1 (com.hbm.items.food.FoodItems - both items exist in this
                    // port, but are registered anonymously with no queryable ModItems-style
                    // field). Dropped as flavor, not logic, rather than reach into FoodItems'
                    // registration internals from this unrelated area.

                    if (!isFireExempt(e)) {
                        e.hurt(level.damageSources().inFire(), fireDmg);
                        e.igniteForSeconds(5);
                    }
                }
            }

            if (len < blastRange && blast3d > 0.025) {
                float blastDmg = blast3d;
                blastDmg /= (float) (dmgLen * dmgLen * res);
                if (blastDmg > 0.025) {
                    if (rad3d > 0) {
                        e.hurt(level.damageSources().source(ModDamageTypes.NUCLEAR_BLAST), blastDmg);
                    } else {
                        e.hurt(level.damageSources().source(ModDamageTypes.BLAST), blastDmg);
                    }
                }
                e.setDeltaMovement(e.getDeltaMovement().add(vec.x * 0.005D * blastDmg, vec.y * 0.005D * blastDmg, vec.z * 0.005D * blastDmg));
                e.hasImpulse = true;
            }
        }
    }

    private static boolean isFireExempt(Entity e) {
        return e instanceof ArmorStand || e instanceof Boat || e instanceof HangingEntity;
    }

    /**
     * CE also exempts several nuke/grenade/projectile entities here
     * ({@code EntityNukeTorex}/{@code EntityNukeExplosionMK5}/{@code EntityMIRV}/
     * {@code EntityMiniNuke}/{@code EntityMiniMIRV}/{@code EntityExplosiveBeam}/
     * {@code EntityBulletBase}, and any {@code EntityGrenadeUniversal} carrying a
     * NUCLEAR/NUCLEAR_DEMO/SCHRAB filling) - none of those entity/weapon classes exist in this
     * port yet (this same phase's own weapons/explosion-content packages, not this
     * contamination/armor-util area). Add their checks back in as each one is ported, per the
     * research report's Deferred scope.
     */
    private static boolean isExplosionExempt(Entity e) {
        if (e instanceof Ocelot) return true;

        if (e instanceof LivingEntity living && ArmorEuphemium.isFullSetWorn(living)) return true;

        return e instanceof Player p && (p.isCreative() || p.isSpectator());
    }

    /**
     * {@code String.format("%.<decimals>e"/"%.<decimals>f", ...)} switching to scientific notation
     * once the magnitude falls outside {@code [10^-decimals, 10^6)}, matching CE's per-message
     * threshold checks in {@code printGeigerData} exactly (decimals 3/6/2 there).
     */
    private static String formatMagnitude(double value, int decimals) {
        double abs = Math.abs(value);
        double sciThreshold = Math.pow(10, -decimals);
        if (abs >= 1.0e6 || (abs > 0.0 && abs < sciThreshold)) {
            return String.format("%." + decimals + "e", value);
        }
        return String.format("%." + decimals + "f", value);
    }

    /**
     * Small self-contained duration formatter for {@link #printLungDiagnosticData}'s MKU-duration
     * readout, mirroring CE's {@code BobMathUtil.ticksToDateString}. Not delegated to
     * {@code com.hbm.util.BobMathUtil} because that class does not exist anywhere in this port
     * yet (a separate, pre-existing gap noticed while porting this area but out of this area's own
     * scope - several already-committed files import it too, e.g. {@code HazardTypeRadiation});
     * duplicating this one small helper locally avoids taking on a new hard dependency on it.
     */
    private static String ticksToDateString(long ticks, int tickHour) {
        int tickDay = 24 * tickHour;
        long tickYear = 365L * tickDay;
        double tickMinute = tickHour / 60D;
        double tickSecond = tickHour / 3600D;

        long year = Math.floorDiv(ticks, tickYear);
        int day = (int) Math.floorDiv(ticks - tickYear * year, tickDay);
        int h = (int) Math.floorDiv(ticks - tickYear * year - (long) tickDay * day, tickHour);
        int min = (int) Math.floor((ticks - tickYear * year - (long) tickDay * day - (long) tickHour * h) / tickMinute);
        int s = (int) Math.floor((ticks - tickYear * year - (long) tickDay * day - (long) tickHour * h - min * tickMinute) / tickSecond);

        if (year != 0) return year + "y " + day + "d " + h + "h " + min + "m " + s + "s";
        if (day != 0) return day + "d " + h + "h " + min + "m " + s + "s";
        if (h != 0) return h + "h " + min + "m " + s + "s";
        if (min != 0) return min + "m " + s + "s";
        return s + "s";
    }

    public enum HazardType {
        MONOXIDE,
        RADIATION,
        NEUTRON,
        DIGAMMA
    }

    public enum ContaminationType {
        /** preventable by metal armor */
        FARADAY,
        /** preventable by hazmat */
        HAZMAT,
        /** preventable by heavy hazmat */
        HAZMAT2,
        /** preventable by fau armor or stability */
        DIGAMMA,
        /** preventable by robes - CE no-op, see {@link #contaminate} */
        DIGAMMA2,
        /** preventable by creative mode; for rad calculation armor piece bonuses still apply */
        CREATIVE,
        /** same as CREATIVE but does not apply radiation resistance calculation */
        RAD_BYPASS,
        /** not preventable */
        NONE
    }

    /**
     * The central hazard dispatcher. Ported verbatim from CE, including its
     * {@code DIGAMMA2 -> break;} documented no-op case.
     */
    public static boolean contaminate(LivingEntity entity, HazardType hazard, ContaminationType cont, double amount) {

        if (hazard == HazardType.RADIATION) {
            double radEnv = HbmLivingProps.getRadEnv(entity);
            HbmLivingProps.setRadEnv(entity, radEnv + amount);
        }

        if (entity instanceof Player player) {
            if (player.isSpectator()) return false;

            switch (cont) {
                case FARADAY -> { if (ArmorUtil.checkForFaraday(player)) return false; }
                case HAZMAT -> { if (ArmorUtil.checkForHazmat(player)) return false; }
                case HAZMAT2 -> { if (ArmorUtil.checkForHaz2(player)) return false; }
                case DIGAMMA -> { if (ArmorUtil.checkForDigamma(player)) return false; }
                case DIGAMMA2 -> { /* CE no-op */ }
                default -> { }
            }

            if (player.isCreative() && cont != ContaminationType.NONE) {
                if (hazard == HazardType.NEUTRON) {
                    HbmLivingProps.setNeutron(entity, amount);
                }
                return false;
            }

            if (player.tickCount < 200) return false;
        }

        if ((hazard == HazardType.RADIATION || hazard == HazardType.NEUTRON) && isRadImmune(entity)) {
            return false;
        }

        switch (hazard) {
            case MONOXIDE -> entity.hurt(entity.damageSources().source(ModDamageTypes.MONOXIDE), (float) amount);
            case RADIATION -> HbmLivingProps.incrementRadiation(entity,
                    amount * (cont == ContaminationType.RAD_BYPASS ? 1D : calculateRadiationMod(entity)));
            case NEUTRON -> {
                HbmLivingProps.incrementRadiation(entity,
                        amount * (cont == ContaminationType.RAD_BYPASS ? 1D : calculateRadiationMod(entity)));
                HbmLivingProps.setNeutron(entity, amount);
            }
            case DIGAMMA -> applyDigammaData(entity, amount);
            default -> { }
        }

        return true;
    }
}
