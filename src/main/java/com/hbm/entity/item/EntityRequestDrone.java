package com.hbm.entity.item;

import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.RecipesCommon.OreDictStack;
import com.hbm.items.tool.CouplingToolItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Ported from CE's {@code com.hbm.entity.item.EntityRequestDrone} (285 lines, read in full,
 * CE's own {@code @Spaghetti("onUpdate needs to be cleaned up")}) - the "on-demand fetch" variant of
 * {@link EntityDroneBase}, per {@code docs/phase4/entities_vehicles_aircraft.md}'s "Logistics-drone
 * entity family" section.
 * <p>
 * <b>Instruction-queue executor, implemented in full; block-network dispatch degrades gracefully.</b>
 * This class's {@link #program} queue, its "only advance once stopped" gate, and its 4-block
 * downward raytrace at each pickup/dropoff/dock waypoint are all ported faithfully below. What each
 * raytrace *does* with a hit, though, is CE reaching directly into the not-yet-ported block/GUI half
 * of this subsystem ({@code TileEntityDroneProvider}/{@code Requester}/{@code Dock}, confirmed absent
 * from this port by repo-wide search) - per this task's own explicit boundary, no placeholder classes
 * for those are invented here. Each of {@link #tryPickup}, {@link #tryUnload}, {@link #tryDock} still
 * performs the real raytrace and block-entity lookup, but the actual transfer only happens behind an
 * {@code instanceof} check against a real class that does not exist yet; until the still-unclaimed
 * Phase 2 "drone logistics" package (see {@code docs/phase2/
 * items_tool_machine_coupling_and_recipe_system.md}) lands those block entities, every lookup falls
 * through its documented TODO as a no-op - {@link #tryDock} in particular always reaches CE's own
 * "couldn't dock" fallback (drop cargo, self-destruct), exactly as it would in real CE if no dock were
 * in range.
 * <p>
 * <b>{@code AStack} program entries adapted to this port's own real (meta-less) {@code RecipesCommon}
 * shape.</b> CE's NBT round-trip for a queued "go pick up this item type" instruction stores a numeric
 * {@code Item} id plus a {@code meta} short for its {@code ComparableStack}/{@code OreDictStack}
 * variants. This port's own already-committed {@link ComparableStack}/{@link OreDictStack} (read in
 * full) dropped {@code meta} entirely (1.21 items have no damage-value subtypes) and key
 * {@link OreDictStack} by a real {@link TagKey} rather than a raw ore-dictionary name string - so the
 * NBT shape here round-trips a registry-name string (matching this port's own established
 * {@code BuiltInRegistries.ITEM.getKey}/{@code ResourceLocation.parse} idiom, e.g.
 * {@code EntityGrenadeImpactGeneric}) and a tag location string, respectively, rather than CE's
 * numeric id/meta pair.
 * <p>
 * <b>CE's own literal control-flow quirks, preserved rather than patched</b> (matching this class's
 * own {@code @Spaghetti} self-assessment, and this task's "preserve exactly" instruction): a program
 * instruction is popped off the queue *before* its own guard condition is checked (e.g. an
 * {@link AStack} instruction popped while {@link #heldItem} is already non-empty is silently discarded
 * with no effect and no cooldown, rather than being retried or skipped-with-cooldown); the
 * {@link #nextActionTimer} cooldown is set only by the {@code AStack}/{@code UNLOAD} branches, not by
 * plain waypoint or {@code DOCK} instructions; and CE's own redundant re-read/re-write of the
 * (already-{@code EntityDroneBase}-persisted) appearance byte inside this subclass's own NBT methods
 * is dropped as pure dead weight (the base class's {@code super} call already round-trips it).
 */
public class EntityRequestDrone extends EntityDroneBase {

    public ItemStack heldItem = ItemStack.EMPTY;
    public final List<Object> program = new ArrayList<>();
    private int nextActionTimer = 0;

    public enum DroneProgram {
        UNLOAD, DOCK
    }

    public EntityRequestDrone(EntityType<? extends EntityRequestDrone> type, Level level) {
        super(type, level);
    }

    /** CE: {@code setTarget} override adds a flat {@code +1} to the target Y - preserved exactly. */
    @Override
    public void setTarget(double x, double y, double z) {
        this.targetX = x;
        this.targetY = y + 1;
        this.targetZ = z;
    }

    /**
     * CE: {@code hitByEntity(Entity)} - any player hit destroys this drone outright, dropping its
     * held cargo (if any) plus a {@code drone} item. See {@link EntityDeliveryDrone#hurt} for the
     * identical 1.21.1 substitution rationale, including why this checks
     * {@link DamageSource#getDirectEntity()} (melee-only, matching CE's actual trigger scope) rather
     * than {@link DamageSource#getEntity()}.
     */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (this.isRemoved()) return false;

        if (!this.level().isClientSide() && source.getDirectEntity() instanceof Player) {
            if (!this.heldItem.isEmpty()) {
                this.spawnAtLocation(this.heldItem, 1.0F);
            }
            this.spawnAtLocation(new ItemStack(CouplingToolItems.DRONE_REQUEST.get()), 1.0F);
            this.discard();
        }

        return true;
    }

    @Override
    public double getSpeed() {
        return 0.625D;
    }

    @Override
    public void tick() {
        if (!this.level().isClientSide()) {
            // CE checks the motion carried over from the end of the previous tick (this tick's own
            // EntityDroneBase.tick() below hasn't recomputed it yet) - "arrived and stopped" gate.
            if (this.getDeltaMovement().lengthSqr() < 0.0001D) {
                if (this.nextActionTimer > 0) {
                    this.nextActionTimer--;
                } else if (this.program.isEmpty()) {
                    // CE: self-destructs with no further operations pending - this is the one branch
                    // that skips super.onUpdate()/tick() entirely (an explicit `return` in CE).
                    this.discard();
                    this.spawnAtLocation(new ItemStack(CouplingToolItems.DRONE_REQUEST.get()), 1.0F);
                    return;
                } else {
                    this.runNextProgramStep();
                }
            }
        }
        super.tick();
    }

    private void runNextProgramStep() {
        Object next = this.program.remove(0);

        if (next instanceof BlockPos pos) {
            this.setTarget(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        } else if (next instanceof AStack aStack && this.heldItem.isEmpty()) {
            this.tryPickup(aStack);
            this.nextActionTimer = 5;
        } else if (next == DroneProgram.UNLOAD && !this.heldItem.isEmpty()) {
            this.tryUnload();
            this.nextActionTimer = 5;
        } else if (next == DroneProgram.DOCK) {
            this.tryDock();
        }
        // Any other combination (e.g. an AStack popped while already holding an item) is silently
        // dropped with no effect and no cooldown - CE's own literal behavior, see class javadoc.
    }

    private void tryPickup(AStack aStack) {
        BlockHitResult hit = this.rayTraceDown(4.0D);

        if (hit.getType() == HitResult.Type.BLOCK) {
            BlockEntity blockEntity = this.level().getBlockEntity(hit.getBlockPos());

            // TODO(cross-area follow-up, Phase 2 "drone logistics" package per docs/phase2/
            // items_tool_machine_coupling_and_recipe_system.md - still unwritten): once
            // com.hbm.blockentity.network.DroneProviderBlockEntity (CE: TileEntityDroneProvider)
            // exists, dispatch on `blockEntity instanceof DroneProviderBlockEntity provider` here,
            // scan its inventory for the first stack `aStack.matchesRecipe(stack, true)` accepts, and
            // pull it into `heldItem` (setAppearance(1), play the pickup sound, clear the provider's
            // slot, markDirty). No recognized drone-network block entity exists yet, so this always
            // degrades to a no-op - see EntityDroneBase's own class javadoc for the still-unclaimed
            // package boundary.
            if (blockEntity != null) {
                // no-op: see TODO above.
            }
        }
    }

    private void tryUnload() {
        BlockHitResult hit = this.rayTraceDown(4.0D);

        if (hit.getType() == HitResult.Type.BLOCK) {
            BlockEntity blockEntity = this.level().getBlockEntity(hit.getBlockPos());

            // TODO(cross-area follow-up, Phase 2 "drone logistics" package): once
            // com.hbm.blockentity.network.DroneRequesterBlockEntity (CE: TileEntityDroneRequester)
            // exists, dispatch on `blockEntity instanceof DroneRequesterBlockEntity requester` here
            // and merge `heldItem` into its slots 9-17 (CE's own input-buffer range), clearing
            // `heldItem`/resetting the cosmetic appearance once fully unloaded, markDirty. No
            // recognized drone-network block entity exists yet, so this always degrades to a no-op.
            if (blockEntity != null) {
                // no-op: see TODO above.
            }
        }

        // CE sets this unconditionally after the whole raytrace/transfer attempt, hit or miss.
        this.nextActionTimer = 5;
    }

    private void tryDock() {
        BlockHitResult hit = this.rayTraceDown(4.0D);
        boolean docked = false;

        if (hit.getType() == HitResult.Type.BLOCK) {
            BlockEntity blockEntity = this.level().getBlockEntity(hit.getBlockPos());

            // TODO(cross-area follow-up, Phase 2 "drone logistics" package): once
            // com.hbm.blockentity.network.DroneDockBlockEntity (CE: TileEntityDroneDock) exists,
            // dispatch on `blockEntity instanceof DroneDockBlockEntity dock` here, try to slot this
            // drone (plus any held cargo, into the next free/adjacent slot) into its inventory, and
            // set `docked = true` on success. No recognized drone-network block entity exists yet, so
            // this always falls through to the "couldn't dock" branch below - the exact behavior real
            // CE has whenever no dock happens to be in range either.
            if (blockEntity != null) {
                // no-op: see TODO above.
            }
        }

        if (!docked) {
            this.discard();
            if (!this.heldItem.isEmpty()) {
                this.spawnAtLocation(this.heldItem, 1.0F);
            }
            this.spawnAtLocation(new ItemStack(CouplingToolItems.DRONE_REQUEST.get()), 1.0F);
        }
    }

    private BlockHitResult rayTraceDown(double distance) {
        Vec3 start = this.position();
        Vec3 end = start.add(0.0D, -distance, 0.0D);
        return this.level().clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);

        if (!this.heldItem.isEmpty()) {
            tag.put("held", this.heldItem.save(this.registryAccess()));
        }

        int size = this.program.size();
        tag.putInt("programSize", size);

        for (int i = 0; i < size; i++) {
            CompoundTag data = new CompoundTag();
            Object p = this.program.get(i);

            if (p instanceof BlockPos pos) {
                data.putString("type", "pos");
                data.putIntArray("pos", new int[]{pos.getX(), pos.getY(), pos.getZ()});
            } else if (p instanceof ComparableStack comp) {
                data.putString("type", "comp");
                ResourceLocation id = comp.item == null ? null : BuiltInRegistries.ITEM.getKey(comp.item);
                data.putString("item", id == null ? "" : id.toString());
            } else if (p instanceof OreDictStack dict) {
                data.putString("type", "dict");
                data.putString("tag", dict.tag.location().toString());
            } else if (p == DroneProgram.UNLOAD) {
                data.putString("type", "unload");
            } else if (p == DroneProgram.DOCK) {
                data.putString("type", "dock");
            }

            tag.put("program" + i, data);
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);

        if (tag.contains("held", Tag.TAG_COMPOUND)) {
            this.heldItem = ItemStack.parseOptional(this.registryAccess(), tag.getCompound("held"));
        }

        // CE always resets this to 5 on load, regardless of whatever value it had before saving.
        this.nextActionTimer = 5;

        this.program.clear();
        int size = tag.getInt("programSize");

        for (int i = 0; i < size; i++) {
            CompoundTag data = tag.getCompound("program" + i);
            String type = data.getString("type");

            switch (type) {
                case "pos" -> {
                    int[] pos = data.getIntArray("pos");
                    if (pos.length == 3) this.program.add(new BlockPos(pos[0], pos[1], pos[2]));
                }
                case "unload" -> this.program.add(DroneProgram.UNLOAD);
                case "dock" -> this.program.add(DroneProgram.DOCK);
                case "comp" -> {
                    String raw = data.getString("item");
                    if (!raw.isEmpty()) {
                        Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(raw));
                        this.program.add(new ComparableStack(item, 1));
                    }
                }
                case "dict" -> {
                    String raw = data.getString("tag");
                    if (!raw.isEmpty()) {
                        TagKey<Item> tagKey = TagKey.create(Registries.ITEM, ResourceLocation.parse(raw));
                        this.program.add(new OreDictStack(tagKey));
                    }
                }
                default -> {
                }
            }
        }
    }
}
