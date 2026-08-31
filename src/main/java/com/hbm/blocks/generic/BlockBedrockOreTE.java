package com.hbm.blocks.generic;

import com.hbm.blocks.ILookOverlay;
import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.items.BilletPowderItems;
import com.hbm.util.i18n.I18nUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Ported from CE's {@code BlockBedrockOreTE} ({@code ore_bedrock_block}, 153 CE lines, read in full):
 * a tiered, {@link BlockEntity}-backed mineral deposit that {@code com.hbm.world.feature.BedrockOre}
 * places at/around {@code y=0}, feeding the (separate, unported) Excavator machine. Per
 * docs/phase4/ore_veins_and_bedrock_ores.md's "blocking dependency" note, this class exists purely so
 * that report's (a different Phase 4 package's) world-gen placement code has a real block+TE pair to
 * construct - no world-gen, drill-interaction or Excavator logic is added here.
 * <p>
 * CE's {@code ore_bedrock_block} registration is {@code setBlockUnbreakable().setResistance(3_600_000)}
 * - this port's own already-established "finite huge resistance instead of literal infinity" convention
 * (see {@code OilChainBlocks}/{@code GenericDecoBlocks#UNBREAKABLE_RESISTANCE}, both {@code 3_600_000.0F})
 * is reused verbatim by {@link com.hbm.blocks.generic.BedrockOreBlocks}, which supplies this block's
 * {@code Properties}.
 */
public class BlockBedrockOreTE extends BaseEntityBlock implements ILookOverlay {

    public BlockBedrockOreTE(Properties properties) {
        super(properties);
    }

    /** CE's {@code onEntityWalk}: walking across the deposit sets the entity on fire for 3 seconds. */
    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        entity.igniteForSeconds(3);
        super.stepOn(level, pos, state, entity);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BedrockOreBlockEntity(pos, state);
    }

    /** CE's own explicit {@code getRenderType() -> EnumBlockRenderType.MODEL} override, preserved verbatim. */
    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    /**
     * Ported from CE's {@code printHook} (an {@code ILookOverlay} HUD hint, Phase 5-adjacent). Dead
     * code today exactly like every other {@link ILookOverlay} implementor in this port - see that
     * interface's own javadoc: nothing yet raytraces the player's look target and calls this.
     */
    @Override
    @OnlyIn(Dist.CLIENT)
    public void printHook(RenderGuiEvent.Pre event, Level level, BlockPos pos) {
        if (!(level.getBlockEntity(pos) instanceof BedrockOreBlockEntity ore)) return;

        List<Component> text = new ArrayList<>();
        if (!ore.resource.isEmpty()) {
            text.add(ore.resource.getHoverName());
        }
        text.add(Component.literal(I18nUtil.resolveKey("desc.tier", ore.tier)));
        if (ore.acidRequirement != null) {
            text.add(Component.literal(I18nUtil.resolveKey("desc.requires", ore.acidRequirement.fill,
                    ore.acidRequirement.type.getLocalizedName().getString())));
        }

        ILookOverlay.printGeneric(event, Component.translatable(this.getDescriptionId() + ".name"), 0xffff00, 0x404000, text);
    }

    /**
     * Ported from CE's nested {@code TileEntityBedrockOre}: exactly the 5 fields CE's own class has
     * (no inventory, no ticking logic beyond NBT read/write), per the research report's precise
     * accounting. {@code resource}'s NBT id/size/meta triple and the update-packet trio are translated
     * to this port's already-established modern equivalents (see {@code BlockLoot.LootBlockEntity} for
     * the same {@code saveAdditional}/{@code loadAdditional}/{@code getUpdateTag}/{@code getUpdatePacket}
     * shape this class mirrors); {@code acidRequirement}'s fluid type is persisted name-based via
     * {@link Fluids#writeType}/{@link Fluids#readType}, exactly like CE's own call and this port's
     * {@code FluidTankNTM}.
     */
    public static class BedrockOreBlockEntity extends BlockEntity {

        public ItemStack resource = ItemStack.EMPTY;
        @Nullable
        public FluidStack acidRequirement;
        public int tier;
        public int color;
        public int shape;

        public BedrockOreBlockEntity(BlockPos pos, BlockState state) {
            super(BedrockOreBlocks.BEDROCK_ORE_ENTITY_TYPE.get(), pos, state);
        }

        public BedrockOreBlockEntity setStyle(int color, int shape) {
            this.color = color;
            this.shape = shape;
            return this;
        }

        @Override
        protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
            super.saveAdditional(tag, registries);

            if (!resource.isEmpty()) {
                tag.put("resource", resource.save(registries, new CompoundTag()));
            }
            if (acidRequirement != null) {
                Fluids.writeType(tag, "fluid", acidRequirement.type); //stored by name, IDs shift when fluids are added/removed
                tag.putInt("amount", acidRequirement.fill);
            }
            tag.putInt("tier", tier);
            tag.putInt("color", color);
            tag.putInt("shape", shape);
        }

        @Override
        protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
            super.loadAdditional(tag, registries);

            resource = tag.contains("resource")
                    ? ItemStack.parseOptional(registries, tag.getCompound("resource"))
                    : ItemStack.EMPTY;
            if (resource.isEmpty()) {
                // CE's own fallback when the saved resource fails to resolve: new ItemStack(ModItems.powder_iron).
                resource = new ItemStack(BilletPowderItems.POWDER_IRON.get());
            }

            FluidType type = Fluids.readType(tag, "fluid"); //name-based, with legacy numeric-ID fallback
            acidRequirement = type != Fluids.NONE ? new FluidStack(type, tag.getInt("amount")) : null;

            tier = tag.getInt("tier");
            color = tag.getInt("color");
            shape = tag.getInt("shape");
        }

        @Override
        public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
            CompoundTag tag = new CompoundTag();
            saveAdditional(tag, registries);
            return tag;
        }

        @Override
        public Packet<ClientGamePacketListener> getUpdatePacket() {
            return ClientboundBlockEntityDataPacket.create(this);
        }
    }
}
