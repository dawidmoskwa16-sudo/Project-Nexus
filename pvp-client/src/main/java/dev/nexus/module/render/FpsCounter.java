package dev.nexus.module.render;

import dev.nexus.setting.BooleanSetting;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.PlayerListEntry;

import java.util.ArrayList;
import java.util.List;

public final class FpsCounter extends HudModule {
    private final BooleanSetting ping = add(new BooleanSetting("Show ping", true));

    public FpsCounter() {
        super("FPS", "Licznik FPS i pingu.", dev.nexus.module.Category.PERFORMANCE, 4, 40);
    }

    @Override
    protected void renderHud(DrawContext ctx) {
        List<String> lines = new ArrayList<>();
        lines.add("FPS: " + mc.getCurrentFps());
        if (ping.get() && mc.getNetworkHandler() != null) {
            PlayerListEntry entry = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
            if (entry != null) lines.add("Ping: " + entry.getLatency() + " ms");
        }
        drawBlock(ctx, lines);
    }
}
