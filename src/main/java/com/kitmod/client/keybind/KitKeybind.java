package com.kitmod.client.keybind;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.glfw.GLFW;

public class KitKeybind {

    public static KeyMapping OPEN_GUI;

    public static void register() {
        OPEN_GUI = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.kitmod.open_gui",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_V,
                "KitMod"
        ));
    }
}
