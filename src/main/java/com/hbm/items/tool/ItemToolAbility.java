package com.hbm.items.tool;

import com.hbm.api.item.IDepthRockTool;
import com.hbm.handler.HbmKeybinds;
import com.hbm.handler.ability.AvailableAbilities;
import com.hbm.handler.ability.IBaseAbility;
import com.hbm.handler.ability.IToolAreaAbility;
import com.hbm.handler.ability.IToolHarvestAbility;
import com.hbm.handler.ability.ToolPreset;
import com.hbm.items.ICustomItemModelRegister;
import com.hbm.items.IItemControlReceiver;
import com.hbm.items.IKeybindReceiver;
import com.hbm.util.TagsUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.common.IShearable;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.ItemAbility;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Base for every material-tiered mining tool (pickaxe/axe/shovel/"miner") CE builds through
 * {@code ItemToolAbility}. Ported from CE's {@code com.hbm.items.tool.ItemToolAbility}, retargeted
 * at the 1.21 tool API ({@link TieredItem} + {@link Tier} + {@link ItemAbility} replace 1.12's
 * {@code ItemTool}/{@code ToolMaterial}/hardcoded block-and-material sets).
 *
 * <p>Scope notes (see {@code docs/phase1/items_tool.md} for the full research):
 * <ul>
 *     <li>Dynamic model baking ({@code IDynamicModels}/{@code IClaimedModelLocation},
 *     {@code ModelBakeEvent} retexturing) is a 1.12-era workaround for handheld item models and has
 *     no equivalent need in 1.21 (items get their model from a plain item model JSON keyed to the
 *     registry name) - not ported because it is not needed, not because it was skipped.</li>
 *     <li>{@code IItemHUD} (the crosshair ability-icon overlay) and the ability-customization GUI
 *     ({@code GUIScreenToolAbility}, opened via the alt-click keybind) are both deferred: neither
 *     the HUD icon texture atlas nor a generic screen for editing presets exists in this port yet.
 *     Cycling through presets via right-click (and sneak-right-click to jump to the first preset)
 *     works fully without them.</li>
 *     <li>{@link com.hbm.handler.ability.IToolAreaAbility#EXPLOSION} falls back to a plain vanilla
 *     explosion instead of CE's custom {@code ExplosionNT} pipeline - see that class's javadoc.</li>
 * </ul>
 */
public class ItemToolAbility extends TieredItem implements IDepthRockTool, IItemControlReceiver, IKeybindReceiver, ICustomItemModelRegister {

    /** Mirrors CE's {@code EnumToolType} (PICKAXE/AXE/SHOVEL/MINER - CE never had a HOE variant here). */
    public enum ToolRole {
        PICKAXE,
        AXE,
        SHOVEL,
        MINER
    }

    protected final AvailableAbilities availableAbilities = new AvailableAbilities().addToolAbilities();
    private final ToolRole toolRole;
    private boolean rockBreaker = false;
    private boolean isShears = false;

    public ItemToolAbility(Properties properties, Tier tier, ToolRole toolRole) {
        super(tier, properties);
        this.toolRole = toolRole;
    }

    public ItemToolAbility addAbility(IBaseAbility ability, int level) {
        this.availableAbilities.addAbility(ability, level);
        return this;
    }

    public ItemToolAbility setDepthRockBreaker() {
        this.rockBreaker = true;
        return this;
    }

    public ItemToolAbility setShears() {
        this.isShears = true;
        return this;
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!attacker.level().isClientSide() && attacker instanceof Player player && canOperate(stack)) {
            this.availableAbilities.getWeaponAbilities().forEach((ability, level) -> ability.onHit(level, attacker.level(), player, target, this));
        }
        stack.hurtAndBreak(2, attacker, EquipmentSlot.MAINHAND);
        return true;
    }

    @Override
    public boolean canPerformAction(ItemStack stack, ItemAbility itemAbility) {
        return switch (this.toolRole) {
            case PICKAXE -> ItemAbilities.DEFAULT_PICKAXE_ACTIONS.contains(itemAbility);
            case AXE -> ItemAbilities.DEFAULT_AXE_ACTIONS.contains(itemAbility);
            case SHOVEL -> ItemAbilities.DEFAULT_SHOVEL_ACTIONS.contains(itemAbility);
            case MINER -> ItemAbilities.DEFAULT_PICKAXE_ACTIONS.contains(itemAbility) || ItemAbilities.DEFAULT_SHOVEL_ACTIONS.contains(itemAbility);
        };
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return stack.isDamageableItem();
    }

    /**
     * Adapts CE's {@code onBlockStartBreak}+{@code breakExtraBlock} 1.12 workaround onto 1.21's
     * native {@link net.minecraft.world.item.ItemStack#mineBlock} hook, which is the correct place
     * to do this in the modern API (CE's own comment on {@code onBlockStartBreak} explains it was
     * only ever a workaround for the lack of exactly this hook in 1.12). Behavior: the area ability
     * (if any) breaks extra blocks around the reference block first, then the reference block
     * itself is routed through the harvest ability (if one is active) instead of plain vanilla
     * mining; with no harvest ability active, vanilla mining proceeds normally for the reference
     * block.
     */
    @Override
    public boolean mineBlock(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity miningEntity) {
        boolean usedHarvestAbility = false;

        if (!level.isClientSide() && miningEntity instanceof Player player && canOperate(stack)) {
            Configuration config = getConfiguration(stack);
            var preset = config.getActivePreset();
            boolean canProcess = canHarvest(stack, state, player, level, pos) || canShearBlock(state, stack, level, pos);

            if (canProcess) {
                preset.harvestAbility.preHarvestAll(preset.harvestAbilityLevel, level, player, stack);
                try {
                    if (preset.areaAbility.isAllowed()) {
                        preset.areaAbility.onDig(preset.areaAbilityLevel, level, pos, player, this);
                    }

                    if (preset.harvestAbility != IToolHarvestAbility.NONE) {
                        preset.harvestAbility.onHarvestBlock(level, pos, player, pos);
                        usedHarvestAbility = true;
                    }
                } finally {
                    preset.harvestAbility.postHarvestAll(preset.harvestAbilityLevel, level, player, stack);
                }
            }
        }

        if (usedHarvestAbility) {
            return false;
        }

        return super.mineBlock(stack, level, state, pos, miningEntity);
    }

    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state) {
        if (!canOperate(stack)) {
            return 1.0F;
        }

        return isEffectiveForState(state) ? this.getTier().getSpeed() : 1.0F;
    }

    @Override
    public boolean isCorrectToolForDrops(ItemStack stack, BlockState state) {
        return isEffectiveForState(state);
    }

    private boolean isEffectiveForState(BlockState state) {
        return switch (this.toolRole) {
            case PICKAXE -> state.is(BlockTags.MINEABLE_WITH_PICKAXE);
            case AXE -> state.is(BlockTags.MINEABLE_WITH_AXE);
            case SHOVEL -> state.is(BlockTags.MINEABLE_WITH_SHOVEL);
            case MINER -> state.is(BlockTags.MINEABLE_WITH_PICKAXE) || state.is(BlockTags.MINEABLE_WITH_SHOVEL);
        };
    }

    public boolean canOperate(ItemStack stack) {
        return true;
    }

    public boolean canHarvest(ItemStack stack, BlockState state, Player player, Level level, BlockPos pos) {
        if (!canOperate(stack)) {
            return false;
        }

        if (isForbiddenBlock(state)) {
            return false;
        }

        if (getConfiguration(stack).getActivePreset().harvestAbility == IToolHarvestAbility.SILK) {
            return true;
        }

        return getDestroySpeed(stack, state) > 1.0F;
    }

    @Override
    public boolean canBreakRock(Level world, Player player, ItemStack tool, BlockState block, BlockPos pos) {
        return canOperate(tool) && this.rockBreaker;
    }

    public boolean canShearBlock(BlockState state, ItemStack stack, Level level, BlockPos pos) {
        return this.isShears(stack) && state.getBlock() instanceof IShearable shearable && shearable.isShearable(null, stack, level, pos);
    }

    public boolean isShears(ItemStack stack) {
        return this.isShears;
    }

    /**
     * CE additionally excludes its own bedrock-ore/depth-rock blocks here; those block classes
     * don't exist in this port yet (owned by a different area/phase), so only the always-relevant
     * vanilla set is checked. Whoever ports the depth-rock ore blocks should extend this.
     */
    private static boolean isForbiddenBlock(BlockState state) {
        return state.is(Blocks.BARRIER) || state.is(Blocks.BEDROCK) || state.is(Blocks.COMMAND_BLOCK)
                || state.is(Blocks.CHAIN_COMMAND_BLOCK) || state.is(Blocks.REPEATING_COMMAND_BLOCK);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return super.isFoil(stack) || !getConfiguration(stack).getActivePreset().isNone();
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        availableAbilities.appendHoverText(tooltipComponents);

        if (rockBreaker) {
            tooltipComponents.add(Component.empty());
            tooltipComponents.add(Component.literal("Can break depth rock!").withStyle(ChatFormatting.RED));
        }
    }

    /** Applies the active harvest ability to one extra block, given the reference block that started the dig. */
    public void breakExtraBlock(Level level, BlockPos pos, Player player, BlockPos refPos) {
        BlockState state = level.getBlockState(pos);
        if (state.isAir()) {
            return;
        }

        if (!(player instanceof ServerPlayer)) {
            return;
        }

        ItemStack stack = player.getMainHandItem();
        if (stack.isEmpty()) {
            return;
        }

        if (!(canHarvest(stack, state, player, level, pos) || canShearBlock(state, stack, level, pos))) {
            return;
        }

        BlockState refState = level.getBlockState(refPos);
        float refStrength = refState.getDestroyProgress(player, level, refPos);
        float strength = state.getDestroyProgress(player, level, pos);

        if (strength <= 0.0F || refStrength / strength > 10F) {
            return;
        }

        if (!player.hasCorrectToolForDrops(state)) {
            return;
        }

        Configuration config = getConfiguration(stack);
        config.getActivePreset().harvestAbility.onHarvestBlock(level, pos, player, refPos);
    }

    public static boolean canTakeDamage(ItemStack stack) {
        return !stack.isEmpty() && stack.isDamageableItem() && stack.getMaxDamage() > 0;
    }

    /**
     * Dispatch point the ability framework calls into after every successful harvest. Plain
     * {@link ItemToolAbility} wears vanilla durability; {@link ItemToolAbilityFueled} and
     * {@link ItemToolAbilityPower} override {@link #applyWear} to drain fuel/charge instead
     * (mirroring CE's {@code setDamage(ItemStack, int)} overrides, which 1.21's data-component-based
     * durability has no direct virtual-method equivalent for).
     */
    public static void damageTool(ItemStack stack, Player player, int amount) {
        if (stack.getItem() instanceof ItemToolAbility tool) {
            tool.applyWear(stack, player, amount);
        }
    }

    protected void applyWear(ItemStack stack, Player player, int amount) {
        if (canTakeDamage(stack)) {
            stack.hurtAndBreak(amount, player, EquipmentSlot.MAINHAND);
        }
    }

    /** Per-stack ability-preset selection, persisted through {@code minecraft:custom_data} via {@link TagsUtil}. */
    public static class Configuration {
        public List<ToolPreset> presets;
        public int currentPreset;

        public Configuration() {
            this.presets = null;
            this.currentPreset = 0;
        }

        public void writeToNBT(CompoundTag tag) {
            tag.putInt("ability", currentPreset);

            ListTag nbtPresets = new ListTag();
            for (var preset : presets) {
                CompoundTag nbtPreset = new CompoundTag();
                preset.writeToNBT(nbtPreset);
                nbtPresets.add(nbtPreset);
            }
            tag.put("abilityPresets", nbtPresets);
        }

        public void readFromNBT(CompoundTag tag) {
            currentPreset = tag.getInt("ability");

            ListTag nbtPresets = tag.getList("abilityPresets", Tag.TAG_COMPOUND);
            int numPresets = Math.min(nbtPresets.size(), 99);

            presets = new ArrayList<>(numPresets);
            for (int i = 0; i < numPresets; i++) {
                ToolPreset preset = new ToolPreset();
                preset.readFromNBT(nbtPresets.getCompound(i));
                presets.add(preset);
            }

            currentPreset = Math.max(0, Math.min(currentPreset, presets.size() - 1));
        }

        public void reset(AvailableAbilities availableAbilities) {
            currentPreset = 0;

            presets = new ArrayList<>(availableAbilities.size());
            presets.add(new ToolPreset());

            availableAbilities.getToolAreaAbilities().forEach((ability, level) -> {
                if (ability == IToolAreaAbility.NONE) return;
                presets.add(new ToolPreset(ability, level, IToolHarvestAbility.NONE, 0));
            });

            availableAbilities.getToolHarvestAbilities().forEach((ability, level) -> {
                if (ability == IToolHarvestAbility.NONE) return;
                presets.add(new ToolPreset(IToolAreaAbility.NONE, 0, ability, level));
            });

            presets.sort(
                    Comparator.comparing((ToolPreset p) -> p.harvestAbility)
                            .thenComparingInt(p -> p.harvestAbilityLevel)
                            .thenComparing(p -> p.areaAbility)
                            .thenComparingInt(p -> p.areaAbilityLevel)
            );
        }

        public void restrictTo(AvailableAbilities availableAbilities) {
            for (var preset : presets) {
                preset.restrictTo(availableAbilities);
            }
        }

        public ToolPreset getActivePreset() {
            return presets.get(currentPreset);
        }
    }

    public Configuration getConfiguration(ItemStack stack) {
        Configuration config = new Configuration();

        if (stack.isEmpty() || !TagsUtil.hasCustomData(stack)) {
            config.reset(availableAbilities);
            return config;
        }

        CompoundTag tag = TagsUtil.getCustomData(stack);
        if (!tag.contains("ability") || !tag.contains("abilityPresets")) {
            config.reset(availableAbilities);
            return config;
        }

        config.readFromNBT(tag);
        config.restrictTo(availableAbilities);
        return config;
    }

    public void setConfiguration(ItemStack stack, Configuration config) {
        if (stack.isEmpty()) {
            return;
        }

        CompoundTag tag = TagsUtil.getCustomData(stack);
        config.writeToNBT(tag);
        TagsUtil.putCustomData(stack, tag);
    }

    @Override
    public void receiveControl(ItemStack stack, CompoundTag data) {
        Configuration config = new Configuration();
        config.readFromNBT(data);
        config.restrictTo(availableAbilities);
        setConfiguration(stack, config);
    }

    @Override
    public boolean canHandleKeybind(Player player, ItemStack stack, HbmKeybinds.EnumKeybind keybind) {
        if (player.level().isClientSide()) {
            return keybind == HbmKeybinds.EnumKeybind.ABILITY_ALT;
        }
        return keybind == HbmKeybinds.EnumKeybind.ABILITY_CYCLE;
    }

    @Override
    public void handleKeybind(Player player, ItemStack stack, HbmKeybinds.EnumKeybind keybind, boolean state) {
        if (keybind != HbmKeybinds.EnumKeybind.ABILITY_CYCLE || !state) {
            return;
        }

        if (!canOperate(stack)) {
            return;
        }

        Configuration config = getConfiguration(stack);
        if (config.presets.size() < 2) {
            return;
        }

        if (player.isCrouching()) {
            config.currentPreset = 0;
        } else {
            config.currentPreset = (config.currentPreset + 1) % config.presets.size();
        }

        setConfiguration(stack, config);
        player.displayClientMessage(config.getActivePreset().getMessage(), true);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.25F,
                config.getActivePreset().isNone() ? 0.75F : 1.25F);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void handleKeybindClient(LocalPlayer player, ItemStack stack, HbmKeybinds.EnumKeybind keybind, boolean state) {
        // CE opens GUIScreenToolAbility here to customize presets; that screen isn't part of this
        // port yet (see class javadoc) - cycling via ABILITY_CYCLE above still fully works.
    }

    /**
     * All of this class's ~40 instances (plus {@link ItemToolAbilityFueled}/{@link ItemToolAbilityPower}
     * /{@link ItemChainsaw}, which inherit this override rather than repeating it) are handheld 3D
     * tools, not flat 2D resource icons - {@code ModItemModelProvider}'s {@code basicItem(...)}
     * default (single {@code item/generated} layer) would render them as a flat picture instead of
     * the angled in-hand tool pose every vanilla tool uses. Parents to vanilla's {@code item/handheld}
     * with a single {@code layer0} texture at this item's own registry-name path under {@code item/}
     * - the same texture-path convention {@code basicItem(...)} itself would have used, just against
     * the handheld parent instead of {@code item/generated} (confirmed real pattern: matches the Neo
     * Edition reference's {@code ItemModelProvider.handheldItem(Item)} helper, which resolves to this
     * exact {@code withExistingParent(...).texture("layer0", ...)} call).
     */
    @Override
    public void registerItemModel(ItemModelProvider provider, ResourceLocation modelLocation) {
        provider.withExistingParent(modelLocation.getPath(), provider.mcLoc("item/handheld"))
                .texture("layer0", provider.modLoc("item/" + modelLocation.getPath()));
    }
}
