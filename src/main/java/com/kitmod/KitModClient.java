package com.kitmod;

import com.kitmod.client.gui.KitManagerScreen;
import com.kitmod.client.keybind.KitKeybind;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.minecraft.client.Minecraft;

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

        // Hook mouse clicks and key presses into KitManagerScreen
        // This is needed because the new 1.21.9+ mouseClicked/keyPressed signatures
        // on Screen use event objects we can't import, so we use Fabric ScreenEvents instead.
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (!(screen instanceof KitManagerScreen kitScreen)) return;

            ScreenMouseEvents.beforeMouseClick(screen).register((s, mouseX, mouseY, button) -> {
                // Let our custom handler run first; buttons handle themselves via widget system
                kitScreen.handleClick(mouseX, mouseY, button);
            });

            ScreenKeyboardEvents.beforeKeyPress(screen).register((s, key, scancode, modifiers) -> {
                kitScreen.handleKey(key);
            });
        });
    }
}
