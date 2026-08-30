/**
 * RBMK reactor column blocks and block entities (Phase 2, "RBMK column blocks" work package).
 *
 * <h2>Reconciled against the real, concurrently-landed {@code rbmk_core_logic} package</h2>
 * This package implements every concrete RBMK column type (fuel rod channel, control rod, moderator,
 * absorber, reflector, blank, boiler, outgasser, cooler, heater, autoloader, storage, inlet, outlet)
 * plus the console and {@link com.hbm.blocks.machine.rbmk.RBMKBaseBlock} multiblock-casing framework,
 * per {@code docs/phase2/rbmk_reactor.md}. The sibling {@code rbmk_core_logic} package landed in this
 * same working tree while this package was being written and was read in full to reconcile against,
 * rather than left as an untested forward-reference guess - see {@code com.hbm.api.rbmk} for its real
 * shape:
 * <ul>
 *   <li>{@link com.hbm.api.rbmk.IRBMKColumn} - the generalized column contract
 *   {@link RBMKBaseBlockEntity} (this package, below) implements: {@code getRbmkLevel}/
 *   {@code getRbmkPos}/{@code getHeat}/{@code setHeat}/{@code hasLid}/{@code isModerated}/
 *   {@code maxHeat}/{@code isRemoved}. Deliberately has no concrete {@code BlockEntity} dependency in
 *   either direction - {@code com.hbm.handler.neutron.RBMKNeutronHandler} looks columns up generically
 *   via {@code instanceof IRBMKColumn}.</li>
 *   <li>{@link com.hbm.api.rbmk.IRBMKFluxReceiver} (+ nested {@code NType}) - implemented by
 *   {@link RBMKRodBlockEntity} and {@link RBMKOutgasserBlockEntity}, each also implementing
 *   {@code canReceiveFlux()} ({@code hasRod}/{@code canProcess()} respectively, per that interface's
 *   own javadoc).</li>
 *   <li>{@link com.hbm.api.rbmk.IRBMKControlColumn} - implemented by
 *   {@link RBMKControlBlockEntity} ({@code getLevel}/{@code getMult}), with
 *   {@link RBMKControlManualBlockEntity} overriding {@code getMult()} via
 *   {@link com.hbm.api.rbmk.RBMKControlMath#getEffectiveMult} for the withdrawal power surge.</li>
 *   <li>{@link com.hbm.api.rbmk.IRBMKLoadable} - implemented unchanged by every loadable column
 *   (rod, outgasser, storage).</li>
 *   <li>{@link com.hbm.api.rbmk.RBMKDials} - the real config-backed (not gamerule) dial accessor,
 *   {@code ServerLevel}-parameterized (not {@code Level} - every call site here casts/guards
 *   accordingly).</li>
 *   <li>{@link com.hbm.api.rbmk.RBMKColumnHeatMath}/{@link com.hbm.api.rbmk.RBMKMeltdownTrigger} -
 *   the pure heat-diffusion and meltdown-trigger-condition math {@link RBMKBaseBlockEntity} calls
 *   from its own {@code moveHeat()}/{@code checkMeltdown()}. Per
 *   {@link com.hbm.api.rbmk.IRBMKMeltdownHandler}'s own javadoc, the actual meltdown BFS/per-column
 *   {@code onMelt} dispatch is THIS package's responsibility (see
 *   {@link RBMKBaseBlockEntity#runMeltdown}) - real byproduct blocks/entities remain a Phase 3
 *   forward reference ({@code com.hbm.entity.projectile.EntityRBMKDebris} does not exist anywhere in
 *   this port).</li>
 *   <li>{@code com.hbm.items.machine.ItemRBMKRod} - also landed concurrently (not in this package's
 *   own {@code com.hbm.items.machine.rbmk} subpackage as originally planned); this package's
 *   {@link com.hbm.items.machine.rbmk.RBMKRods} registers all 31 concrete fuel rods against it.</li>
 * </ul>
 *
 * <h2>Written in this package (not forward-referenced)</h2>
 * {@link RBMKColumn} (the console's data DTO) has no equivalent in {@code com.hbm.api.rbmk} and is
 * this package's own, in full. {@link RBMKBaseBlockEntity}/{@link RBMKSlottedBlockEntity} are this
 * package's own concrete base classes (not forward references) - see each one's own javadoc.
 */
package com.hbm.blockentity.machine.rbmk;
