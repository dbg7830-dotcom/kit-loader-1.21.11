package com.kitmod;

import com.kitmod.client.gui.KitManagerScreen;
import com.kitmod.client.keybind.KitKeybind;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

public class KitModClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        KitKeybind.register();

        // Open GUI on keybind press
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (KitKeybind.OPEN_GUI.consumeClick()) {
                if (client.screen == null) {
                    Minecraft.getInstance().setScreen(new KitManagerScreen());
                }
            }
        });

        // Hook mouse clicks and key presses into KitManagerScreen via Fabric ScreenEvents.
        // BeforeMouseClick lambda: (Screen screen, double mouseX, double mouseY, int button)
        // BeforeKeyPress lambda:   (Screen screen, int key, int scancode, int modifiers)
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (!(screen instanceof KitManagerScreen kitScreen)) return;

            ScreenMouseEvents.beforeMouseClick(screen).register(
                (Screen s, double mouseX, double mouseY, int button) ->
                    kitScreen.handleClick(mouseX, mouseY, button)
            );

            ScreenKeyboardEvents.beforeKeyPress(screen).register(
                (Screen s, int key, int scancode, int modifiers) ->
                    kitScreen.handleKey(key)
            );
        });
    }
}
