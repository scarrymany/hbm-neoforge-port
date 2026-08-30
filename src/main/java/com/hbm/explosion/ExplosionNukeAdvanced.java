package com.hbm.explosion;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;

import java.util.UUID;

/**
 * Ported from CE's {@code com.hbm.explosion.ExplosionNukeAdvanced} (156 lines, read in full) - the
 * canonical shape of the "column-carving" family (see {@code docs/phase3/explosion_engine.md}):
 * one {@link #update()} call carves exactly one {@code (x, z)} column, walked in roughly-increasing-
 * radius order via an Ulam-spiral (shell/leg/element) iterator. {@code EntityNukeExplosionMK3}'s
 * "waste" path runs 3 instances of this simultaneously ({@code type} 0 = crater/{@link
 * ExplosionNukeGeneric#destruction}, 1 = vapor/{@link ExplosionNukeGeneric#vaporDest}, 2 = waste/
 * {@link ExplosionNukeGeneric#wasteDest}) at radii {@code r}/{@code r*1.8}/{@code r*2.5}.
 * <p>
 * Mechanically portable 1:1 - no threading, no chunk-batching in CE itself either (per the research
 * report, this family never groups writes by chunk, on 1.12 or here), so no batching is added here.
 * CE's {@code isWarDim} gate is dropped per this port's documented always-true default.
 */
public class ExplosionNukeAdvanced {

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
    public int type = 0;
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
        nbt.putFloat(name + "explosionCoefficient", explosionCoefficient);
        nbt.putInt(name + "type", type);
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
        type = nbt.getInt(name + "type");
    }

    public ExplosionNukeAdvanced(int x, int y, int z, Level level, int rad, float coefficient, int typ) {
        this.posX = x;
        this.posY = y;
        this.posZ = z;

        this.levelObj = level;

        this.radius = rad;
        this.radius2 = this.radius * this.radius;

        // scale the coefficient depending on detonation height
        this.explosionCoefficient = Math.min(Math.max((rad + coefficient * (y - 60)) / (coefficient * rad), 1 / coefficient), 1.0f);
        this.type = typ;

        // How many total columns should be broken (radius^2 is one quadrant, there are 4 quadrants)
        this.nlimit = this.radius2 * 4;
    }

    public boolean update() {
        switch (this.type) {
            case 0 -> breakColumn(this.lastposX, this.lastposZ);
            case 1 -> vapor(this.lastposX, this.lastposZ);
            case 2 -> waste(this.lastposX, this.lastposZ);
            default -> {
            }
        }
        this.shell = (int) Math.floor((Math.sqrt(n) + 1) / 2); // crazy stuff I can't explain (CE's own comment)
        int shell2 = this.shell * 2;
        this.leg = (int) Math.floor((this.n - (shell2 - 1) * (shell2 - 1)) / shell2);
        this.element = (this.n - (shell2 - 1) * (shell2 - 1)) - shell2 * this.leg - this.shell + 1;
        this.lastposX = this.leg == 0 ? this.shell : this.leg == 1 ? -this.element : this.leg == 2 ? -this.shell : this.element;
        this.lastposZ = this.leg == 0 ? this.element : this.leg == 1 ? this.shell : this.leg == 2 ? -this.element : -this.shell;
        this.n++;
        return this.n > this.nlimit; // whether we are done or not
    }

    private void breakColumn(int x, int z) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int dist = this.radius2 - (x * x + z * z); // third leg of the right triangle (hypotenuse = radius)
        if (dist > 0) {
            dist = (int) Math.sqrt(dist); // sphere height at this (x,z)
            for (int y = dist; y > -dist * this.explosionCoefficient; y--) { // top to bottom, favors light updates
                pos.set(this.posX + x, this.posY + y, this.posZ + z);
                if (y < 8) { // only spare blocks mostly below the epicenter
                    y -= ExplosionNukeGeneric.destruction(this.levelObj, pos);
                } else { // never spare blocks above the epicenter
                    ExplosionNukeGeneric.destruction(this.levelObj, pos);
                }
            }
        }
    }

    private void vapor(int x, int z) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int dist = this.radius2 - (x * x + z * z);
        if (dist > 0) {
            dist = (int) Math.sqrt(dist);
            for (int y = dist; y > -dist * this.explosionCoefficient; y--) {
                pos.set(this.posX + x, this.posY + y, this.posZ + z);
                y -= ExplosionNukeGeneric.vaporDest(this.levelObj, pos);
            }
        }
    }

    private void waste(int x, int z) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int dist = this.radius2 - (x * x + z * z);
        if (dist > 0) {
            dist = (int) Math.sqrt(dist);
            for (int y = dist; y > -dist * this.explosionCoefficient; y--) {
                pos.set(this.posX + x, this.posY + y, this.posZ + z);
                // CE picks wasteDestNoSchrab below radius 95 (small-nuke schrabidium-ore exclusion);
                // that variant isn't ported in this pass (no consumer among the required entities -
                // see this pass's structured output), so wasteDest is used unconditionally here.
                ExplosionNukeGeneric.wasteDest(this.levelObj, pos);
            }
        }
    }
}
