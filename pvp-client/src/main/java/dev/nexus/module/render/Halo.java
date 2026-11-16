package dev.nexus.module.render;

import dev.nexus.cosmetics.CosmeticsBuiltin;

import java.awt.image.BufferedImage;

/** Aureola nad glowa (3 dopracowane wbudowane wzory + pliki z cosmetics/halos/). */
public final class Halo extends TexturedCosmetic {
    public Halo() {
        super("Aureola", "Aureola nad głową (widoczna tylko dla Ciebie).", "halos", CosmeticsBuiltin.HALOS);
    }

    @Override protected BufferedImage builtinImage(String name) { return CosmeticsBuiltin.halo(name); }
}
