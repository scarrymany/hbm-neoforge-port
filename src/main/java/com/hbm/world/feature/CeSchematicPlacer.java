package com.hbm.world.feature;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.hbm.blockentity.machine.CrateBlockEntity;
import com.hbm.blocks.generic.BlockScaffold;
import com.hbm.blocks.generic.BlockSellafield;
import com.hbm.itempool.ItemPool;
import com.hbm.main.MainRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.block.state.properties.SlabType;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Places a CE Java schematic extracted from {@code setBlockState} / {@code placeDoorWithoutCheck}
 * (Bunker.java / Radio01.java / Radio02.java). JSON under {@code data/hbm/schematic/} is the
 * literal CE coordinate+id table — not a fake wall box.
 */
public final class CeSchematicPlacer {

    private static final ConcurrentHashMap<String, Schematic> CACHE = new ConcurrentHashMap<>();

    private CeSchematicPlacer() {
    }

    public static void place(WorldGenLevel level, BlockPos origin, RandomSource random, String name) {
        Schematic schematic = CACHE.computeIfAbsent(name, CeSchematicPlacer::load);
        for (Cell cell : schematic.cells) {
            placeCell(level, origin, random, cell);
        }
    }

    /** CE {@code Library.getRandomConcrete(Random)} at Library.java:1108-1116. */
    public static Block randomConcrete(RandomSource random) {
        int i = random.nextInt(100);
        if (i < 5) return block("hbm:brick_concrete_broken");
        if (i < 20) return block("hbm:brick_concrete_cracked");
        if (i < 50) return block("hbm:brick_concrete_mossy");
        return block("hbm:brick_concrete");
    }

    private static void placeCell(WorldGenLevel level, BlockPos origin, RandomSource random, Cell cell) {
        BlockPos pos = origin.offset(cell.x, cell.y, cell.z);
        if (cell.special != null) {
            switch (cell.special.type) {
                case "chest" -> placeChest(level, pos, random, cell);
                case "crate" -> placeCrate(level, pos, random, cell);
                case "door" -> placeDoor(level, pos, cell);
                case "geiger" -> placeGeiger(level, pos, cell);
                default -> placeBlock(level, pos, resolveBlock(cell, random), cell);
            }
            return;
        }
        placeBlock(level, pos, resolveBlock(cell, random), cell);
    }

    private static Block resolveBlock(Cell cell, RandomSource random) {
        if ("hbm:random_concrete".equals(cell.blockId)) return randomConcrete(random);
        return block(cell.blockId);
    }

    private static void placeBlock(WorldGenLevel level, BlockPos pos, Block block, Cell cell) {
        if (block == null) return;
        BlockState state = apply(block.defaultBlockState(), cell);
        setBlockSafe(level, pos, state);
    }

    private static void placeChest(WorldGenLevel level, BlockPos pos, RandomSource random, Cell cell) {
        Direction facing = direction(cell.special.facing, Direction.EAST);
        setBlockSafe(level, pos, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, facing));
        fillContainer(level, pos, random, cell.special);
    }

    private static void placeCrate(WorldGenLevel level, BlockPos pos, RandomSource random, Cell cell) {
        Block crate = block(cell.blockId);
        if (crate == null) return;
        setBlockSafe(level, pos, crate.defaultBlockState());
        fillContainer(level, pos, random, cell.special);
    }

    /**
     * Public write used by hive/atom (and {@link #place}). FEATURES write-radius is 0 —
     * skip cells outside the generating chunk. Do not {@code ServerLevel.setBlock}.
     */
    public static boolean setBlockInRegion(WorldGenLevel level, BlockPos pos, BlockState state) {
        return setBlockSafe(level, pos, state);
    }

    /**
     * CE {@code IWorldGenerator} wrote the full wreck. 1.21 {@code WorldGenRegion} rejects
     * {@code setBlock} outside the generating write-radius (spaceship 12×46 / satellite 25×31)
     * and logs {@code Detected setBlock in a far chunk}. Skip those cells — do not
     * {@code ServerLevel.setBlock} (creates/cascades chunks at forced 1/1).
     */
    private static boolean setBlockSafe(WorldGenLevel level, BlockPos pos, BlockState state) {
        // FEATURES write-radius is 0. ensureCanWrite logs the far-chunk ERROR
        // when it returns false — skip silently instead.
        if (level instanceof WorldGenRegion region) {
            var center = region.getCenter();
            if ((pos.getX() >> 4) != center.x || (pos.getZ() >> 4) != center.z) return false;
        }
        level.setBlock(pos, state, 3);
        return true;
    }

    private static void fillContainer(WorldGenLevel level, BlockPos pos, RandomSource random, Special special) {
        ItemPool pool = ItemPool.getPool(special.pool);
        int rolls = special.rolls;
        if (special.rand > 0) rolls = special.base + random.nextInt(special.rand);
        BlockEntity be = level.getBlockEntity(pos);
        if (be == null) be = level.getLevel().getBlockEntity(pos);
        if (be instanceof RandomizableContainerBlockEntity chest) {
            int slots = chest.getContainerSize();
            if (slots <= 0) return;
            for (int i = 0; i < rolls; i++) {
                ItemStack stack = ItemPool.getStack(pool, random);
                if (!stack.isEmpty()) chest.setItem(random.nextInt(slots), stack);
            }
            extraItem(random, special).ifPresent(stack -> chest.setItem(random.nextInt(slots), stack));
            return;
        }
        if (be instanceof CrateBlockEntity crate) {
            var inv = crate.getCheckedInventory();
            int slots = inv.getSlots();
            if (slots <= 0) return;
            for (int i = 0; i < rolls; i++) {
                ItemStack stack = ItemPool.getStack(pool, random);
                if (!stack.isEmpty()) inv.setStackInSlot(random.nextInt(slots), stack);
            }
            extraItem(random, special).ifPresent(stack -> inv.setStackInSlot(random.nextInt(slots), stack));
        }
    }

    private static Optional<ItemStack> extraItem(RandomSource random, Special special) {
        if (special.item == null || special.chance <= 0 || random.nextInt(special.chance) != 0) {
            return Optional.empty();
        }
        Item item = BuiltInRegistries.ITEM.getOptional(ResourceLocation.parse(special.item)).orElse(null);
        if (item == null) return Optional.empty();
        return Optional.of(new ItemStack(item));
    }

    /** CE {@code Library.placeDoorWithoutCheck} at Library.java:1119-1127. */
    private static void placeDoor(WorldGenLevel level, BlockPos pos, Cell cell) {
        Block door = block(cell.blockId);
        if (door == null || !(door instanceof DoorBlock)) {
            door = Blocks.IRON_DOOR;
        }
        Direction facing = direction(cell.special.facing, Direction.NORTH);
        DoorHingeSide hinge = "right".equals(cell.special.hinge) ? DoorHingeSide.RIGHT : DoorHingeSide.LEFT;
        BlockState base = door.defaultBlockState()
                .setValue(DoorBlock.FACING, facing)
                .setValue(DoorBlock.HINGE, hinge)
                .setValue(DoorBlock.OPEN, false)
                .setValue(DoorBlock.POWERED, false);
        setBlockSafe(level, pos, base.setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER));
        setBlockSafe(level, pos.above(), base.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
    }

    private static void placeGeiger(WorldGenLevel level, BlockPos pos, Cell cell) {
        Block geiger = block("hbm:geiger");
        if (geiger == null) return;
        BlockState state = geiger.defaultBlockState();
        Direction facing = direction(cell.special.facing, Direction.SOUTH);
        state = applyFacing(state, facing);
        setBlockSafe(level, pos, state);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static BlockState apply(BlockState state, Cell cell) {
        if (cell.facing != null) {
            state = applyFacing(state, direction(cell.facing, Direction.NORTH));
        }
        if (cell.half != null) {
            Half half = "top".equals(cell.half) ? Half.TOP : Half.BOTTOM;
            if (state.hasProperty(StairBlock.HALF)) state = state.setValue(StairBlock.HALF, half);
            if (state.hasProperty(TrapDoorBlock.HALF)) state = state.setValue(TrapDoorBlock.HALF, half);
            if (state.hasProperty(BlockStateProperties.SLAB_TYPE)) {
                state = state.setValue(BlockStateProperties.SLAB_TYPE, half == Half.TOP ? SlabType.TOP : SlabType.BOTTOM);
            }
        }
        if (cell.open != null && state.hasProperty(TrapDoorBlock.OPEN)) {
            state = state.setValue(TrapDoorBlock.OPEN, cell.open);
        }
        if (cell.level != null && state.hasProperty(BlockSellafield.LEVEL)) {
            int lv = Math.max(0, Math.min(5, cell.level));
            state = state.setValue(BlockSellafield.LEVEL, lv);
        }
        if (cell.orient != null && state.hasProperty(BlockScaffold.ORIENT)) {
            for (BlockScaffold.Orient value : BlockScaffold.Orient.values()) {
                if (value.getSerializedName().equals(cell.orient)) {
                    state = state.setValue(BlockScaffold.ORIENT, value);
                    break;
                }
            }
        }
        return state;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static BlockState applyFacing(BlockState state, Direction facing) {
        for (Property property : state.getProperties()) {
            String name = property.getName();
            if (("facing".equals(name) || "horizontal_facing".equals(name)) && property.getPossibleValues().contains(facing)) {
                return state.setValue(property, facing);
            }
        }
        return state;
    }

    private static Direction direction(String name, Direction fallback) {
        if (name == null) return fallback;
        try {
            return Direction.valueOf(name.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return fallback;
        }
    }

    private static Block block(String id) {
        ResourceLocation loc = ResourceLocation.parse(id);
        return BuiltInRegistries.BLOCK.getOptional(loc).orElse(null);
    }

    private static Schematic load(String name) {
        String path = "/data/hbm/schematic/" + name + ".json";
        InputStream in = CeSchematicPlacer.class.getResourceAsStream(path);
        if (in == null) {
            throw new IllegalStateException("Missing CE schematic " + path);
        }
        try (InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            JsonArray paletteJson = root.getAsJsonArray("palette");
            String[] palette = new String[paletteJson.size()];
            for (int i = 0; i < palette.length; i++) {
                palette[i] = paletteJson.get(i).getAsString();
            }
            JsonArray blocks = root.getAsJsonArray("blocks");
            List<Cell> cells = new ArrayList<>(blocks.size());
            for (JsonElement el : blocks) {
                JsonArray rec = el.getAsJsonArray();
                int x = rec.get(0).getAsInt();
                int y = rec.get(1).getAsInt();
                int z = rec.get(2).getAsInt();
                String id = palette[rec.get(3).getAsInt()];
                String facing = null;
                String half = null;
                Boolean open = null;
                Integer level = null;
                String orient = null;
                Special special = null;
                if (rec.size() > 4) {
                    JsonObject extra = rec.get(4).getAsJsonObject();
                    if (extra.has("f")) facing = extra.get("f").getAsString();
                    if (extra.has("h")) half = extra.get("h").getAsString();
                    if (extra.has("o")) open = extra.get("o").getAsBoolean();
                    if (extra.has("l")) level = extra.get("l").getAsInt();
                    if (extra.has("or")) orient = extra.get("or").getAsString();
                    if (extra.has("s")) {
                        JsonObject s = extra.getAsJsonObject("s");
                        special = new Special(
                                s.get("t").getAsString(),
                                s.has("pool") ? s.get("pool").getAsString() : "POOL_GENERIC",
                                s.has("rolls") ? s.get("rolls").getAsInt() : 8,
                                s.has("rand") ? s.get("rand").getAsInt() : 0,
                                s.has("base") ? s.get("base").getAsInt() : 0,
                                s.has("facing") ? s.get("facing").getAsString() : facing,
                                s.has("hinge") ? s.get("hinge").getAsString() : "left",
                                s.has("item") ? s.get("item").getAsString() : null,
                                s.has("chance") ? s.get("chance").getAsInt() : 0);
                    }
                }
                cells.add(new Cell(x, y, z, id, facing, half, open, level, orient, special));
            }
            MainRegistry.logger.info("Loaded CE schematic {} ({} cells)", name, cells.size());
            return new Schematic(cells);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse CE schematic " + path, e);
        }
    }

    private record Schematic(List<Cell> cells) {
    }

    private record Cell(int x, int y, int z, String blockId, String facing, String half, Boolean open, Integer level, String orient, Special special) {
    }

    private record Special(String type, String pool, int rolls, int rand, int base, String facing, String hinge, String item, int chance) {
    }
}
