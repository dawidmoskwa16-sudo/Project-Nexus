package dev.nexus.cosmetics;

import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityFeatureRendererRegistrationCallback;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;

public final class CosmeticsRenderers {
    private CosmeticsRenderers() {}

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void register() {
        LivingEntityFeatureRendererRegistrationCallback.EVENT.register((entityType, renderer, helper, context) -> {
            if (renderer instanceof PlayerEntityRenderer) {
                helper.register(new CosmeticsFeatureRenderer((FeatureRendererContext) renderer));
            }
        });
    }
}
