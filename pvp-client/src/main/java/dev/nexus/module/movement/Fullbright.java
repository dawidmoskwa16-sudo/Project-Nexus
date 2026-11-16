package dev.nexus.module.movement;

import dev.nexus.module.Category;
import dev.nexus.module.Module;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;

/** Lokalny efekt widzenia w ciemnosci (bez ikony i czastek). */
public final class Fullbright extends Module {
    private boolean applied;

    public Fullbright() {
        super("Pełna jasność", "Rozjaśnia ciemne miejsca.", Category.GAMEPLAY);
    }

    @Override
    public void onTick() {
        ClientPlayerEntity p = mc.player;
        if (p == null) return;
        StatusEffectInstance cur = p.getStatusEffect(StatusEffects.NIGHT_VISION);
        if (cur == null || cur.getDuration() < 400) {
            p.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, 1_000_000, 0, false, false, false));
            applied = true;
        }
    }

    @Override
    protected void onDisable() {
        if (applied && mc.player != null) mc.player.removeStatusEffect(StatusEffects.NIGHT_VISION);
        applied = false;
    }
}
