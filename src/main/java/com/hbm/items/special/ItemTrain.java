package com.hbm.items.special;

import com.hbm.blocks.rail.IRailNTM;
import com.hbm.blocks.rail.IRailNTM.MoveContext;
import com.hbm.blocks.rail.IRailNTM.RailCheckType;
import com.hbm.entity.train.EntityRailCarBase;
import com.hbm.entity.train.TrainCargoTram;
import com.hbm.entity.train.TrainCargoTramTrailer;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.function.Function;

/**
 * Port of CE's {@code ItemTrain}: an {@code ItemEnumMulti}-style multi-metadata item over 2
 * {@link EnumTrainType} rail-car types. Per docs/phase1/items_special.md finding 1, this flattens
 * into one registry entry per type (see {@link SpecialItems}) instead of a single
 * metadata-multiplexed item, the same treatment {@link ItemSoyuz} gets in this same package.
 * <p>
 * {@link #useOn} (CE: {@code onItemUse}) is now ported - the rail/train entity-side package
 * ({@code docs/phase4/entities_vehicles_aircraft.md}'s rail/train table:
 * {@code com.hbm.entity.train.*}) landed, closing the forward reference this class's javadoc
 * previously tracked. Spawns {@link EnumTrainType#factory}'s car on the clicked block only if it
 * {@code instanceof} {@link IRailNTM} and gauges match - exactly CE's own gate. Per that package's own
 * scope boundary, the actual rail <em>blocks</em> ({@code com.hbm.blocks.rail.*}) do not exist yet
 * (gated on the Phase 1/2 multiblock framework - see {@link IRailNTM}'s own javadoc), so this
 * {@code instanceof IRailNTM} check has nothing to match against yet and this item cannot actually
 * place a car in-game until that block package lands - not a bug here, the same "interface ready,
 * no implementor yet" state {@link IRailNTM} itself documents.
 */
public class ItemTrain extends Item {

    private final EnumTrainType trainType;

    public ItemTrain(Properties properties, EnumTrainType trainType) {
        super(properties);
        this.trainType = trainType;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        if (trainType.engine != null) {
            tooltip.add(Component.literal(ChatFormatting.GREEN + "Engine: " + ChatFormatting.RESET + trainType.engine));
        }
        tooltip.add(Component.literal(ChatFormatting.GREEN + "Gauge: " + ChatFormatting.RESET + trainType.gauge));
        if (trainType.maxSpeed != null) {
            tooltip.add(Component.literal(ChatFormatting.GREEN + "Max Speed: " + ChatFormatting.RESET + trainType.maxSpeed));
        }
        if (trainType.acceleration != null) {
            tooltip.add(Component.literal(ChatFormatting.GREEN + "Acceleration: " + ChatFormatting.RESET + trainType.acceleration));
        }
        if (trainType.brakeThreshold != null) {
            tooltip.add(Component.literal(ChatFormatting.GREEN + "Engine Brake Threshold: " + ChatFormatting.RESET + trainType.brakeThreshold));
        }
        if (trainType.parkingBrake != null) {
            tooltip.add(Component.literal(ChatFormatting.GREEN + "Parking Brake: " + ChatFormatting.RESET + trainType.parkingBrake));
        }
    }

    /**
     * CE: {@code onItemUse} - spawns {@link EnumTrainType#factory}'s car on the clicked block, gauge-
     * checked, positioned and yaw-aligned along the rail exactly like CE's original. See class javadoc
     * for why the {@code instanceof IRailNTM} check below cannot match anything yet.
     */
    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Block block = level.getBlockState(pos).getBlock();

        if (!(block instanceof IRailNTM rail)) return InteractionResult.PASS;

        ItemStack stack = context.getItemInHand();
        Player player = context.getPlayer();
        EntityRailCarBase train = trainType.factory.apply(level);

        if (train == null || train.getGauge() != rail.getGauge(level, pos.getX(), pos.getY(), pos.getZ())) {
            return InteractionResult.PASS;
        }

        Vec3 hit = context.getClickLocation();
        train.setPos(hit.x, hit.y, hit.z);
        train.setYRot(player != null ? player.getYRot() : 0F);
        BlockPos anchor = train.getCurrentAnchorPos();
        Vec3 corePos = train.getRelPosAlongRail(anchor, 0, new MoveContext(RailCheckType.CORE, 0));

        if (corePos == null) return InteractionResult.PASS;

        train.setPos(corePos.x, corePos.y, corePos.z);
        Vec3 frontPos = train.getRelPosAlongRail(anchor, train.getLengthSpan(), new MoveContext(RailCheckType.FRONT, train.getCollisionSpan() - train.getLengthSpan()));
        Vec3 backPos = train.getRelPosAlongRail(anchor, -train.getLengthSpan(), new MoveContext(RailCheckType.BACK, train.getCollisionSpan() - train.getLengthSpan()));

        if (frontPos == null || backPos == null) return InteractionResult.PASS;

        if (!level.isClientSide) {
            train.setYRot(EntityRailCarBase.generateYaw(frontPos, backPos));
            level.addFreshEntity(train);
        }

        stack.shrink(1);
        return InteractionResult.SUCCESS;
    }

    /**
     * Mirrors CE's {@code ItemTrain.EnumTrainType} (2 constants; confirmed against
     * {@code upstream/hbm-ce/.../items/special/ItemTrain.java}). CE's original {@code train} field was
     * a bare {@code Class<? extends EntityRailCarBase>} reflectively {@code .getConstructor(World.class)
     * .newInstance(world)}'d in {@code onItemUse}; {@link #factory} replaces that reflection with a
     * plain constructor reference (identical effect, no reflection) now that the rail-car entity
     * classes exist. Every display string is otherwise kept verbatim, including CE's own
     * {@code CARGO_TRAM_TRAILER} oddity of putting {@code "Yes"} in the "max speed" slot rather than a
     * speed value; this is CE's original data, not a copy error here.
     */
    public enum EnumTrainType {

        CARGO_TRAM(TrainCargoTram::new, "Electric", "Standard Gauge", "10m/s", "0.2m/s²", "<1m/s", "Yes"),
        CARGO_TRAM_TRAILER(TrainCargoTramTrailer::new, null, "Standard Gauge", "Yes", null, null, "No");

        public static final EnumTrainType[] VALUES = values();

        public final Function<Level, EntityRailCarBase> factory;
        public final String engine;
        public final String maxSpeed;
        public final String acceleration;
        public final String brakeThreshold;
        public final String parkingBrake;
        public final String gauge;

        EnumTrainType(Function<Level, EntityRailCarBase> factory, String engine, String gauge, String maxSpeed, String acceleration, String brakeThreshold, String parkingBrake) {
            this.factory = factory;
            this.engine = engine;
            this.maxSpeed = maxSpeed;
            this.acceleration = acceleration;
            this.brakeThreshold = brakeThreshold;
            this.parkingBrake = parkingBrake;
            this.gauge = gauge;
        }
    }
}
