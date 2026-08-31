package com.hbm.world.gen.nbt;

import com.hbm.blocks.generic.BlockWandLoot;
import com.hbm.main.MainRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Places a CE 1.10+ {@code StructureTemplate} NBT ({@code palette}/{@code blocks}/{@code size})
 * from {@code data/hbm/structure/<path>.nbt}. CE stores these under {@code assets/hbm/structures/}
 * ({@code StructureManager.java}); 1.21 datapack path is {@code data/<ns>/structure/}.
 */
public final class NbtTemplatePlacer {

    private static final Map<String, CompoundTag> CACHE = new HashMap<>();

    private NbtTemplatePlacer() {
    }

    public static int[] sizeOf(ResourceManager resources, String template) {
        CompoundTag root = load(resources, template);
        if (root == null) return new int[]{8, 8, 8};
        ListTag size = root.getList("size", Tag.TAG_INT);
        if (size.size() < 3) return new int[]{8, 8, 8};
        return new int[]{Math.max(1, size.getInt(0)), Math.max(1, size.getInt(1)), Math.max(1, size.getInt(2))};
    }

    public static void place(ServerLevelAccessor level, String template, BlockPos origin, Rotation rotation, BoundingBox chunkBox) {
        CompoundTag root = load(level.getLevel().getServer().getResourceManager(), template);
        if (root == null) return;
        ListTag palette = paletteOf(root);
        ListTag blocks = root.getList("blocks", Tag.TAG_COMPOUND);
        BlockState[] states = new BlockState[palette.size()];
        for (int i = 0; i < palette.size(); i++) {
            states[i] = parseState(palette.getCompound(i));
        }
        for (int i = 0; i < blocks.size(); i++) {
            CompoundTag entry = blocks.getCompound(i);
            ListTag pos = entry.getList("pos", Tag.TAG_INT);
            if (pos.size() < 3) continue;
            BlockPos local = new BlockPos(pos.getInt(0), pos.getInt(1), pos.getInt(2));
            BlockPos world = transform(local, origin, rotation);
            if (!chunkBox.isInside(world)) continue;
            int stateIdx = entry.getInt("state");
            if (stateIdx < 0 || stateIdx >= states.length) continue;
            BlockState state = states[stateIdx].rotate(rotation);
            if (state.isAir()) continue;
            level.setBlock(world, state, 2);
            if (entry.contains("nbt", Tag.TAG_COMPOUND)) {
                applyBlockEntity(level, world, state, entry.getCompound("nbt"));
            }
        }
    }

    private static void applyBlockEntity(ServerLevelAccessor level, BlockPos pos, BlockState state, CompoundTag raw) {
        CompoundTag nbt = raw.copy();
        String id = nbt.contains("id") ? nbt.getString("id") : "";
        if (id.contains("wand_loot") || state.getBlock() instanceof BlockWandLoot) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof BlockWandLoot.WandLootBlockEntity loot) {
                loot.loadFromStructureNbt(nbt);
                loot.markForReplace();
                loot.setChanged();
            }
            return;
        }
        try {
            nbt.putInt("x", pos.getX());
            nbt.putInt("y", pos.getY());
            nbt.putInt("z", pos.getZ());
            BlockEntity be = level.getBlockEntity(pos);
            if (be != null) {
                be.loadWithComponents(nbt, level.registryAccess());
            }
        } catch (Exception ignored) {
            // 1.12 TE nbt is not 1.21 component-shaped; wand_loot is the load-bearing case.
        }
    }

    private static BlockPos transform(BlockPos local, BlockPos origin, Rotation rotation) {
        BlockPos rotated = BlockPos.ZERO.offset(local).rotate(rotation);
        return origin.offset(rotated);
    }

    private static ListTag paletteOf(CompoundTag root) {
        if (root.contains("palette", Tag.TAG_LIST)) {
            return root.getList("palette", Tag.TAG_COMPOUND);
        }
        if (root.contains("palettes", Tag.TAG_LIST)) {
            ListTag palettes = root.getList("palettes", Tag.TAG_LIST);
            if (!palettes.isEmpty()) {
                return palettes.getList(0);
            }
        }
        return new ListTag();
    }

    private static BlockState parseState(CompoundTag entry) {
        String name = StructureBlockRemap.remap(entry.getString("Name"));
        Block block = BuiltInRegistries.BLOCK.getOptional(ResourceLocation.parse(name)).orElse(Blocks.AIR);
        if (block == Blocks.AIR) return Blocks.AIR.defaultBlockState();
        BlockState state = block.defaultBlockState();
        if (!entry.contains("Properties", Tag.TAG_COMPOUND)) return state;
        CompoundTag props = entry.getCompound("Properties");
        for (String key : props.getAllKeys()) {
            Property<?> property = state.getBlock().getStateDefinition().getProperty(key);
            if (property == null) continue;
            state = setValue(state, property, props.getString(key));
        }
        return state;
    }

    private static <T extends Comparable<T>> BlockState setValue(BlockState state, Property<T> property, String value) {
        Optional<T> parsed = property.getValue(value);
        return parsed.map(t -> state.setValue(property, t)).orElse(state);
    }

    private static CompoundTag load(ResourceManager resources, String template) {
        CompoundTag cached = CACHE.get(template);
        if (cached != null) return cached;
        ResourceLocation loc = ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "structure/" + template + ".nbt");
        try {
            Optional<Resource> resource = resources.getResource(loc);
            if (resource.isEmpty()) {
                MainRegistry.logger.warn("Missing CE structure template {}", loc);
                return null;
            }
            try (InputStream in = resource.get().open()) {
                CompoundTag tag = NbtIo.readCompressed(in, NbtAccounter.unlimitedHeap());
                CACHE.put(template, tag);
                return tag;
            }
        } catch (Exception e) {
            MainRegistry.logger.warn("Failed to read structure template {}: {}", loc, e.toString());
            return null;
        }
    }
}
