package dev.nexus.cosmetics;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** Zestaw zarejestrowanych tekstur (klatki GIF lub pojedynczy obraz) z wyborem klatki wg czasu. */
public final class AnimatedTexture {
    private static int counter;

    private final List<Identifier> ids = new ArrayList<>();
    private final int[] cumulative;
    private final long start = System.currentTimeMillis();

    private AnimatedTexture(int n) { cumulative = new int[n]; }

    /** Wywolywac w watku renderowania. */
    public static AnimatedTexture create(List<CapeManager.Frame> frames) throws IOException {
        AnimatedTexture t = new AnimatedTexture(frames.size());
        MinecraftClient mc = MinecraftClient.getInstance();
        int total = 0, i = 0;
        try {
            for (CapeManager.Frame f : frames) {
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                ImageIO.write(f.image(), "png", bos);
                NativeImage ni = NativeImage.read(new ByteArrayInputStream(bos.toByteArray()));
                Identifier id = Identifier.of("nexusclient", "cosmetic/" + (counter++));
                mc.getTextureManager().registerTexture(id, new NativeImageBackedTexture(() -> "nexus cosmetic " + id, ni));
                t.ids.add(id);
                total += Math.max(20, f.delayMs());
                t.cumulative[i++] = total;
            }
        } catch (IOException | RuntimeException e) {
            t.destroy();
            throw e;
        }
        return t;
    }

    public Identifier current(float speed) {
        if (ids.size() <= 1) return ids.isEmpty() ? null : ids.get(0);
        long t = (long) ((System.currentTimeMillis() - start) * speed) % cumulative[cumulative.length - 1];
        for (int i = 0; i < cumulative.length; i++) if (t < cumulative[i]) return ids.get(i);
        return ids.get(0);
    }

    public void destroy() {
        MinecraftClient mc = MinecraftClient.getInstance();
        for (Identifier id : ids) mc.getTextureManager().destroyTexture(id);
        ids.clear();
    }
}
