package dev.nexus.module.render;

import dev.nexus.cosmetics.CosmeticsBuiltin;
import dev.nexus.setting.NumberSetting;

import java.awt.image.BufferedImage;

/** Skrzydla widoczne tylko dla Ciebie (3 dopracowane wbudowane wzory + pliki z cosmetics/wings/). */
public final class Wings extends TexturedCosmetic {
    private final NumberSetting flap = add(new NumberSetting("Machanie", 1.0, 0.0, 3.0, 0.1));

    public Wings() {
        super("Skrzydła", "Skrzydła na plecach (widoczne tylko dla Ciebie).", "wings", CosmeticsBuiltin.WINGS);
    }

    public float flapSpeed() { return flap.floatValue(); }

    @Override protected BufferedImage builtinImage(String name) { return CosmeticsBuiltin.wings(name); }
}
