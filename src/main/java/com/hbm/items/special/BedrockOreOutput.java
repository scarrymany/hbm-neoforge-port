package com.hbm.items.special;

import com.hbm.inventory.material.NTMMaterial;

/**
 * One material output of a {@link BedrockOreType}'s ore-slopper processing chain (primary yield or
 * acid/solvent/rad-solvent byproduct). Consumed by the Phase 2 ore-processing machinery
 * (excavator/ore slopper); this port only carries the data forward, it does not implement any
 * machine that reads it.
 * <p>
 * Ported from CE's {@code ItemBedrockOreNew.BedrockOreOutput}.
 */
public record BedrockOreOutput(NTMMaterial material, int amount) {
}
