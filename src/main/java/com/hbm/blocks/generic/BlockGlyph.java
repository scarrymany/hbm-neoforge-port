package com.hbm.blocks.generic;

import com.mojang.serialization.MapCodec;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;

import java.util.List;

/**
 * Ported from CE's {@code BlockGlyph}: a decorative dungeon glyph wall with 16 texture variants
 * (no tile entity). CE modelled the variants as one block with a 0-15 {@code PropertyInteger} and
 * gave each meta value a raw (non-localized) tooltip name via a {@code switch} in
 * {@code addInformation}; each {@link Type} constant is its own registered block here (see
 * {@link PlantBlocks}), and the tooltip switch becomes a per-constant name carried
 * on the enum itself. CE's raw-string tooltips (not translation keys) are kept verbatim rather
 * than invented as lang-file entries.
 * <p>
 * Deliberately not ported: CE's {@code getStateForPlacement} override forcing metadata 0 on
 * placement (a 1.12-only metadata-multi workaround with nothing left to guard now that every
 * glyph texture is its own block) and the {@code onExplosionDestroy} override, which only called
 * {@code super} and did nothing else.
 */
public class BlockGlyph extends Block {

    public static final MapCodec<BlockGlyph> CODEC = simpleCodec(BlockGlyph::new);

    public final Type type;

    public BlockGlyph(Properties properties) {
        this(properties, Type.HOURGLASS);
    }

    public BlockGlyph(Properties properties, Type type) {
        super(properties);
        this.type = type;
    }

    @Override
    protected MapCodec<? extends BlockGlyph> codec() {
        return CODEC;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.literal(type.label));
    }

    public enum Type {
        HOURGLASS("Hourglass"),
        EYE("Eye"),
        PILLAR("'Pillar'"),
        IOI("IOI"),
        DELTA("Delta"),
        VTPC("VTPC"),
        COOL_S("Cool S"),
        TREFOIL("Trefoil"),
        PONY("Pony"),
        SPARKLE("Sparkle"),
        PIP("PiP"),
        TRIANGLES("Triangles"),
        LINUX_MINT("Linux Mint"),
        THIRTEEN("13"),
        DIGAMMA("Digamma"),
        CELESTIAL_ALTAR("Celestial Altar");

        public static final Type[] VALUES = values();

        public final String label;

        Type(String label) {
            this.label = label;
        }
    }
}
