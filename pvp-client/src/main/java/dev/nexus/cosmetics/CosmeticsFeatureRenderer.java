
package dev.nexus.cosmetics;

import dev.nexus.NexusClient;
import dev.nexus.module.render.Halo;
import dev.nexus.module.render.Wings;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

/**
 * Rysuje skrzydla i aureole lokalnego gracza.
 */
public final class CosmeticsFeatureRenderer
        extends FeatureRenderer<PlayerEntityRenderState, PlayerEntityModel> {

    public CosmeticsFeatureRenderer(
            FeatureRendererContext<PlayerEntityRenderState, PlayerEntityModel> context) {
        super(context);
    }

    private static boolean isLocal(PlayerEntityRenderState state) {
        MinecraftClient mc = MinecraftClient.getInstance();
        return mc.player != null && state.id == mc.player.getId();
    }

    @Override
    public void render(
            MatrixStack matrices,
            OrderedRenderCommandQueue queue,
            int light,
            PlayerEntityRenderState state,
            float a,
            float b) {

        if (state.invisible || !isLocal(state)) return;

        Wings wings = NexusClient.modules().get(Wings.class);

        if (wings != null && wings.isEnabled()) {
            Identifier id = wings.texture();

            if (id != null) {
                drawWings(
                        matrices,
                        queue,
                        light,
                        id,
                        wings.scaleValue(),
                        wings.flapSpeed(),
                        state.sneaking
                );
            }
        }

        Halo halo = NexusClient.modules().get(Halo.class);

        if (halo != null && halo.isEnabled()) {
            Identifier id = halo.texture();

            if (id != null) {
                drawHalo(
                        matrices,
                        queue,
                        light,
                        id,
                        halo.scaleValue()
                );
            }
        }
    }

    private static void vert(
            VertexConsumer vc,
            MatrixStack.Entry e,
            float x, float y, float z,
            float u, float v,
            int light) {

        vc.vertex(e, x, y, z)
                .color(0xFFFFFFFF)
                .texture(u, v)
                .overlay(OverlayTexture.DEFAULT_UV)
                .light(light)
                .normal(e, 0f, 0f, 1f);
    }

    private static void quad(
            VertexConsumer vc,
            MatrixStack.Entry e,
            float[][] p,
            float[][] uv,
            int light) {

        for (int i = 0; i < 4; i++) {
            vert(vc, e,
                    p[i][0], p[i][1], p[i][2],
                    uv[i][0], uv[i][1], light);
        }

        for (int i = 3; i >= 0; i--) {
            vert(vc, e,
                    p[i][0], p[i][1], p[i][2],
                    uv[i][0], uv[i][1], light);
        }
    }

    private static void drawWings(
            MatrixStack matrices,
            OrderedRenderCommandQueue queue,
            int light,
            Identifier tex,
            float scale,
            float flapSpeed,
            boolean sneaking) {

        float t = (System.currentTimeMillis() % 1_000_000L) / 1000f;

        float flap = 0.5f + 0.5f
                * (float) Math.sin(t * 2.4f * flapSpeed);

        float phi = (float) Math.toRadians(16)
                + 0.55f * flap * (flapSpeed > 0 ? 1f : 0f)
                + (sneaking ? 0.25f : 0f);

        float w = 22f * scale / 16f;
        float h = 22f * scale / 16f;

        float top = -0.45f * h;
        float bottom = 0.65f * h;

        float pivotY = (sneaking ? 1.5f : 0.5f) / 16f;
        float pivotZ = 2.7f / 16f;

        float cos = (float) Math.cos(phi);
        float sin = (float) Math.sin(phi);

        float[][] uv = {
                {0, 0},
                {1, 0},
                {1, 1},
                {0, 1}
        };

        queue.submitCustom(
                matrices,
                RenderLayers.entityTranslucent(tex),
                (entry, vc) -> {
                    for (int s = -1; s <= 1; s += 2) {
                        float px = s * 1.2f / 16f;

                        float[][] p = new float[4][3];

                        float[] lx = {0, w, w, 0};
                        float[] ly = {top, top, bottom, bottom};

                        for (int i = 0; i < 4; i++) {
                            p[i][0] = px + s * lx[i] * cos;
                            p[i][1] = pivotY + ly[i];
                            p[i][2] = pivotZ + lx[i] * sin;
                        }

                        quad(vc, entry, p, uv, light);
                    }
                }
        );
    }

    private static void drawHalo(
            MatrixStack matrices,
            OrderedRenderCommandQueue queue,
            int light,
            Identifier tex,
            float scale) {

        float t = (System.currentTimeMillis() % 1_000_000L) / 1000f;

        float y = -(8f + 3.5f * scale) / 16f
                + (float) Math.sin(t * 2f) * 0.02f;

        float half = 5.5f * scale / 16f;

        float[][] p = {
                {-half, y, -half},
                {half, y, -half},
                {half, y, half},
                {-half, y, half}
        };

        float[][] uv = {
                {0, 0},
                {1, 0},
                {1, 1},
                {0, 1}
        };

        queue.submitCustom(
                matrices,
                RenderLayers.entityTranslucent(tex),
                (entry, vc) -> quad(vc, entry, p, uv, light)
        );
    }
}
