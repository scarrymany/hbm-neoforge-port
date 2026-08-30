package com.hbm.handler;

import com.hbm.items.weapon.ItemMissile;

import javax.annotation.Nullable;

/**
 * Ported from CE's {@code com.hbm.handler.MissileStruct} (172 lines, read in full) - the transient
 * (warhead, fuselage, fins, thruster) holder consumed by {@code ItemCustomMissile.getStruct}/
 * {@code EntityMissileCustom}'s construction path. No {@code chip} field, matching CE exactly (see
 * {@code docs/phase3/missile_framework.md}'s Open questions - {@code ItemCustomMissile.getStruct}
 * never reads chip back either, a confirmed-real CE asymmetry, not a bug this port should "fix").
 * <p>
 * CE's version additionally carried a Forge-1.12 {@code DataSerializer<MissileStruct>} (for
 * {@code EntityDataManager} sync) and a hand-rolled {@code ByteBuf} (de)serialization pair (for
 * {@code TEMissileMultipartPacket}, the {@code TileEntityMachineMissileAssembly} live-preview
 * broadcast). Neither is ported here: both consumers are the missile-assembly machine and its GUI,
 * which are explicitly out of this pass's scope (see the missile-framework package's task brief -
 * "your job stops at the missile entity exists, flies, and explodes correctly when spawned
 * programmatically"). A future assembly-machine pass adds a {@code CustomPacketPayload} record here
 * (or wherever that machine's own package lives) following {@code com.hbm.packet.toclient.BufPacket}'s
 * established pattern - this record only needs to gain fields, not a rewritten shape, when that
 * lands.
 * <p>
 * Plain {@code record} instead of a hand-written class: {@link Object#equals}/{@link
 * Object#hashCode} fall out for free with the same field-identity semantics CE's own hand-written
 * overrides provided (CE's {@code hashCode} skipped a null-check on {@code fins} only - the
 * generated one is uniformly null-safe across all 4 fields, a strict improvement, not a behavior
 * change any real caller depends on).
 */
public record MissileStruct(ItemMissile warhead, ItemMissile fuselage, @Nullable ItemMissile fins, ItemMissile thruster) {
}
