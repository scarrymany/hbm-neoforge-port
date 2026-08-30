package com.hbm.explosion;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.UUID;

/**
 * Ported from CE's {@code com.hbm.explosion.ExplosionFleija} (114 lines, read in full) - the
 * antimatter column-carving variant {@code EntityNukeExplosionMK3} picks for {@code extType == 0}.
 * <p>
 * <b>Not ported</b>: CE's {@code CompatDynamicTrees.destroyTreeAt} compat hook (no consumer mod
 * present in this port) and its {@code DecoBlockAlt} instanceof guard (that decorative-block base
 * class doesn't exist in this port yet) - both dropped rather than guessed at. Net effect: this
 * port's Fleija column removes a (small) superset of what CE's would (a few extra decorative-block
 * positions that CE would have preserved), a documented, low-risk gap rather than a silent one.
 */
public class ExplosionFleija {

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
    public float explosionCoefficient = 1.0F;
    public float explosionCoefficient2 = 1.0F;
    public UUID detonator;

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
        nbt.putFloat(name + "explosionCoefficient", explosionCoefficient);
        nbt.putFloat(name + "explosionCoefficient2", explosionCoefficient2);
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
        explosionCoefficient = nbt.getFloat(name + "explosionCoefficient");
        explosionCoefficient2 = nbt.getFloat(name + "explosionCoefficient2");
    }

    public ExplosionFleija(int x, int y, int z, Level level, int rad, float coefficient, float coefficient2) {
        this.posX = x;
        this.posY = y;
        this.posZ = z;

        this.levelObj = level;

        this.radius = rad;
        this.radius2 = this.radius * this.radius;

        this.explosionCoefficient = coefficient;
        this.explosionCoefficient2 = coefficient2;

        this.nlimit = this.radius2 * 4;
    }

    public boolean update() {
        breakColumn(this.lastposX, this.lastposZ);
        this.shell = (int) Math.floor((Math.sqrt(n) + 1) / 2);
        if (shell == 0) shell = 1; // CE: guards a division-by-zero someone reported in the wild
        int shell2 = this.shell * 2;
        this.leg = (int) Math.floor((this.n - (shell2 - 1) * (shell2 - 1)) / shell2);
        this.element = (this.n - (shell2 - 1) * (shell2 - 1)) - shell2 * this.leg - this.shell + 1;
        this.lastposX = this.leg == 0 ? this.shell : this.leg == 1 ? -this.element : this.leg == 2 ? -this.shell : this.element;
        this.lastposZ = this.leg == 0 ? this.element : this.leg == 1 ? this.shell : this.leg == 2 ? -this.element : -this.shell;
        this.n++;
        return this.n > this.nlimit;
    }

    private void breakColumn(int x, int z) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int dist = this.radius2 - (x * x + z * z);
        if (dist > 0) {
            dist = (int) Math.sqrt(dist);
            for (int y = (int) (dist / this.explosionCoefficient2); y > -dist / this.explosionCoefficient; y--) {
                pos.set(this.posX + x, this.posY + y, this.posZ + z);
                BlockState state = this.levelObj.getBlockState(pos);
                boolean nearBedrockLike = state.getBlock().getExplosionResistance() > 2_000_000 && this.posY + y <= 0;
                // TODO(DecoBlockAlt): CE also spares any block that is an instance of its
                // DecoBlockAlt decorative-block base class here; that class doesn't exist in this
                // port yet, so that exclusion is dropped rather than guessed at (documented,
                // low-risk over-destruction gap: this port's Fleija may remove a few decorative
                // blocks CE's would have spared).
                if (!nearBedrockLike) {
                    this.levelObj.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                }
            }
        }
    }
}
