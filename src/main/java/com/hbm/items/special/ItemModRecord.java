package com.hbm.items.special;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * Port of CE's {@code ItemModRecord}. CE extended {@code net.minecraft.item.ItemRecord}, which
 * carried its {@code SoundEvent} directly on the item instance - removed in modern Minecraft
 * (1.19.3+) in favor of a data-driven {@code JukeboxSong} registry entry referenced from the item
 * via the {@code JukeboxPlayable} data component (verified against this project's own decompiled
 * 1.21.1 sources: {@code JukeboxPlayable(EitherHolder<JukeboxSong>, boolean)},
 * {@code Item.Properties#jukeboxPlayable(ResourceKey<JukeboxSong>)}). This class only needs to carry
 * the display-name/tooltip behavior CE layered on top of the base record item; the actual
 * jukebox-insert interaction (matching a {@code Blocks.JUKEBOX} block state and consuming the stack)
 * is handled generically by vanilla's own {@code JukeboxPlayable.tryInsertIntoJukebox}, wired
 * through the {@code JUKEBOX_PLAYABLE} component set at registration - no override needed here.
 * <p>
 * CE's {@code getRarity} override returning {@code EnumRarity.RARE} has no per-instance override
 * hook left on modern {@code Item} - rarity is now a plain {@code Item.Properties.rarity(Rarity)}
 * value baked in at registration, so every record is registered with {@code Rarity.RARE} directly
 * instead.
 * <p>
 * NOT YET DONE: the four {@code JukeboxSong} datapack entries this item's
 * {@code Item.Properties#jukeboxPlayable} keys resolve to ({@code data/hbm/jukebox_song/lc.json},
 * {@code ss.json}, {@code vc.json}, {@code glass.json}) do not exist on disk yet - without them,
 * {@code JukeboxPlayable.tryInsertIntoJukebox} will fail its registry lookup and these items will not
 * play in a jukebox at runtime, though they will still register and hold their tooltip/name/rarity
 * correctly. The {@code SoundEvent} each entry would name is already ported
 * ({@code HBMSoundHandler.lambdaCore}/{@code sectorSweep}/{@code vortalCombat}/{@code glass}, ids
 * {@code hbm:music.recordlambdacore}/{@code music.recordsectorsweep}/{@code music.recordvortalcombat}/
 * {@code music.transmission}), but no {@code .ogg} audio asset or {@code sounds.json} entry for any of
 * them has landed in this port yet either (see {@code HBMSoundHandler}'s own javadoc) - authoring the
 * {@code length_in_seconds}/{@code comparator_output} datapack fields now would mean inventing values
 * for audio that cannot be verified or heard in this build. Left as a followup for whichever area
 * lands the sound asset pipeline, at which point the real track lengths are knowable and the four
 * JSON files can be authored correctly in the same pass.
 */
public class ItemModRecord extends Item {

    private final String recordKey;

    public ItemModRecord(Properties properties, String recordKey) {
        super(properties);
        this.recordKey = recordKey;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.record." + recordKey + ".desc"));
    }
}
