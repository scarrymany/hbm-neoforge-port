package com.hbm.blocks.bomb;

import com.hbm.blockentity.bomb.NukeCasingBlockEntities;
import com.hbm.blockentity.bomb.NukeSoliniumBlockEntity;
import com.hbm.config.BombConfig;
import com.hbm.entity.effect.EntityCloudSolinium;
import com.hbm.entity.logic.EntityNukeExplosionMK3;
import com.hbm.entity.logic.NukeEntityTypes;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

/**
 * CE {@code NukeSolinium}. 1×1, hardness 5/6000 in CE; uses shared casing props like the other
 * ported casings. Yield {@link BombConfig#SOLINIUM_RADIUS} (150). MK3 {@code extType=1}, no waste.
 */
public class NukeSoliniumBlock extends NukeCasingBlockBase {

    public NukeSoliniumBlock(Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new NukeSoliniumBlockEntity(NukeCasingBlockEntities.NUKE_SOLINIUM.get(), pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return null;
    }

    private void igniteTestBomb(Level level, @Nullable Entity detonator, @Nullable UUID placer, BlockPos pos, int radius) {
        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();
        level.playSound(null, x, y, z, SoundEvents.GENERIC_EXPLODE, SoundSource.BLOCKS, 1.0F, level.getRandom().nextFloat() * 0.1F + 0.9F);

        EntityNukeExplosionMK3 entity = new EntityNukeExplosionMK3(NukeEntityTypes.NUKE_MK3.get(), level);
        entity.setPos(x, y, z);
        entity.destructionRange = radius;
        entity.speed = BombConfig.BLAST_SPEED.get();
        entity.coefficient = 1.0F;
        entity.waste = false;
        entity.extType = 1;
        if (detonator != null) entity.setDetonator(detonator);
        else entity.detonator = placer;
        level.addFreshEntity(entity);
        level.addFreshEntity(EntityCloudSolinium.create(level, x, y, z, radius));
    }

    @Override
    public BombReturnCode explode(Level level, BlockPos pos, Entity detonator) {
        if (level.isClientSide()) return BombReturnCode.UNDEFINED;
        if (level.getBlockEntity(pos) instanceof NukeSoliniumBlockEntity be && be.isReady()) {
            UUID placer = be.placerID;
            be.clearSlots();
            level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
            igniteTestBomb(level, detonator, placer, pos, BombConfig.SOLINIUM_RADIUS.get());
            return BombReturnCode.DETONATED;
        }
        return BombReturnCode.ERROR_MISSING_COMPONENT;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("trait.soliniumbomb").withStyle(ChatFormatting.DARK_AQUA));
        tooltip.add(Component.literal(" ").append(
                Component.translatable("desc.radius", BombConfig.SOLINIUM_RADIUS.get()).withStyle(ChatFormatting.YELLOW)));
        tooltip.add(Component.empty());
        tooltip.add(Component.translatable("desc.nukesolinium1").withStyle(ChatFormatting.LIGHT_PURPLE));
        tooltip.add(Component.translatable("desc.nukesolinium2").withStyle(ChatFormatting.LIGHT_PURPLE));
    }
}
