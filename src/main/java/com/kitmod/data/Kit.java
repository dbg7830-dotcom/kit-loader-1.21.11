package com.kitmod.data;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents a saved inventory kit.
 *
 * Slot layout (mirrors vanilla PlayerInventory):
 *   0–8   = hotbar
 *   9–35  = main inventory
 *   36    = boots
 *   37    = leggings
 *   38    = chestplate
 *   39    = helmet
 *   40    = offhand
 */
public class Kit {

    public String name;
    public String iconItemId;          // e.g. "minecraft:diamond_sword"
    public String savedAt;             // ISO-8601 timestamp string
    /** Map of slot index (as String for JSON) → serialised ItemStack NBT string */
    public Map<String, String> slots;

    /** No-arg constructor required by Gson */
    public Kit() {}

    public Kit(String name, String iconItemId) {
        this.name       = name;
        this.iconItemId = iconItemId;
        this.savedAt    = Instant.now().toString();
        this.slots      = new HashMap<>();
    }

    /** Human-readable short date, e.g. "2025-01-15" */
    public String shortDate() {
        if (savedAt == null || savedAt.length() < 10) return "";
        return savedAt.substring(0, 10);
    }
}
