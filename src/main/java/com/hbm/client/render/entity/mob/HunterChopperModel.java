package com.hbm.client.render.entity.mob;

import com.hbm.entity.mob.EntityHunterChopper;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.RenderType;

/**
 * Ported from CE's {@code com.hbm.render.model.ModelHunterChopper} ({@code upstream/hbm-ce/.../
 * render/model/ModelHunterChopper.java}, 492 lines, {@code extends ModelBase}, read in full) - a
 * plain, vanilla-style box-cuboid rig (47 real {@code ModelRenderer} boxes rendered by {@code
 * renderAll}; 3 more - {@code GunPivot}/{@code GunBarrel}/{@code GunBack} - are declared but their
 * {@code .render(f5)} calls are commented out in CE's own real source, i.e. CE itself never draws
 * them, matching the boss/vehicle report's "CE disabling its own turret-tracking feature" finding -
 * not ported here either). Confirmed by that report's Headline finding #3/section D as one of only
 * two genuinely vanilla-box-cuboid models in this task's whole scope - <b>zero OBJ dependency</b>,
 * despite CE's own dead, commented-out reference to an alternate {@code ProtoCopter} renderer in the
 * same file ({@code //mine = new ProtoCopter();}, never instantiated).
 *
 * <h2>Box-for-box fidelity</h2>
 * Every one of the 47 boxes below was transcribed directly from CE's real source: texture offset
 * (from each {@code new ModelRenderer(this, u, v)} constructor call), box origin/size (from {@code
 * .addBox(...)}), pivot (from {@code .setRotationPoint(...)}), and initial rotation in radians (from
 * {@code setRotation(part, x, y, z)}) - not approximated or reduced to a representative subset, per
 * {@code docs/phase5/boss_and_vehicle_entity_renderers.md}'s own "Open questions/risks" instruction
 * ("whoever implements these two models should do a full line-by-line port rather than treating this
 * report's classification as a substitute for reading the complete source" - this class is that full
 * line-by-line port). {@code RotorBlades} (a 60x0x60 zero-height box) and the two smaller rotor-blade
 * cross-planes ({@code TailRotorBlades}/{@code TorsoRotorBlades}, both 3x3x0) are CE's own real,
 * deliberate degenerate ("flat cross") boxes - a classic cheap "spinning blur" trick, not a porting
 * error - reproduced with the identical zero dimension.
 *
 * <h2>Continuous rotor spin - a real, deliberately preserved CE quirk</h2>
 * CE's {@code renderAll(float f5)} (the method CE's own {@code RenderHunterChopper.doRender} calls,
 * <i>not</i> the vanilla {@code render(Entity,...)} override, which is dead code here - CE's own
 * renderer bypasses it entirely) ends with three unconditional increments: {@code
 * RotorBlades.rotateAngleY += f * 5; TorsoRotorBlades.rotateAngleZ += f * 5; TailRotorBlades
 * .rotateAngleZ += f * 5;} where {@code f} is a <b>class field</b> ({@code float f = 0.1F;}), not the
 * method's own {@code f5} parameter - i.e. every one of the 3 rotor parts' rotation is bumped by a
 * fixed {@code 0.5} radians <i>every single render call</i> (every frame drawn), monotonically,
 * forever, with no reset and no dependency on elapsed real time or tick count. This is a real,
 * confirmed CE behavior (frame-rate-dependent spin speed, not time-based) - reproduced in {@link
 * #setupAnim} below via the same "mutate the {@link ModelPart}'s own persistent rotation field every
 * call" idiom, since this baked {@link ModelPart} tree is shared/reused across every rendered
 * chopper instance exactly like CE's own single {@code ModelHunterChopper mine2} field is (see
 * {@link HunterChopperRenderer}'s own javadoc for why one shared model instance per renderer, not
 * per-entity, is CE-faithful here).
 *
 * <h2>1.21.1 translation notes</h2>
 * <ul>
 *   <li><b>{@link LayerDefinition}/{@link ModelPart} tree, not raw {@code ModelRenderer} fields</b> -
 *       same already-established (and, per {@link CrabModel}'s own javadoc, real-code-cross-checked
 *       against {@code upstream/neo-edition}'s {@code ModelRubble}) convention as {@link CrabModel}.</li>
 *   <li><b>All 47 parts are direct children of {@link #root}</b>, not nested under an intermediate
 *       wrapper part - unlike {@link CrabModel}'s {@code "crab"} pivot, CE's own {@code
 *       RenderHunterChopper.doRender} (not the model) does the translate/scale/rotate wrapping
 *       (see {@link HunterChopperRenderer}), so no equivalent wrapper part is needed here.</li>
 *   <li><b>{@code mirror = true} on every box</b> - CE sets this blanket-universally (all 47 boxes,
 *       confirmed by direct read); reproduced via {@code .mirror()} on every {@link CubeListBuilder}
 *       call below.</li>
 *   <li><b>The 3 commented-out gun parts</b> ({@code GunPivot}/{@code GunBarrel}/{@code GunBack})
 *       are declared as real fields in CE's source (with real box/pivot data) but never rendered -
 *       omitted entirely here rather than built-but-unused, since CE's own dead code has no visual
 *       effect to preserve.</li>
 * </ul>
 */
public class HunterChopperModel extends EntityModel<EntityHunterChopper> {

    private final ModelPart root;
    private final ModelPart rotorBlades;
    private final ModelPart torsoRotorBlades;
    private final ModelPart tailRotorBlades;

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        box(root, "RotorPivotStem", 40, 22, 0F, 0F, 0F, 1, 4, 1, -0.5F, 0F, -0.5F, 0F, 0F, 0F);
        box(root, "RotorPivotTop", 40, 27, 0F, 0F, 0F, 3, 1, 3, -1.5F, -1F, -1.5F, 0F, 0F, 0F);
        box(root, "RotorPivotPlate", 40, 31, 0F, 0F, 0F, 6, 1, 6, -3F, 1.5F, -3F, 0F, 0F, 0F);

        box(root, "TorsoBaseCenter", 70, 0, 0F, 0F, 0F, 14, 4, 2, -8F, 4F, -1F, 0F, 0F, 0F);
        box(root, "TorsoPlateLeft", 70, 6, 0F, -4F, 0F, 14, 4, 1, -8F, 8F, -2F, -0.2268928F, 0F, 0F);
        box(root, "TorsoBaseBottom", 70, 11, 0F, 0F, 0F, 7, 2, 4, -4F, 8F, -2F, 0F, 0F, 0F);
        box(root, "TorsoPlateRight", 70, 17, 0F, -4F, -1F, 14, 4, 1, -8F, 8F, 2F, 0.2268928F, 0F, 0F);
        box(root, "TorsoPlateBottom", 70, 22, -5F, -2F, 0F, 5, 2, 4, -4F, 10F, -2F, 0F, 0F, 0.2094395F);

        box(root, "WingLeftPlate", 110, 0, 0F, -3F, 0F, 9, 3, 1, -8F, 9F, -3F, -0.2268928F, 0F, 0F);
        box(root, "WingRightPlate", 130, 0, 0F, -3F, 0F, 9, 3, 1, -8F, 9F, 2F, 0.2268928F, 0F, 0F);
        box(root, "WingLeft", 110, 4, 0F, 0F, 0F, 3, 1, 6, -3F, 10F, -8F, 0.3490659F, 0F, 0F);
        box(root, "WingLeftFront", 110, 11, 0F, 0F, 0F, 2, 1, 7, -3F, 10F, -8F, 0.3490659F, -0.3490659F, -0.1745329F);
        box(root, "WingLeftTip", 110, 19, 0F, 0F, 0F, 5, 2, 1, -4F, 9F, -8F, 0F, 0F, 0F);
        box(root, "WingRight", 130, 4, 0F, 0F, -6F, 3, 1, 6, -3F, 10F, 8F, -0.3490659F, 0F, 0F);
        box(root, "WingRightFront", 130, 11, 0F, 0F, -7F, 2, 1, 7, -3F, 10F, 8F, -0.3490659F, 0.3490659F, -0.1745329F);
        box(root, "WingRightTip", 130, 19, 0F, 0F, 0F, 5, 2, 1, -4F, 9F, 7F, 0F, 0F, 0F);

        box(root, "TorsoBaseBack", 70, 28, 0F, 0F, 0F, 3, 2, 3, 3F, 7.5F, -1.5F, 0F, 0F, 0F);
        box(root, "TorsoBoxBottom", 70, 33, 0F, -2F, 0F, 7, 2, 2, -3F, 10F, -1F, 0F, 0F, 0.1570796F);
        box(root, "TorsoPlateBack", 70, 37, 0F, 0F, 0F, 3, 1, 2, 6F, 4F, -1F, 0F, 0F, 0.2268928F);
        box(root, "TorsoBoxBack", 70, 40, 0F, 0F, 0F, 2, 4, 2, 6F, 5F, -1F, 0F, 0F, 0F);
        box(root, "TorsoPlateLeftBack", 70, 46, 0F, -4F, -1F, 3, 4, 1, 6F, 8.5F, -1F, -0.2268928F, 0F, 0F);
        box(root, "TorsoPlateRightBack", 70, 51, 0F, -4F, 0F, 3, 4, 1, 6F, 8.5F, 1F, 0.2268928F, 0F, 0F);

        box(root, "TailFrontBase", 24, 54, 0F, 0F, 0F, 5, 2, 2, 8F, 6F, -1F, 0F, 0F, 0F);
        box(root, "TailFrontPlate", 24, 58, -5F, 0F, 0F, 5, 1, 2, 13F, 6F, -1F, 0F, 0F, 0.2268928F);
        box(root, "TailBackBase", 24, 61, 0F, 0F, 0F, 4, 2, 1, 13F, 6F, -0.5F, 0F, 0F, 0F);
        box(root, "TailRotorFront", 24, 64, 0F, 0F, 0F, 1, 3, 1, 15.5F, 8F, -0.5F, 0F, 0F, -0.2268928F);
        box(root, "TailRotorTop", 24, 68, 0F, 0F, 0F, 3, 1, 1, 17F, 6F, -0.5F, 0F, 0F, 0F);
        box(root, "TailRotorBack", 24, 70, 0F, 0F, 0F, 1, 4, 1, 20F, 6F, -0.5F, 0F, 0F, 0F);
        box(root, "TailRotorBottom", 24, 75, 0F, 0F, 0F, 3, 1, 1, 18F, 10F, -0.5F, 0F, 0F, 0F);
        box(root, "TailRotorBlades", 120, 120, -1.5F, -1.5F, 0F, 3, 3, 0, 18.5F, 8.5F, 0F, 0F, 0F, 0F);
        box(root, "TailRotorPivot", 24, 77, 0F, 0F, 0F, 1, 2, 1, 18F, 8F, -0.5F, 0F, 0F, 0F);

        box(root, "HeadNeck", 0, 40, -1F, 0F, 0F, 1, 6, 3, -7F, 4F, -1.5F, 0F, 0F, 0.2268928F);
        box(root, "HeadBack", 0, 49, 0F, 0F, 0F, 1, 7, 4, -8.5F, 3.5F, -2F, 0F, 0F, 0.2268928F);
        box(root, "HeadBase", 0, 60, -2F, 1F, 0F, 2, 6, 4, -8.5F, 3.5F, -2F, 0F, 0F, 0.2268928F);
        box(root, "HeadTop", 0, 70, -2F, 0F, 0F, 2, 2, 4, -8.5F, 3.5F, -2F, 0F, 0F, -0.2268928F);
        box(root, "HeadFront", 0, 76, 0F, 0F, 0F, 2, 4, 2, -13F, 5F, -1F, 0F, 0F, 0F);
        box(root, "HeadLeft", 0, 82, -3F, 0F, 0F, 3, 4, 1, -10F, 5F, -2F, 0F, 0.3490659F, 0F);
        box(root, "HeadRight", 0, 87, -3F, 0F, -1F, 3, 4, 1, -10F, 5F, 2F, 0F, -0.3490659F, 0F);
        box(root, "HeadFrontTop", 0, 92, -3F, 0F, 0F, 3, 1, 2, -10.5F, 4F, -1F, 0F, 0F, -0.3490659F);

        box(root, "TorsoRotorBottom", 0, 0, 0F, 0F, 0F, 3, 1, 1, -7F, 11.5F, -0.5F, 0F, 0F, 0F);
        box(root, "TorsoRotorFront", 0, 2, 0F, 0F, 0F, 1, 3, 1, -8F, 9F, -0.5F, 0F, 0F, 0F);
        box(root, "TorsoRotorBack", 0, 6, 0F, 0F, 0F, 1, 2, 1, -4F, 10F, -0.5F, 0F, 0F, 0F);
        box(root, "TorsoRotorBlades", 112, 120, -1.5F, -1.5F, 0F, 3, 3, 0, -5.5F, 10F, 0F, 0F, 0F, 0F);
        box(root, "TorsoRotorPivot", 0, 9, 0F, 0F, 0F, 1, 2, 1, -6F, 8.5F, -0.5F, 0F, 0F, 0F);

        box(root, "RotorBlades", 76, 68, -30F, 0F, -30F, 60, 0, 60, 0F, 1.5F, 0F, 0F, 0F, 0F);
        box(root, "Antenna1", 0, 95, 0F, 0F, 0F, 4, 1, 1, -14F, 4F, 0.5F, 0F, 0F, 0F);
        box(root, "Antenna2", 0, 97, 0F, 0F, 0F, 2, 1, 1, -15F, 7F, 0F, 0F, 0F, 0F);

        return LayerDefinition.create(mesh, 256, 128);
    }

    private static void box(PartDefinition root, String name, int u, int v,
                             float bx, float by, float bz, int w, int h, int d,
                             float px, float py, float pz, float rx, float ry, float rz) {
        root.addOrReplaceChild(name,
                CubeListBuilder.create().texOffs(u, v).mirror().addBox(bx, by, bz, w, h, d),
                PartPose.offsetAndRotation(px, py, pz, rx, ry, rz));
    }

    public HunterChopperModel(ModelPart root) {
        super(RenderType::entityCutoutNoCull);
        this.root = root;
        this.rotorBlades = root.getChild("RotorBlades");
        this.torsoRotorBlades = root.getChild("TorsoRotorBlades");
        this.tailRotorBlades = root.getChild("TailRotorBlades");
    }

    /** CE: {@code ModelHunterChopper.renderAll}'s trailing spin increments - see class javadoc. */
    @Override
    public void setupAnim(EntityHunterChopper entity, float limbSwing, float limbSwingAmount, float ageInTicks,
                           float netHeadYaw, float headPitch) {
        rotorBlades.yRot += 0.5F;
        torsoRotorBlades.zRot += 0.5F;
        tailRotorBlades.zRot += 0.5F;
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer consumer, int packedLight, int packedOverlay, int color) {
        root.render(poseStack, consumer, packedLight, packedOverlay, color);
    }

    public ModelPart root() {
        return root;
    }
}
