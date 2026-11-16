package dev.nexus.mixin;

import dev.nexus.cosmetics.CapeManager;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.player.SkinTextures;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Podmienia pelerynę lokalnego gracza w SkinTextures. */
@Mixin(AbstractClientPlayerEntity.class)
public abstract class AbstractClientPlayerEntityMixin {
    @Inject(method = "getSkin", at = @At("RETURN"), cancellable = true)
    private void nexus$cape(CallbackInfoReturnable<SkinTextures> cir) {
        SkinTextures custom = CapeManager.skinFor((AbstractClientPlayerEntity) (Object) this, cir.getReturnValue());
        if (custom != null) cir.setReturnValue(custom);
    }
}
