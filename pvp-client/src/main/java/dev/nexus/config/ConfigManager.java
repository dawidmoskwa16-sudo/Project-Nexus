package dev.nexus.config;

import com.google.gson.*;
import dev.nexus.NexusClient;
import dev.nexus.module.Module;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/** Profile JSON: .minecraft/config/nexus/profiles/&lt;nazwa&gt;.json */
public final class ConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static Path profilesDir;
    private static Path activeFile;
    private static String active = "default";
    private static boolean dirty;
    private static int dirtyTicks;

    private ConfigManager() {}

    public static void init() {
        try {
            profilesDir = FabricLoader.getInstance().getConfigDir().resolve("nexus").resolve("profiles");
            Files.createDirectories(profilesDir);
            activeFile = profilesDir.getParent().resolve("active.txt");
            if (Files.exists(activeFile)) active = sanitize(Files.readString(activeFile).trim());
        } catch (IOException e) {
            NexusClient.LOGGER.error("Nie mozna utworzyc katalogu konfiguracji", e);
            return;
        }
        load(active);
    }

    public static String getActive() { return active; }

    public static void markDirty() { dirty = true; }

    /** Wywolywane co tick: zapis z opoznieniem 3 s po ostatniej zmianie. */
    public static void tick() {
        if (!dirty) { dirtyTicks = 0; return; }
        if (++dirtyTicks >= 60) save();
    }

    public static synchronized void save() {
        if (profilesDir == null) return;
        dirty = false;
        dirtyTicks = 0;
        JsonObject root = new JsonObject();
        JsonObject mods = new JsonObject();
        for (Module m : NexusClient.modules().getAll()) mods.add(m.getName(), m.toJson());
        root.add("modules", mods);
        JsonObject gui = new JsonObject();
        for (Map.Entry<String, float[]> e : GuiState.POSITIONS.entrySet()) {
            JsonArray a = new JsonArray();
            a.add(e.getValue()[0]);
            a.add(e.getValue()[1]);
            gui.add(e.getKey(), a);
        }
        root.add("gui", gui);

        Path target = profilesDir.resolve(active + ".json");
        Path tmp = profilesDir.resolve(active + ".json.tmp");
        try {
            Files.writeString(tmp, GSON.toJson(root));
            Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
            Files.writeString(activeFile, active);
        } catch (IOException e) {
            NexusClient.LOGGER.error("Nie mozna zapisac profilu {}", active, e);
        }
    }

    public static synchronized void load(String name) {
        if (profilesDir == null) return;
        active = sanitize(name);
        Path file = profilesDir.resolve(active + ".json");
        if (!Files.exists(file)) {
            save();
            return;
        }
        try (Reader r = Files.newBufferedReader(file)) {
            JsonObject root = JsonParser.parseReader(r).getAsJsonObject();
            if (root.has("modules")) {
                JsonObject mods = root.getAsJsonObject("modules");
                for (Module m : NexusClient.modules().getAll()) {
                    JsonElement el = mods.get(m.getName());
                    if (el != null && el.isJsonObject()) m.fromJson(el.getAsJsonObject());
                }
            }
            GuiState.POSITIONS.clear();
            if (root.has("gui")) {
                for (Map.Entry<String, JsonElement> e : root.getAsJsonObject("gui").entrySet()) {
                    JsonArray a = e.getValue().getAsJsonArray();
                    GuiState.POSITIONS.put(e.getKey(), new float[]{a.get(0).getAsFloat(), a.get(1).getAsFloat()});
                }
            }
            Files.writeString(activeFile, active);
            dirty = false;
        } catch (Exception e) {
            NexusClient.LOGGER.error("Nie mozna wczytac profilu {}", active, e);
        }
    }

    /** Tworzy nowy profil jako kopie biezacych ustawien i przelacza sie na niego. */
    public static synchronized void createProfile(String name) {
        active = sanitize(name);
        save();
    }

    public static synchronized boolean deleteProfile(String name) {
        name = sanitize(name);
        if (name.equals(active) || name.equals("default") || profilesDir == null) return false;
        try {
            return Files.deleteIfExists(profilesDir.resolve(name + ".json"));
        } catch (IOException e) {
            return false;
        }
    }

    public static List<String> listProfiles() {
        List<String> out = new ArrayList<>();
        if (profilesDir != null) {
            try (Stream<Path> s = Files.list(profilesDir)) {
                s.map(p -> p.getFileName().toString())
                        .filter(n -> n.endsWith(".json"))
                        .map(n -> n.substring(0, n.length() - 5))
                        .sorted()
                        .forEach(out::add);
            } catch (IOException ignored) {
            }
        }
        if (!out.contains(active)) out.add(active);
        return out;
    }

    public static String nextProfileName() {
        List<String> existing = listProfiles();
        int i = 1;
        while (existing.contains("profile-" + i)) i++;
        return "profile-" + i;
    }

    private static String sanitize(String n) {
        String s = n == null ? "" : n.replaceAll("[^a-zA-Z0-9_\\-]", "_");
        return s.isEmpty() ? "default" : s;
    }
}
