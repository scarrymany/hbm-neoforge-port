package com.hbm.handler.ability;

import com.hbm.main.MainRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * All abilities a given tool/weapon item instance supports, plus the level each was registered
 * at. One instance per item (built once, at item construction). Ported verbatim from CE's
 * {@code com.hbm.handler.ability.AvailableAbilities}.
 */
public class AvailableAbilities {

    /** Insertion order matters (drives {@link ToolPreset} ordering via {@code Configuration.reset}). */
    private final HashMap<IBaseAbility, Integer> abilities = new HashMap<>();

    public AvailableAbilities() {
    }

    public AvailableAbilities addAbility(IBaseAbility ability, int level) {
        if (level < 0 || level >= ability.levels()) {
            MainRegistry.logger.warn("Illegal level {} for ability {}", level, ability.getName());
            level = ability.levels() - 1;
        }

        if (abilities.containsKey(ability)) {
            MainRegistry.logger.warn("Ability {} already had level {}, overwriting with level {}", ability.getName(), abilities.get(ability), level);
        }

        if (ability.isAllowed()) {
            abilities.put(ability, level);
        }

        return this;
    }

    public AvailableAbilities addToolAbilities() {
        addAbility(IToolAreaAbility.NONE, 0);
        addAbility(IToolHarvestAbility.NONE, 0);
        return this;
    }

    public AvailableAbilities removeAbility(IBaseAbility ability) {
        abilities.remove(ability);
        return this;
    }

    public boolean supportsAbility(IBaseAbility ability) {
        return abilities.containsKey(ability);
    }

    public int maxLevel(IBaseAbility ability) {
        return abilities.getOrDefault(ability, -1);
    }

    public Map<IBaseAbility, Integer> get() {
        return Collections.unmodifiableMap(abilities);
    }

    public Map<IWeaponAbility, Integer> getWeaponAbilities() {
        return abilities.keySet().stream()
                .filter(a -> a instanceof IWeaponAbility)
                .collect(Collectors.toMap(a -> (IWeaponAbility) a, abilities::get));
    }

    public Map<IBaseAbility, Integer> getToolAbilities() {
        return abilities.keySet().stream()
                .filter(a -> a instanceof IToolAreaAbility || a instanceof IToolHarvestAbility)
                .collect(Collectors.toMap(a -> a, abilities::get));
    }

    public Map<IToolAreaAbility, Integer> getToolAreaAbilities() {
        return abilities.keySet().stream()
                .filter(a -> a instanceof IToolAreaAbility)
                .collect(Collectors.toMap(a -> (IToolAreaAbility) a, abilities::get));
    }

    public Map<IToolHarvestAbility, Integer> getToolHarvestAbilities() {
        return abilities.keySet().stream()
                .filter(a -> a instanceof IToolHarvestAbility)
                .collect(Collectors.toMap(a -> (IToolHarvestAbility) a, abilities::get));
    }

    public int size() {
        return abilities.size();
    }

    public boolean isEmpty() {
        return abilities.isEmpty();
    }

    /** Appends the ability tooltip block (and controls hint) to an item's hover text. */
    public void appendHoverText(java.util.List<Component> list) {
        var toolAbilities = abilities.entrySet().stream()
                .filter(entry -> (entry.getKey() instanceof IToolAreaAbility && entry.getKey() != IToolAreaAbility.NONE)
                        || (entry.getKey() instanceof IToolHarvestAbility && entry.getKey() != IToolHarvestAbility.NONE))
                .sorted(Comparator.comparing((Map.Entry<IBaseAbility, Integer> e) -> e.getKey()).thenComparing(Map.Entry::getValue))
                .toList();

        if (!toolAbilities.isEmpty()) {
            list.add(Component.literal("Abilities:").withStyle(ChatFormatting.GRAY));

            toolAbilities.forEach(entry -> {
                IBaseAbility ability = entry.getKey();
                int level = entry.getValue();
                list.add(Component.literal("  " + ability.getFullName(level)).withStyle(ChatFormatting.GOLD));
            });

            list.add(Component.literal("Right click to cycle through presets!").withStyle(ChatFormatting.GRAY));
            list.add(Component.literal("Sneak-click to go to first preset!").withStyle(ChatFormatting.GRAY));
        }

        var weaponAbilities = abilities.entrySet().stream()
                .filter(entry -> entry.getKey() instanceof IWeaponAbility && entry.getKey() != IWeaponAbility.NONE)
                .sorted(Comparator.comparing((Map.Entry<IBaseAbility, Integer> e) -> e.getKey()).thenComparing(Map.Entry::getValue))
                .toList();

        if (!weaponAbilities.isEmpty()) {
            list.add(Component.literal("Weapon modifiers:").withStyle(ChatFormatting.GRAY));

            weaponAbilities.forEach(entry -> {
                IBaseAbility ability = entry.getKey();
                int level = entry.getValue();
                list.add(Component.literal("  " + ability.getFullName(level)).withStyle(ChatFormatting.RED));
            });
        }
    }
}
