package com.kitmod.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles persistence of Kit objects to/from
 * .minecraft/kits/<name>.json
 *
 * File names are sanitised so they are safe on all OSes.
 */
public class KitStorage {

    private static final Logger LOGGER = LoggerFactory.getLogger("KitMod/Storage");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static Path kitsDir() {
        Path dir = FabricLoader.getInstance().getGameDir().resolve("kits");
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            LOGGER.error("Could not create kits directory", e);
        }
        return dir;
    }

    /** Convert a kit name to a safe filename (strip non-alphanumeric except space/dash/underscore) */
    public static String sanitise(String name) {
        return name.replaceAll("[^a-zA-Z0-9 _\\-]", "_").trim();
    }

    private static Path pathFor(Kit kit) {
        return kitsDir().resolve(sanitise(kit.name) + ".json");
    }

    // -------------------------------------------------------------------------

    public static void save(Kit kit) {
        try (Writer w = Files.newBufferedWriter(pathFor(kit))) {
            GSON.toJson(kit, w);
        } catch (IOException e) {
            LOGGER.error("Failed to save kit '{}'", kit.name, e);
        }
    }

    public static void delete(Kit kit) {
        try {
            Files.deleteIfExists(pathFor(kit));
        } catch (IOException e) {
            LOGGER.error("Failed to delete kit '{}'", kit.name, e);
        }
    }

    /** Rename a kit on disk (delete old file, save new) */
    public static void rename(Kit kit, String oldName) {
        Path old = kitsDir().resolve(sanitise(oldName) + ".json");
        try {
            Files.deleteIfExists(old);
        } catch (IOException e) {
            LOGGER.error("Failed to delete old kit file for rename", e);
        }
        save(kit);
    }

    /** Load all kits from .minecraft/kits/ */
    public static List<Kit> loadAll() {
        List<Kit> result = new ArrayList<>();
        Path dir = kitsDir();
        try (var stream = Files.list(dir)) {
            stream.filter(p -> p.toString().endsWith(".json"))
                  .forEach(p -> {
                      try (Reader r = Files.newBufferedReader(p)) {
                          Kit k = GSON.fromJson(r, Kit.class);
                          if (k != null && k.name != null) {
                              result.add(k);
                          }
                      } catch (IOException | com.google.gson.JsonSyntaxException e) {
                          LOGGER.warn("Skipping malformed kit file: {}", p.getFileName(), e);
                      }
                  });
        } catch (IOException e) {
            LOGGER.error("Failed to list kits directory", e);
        }
        result.sort((a, b) -> {
            if (a.savedAt == null) return 1;
            if (b.savedAt == null) return -1;
            return b.savedAt.compareTo(a.savedAt); // newest first
        });
        return result;
    }
}
