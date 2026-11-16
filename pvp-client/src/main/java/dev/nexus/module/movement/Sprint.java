package dev.nexus.module.movement;

import dev.nexus.module.Category;
import dev.nexus.module.Module;
import net.minecraft.client.network.ClientPlayerEntity;

public final class Sprint extends Module {
    public Sprint() {
        super("Toggle Sprint", "Automatyczny sprint podczas ruchu do przodu.", Category.GAMEPLAY);
    }

    @Override
    public void onTick() {
        ClientPlayerEntity p = mc.player;
        if (p == null) return;
        if (mc.options.forwardKey.isPressed() && !p.isSneaking() && !p.horizontalCollision
                && !p.isUsingItem() && p.getHungerManager().getFoodLevel() > 6) {
            p.setSprinting(true);
        }
    }
}
