package com.kitmod;

import com.kitmod.client.gui.KitManagerScreen;
import com.kitmod.client.gui.MarketplaceKitDetailScreen;
import com.kitmod.client.gui.MarketplaceScreen;
import com.kitmod.client.gui.MarketplaceUploadScreen;
import com.kitmod.client.keybind.KitKeybind;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

public class KitModClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        KitKeybind.register();

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (KitKeybind.OPEN_GUI.consumeClick()) {
                if (client.screen == null) {
                    Minecraft.getInstance().setScreen(new KitManagerScreen());
                }
            }
        });

        ScreenEvents.AFTER_INIT.register(
            (Minecraft client, Screen screen, int sw, int sh) -> {

                if (screen instanceof KitManagerScreen s) {
                    ScreenMouseEvents.beforeMouseClick(screen).register(
                        (Screen sc, double mx, double my, int btn) -> { s.handleClick(mx, my, btn); });
                    ScreenKeyboardEvents.beforeKeyPress(screen).register(
                        (Screen sc, int key, int scan, int mods) -> { s.handleKey(key); });

                } else if (screen instanceof MarketplaceScreen s) {
                    ScreenMouseEvents.beforeMouseClick(screen).register(
                        (Screen sc, double mx, double my, int btn) -> { s.handleClick(mx, my); });

                } else if (screen instanceof MarketplaceKitDetailScreen s) {
                    ScreenMouseEvents.beforeMouseClick(screen).register(
                        (Screen sc, double mx, double my, int btn) -> { s.handleClick(mx, my); });
                    ScreenKeyboardEvents.beforeKeyPress(screen).register(
                        (Screen sc, int key, int scan, int mods) -> { s.handleKey(key); });

                } else if (screen instanceof MarketplaceUploadScreen s) {
                    ScreenMouseEvents.beforeMouseClick(screen).register(
                        (Screen sc, double mx, double my, int btn) -> { s.handleClick(mx, my); });
                    ScreenKeyboardEvents.beforeKeyPress(screen).register(
                        (Screen sc, int key, int scan, int mods) -> { s.handleKey(key); });
                }
            }
        );
    }
}
