package com.hbm.explosion.vanillant.standard;

import com.hbm.explosion.vanillant.ExplosionVNT;
import com.hbm.explosion.vanillant.interfaces.IBlockMutator;
import com.hbm.explosion.vanillant.interfaces.IBlockProcessor;
import com.hbm.explosion.vanillant.interfaces.IDropChanceMutator;
import com.hbm.explosion.vanillant.interfaces.IFortuneMutator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

/**
 * CE: {@code BlockProcessorStandard} - for every block an {@code IBlockAllocator} marked affected:
 * roll item drops (subject to {@link IDropChanceMutator}/{@link IFortuneMutator}), notify the block of
 * its own explosion ({@code BlockState#onBlockExploded}, the modern delegate to
 * {@code Block#wasExploded} - CE's {@code Block#onBlockExploded} by the same name, still present in
 * 1.21.1 just moved onto {@code BlockState} - confirmed against Neo Edition's real, compiling port of
 * this same class), then run any {@link IBlockMutator#mutatePre}/{@code mutatePost} hooks around the
 * removal.
 * <p>
 * CE's 1.12 default {@code Block#onBlockExploded} implementation performed the actual removal itself
 * (a bare {@code setBlockToAir}); modern vanilla split that into two independent steps - the engine's
 * own {@code Explosion#finalizeExplosion} does the removal, then separately calls {@code wasExploded}
 * as a pure notification hook whose <em>default</em> body does nothing (only blocks that override it,
 * e.g. a detonatable block priming its own TNT entity, change anything). Since this class - like CE's
 * own equivalent - never calls {@code compat.finalizeExplosion()}, the removal step has no other home:
 * it happens explicitly below, via {@link ChunkBatchedBlockRemoval} rather than a naive per-position
 * {@code Level#setBlock} loop (see that class's javadoc for the full reasoning - this is this port's
 * answer to PORT_SPEC's explicit performance mandate for this exact call site). A position is only
 * left out of the batched removal if {@code onBlockExploded} already changed it itself (e.g. the
 * detonatable-block case above) - the notify hook still always fires first, exactly where CE calls it,
 * so any such override still gets its chance to run before this class's own removal would otherwise
 * clobber it.
 * <p>
 * The loot-table-based drop computation ({@code LootParams} + {@code BlockState#getDrops}) replaces
 * CE's direct {@code Block#dropBlockAsItemWithChance} call, which no longer exists - modern Minecraft
 * always resolves block drops through loot tables. This exact replacement shape (a fake enchanted pick
 * used only to carry a fortune level into the loot context) is confirmed against Neo Edition's real,
 * compiling port of this same class - a pure engine API-shape adaptation, not a CE behavior choice
 * (the drop chance and fortune values themselves are still exactly what CE's own mutators computed).
 */
public class BlockProcessorStandard implements IBlockProcessor {

    protected IDropChanceMutator chance;
    protected IFortuneMutator fortune;
    protected IBlockMutator convert;

    public BlockProcessorStandard() {
    }

    public BlockProcessorStandard withChance(IDropChanceMutator chance) {
        this.chance = chance;
        return this;
    }

    public BlockProcessorStandard withFortune(IFortuneMutator fortune) {
        this.fortune = fortune;
        return this;
    }

    public BlockProcessorStandard withBlockEffect(IBlockMutator convert) {
        this.convert = convert;
        return this;
    }

    @Override
    public void process(ExplosionVNT explosion, Level level, double x, double y, double z, HashSet<BlockPos> affectedBlocks) {
        Iterator<BlockPos> iterator = affectedBlocks.iterator();
        float dropChance = 1.0F / explosion.size;

        List<BlockPos> toRemove = new ArrayList<>();

        while (iterator.hasNext()) {
            BlockPos pos = iterator.next();
            BlockState state = level.getBlockState(pos);
            Block block = state.getBlock();

            if (state.isAir()) {
                iterator.remove();
                continue;
            }

            if (state.canDropFromExplosion(level, pos, explosion.compat)) {

                float rolledChance = dropChance;
                if (this.chance != null) {
                    rolledChance = this.chance.mutateDropChance(explosion, block, pos, rolledChance);
                }

                int dropFortune = (this.fortune == null) ? 0 : this.fortune.mutateFortune(explosion, block, pos);

                if (level instanceof ServerLevel serverLevel) {
                    ItemStack toolWith = ItemStack.EMPTY;
                    if (dropFortune > 0) {
                        toolWith = new ItemStack(Items.DIAMOND_PICKAXE);
                        Holder<Enchantment> fortuneEnchant = level.registryAccess()
                                .registryOrThrow(Registries.ENCHANTMENT)
                                .getHolderOrThrow(Enchantments.FORTUNE);
                        toolWith.enchant(fortuneEnchant, dropFortune);
                    }

                    LootParams.Builder builder = new LootParams.Builder(serverLevel)
                            .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(pos))
                            .withParameter(LootContextParams.TOOL, toolWith)
                            .withParameter(LootContextParams.BLOCK_STATE, state)
                            .withOptionalParameter(LootContextParams.BLOCK_ENTITY, level.getBlockEntity(pos));

                    float finalChance = rolledChance;
                    for (ItemStack drop : state.getDrops(builder)) {
                        if (serverLevel.random.nextFloat() <= finalChance) {
                            Block.popResource(serverLevel, pos, drop);
                        }
                    }
                }
            }

            state.onBlockExploded(level, pos, explosion.compat);

            if (this.convert != null) {
                this.convert.mutatePre(explosion, state, pos);
            }

            // Only this port's own batched removal handles the "ordinary block" case; if
            // onBlockExploded already changed this position itself (e.g. a detonatable block priming
            // its own entity), don't clobber whatever it left behind.
            if (level.getBlockState(pos).is(block)) {
                toRemove.add(pos);
            }
        }

        ChunkBatchedBlockRemoval.removeAndSync(level, toRemove);

        if (this.convert != null) {
            for (BlockPos pos : affectedBlocks) {
                if (level.getBlockState(pos).isAir()) {
                    this.convert.mutatePost(explosion, pos);
                }
            }
        }
    }

    public BlockProcessorStandard setNoDrop() {
        this.chance = new DropChanceMutatorStandard(0F);
        return this;
    }

    public BlockProcessorStandard setAllDrop() {
        this.chance = new DropChanceMutatorStandard(1F);
        return this;
    }

    public BlockProcessorStandard setFortune(int fortune) {
        this.fortune = (explosion, block, pos) -> fortune;
        return this;
    }
}
