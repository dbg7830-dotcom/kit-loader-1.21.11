package com.kitmod.util;

import com.kitmod.data.Kit;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.TagParser;
import net.minecraft.world.item.ItemStack;

import java.util.Map;

public class InventoryHelper {

    public static void snapshotTo(LocalPlayer player, Kit kit) {
        HolderLookup.Provider registries = player.level().registryAccess();
        var ops = registries.createSerializationContext(NbtOps.INSTANCE);

        for (int i = 0; i < 36; i++) {
            ItemStack stack = player.getInventory().items.get(i);
            if (!stack.isEmpty()) kit.slots.put(String.valueOf(i), serialise(stack, ops));
        }
        for (int i = 0; i < 4; i++) {
            ItemStack stack = player.getInventory().armor.get(i);
            if (!stack.isEmpty()) kit.slots.put(String.valueOf(36 + i), serialise(stack, ops));
        }
        ItemStack offhand = player.getInventory().offhand.get(0);
        if (!offhand.isEmpty()) kit.slots.put("40", serialise(offhand, ops));
    }

    public static void restoreFrom(LocalPlayer player, Kit kit) {
        HolderLookup.Provider registries = player.level().registryAccess();
        var ops = registries.createSerializationContext(NbtOps.INSTANCE);

        for (int i = 0; i < 36; i++) player.getInventory().items.set(i, ItemStack.EMPTY);
        for (int i = 0; i < 4;  i++) player.getInventory().armor.set(i, ItemStack.EMPTY);
        player.getInventory().offhand.set(0, ItemStack.EMPTY);

        for (Map.Entry<String, String> entry : kit.slots.entrySet()) {
            int slot;
            try { slot = Integer.parseInt(entry.getKey()); }
            catch (NumberFormatException e) { continue; }

            ItemStack stack = deserialise(entry.getValue(), ops, registries);
            if (stack == null || stack.isEmpty()) continue;

            if      (slot >= 0  && slot < 36) player.getInventory().items.set(slot, stack);
            else if (slot >= 36 && slot < 40) player.getInventory().armor.set(slot - 36, stack);
            else if (slot == 40)              player.getInventory().offhand.set(0, stack);
        }
    }

    private static String serialise(ItemStack stack,
            com.mojang.serialization.DynamicOps<net.minecraft.nbt.Tag> ops) {
        return ItemStack.CODEC.encodeStart(ops, stack)
                .result().map(Object::toString).orElse("");
    }

    private static ItemStack deserialise(String nbtString,
            com.mojang.serialization.DynamicOps<net.minecraft.nbt.Tag> ops,
            HolderLookup.Provider registries) {
        try {
            var nbt = TagParser.parseTag(nbtString);
            return ItemStack.CODEC.parse(ops, nbt).result().orElse(ItemStack.EMPTY);
        } catch (Exception e) {
            return ItemStack.EMPTY;
        }
    }
}
