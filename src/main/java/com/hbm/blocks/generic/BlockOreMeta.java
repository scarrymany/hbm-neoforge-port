package com.hbm.blocks.generic;

import com.hbm.blocks.BlockEnums.OreType;
import net.minecraft.world.level.block.Block;

/**
 * Ported from CE's {@code BlockOreMeta}: a generic "ore overlay" family where one base texture is
 * multiplied against a list of {@link OreType} overlay textures via a custom
 * {@code TextureAtlasSpriteMultipass}/{@code ModelBakeEvent} pipeline, one item-metadata value per
 * overlay. That whole runtime texture-layering/model-baking mechanism is dropped per the port's
 * datagen ground rule (layered textures become ordinary datagen-authored block models, one per
 * flattened registry entry, exactly like every other overlay-driven block in this package).
 * <p>
 * <b>No concrete instance is registered from this area.</b> CE itself never constructs a
 * {@code BlockOreMeta} anywhere in its own {@code ModBlocks} (confirmed by searching the upstream
 * CE source tree) - every CE ore block that looks superficially similar is actually a
 * {@code BlockNTMOre}/{@code BlockDepthOre} instance (ported and registered by
 * {@link com.hbm.blocks.OreBlocks}) or one of this package's other {@code EnumMeta}-flattened
 * classes ({@link BlockMeteorOre}, {@link BlockResourceStone}, {@link BlockOreBasalt}). This class
 * is kept as compiled, reusable infrastructure - matching the precedent {@link BlockGenericSlab}/
 * {@link BlockGenericStairs}/{@link BlockRBMKSlab} already set for CE base classes with no CE
 * instance to carry over - for whichever future content actually needs a base-texture-plus-overlay
 * ore family.
 */
public class BlockOreMeta extends Block {

    public final String baseTextureName;
    public final OreType[] overlays;

    public BlockOreMeta(Properties properties, String baseTextureName, OreType... overlays) {
        super(properties);
        this.baseTextureName = baseTextureName;
        this.overlays = overlays;
    }
}
