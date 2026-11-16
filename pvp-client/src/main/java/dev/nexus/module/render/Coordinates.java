package dev.nexus.module.render;

import dev.nexus.setting.BooleanSetting;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class Coordinates extends HudModule {
    private final BooleanSetting dimensionConversion = add(new BooleanSetting("Nether/Overworld", true));
    private final BooleanSetting facing = add(new BooleanSetting("Facing", true));

    public Coordinates() {
        super("Współrzędne", "Wyświetla współrzędne gracza.", 4, 4);
    }

    @Override
    protected void renderHud(DrawContext ctx) {
        var p = mc.player;
        List<String> lines = new ArrayList<>();
        lines.add(String.format(Locale.ROOT, "XYZ: %.1f %.1f %.1f", p.getX(), p.getY(), p.getZ()));
        if (dimensionConversion.get() && mc.world != null) {
            if (mc.world.getRegistryKey() == World.OVERWORLD) {
                lines.add(String.format(Locale.ROOT, "Nether: %.1f %.1f", p.getX() / 8.0, p.getZ() / 8.0));
            } else if (mc.world.getRegistryKey() == World.NETHER) {
                lines.add(String.format(Locale.ROOT, "Overworld: %.1f %.1f", p.getX() * 8.0, p.getZ() * 8.0));
            }
        }
        if (facing.get()) lines.add("Facing: " + p.getHorizontalFacing().name());
        drawBlock(ctx, lines);
    }
}
