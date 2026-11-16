package dev.nexus.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

/** Kolor RGB (bez alfy), przechowywany jako 0xRRGGBB. */
public final class ColorSetting extends Setting<Integer> {
    public ColorSetting(String name, int rgb) { super(name, rgb & 0xFFFFFF); }

    public int argb() { return 0xFF000000 | value; }

    @Override public void set(Integer v) { super.set(v & 0xFFFFFF); }
    @Override public JsonElement toJson() { return new JsonPrimitive(value); }
    @Override public void fromJson(JsonElement e) { set(e.getAsInt()); }
}
