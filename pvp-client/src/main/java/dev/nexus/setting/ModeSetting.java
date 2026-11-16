package dev.nexus.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import java.util.Arrays;
import java.util.List;

public final class ModeSetting extends Setting<String> {
    private List<String> modes;

    public ModeSetting(String name, String def, String... modes) {
        super(name, def);
        this.modes = Arrays.asList(modes);
    }

    public List<String> getModes() { return modes; }

    public void setModes(List<String> list) {
        this.modes = new java.util.ArrayList<>(list);
        if (!modes.contains(value)) value = modes.get(0);
    }
    public boolean is(String mode) { return value.equalsIgnoreCase(mode); }

    public void cycle(int dir) {
        int i = modes.indexOf(value);
        i = Math.floorMod(i + dir, modes.size());
        set(modes.get(i));
    }

    @Override public JsonElement toJson() { return new JsonPrimitive(value); }

    @Override
    public void fromJson(JsonElement e) {
        String s = e.getAsString();
        if (modes.contains(s)) value = s;
    }
}
