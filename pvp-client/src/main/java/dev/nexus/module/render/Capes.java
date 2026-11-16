package dev.nexus.module.render;

import dev.nexus.cosmetics.CapeManager;
import dev.nexus.module.Category;
import dev.nexus.module.Module;
import dev.nexus.setting.BooleanSetting;
import dev.nexus.setting.ModeSetting;
import dev.nexus.setting.NumberSetting;

/** Peleryny: 4 dopracowane wbudowane wzory + pliki z .minecraft/config/nexus/cosmetics/ (PNG, JPG, GIF). */
public final class Capes extends Module implements dev.nexus.cosmetics.Cosmetic {
    private final ModeSetting cape = add(new ModeSetting("Cape", "Nexus", CapeManager.availableNames().toArray(new String[0])));
    private final ModeSetting fit = add(new ModeSetting("Fit", "Cover", "Cover", "Stretch"));
    private final NumberSetting speed = add(new NumberSetting("GIF speed", 1.0, 0.25, 3.0, 0.05));
    private final BooleanSetting rescan = add(new BooleanSetting("Rescan files", false));
    private String loadedKey = "";

    public Capes() {
        super("Peleryny", "Własna peleryna z pliku PNG/JPG/GIF (widoczna tylko dla Ciebie).", Category.APPEARANCE);
    }

    @Override public java.util.List<String> options() { return cape.getModes(); }
    @Override public String selected() { return cape.get(); }
    @Override public void select(String name) { cape.set(name); }
    @Override public void refresh() { cape.setModes(CapeManager.availableNames()); loadedKey = ""; }
    @Override public java.util.List<String> builtins() { return CapeManager.BUILTIN; }
    @Override public String folderHint() { return ".minecraft/config/nexus/cosmetics/  (PNG, JPG, GIF)"; }

    @Override
    protected void onEnable() { loadedKey = ""; }

    @Override
    protected void onDisable() {
        CapeManager.unload();
        loadedKey = "";
    }

    @Override
    public void onTick() {
        if (rescan.get()) {
            rescan.set(false);
            cape.setModes(CapeManager.availableNames());
            loadedKey = "";
        }
        CapeManager.setSpeed(speed.floatValue());
        String key = cape.get() + "|" + fit.get();
        if (!key.equals(loadedKey)) {
            loadedKey = key;
            CapeManager.load(cape.get(), fit.is("Cover"));
        }
    }
}
