package com.hbm.blocks.generic;

import com.hbm.blocks.ICustomBlockModelRegister;
import com.hbm.config.GeneralConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;

/**
 * Ported from CE's {@code WasteEarth}: contaminated-earth reskins. CE reused one class (via
 * {@code this == ModBlocks.X} identity checks) for {@code waste_earth}, {@code burning_earth} and
 * {@code frozen_grass}; {@link WasteMycelium} is a separate CE subclass for {@code waste_mycelium}.
 * This port replaces the identity checks with an explicit {@link Kind} passed at construction.
 * <p>
 * Not ported: CE's {@code canEntitySpawn}/{@code ContaminationUtil.isRadImmune} spawn gate and the
 * {@code waste_mycelium} walk-on radiation potion effect ({@code HbmPotion.radiation}) - both
 * depend on the radiation/hazard content system, which is not part of this block-registration
 * pass (see {@code docs/phase1/blocks_generic.md}'s treatment of other radiation-adjacent hooks as
 * inert until that system exists). {@code frozen_grass}'s vanilla-{@link MobEffects#MOVEMENT_SLOWDOWN}
 * slow effect has no such dependency and is preserved. The cosmetic 0-6 "which texture variant"
 * property CE gave every instance of this class turned out, on inspection of CE's own model JSONs,
 * to be entirely unused (every meta variant resolves to the same single model) except as an
 * unused random-appearance hook - it is not ported.
 */
public class WasteEarth extends Block implements ICustomBlockModelRegister {

    public enum Kind {
        WASTE, BURNING, FROZEN
    }

    public final Kind kind;

    public WasteEarth(Properties properties) {
        this(properties, Kind.WASTE);
    }

    public WasteEarth(Properties properties, Kind kind) {
        super(properties);
        this.kind = kind;
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return kind != Kind.FROZEN;
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (kind == Kind.WASTE && GeneralConfig.ENABLE_AUTOMATIC_RAD_CLEANUP.get()) {
            level.setBlock(pos, Blocks.DIRT.defaultBlockState(), 3);
        }
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (kind == Kind.BURNING) {
            level.addParticle(ParticleTypes.FLAME,
                    pos.getX() + 0.25 + random.nextDouble() * 0.5, pos.getY() + 1.1, pos.getZ() + 0.25 + random.nextDouble() * 0.5,
                    0.0, 0.0, 0.0);
            level.addParticle(ParticleTypes.SMOKE,
                    pos.getX() + 0.25 + random.nextDouble() * 0.5, pos.getY() + 1.1, pos.getZ() + 0.25 + random.nextDouble() * 0.5,
                    0.0, 0.0, 0.0);
        }
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        if (kind == Kind.BURNING) {
            entity.igniteForSeconds(3);
        } else if (kind == Kind.FROZEN && entity instanceof LivingEntity living) {
            living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 2 * 60 * 20, 2));
        }
        super.stepOn(level, pos, state, entity);
    }

    /**
     * Every CE instance of this class (except plain {@code waste_earth}, which has no valid flat
     * texture asset at all - see class javadoc) is a top/side/bottom "grass-like" block, not a
     * cube-all one: {@code frozen_grass}/{@code burning_earth} each have real {@code _side}/
     * {@code _top} textures plus a dirt-colored bottom (confirmed against CE's own
     * {@code block/cube_bottom_top}-parented model JSONs), so all three kinds share this one
     * {@link ICustomBlockModelRegister} implementation rather than falling back to the datagen
     * default cube-all path.
     */
    @Override
    public void registerModel(BlockStateProvider provider, ResourceLocation modelLocation) {
        String name = modelLocation.getPath();
        ResourceLocation side = provider.modLoc("block/" + name + "_side");
        ResourceLocation top = provider.modLoc("block/" + name + "_top");
        ResourceLocation bottom = kind == Kind.FROZEN ? provider.modLoc("block/frozen_dirt") : provider.mcLoc("block/dirt");

        provider.simpleBlockWithItem(this, provider.models().cubeBottomTop(name, side, bottom, top));
    }
}
