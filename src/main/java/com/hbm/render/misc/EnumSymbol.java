package com.hbm.render.misc;

/**
 * Ported from CE's {@code com.hbm.render.misc.EnumSymbol} (21 lines), verbatim.
 *
 * <p>Identifies the small overlay icon (radiation trefoil, "no water" cross, corrosive-acid
 * droplet, etc.) blitted onto a fluid's tooltip/HUD/tank-gauge display to flag a hazard trait at a
 * glance. Each constant carries the (x, y) pixel offset of its icon within CE's single, fixed
 * overlay sprite sheet ({@code textures/gui/fluid_symbols.png} in CE) rather than a separate
 * texture path per symbol - CE renders these via a plain {@code drawTexturedModalRect}-style blit
 * at that offset, not a distinct {@link net.minecraft.resources.ResourceLocation} lookup, so the
 * {@code (x, y)} shape is preserved as-is rather than converted to per-value texture paths. Actual
 * blitting of these coordinates against the shared sprite sheet is not yet ported (no consumer of
 * {@link #x}/{@link #y} exists in this port yet); this class exists so that
 * {@link com.hbm.inventory.fluid.FluidType} and {@link com.hbm.inventory.fluid.Fluids} - both
 * already-committed Phase 0 work referencing every one of these constants - compile.
 *
 * <p>{@link #NONE} is CE's "no symbol" sentinel (icon at (0, 0), the sheet's blank corner); every
 * other constant matches CE's name and coordinates exactly, including CE's own misspellings
 * ({@link #CROYGENIC} for "cryogenic") which are preserved verbatim since other CE/port code may
 * already depend on the exact constant name.
 */
public enum EnumSymbol {
	NONE(0, 0),
	RADIATION(195, 2),
	NOWATER(195, 63),
	ACID(195, 124),
	ASPHYXIANT(195, 185),
	CROYGENIC(134, 185),
	ANTIMATTER(73, 185),
	OXIDIZER(12, 185);

	public int x;
	public int y;

	private EnumSymbol(int x, int y) {
		this.x = x;
		this.y = y;
	}
}
