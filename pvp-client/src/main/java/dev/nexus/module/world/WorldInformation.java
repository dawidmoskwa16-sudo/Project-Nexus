package dev.nexus.module.world;

import dev.nexus.module.Category;
import dev.nexus.module.render.HudModule;
import dev.nexus.setting.BooleanSetting;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

/** Informacje o swiecie jako blok HUD (kategoria World, ale rysowany przez baze HudModule). */
public final class WorldInformation extends HudModule {
    private final BooleanSetting target = add(new BooleanSetting("Target block", true));

    public WorldInformation() {
        super("Info o świecie", "Biom, wymiar, czas, światło i pogoda.", Category.HUD, 4, 160);
    }

    @Override
    protected void renderHud(DrawContext ctx) {
        if (mc.world == null) return;
        BlockPos pos = mc.player.getBlockPos();
        long time = mc.world.getTimeOfDay();
        List<String> lines = new ArrayList<>();
        lines.add("Dimension: " + mc.world.getRegistryKey().getValue().getPath());
        lines.add("Biome: " + mc.world.getBiome(pos).getKey().map(k -> k.getValue().getPath()).orElse("?"));
        lines.add("Day " + (time / 24000L) + ", tick " + (time % 24000L));
        lines.add("Light: " + mc.world.getLightLevel(pos));
        lines.add("Weather: " + (mc.world.isThundering() ? "thunder" : mc.world.isRaining() ? "rain" : "clear"));
        if (target.get() && mc.crosshairTarget instanceof BlockHitResult hit
                && hit.getType() == HitResult.Type.BLOCK) {
            lines.add("Target: " + mc.world.getBlockState(hit.getBlockPos()).getBlock().getName().getString());
        }
        drawBlock(ctx, lines);
    }
}
