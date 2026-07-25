package com.kitmod.util;

import com.kitmod.data.Kit;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

import java.util.Map;

/**
 * Snapshots and restores all 41 player inventory slots.
 *
 * Slot map:
 *   0-35  = main inventory (getItem/setItem)
 *   36    = boots     (EquipmentSlot.FEET)
 *   37    = leggings  (EquipmentSlot.LEGS)
 *   38    = chestplate(EquipmentSlot.CHEST)
 *   39    = helmet    (EquipmentSlot.HEAD)
 *   40    = offhand   (EquipmentSlot.OFFHAND)
 */
public class InventoryHelper {

    private static final EquipmentSlot[] ARMOR_SLOTS = {
        EquipmentSlot.FEET,
        EquipmentSlot.LEGS,
        EquipmentSlot.CHEST,
        EquipmentSlot.HEAD
    };

    public static void snapshotTo(LocalPlayer player, Kit kit) {
        HolderLookup.Provider registries = player.level().registryAccess();
        var ops = registries.createSerializationContext(NbtOps.INSTANCE);

        for (int i = 0; i < 36; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.isEmpty()) kit.slots.put(String.valueOf(i), serialise(stack, ops));
        }
        for (int i = 0; i < ARMOR_SLOTS.length; i++) {
            ItemStack stack = player.getItemBySlot(ARMOR_SLOTS[i]);
            if (!stack.isEmpty()) kit.slots.put(String.valueOf(36 + i), serialise(stack, ops));
        }
        ItemStack offhand = player.getItemBySlot(EquipmentSlot.OFFHAND);
        if (!offhand.isEmpty()) kit.slots.put("40", serialise(offhand, ops));
    }

    public static void restoreFrom(LocalPlayer player, Kit kit) {
        HolderLookup.Provider registries = player.level().registryAccess();
        var ops = registries.createSerializationContext(NbtOps.INSTANCE);

        for (int i = 0; i < 36; i++) player.getInventory().setItem(i, ItemStack.EMPTY);
        for (EquipmentSlot slot : ARMOR_SLOTS) player.setItemSlot(slot, ItemStack.EMPTY);
        player.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);

        for (Map.Entry<String, String> entry : kit.slots.entrySet()) {
            int slot;
            try { slot = Integer.parseInt(entry.getKey()); }
            catch (NumberFormatException e) { continue; }

            ItemStack stack = deserialise(entry.getValue(), ops);
            if (stack == null || stack.isEmpty()) continue;

            if      (slot >= 0  && slot < 36) player.getInventory().setItem(slot, stack);
            else if (slot >= 36 && slot < 40) player.setItemSlot(ARMOR_SLOTS[slot - 36], stack);
            else if (slot == 40)              player.setItemSlot(EquipmentSlot.OFFHAND, stack);
        }
    }

    private static String serialise(ItemStack stack,
            com.mojang.serialization.DynamicOps<net.minecraft.nbt.Tag> ops) {
        return ItemStack.CODEC.encodeStart(ops, stack)
                .result().map(Object::toString).orElse("");
    }

    private static ItemStack deserialise(String nbtString,
            com.mojang.serialization.DynamicOps<net.minecraft.nbt.Tag> ops) {
        try {
            // Use snbt parsing via CompoundTag — TagParser.parseCompound in 1.21.11
            net.minecraft.nbt.CompoundTag nbt = net.minecraft.nbt.TagParser.parseCompound(nbtString);
            return ItemStack.CODEC.parse(ops, nbt).result().orElse(ItemStack.EMPTY);
        } catch (Exception e) {
            return ItemStack.EMPTY;
        }
    }
}
