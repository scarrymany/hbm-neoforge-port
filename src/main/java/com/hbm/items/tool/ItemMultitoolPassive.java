package com.hbm.items.tool;

import com.hbm.lib.HBMSoundHandler;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.List;
import java.util.Random;
import java.util.function.Supplier;

/**
 * Ported from CE's {@code com.hbm.items.tool.ItemMultitoolPassive} - rungs 3-10 of the
 * {@code multitool_dig}/{@code multitool_silk} (Phase 1, {@link ItemMultitoolTool}) sneak-click
 * upgrade ladder. Per {@code docs/phase3/melee_weapons.md}'s finding #3/section C: a plain
 * {@link Item} subclass with no {@code Tier}/material concept at all (unlike
 * {@link ItemMultitoolTool}), matching CE's own real class boundary.
 * <p>
 * <b>Deliberately self-contained</b>: this class takes its upgrade target as a constructor-supplied
 * {@link Supplier} rather than referencing {@code ToolItems}'/{@code ModItems}' fields directly, so
 * it carries zero compile-time coupling to those concurrently-owned registration files - whoever
 * registers the 8 concrete rungs (not done by this package, see this package's wiring notes) wires
 * each rung's {@code nextRung} to the following rung's {@code DeferredItem}, closing the loop back
 * to Phase 1's {@code multitool_dig} at {@link Rung#DECON}.
 * <p>
 * Per-rung right-click body, Phase-3-safe today (ported in full): {@link Rung#EXT} (instant-smelt
 * via the vanilla {@code RecipeManager} - same lookup {@code IToolHarvestAbility.SMELTER} already
 * uses), {@link Rung#HIT} (pure combat stats, no right-click action - CE passes through), and
 * {@link Rung#SKY} (15 vanilla {@code LightningBolt}s in a 31x31 area).
 * <p>
 * Left as documented forward-reference TODOs (not silently dropped - each names its exact CE-side
 * blocker, per {@code docs/phase3/melee_weapons.md}'s Deferred scope):
 * <ul>
 *     <li>{@link Rung#MINER}/{@link Rung#BEAM} - need {@code EntityMinerBeam}/{@code EntityLaserBeam}
 *     (unported projectile entities).</li>
 *     <li>{@link Rung#MEGA} - needs {@code ExplosionChaos.levelDown} (unported) gated behind
 *     {@code CompatibilityConfig.isWarDim}, itself a *deliberately* not-ported 1.12-dimension-ID
 *     concept with no 1.21 {@code ResourceKey<Level>} equivalent decided yet (see that class's own
 *     javadoc). Per the report's own explicitly-sanctioned resolution, this rung's right-click is a
 *     permanent no-op until a replacement dimension gate is designed - an explicit decision, not an
 *     accidental "works everywhere now."</li>
 *     <li>{@link Rung#JOULE} - needs {@code Library.getBlockPosInPath} (confirmed absent from this
 *     port's {@code com.hbm.lib.Library}), a 9-ray raycast-fan utility; {@code EntityRubble} itself
 *     (the other half of this rung) is already ported.</li>
 *     <li>{@link Rung#DECON} - needs the specific {@code ModBlocks.waste_*} wasteland block set
 *     (unported world-gen content).</li>
 * </ul>
 */
public class ItemMultitoolPassive extends Item {

    public enum Rung {
        EXT(7), MINER(8), HIT(16), BEAM(8), SKY(5), MEGA(12), JOULE(12), DECON(5);

        public final float attackDamage;

        Rung(float attackDamage) {
            this.attackDamage = attackDamage;
        }
    }

    private static final Random RANDOM = new Random();

    private final Rung rung;
    private final Supplier<? extends Item> nextRung;

    public ItemMultitoolPassive(Properties properties, Rung rung, Supplier<? extends Item> nextRung) {
        super(properties.attributes(createAttributes(rung.attackDamage)));
        this.rung = rung;
        this.nextRung = nextRung;
    }

    /** CE: {@code getAttributeModifiers} adds only a flat {@code ATTACK_DAMAGE} bonus per instance - no attack-speed penalty, unlike the sword family. */
    public static ItemAttributeModifiers createAttributes(float damage) {
        return ItemAttributeModifiers.builder()
                .add(Attributes.ATTACK_DAMAGE, new AttributeModifier(Item.BASE_ATTACK_DAMAGE_ID, damage, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
                .build();
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (player.isCrouching()) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(), HBMSoundHandler.techBoop.get(), SoundSource.PLAYERS, 2.0F, 1.0F);

            // CE computes this swap unconditionally on both sides (deterministic - same enchantment
            // table client and server), so client-side prediction shows the upgrade instantly instead
            // of waiting on a server round-trip; matched here rather than gating on isClientSide().
            ItemStack upgraded = new ItemStack(nextRung.get(), stack.getCount());
            applyUpgradeEnchantments(level, upgraded);
            return InteractionResultHolder.success(upgraded);
        }

        return switch (rung) {
            case SKY -> {
                fireLightningStorm(level, player);
                yield InteractionResultHolder.pass(stack);
            }
            // MINER/BEAM's projectile shots are TODO (see class javadoc) - CE itself still returns
            // PASS from this branch either way (the shot is a side effect, not a result swap).
            default -> InteractionResultHolder.pass(stack);
        };
    }

    /** CE: the sneak-click enchant-on-upgrade table (`multitool_miner`/`multitool_sky`/`multitool_mega`/`multitool_decon` sneak-click targets). */
    private void applyUpgradeEnchantments(Level level, ItemStack upgraded) {
        switch (rung) {
            case MINER -> {
                addEnchant(level, upgraded, Enchantments.LOOTING, 3);
                addEnchant(level, upgraded, Enchantments.KNOCKBACK, 3);
            }
            case SKY -> addEnchant(level, upgraded, Enchantments.KNOCKBACK, 5);
            case MEGA -> addEnchant(level, upgraded, Enchantments.KNOCKBACK, 3);
            case DECON -> {
                addEnchant(level, upgraded, Enchantments.LOOTING, 3);
                addEnchant(level, upgraded, Enchantments.FORTUNE, 3);
            }
            default -> {
                // EXT/HIT/BEAM/JOULE sneak-clicks hand over a plain, unenchanted upgrade in CE too.
            }
        }
    }

    private static void addEnchant(Level level, ItemStack stack, ResourceKey<Enchantment> key, int enchantLevel) {
        Holder<Enchantment> holder = level.registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolderOrThrow(key);
        EnchantmentHelper.updateEnchantments(stack, mutable -> mutable.set(holder, enchantLevel));
    }

    /** CE: `multitool_sky`'s non-sneak right-click - 15 lightning bolts in a 31x31 area around the player. */
    private void fireLightningStorm(Level level, Player player) {
        for (int i = 0; i < 15; i++) {
            int x = (int) player.getX() - 15 + RANDOM.nextInt(31);
            int z = (int) player.getZ() - 15 + RANDOM.nextInt(31);
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);

            LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
            if (bolt != null) {
                bolt.moveTo(x, y, z);
                level.addFreshEntity(bolt);
            }
        }
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        var pos = context.getClickedPos();

        if (rung == Rung.EXT) {
            // CE: FurnaceRecipes.instance().getSmeltingResult(itemFromBlock) - same vanilla RecipeManager
            // lookup IToolHarvestAbility.SMELTER already uses in this port (getSmeltingResult).
            var state = level.getBlockState(pos);
            ItemStack blockAsItem = new ItemStack(state.getBlock().asItem());
            if (!blockAsItem.isEmpty()) {
                ItemStack smelted = getSmeltingResult(level, blockAsItem);
                if (!smelted.isEmpty() && context.getPlayer() != null) {
                    if (!level.isClientSide()) {
                        level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
                        if (!context.getPlayer().getInventory().add(smelted)) {
                            context.getPlayer().drop(smelted, false);
                        }
                    }
                    context.getPlayer().swing(context.getHand());
                    return InteractionResult.SUCCESS;
                }
            }
            return InteractionResult.PASS;
        }

        if (rung == Rung.MEGA) {
            // Permanent no-op until a war-dimension replacement gate is designed - see class javadoc.
            return InteractionResult.PASS;
        }

        // JOULE (Library.getBlockPosInPath missing) and DECON (ModBlocks.waste_* missing) - see class javadoc.
        return InteractionResult.PASS;
    }

    private static ItemStack getSmeltingResult(Level level, ItemStack input) {
        SingleRecipeInput recipeInput = new SingleRecipeInput(input.copy());
        for (var holder : level.getRecipeManager().getAllRecipesFor(RecipeType.SMELTING)) {
            var recipe = holder.value();
            if (!recipe.matches(recipeInput, level)) continue;

            ItemStack result = recipe.getResultItem(level.registryAccess()).copy();
            if (!result.isEmpty()) {
                result.setCount(result.getCount() * input.getCount());
                return result;
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        switch (rung) {
            case EXT -> {
                tooltipComponents.add(Component.literal("Right click instantly destroys smeltable blocks"));
                tooltipComponents.add(Component.literal("Mined blocks will be smelted and put in the player's inventory"));
            }
            case MINER -> {
                tooltipComponents.add(Component.literal("Shoots lasers which destroy smeltable blocks"));
                tooltipComponents.add(Component.literal("These blocks will drop the smelted item"));
            }
            case HIT -> {
                tooltipComponents.add(Component.literal("Very high damage against mobs"));
                tooltipComponents.add(Component.literal("Strong knock back"));
            }
            case BEAM -> {
                tooltipComponents.add(Component.literal("Shoots lasers which ignite blocks and mobs"));
                tooltipComponents.add(Component.literal("Lasers are destroyed by water"));
            }
            case SKY -> {
                tooltipComponents.add(Component.literal("Right click summons a lightning storm around the player"));
                tooltipComponents.add(Component.literal("Lightning can also hit the player using the fist"));
            }
            case MEGA -> {
                tooltipComponents.add(Component.literal("Right click will level down blocks with a powerful punch"));
                tooltipComponents.add(Component.literal("Immense knockback against mobs"));
            }
            case JOULE -> {
                tooltipComponents.add(Component.literal("Right click will break blocks in the line of sight"));
                tooltipComponents.add(Component.literal("These blocks will be flung up as rubble"));
            }
            case DECON -> {
                tooltipComponents.add(Component.literal("Right click will remove radiation effect from blocks"));
                tooltipComponents.add(Component.literal("Blocks like nuclear waste turn into lead"));
            }
        }
    }
}
