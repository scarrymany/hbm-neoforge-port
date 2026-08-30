package com.hbm.items.special;

import com.hbm.util.i18n.I18nUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Port of CE's {@code ItemCustomLore}: generic decorative-item base (runes, coins, eggs) that
 * appends an optional {@code <translationKey>.desc} lore line, split on {@code $} into multiple
 * tooltip lines, plus an optional enchantment glint. CE looked this text up via a lang-key lookup
 * that returned the key itself when missing; the port's {@link I18nUtil} equivalent is used the
 * same way.
 * <p>
 * Not ported: CE's {@code ModItems.undefined} random-item-name Easter egg (a single specific item
 * from a different, not-yet-run Phase 1 area) and the underlying {@code ItemBakedBase} 1.12
 * custom-model-loading path, superseded by 1.21's item model/datagen pipeline (finding 6). The
 * {@code ScramblingName} ticker itself is pure logic with no rendering dependency and is kept
 * verbatim for whichever future area's Easter egg item calls {@link #updateSystem()}.
 */
public class ItemCustomLore extends Item {

    private final boolean hasEffect;

    public ItemCustomLore(Properties properties) {
        this(properties, false);
    }

    public ItemCustomLore(Properties properties, boolean hasEffect) {
        super(properties);
        this.hasEffect = hasEffect;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        String key = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
        String unloc = "item.hbm." + key + ".desc";
        String loc = I18nUtil.resolveKey(unloc);
        if (!unloc.equals(loc)) {
            for (String line : loc.split("\\$")) {
                tooltip.add(Component.literal(line));
            }
        }
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return hasEffect || super.isFoil(stack);
    }

    /**
     * A surprise tool we need for later.
     */
    public static class ScramblingName {

        public String previous;
        public String next;
        public String[] previousFrags;
        public String[] nextFrags;
        public String[] frags;
        public int[] mask;
        public int age = 0;

        private static final Random RAND = new Random();

        public ScramblingName(String init) {
            previous = next = init;
            frags = init.split("");
            mask = new int[frags.length];
            previousFrags = chop(previous, frags.length);
            nextFrags = chop(next, frags.length);
        }

        public String getResult() {
            return String.join("", frags);
        }

        public void updateTick(String[] nextNames) {
            age++;
            try {
                if (age % 200 == 0) nextName(nextNames);
                if (age % 5 == 0) scramble();
            } catch (Exception ignored) {
            }
        }

        public void nextName(String[] nextNames) {
            if (nextNames.length < 2) return;

            this.previous = this.next;

            String initial = next;
            while (initial.equals(next)) {
                next = nextNames[RAND.nextInt(nextNames.length)];
            }

            int length = Math.min(previous.length(), next.length());
            this.previousFrags = chop(previous, length);
            this.frags = chop(previous, length);
            this.nextFrags = chop(next, length);
            mask = new int[length];
        }

        public void scramble() {
            List<Integer> indices = new ArrayList<>();

            for (int i = 0; i < mask.length; i++) {
                int m = mask[i];
                if (m == 0) indices.add(i);
                if (m > 0 && m <= 5) mask[i]++;
                if (m > 5) frags[i] = nextFrags[i];
            }

            if (!indices.isEmpty()) {
                int toSwitch = indices.get(RAND.nextInt(indices.size()));
                mask[toSwitch] = 1;
                frags[toSwitch] = ChatFormatting.OBFUSCATED + previousFrags[toSwitch] + ChatFormatting.RESET;
            }
        }

        public String[] chop(String name, int parts) {
            if (parts == name.length()) return name.split("");

            double index = 0;
            double incrementPerStep = (double) name.length() / (double) parts;
            List<String> slices = new ArrayList<>();

            for (int i = 0; i < parts; i++) {
                int end = (i == parts - 1) ? name.length() : (int) (index + incrementPerStep);
                slices.add(name.substring((int) index, end));
                index += incrementPerStep;
            }

            return slices.toArray(new String[parts]);
        }
    }

    public static final String[] NAMES = new String[] {
            "THE DEFAULT", "NEXT ONE", "ANOTHER ONE", "NON-STANDARD NAME", "AMBIGUOUS TITLE", "SHORT"
    };

    public static final ScramblingName NAME = new ScramblingName(NAMES[0]);

    public static void updateSystem() {
        NAME.updateTick(NAMES);
    }
}
