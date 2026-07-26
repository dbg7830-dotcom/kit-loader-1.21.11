package com.kitmod.client.keybind;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.glfw.GLFW;

public class KitKeybind {

    public static KeyMapping OPEN_GUI;

    private static final KeyMapping.Category CATEGORY =
            KeyMapping.Category.register(Identifier.fromNamespaceAndPath("kitmod", "general"));

    public static void register() {
        OPEN_GUI = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.kitmod.open_gui",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_V,
                CATEGORY
        ));
    }
}
