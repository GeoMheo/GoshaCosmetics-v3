package ru.goshacosmetics;

import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.RotationAxis;

/** Вся косметика на модели игрока (вид от третьего лица и предпросмотр в меню). Нимб в мире — HaloRenderer. */
public class CosmeticsFeature extends FeatureRenderer<PlayerEntityRenderState, PlayerEntityModel> {

    public CosmeticsFeature(FeatureRendererContext<PlayerEntityRenderState, PlayerEntityModel> context) {
        super(context);
    }

    @Override
    public void render(MatrixStack matrices, VertexConsumerProvider vcp, int light,
                       PlayerEntityRenderState state, float limbAngle, float limbDistance) {
        if (state.invisible || !Cosmetics.appliesTo(state.name)) return;
        CosmeticsConfig c = CosmeticsConfig.get();
        // limbAngle/limbDistance в 1.21.4 — поворот и наклон головы; скорость ходьбы берём из состояния
        float move = state.limbAmplitudeMultiplier;
        float t = state.age;
        PlayerEntityModel model = getContextModel();

        // --- тело ---
        matrices.push();
        model.body.rotate(matrices);
        if (c.wings.style > 0) {
            matrices.push();
            matrices.translate(0, 0.22f, 0.15f);
            matrices.scale(c.wings.scale, c.wings.scale, c.wings.scale);
            matrices.translate(0, -0.22f, -0.15f);
            WingModel.render(matrices, vcp, Cosmetics.tint(c.wings), t * c.wings.speed, move, state.isInSneakingPose, c.wings.style);
            matrices.pop();
        }
        if (c.cloak.style > 0 && !c.cloakVanillaRenderer) {
            CloakModel.render(matrices, vcp, c.cloak, t, move, state.isInSneakingPose, light);
        }
        if (c.scarf.style > 0) BodyModel.scarf(matrices, vcp, c.scarf, t, move);
        if (c.backpack.style > 0) BodyModel.backpack(matrices, vcp, c.backpack, t);
        matrices.pop();

        // --- голова ---
        matrices.push();
        model.head.rotate(matrices);
        if (c.hat.style > 0) HatModel.render(matrices, vcp, c.hat, t);
        if (c.ears.style > 0) HeadModel.ears(matrices, vcp, c.ears, t);
        if (c.horns.style > 0) HeadModel.horns(matrices, vcp, c.horns, t);
        if (c.glasses.style > 0) HeadModel.glasses(matrices, vcp, c.glasses, t);
        if (c.halo.style > 0 && Cosmetics.preview) {
            matrices.push();
            matrices.translate(0, -0.72 - c.halo.offset * 0.5f, 0);
            matrices.scale(c.halo.scale, c.halo.scale, c.halo.scale);
            HaloModel.render(matrices, vcp, Cosmetics.tint(c.halo), t * c.halo.speed, c.halo.style, -1f);
            matrices.pop();
        }
        matrices.pop();

        // --- щит на левой руке ---
        if (c.shield.style > 0 && (c.shieldThirdPerson || Cosmetics.preview)) {
            matrices.push();
            model.leftArm.rotate(matrices);
            matrices.translate(0.24f, 0.38f, 0);
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(90));
            matrices.scale(0.55f * c.shield.scale, -0.55f * c.shield.scale, 0.55f * c.shield.scale);
            ShieldModel.render(matrices, vcp, c.shield, t);
            matrices.pop();
        }
    }
}
