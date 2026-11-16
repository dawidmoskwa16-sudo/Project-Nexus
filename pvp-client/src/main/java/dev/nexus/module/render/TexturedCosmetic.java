package dev.nexus.module.render;

import dev.nexus.NexusClient;
import dev.nexus.cosmetics.AnimatedTexture;
import dev.nexus.cosmetics.CapeManager;
import dev.nexus.cosmetics.Cosmetic;
import dev.nexus.module.Category;
import dev.nexus.module.Module;
import dev.nexus.setting.BooleanSetting;
import dev.nexus.setting.ModeSetting;
import dev.nexus.setting.NumberSetting;
import net.minecraft.util.Identifier;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Baza kosmetykow rysowanych jako tekstura (skrzydla, aureola): wybor, ladowanie, GIF. */
public abstract class TexturedCosmetic extends Module implements Cosmetic {
    private static final int MAX_DIM = 256;

    private final String folderName;
    private final List<String> builtins;
    protected final ModeSetting choice;
    protected final NumberSetting scale;
    private final BooleanSetting rescan = add(new BooleanSetting("Odśwież pliki", false));
    private AnimatedTexture tex;
    private String loadedKey = "";

    protected TexturedCosmetic(String name, String desc, String folderName, List<String> builtins) {
        super(name, desc, Category.APPEARANCE);
        this.folderName = folderName;
        this.builtins = builtins;
        this.choice = add(new ModeSetting("Wybór", builtins.get(0), allNames(folderName, builtins).toArray(new String[0])));
        this.scale = add(new NumberSetting("Rozmiar", 1.0, 0.5, 2.0, 0.05));
    }

    private static List<String> allNames(String folderName, List<String> builtins) {
        return CapeManager.namesWith(builtins, CapeManager.subfolder(folderName));
    }

    protected abstract BufferedImage builtinImage(String name);

    public float scaleValue() { return scale.floatValue(); }

    /** Biezaca klatka tekstury albo null, gdy nic nie zaladowano. */
    public Identifier texture() { return tex == null ? null : tex.current(1f); }

    @Override public List<String> options() { return choice.getModes(); }
    @Override public List<String> builtins() { return builtins; }
    @Override public String selected() { return choice.get(); }
    @Override public void select(String name) { choice.set(name); }
    @Override public void refresh() { choice.setModes(allNames(folderName, builtins)); loadedKey = ""; }
    @Override public String folderHint() { return ".minecraft/config/nexus/cosmetics/" + folderName + "/  (PNG z przezroczystością, JPG, GIF)"; }

    @Override
    public void onTick() {
        if (rescan.get()) {
            rescan.set(false);
            refresh();
        }
        String key = choice.get();
        if (key.equals(loadedKey)) return;
        loadedKey = key;
        unload();
        try {
            List<CapeManager.Frame> frames = new ArrayList<>();
            if (builtins.contains(key)) {
                frames.add(new CapeManager.Frame(builtinImage(key), 100));
            } else {
                Path file = CapeManager.subfolder(folderName).resolve(key);
                for (CapeManager.Frame f : CapeManager.decodeFile(file)) frames.add(new CapeManager.Frame(fit(f.image()), f.delayMs()));
            }
            tex = AnimatedTexture.create(frames);
        } catch (Exception e) {
            NexusClient.LOGGER.error("Nie mozna zaladowac kosmetyku {}", key, e);
        }
    }

    @Override protected void onEnable() { loadedKey = ""; }
    @Override protected void onDisable() { unload(); loadedKey = ""; }

    private void unload() {
        if (tex != null) tex.destroy();
        tex = null;
    }

    private static BufferedImage fit(BufferedImage src) {
        int w = src.getWidth(), h = src.getHeight();
        double s = Math.min(1.0, (double) MAX_DIM / Math.max(w, h));
        if (s >= 1.0) return src;
        int nw = Math.max(1, (int) Math.round(w * s)), nh = Math.max(1, (int) Math.round(h * s));
        BufferedImage out = new BufferedImage(nw, nh, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(src, 0, 0, nw, nh, null);
        g.dispose();
        return out;
    }
}
