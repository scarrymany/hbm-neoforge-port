package com.hbm.blocks.generic;

import com.hbm.blockentity.ITickableBE;
import com.hbm.config.MobConfig;
import com.hbm.entity.mob.glyphid.EntityGlyphid;
import com.hbm.entity.mob.glyphid.EntityGlyphidBehemoth;
import com.hbm.entity.mob.glyphid.EntityGlyphidBlaster;
import com.hbm.entity.mob.glyphid.EntityGlyphidBombardier;
import com.hbm.entity.mob.glyphid.EntityGlyphidBrawler;
import com.hbm.entity.mob.glyphid.EntityGlyphidBrenda;
import com.hbm.entity.mob.glyphid.EntityGlyphidDigger;
import com.hbm.entity.mob.glyphid.EntityGlyphidNuclear;
import com.hbm.entity.mob.glyphid.EntityGlyphidScout;
import com.hbm.entity.mob.glyphid.GlyphidEntityTypes;
import com.hbm.handler.pollution.PollutionHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

/**
 * CE {@code BlockGlyphidSpawner.TileEntityGlyphidSpawner} ({@code BlockGlyphidSpawner.java}:112-205).
 */
public class GlyphidSpawnerBlockEntity extends BlockEntity implements ITickableBE {

    private static final List<SpawnEntry> SPAWN_MAP = List.of(
            new SpawnEntry((l, t) -> new EntityGlyphid(GlyphidEntityTypes.GLYPHID.get(), l), MobConfig::glyphidChance),
            new SpawnEntry((l, t) -> new EntityGlyphidBombardier(GlyphidEntityTypes.BOMBARDIER.get(), l), MobConfig::bombardierChance),
            new SpawnEntry((l, t) -> new EntityGlyphidBrawler(GlyphidEntityTypes.BRAWLER.get(), l), MobConfig::brawlerChance),
            new SpawnEntry((l, t) -> new EntityGlyphidDigger(GlyphidEntityTypes.DIGGER.get(), l), MobConfig::diggerChance),
            new SpawnEntry((l, t) -> new EntityGlyphidBlaster(GlyphidEntityTypes.BLASTER.get(), l), MobConfig::blasterChance),
            new SpawnEntry((l, t) -> new EntityGlyphidBehemoth(GlyphidEntityTypes.BEHEMOTH.get(), l), MobConfig::behemothChance),
            new SpawnEntry((l, t) -> new EntityGlyphidBrenda(GlyphidEntityTypes.BRENDA.get(), l), MobConfig::brendaChance),
            new SpawnEntry((l, t) -> new EntityGlyphidNuclear(GlyphidEntityTypes.NUCLEAR.get(), l), MobConfig::johnsonChance)
    );

    private boolean initialSpawn = true;

    public GlyphidSpawnerBlockEntity(BlockPos pos, BlockState state) {
        super(PlantBlocks.GLYPHID_SPAWNER_ENTITY_TYPE.get(), pos, state);
    }

    @Override
    public void updateEntity() {
        if (level == null || level.isClientSide || level.getDifficulty() == Difficulty.PEACEFUL) return;
        int cooldown = MobConfig.SWARM_COOLDOWN_SECONDS.get() * 20;
        if (!initialSpawn && level.getGameTime() % Math.max(1, cooldown) != 0) return;
        initialSpawn = false;

        AABB far = new AABB(worldPosition).inflate(96);
        if (level.getEntitiesOfClass(EntityGlyphid.class, far).size() >= MobConfig.SPAWN_MAX.get().intValue()) {
            return;
        }

        AABB near = new AABB(
                worldPosition.getX() - 5, worldPosition.getY() + 1, worldPosition.getZ() - 5,
                worldPosition.getX() + 6, worldPosition.getY() + 7, worldPosition.getZ() + 6);
        List<EntityGlyphid> nearby = level.getEntitiesOfClass(EntityGlyphid.class, near);
        float soot = PollutionHandler.getPollution(level, worldPosition, PollutionHandler.PollutionType.SOOT);
        int subtype = getBlockState().getValue(BlockGlyphidSpawner.TYPE).ordinal();
        if (nearby.size() <= 3 || subtype == EntityGlyphid.TYPE_RADIOACTIVE) {
            for (EntityGlyphid glyphid : createSwarm(soot, subtype)) {
                trySpawn(glyphid);
            }
            if (level.random.nextInt(MobConfig.effectiveScoutSwarmSpawnChance() + 1) == 0
                    && soot >= MobConfig.effectiveScoutThreshold()
                    && subtype != EntityGlyphid.TYPE_RADIOACTIVE) {
                EntityGlyphidScout scout = new EntityGlyphidScout(GlyphidEntityTypes.SCOUT.get(), level);
                if (subtype == 1) scout.getEntityData().set(EntityGlyphid.SUBTYPE, EntityGlyphid.TYPE_INFECTED);
                trySpawn(scout);
            }
        }
    }

    private void trySpawn(EntityGlyphid glyphid) {
        double offsetX = glyphid.getRandom().nextGaussian() * 3;
        double offsetZ = glyphid.getRandom().nextGaussian() * 3;
        for (int i = 0; i < 7; i++) {
            glyphid.moveTo(worldPosition.getX() + 0.5 + offsetX, worldPosition.getY() - 2 + i,
                    worldPosition.getZ() + 0.5 + offsetZ, level.random.nextFloat() * 360.0F, 0.0F);
            if (level.noCollision(glyphid)) {
                level.addFreshEntity(glyphid);
                return;
            }
        }
    }

    private ArrayList<EntityGlyphid> createSwarm(float soot, int meta) {
        ArrayList<EntityGlyphid> current = new ArrayList<>();
        int swarmAmount = (int) Math.min(
                MobConfig.BASE_SWARM_SIZE.get() * Math.max(MobConfig.SWARM_SCALING_MULT.get() * (soot / MobConfig.SOOT_STEP.get()), 1),
                10);
        int cap = 100;
        while (current.size() <= swarmAmount && cap >= 0) {
            for (SpawnEntry entry : SPAWN_MAP) {
                int[] chance = entry.chance().get();
                int adjusted = (int) (chance[0] + (chance[1] - chance[1] / Math.max(((soot + 1) / 3), 1)));
                if (soot >= chance[2] && level.random.nextInt(100) <= adjusted) {
                    EntityGlyphid entity = entry.factory().apply(level, null);
                    if (meta == 1) entity.getEntityData().set(EntityGlyphid.SUBTYPE, EntityGlyphid.TYPE_INFECTED);
                    if (meta == 2) entity.getEntityData().set(EntityGlyphid.SUBTYPE, EntityGlyphid.TYPE_RADIOACTIVE);
                    current.add(entity);
                }
            }
            cap--;
        }
        return current;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("initialSpawn", initialSpawn);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        initialSpawn = tag.getBoolean("initialSpawn");
    }

    private record SpawnEntry(BiFunction<Level, EntityType<?>, EntityGlyphid> factory, java.util.function.Supplier<int[]> chance) {
    }
}
