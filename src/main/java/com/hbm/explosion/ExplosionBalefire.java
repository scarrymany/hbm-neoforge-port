package com.hbm.explosion;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * Ported from CE's {@code com.hbm.explosion.ExplosionBalefire} (144 lines, read in full) - the
 * standalone digging-column explosion {@code EntityBalefire} wraps (no multi-instance triple, no
 * fallout spawn, confirmed by reading that entity in full).
 * <p>
 * <b>Not ported</b>: CE's {@code ModBlocks.block_schrabidium_cluster}/{@code balefire}/{@code
 * block_euphemium_cluster}/{@code sellafield_slaked} special-casing (the eternal-fire placement
 * above the crater, the schrabidium→euphemium cluster swap, and the sellafield-slaked crater
 * floor) - none of those 4 blocks are registered in this port yet (confirmed absent from {@code
 * ModBlocks.java}; Phase 1/4 content). Each is a real, separately-tracked forward reference,
 * marked at its exact original call site - the crater-digging behavior itself (the actual
 * "balefire eats a hole in the ground" gameplay effect) is fully ported and functional.
 */
public class ExplosionBalefire {

    public int posX;
    public int posY;
    public int posZ;
    public int lastposX = 0;
    public int lastposZ = 0;
    public int radius;
    public int radius2;
    public Level levelObj;
    private int n = 1;
    private int nlimit;
    private int shell;
    private int leg;
    private int element;
    @Nullable
    public UUID detonator = null;

    public void saveToNbt(CompoundTag nbt, String name) {
        nbt.putInt(name + "posX", posX);
        nbt.putInt(name + "posY", posY);
        nbt.putInt(name + "posZ", posZ);
        nbt.putInt(name + "lastposX", lastposX);
        nbt.putInt(name + "lastposZ", lastposZ);
        nbt.putInt(name + "radius", radius);
        nbt.putInt(name + "radius2", radius2);
        nbt.putInt(name + "n", n);
        nbt.putInt(name + "nlimit", nlimit);
        nbt.putInt(name + "shell", shell);
        nbt.putInt(name + "leg", leg);
        nbt.putInt(name + "element", element);
        if (detonator != null) nbt.putUUID(name + "detonator", detonator);
    }

    public void readFromNbt(CompoundTag nbt, String name) {
        posX = nbt.getInt(name + "posX");
        posY = nbt.getInt(name + "posY");
        posZ = nbt.getInt(name + "posZ");
        lastposX = nbt.getInt(name + "lastposX");
        lastposZ = nbt.getInt(name + "lastposZ");
        radius = nbt.getInt(name + "radius");
        radius2 = nbt.getInt(name + "radius2");
        n = nbt.getInt(name + "n");
        nlimit = nbt.getInt(name + "nlimit");
        shell = nbt.getInt(name + "shell");
        leg = nbt.getInt(name + "leg");
        element = nbt.getInt(name + "element");
        if (nbt.hasUUID(name + "detonator")) detonator = nbt.getUUID(name + "detonator");
    }

    public ExplosionBalefire(int x, int y, int z, Level level, int rad) {
        this.posX = x;
        this.posY = y;
        this.posZ = z;

        this.levelObj = level;

        this.radius = rad;
        this.radius2 = this.radius * this.radius;

        this.nlimit = this.radius2 * 4;
    }

    public boolean update() {
        breakColumn(this.lastposX, this.lastposZ);
        this.shell = (int) Math.floor((Math.sqrt(n) + 1) / 2);
        if (shell == 0) shell = 1;
        int shell2 = this.shell * 2;
        this.leg = (int) Math.floor((this.n - (shell2 - 1) * (shell2 - 1)) / shell2);
        this.element = (this.n - (shell2 - 1) * (shell2 - 1)) - shell2 * this.leg - this.shell + 1;
        this.lastposX = this.leg == 0 ? this.shell : this.leg == 1 ? -this.element : this.leg == 2 ? -this.shell : this.element;
        this.lastposZ = this.leg == 0 ? this.element : this.leg == 1 ? this.shell : this.leg == 2 ? -this.element : -this.shell;
        this.n++;
        return this.n > this.nlimit;
    }

    private void breakColumn(int x, int z) {
        int dist = (int) (radius - Math.sqrt(x * x + z * z));
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        if (dist > 0) {
            int pX = posX + x;
            int pZ = posZ + z;

            int y = levelObj.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE, pX, pZ);

            int maxdepth = (int) (10 + radius * 0.25);
            int depth = (int) ((maxdepth * dist / (double) radius) + (Math.sin(dist * 0.15 + 2) * 2));

            depth = Math.max(y - depth, levelObj.getMinBuildHeight());

            while (y > depth) {
                // TODO(ModBlocks.block_schrabidium_cluster/balefire/block_euphemium_cluster): CE
                // checks for a schrabidium cluster block here and, on a 1-in-10 chance, places its
                // eternal "balefire" fire block above it and swaps it for a euphemium cluster
                // instead of digging further down; none of those 3 blocks are registered in this
                // port yet - this check is skipped, so digging always proceeds straight to depth.
                levelObj.setBlock(pos.set(pX, y, pZ), Blocks.AIR.defaultBlockState(), 3);
                y--;
            }

            // TODO(ModBlocks.balefire/block_schrabidium_cluster/block_euphemium_cluster): CE has a
            // 1-in-10 chance to place its eternal fire block at the crater floor here too, with the
            // same schrabidium->euphemium swap check; skipped for the same reason as above.

            // TODO(ModBlocks.sellafield_slaked): CE also turns any stone-like block within 5
            // layers of the crater floor into a slaked-sellafield hazard block here; not
            // registered in this port yet - skipped.
        }
    }
}
