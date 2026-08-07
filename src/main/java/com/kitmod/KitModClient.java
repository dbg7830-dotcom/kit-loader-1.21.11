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
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.input.MouseInput;

public class KitModClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        KitKeybind.register();

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (KitKeybind.OPEN_GUI.wasPressed()) {
                if (client.currentScreen == null) {
                    MinecraftClient.getInstance().setScreen(new KitManagerScreen());
                }
            }
        });

        ScreenEvents.AFTER_INIT.register(new ScreenEvents.AfterInit() {
            @Override
            public void afterInit(MinecraftClient client, Screen screen, int scaledWidth, int scaledHeight) {

                if (screen instanceof KitManagerScreen kitScreen) {
                    ScreenMouseEvents.beforeMouseClick(screen).register(
                        new ScreenMouseEvents.BeforeMouseClick() {
                            @Override
                            public void beforeMouseClick(Screen s, MouseInput event) {
                                // Coordinates come from the mouse object, not the event
                                double mx = client.mouse.getX() * scaledWidth / client.getWindow().getWidth();
                                double my = client.mouse.getY() * scaledHeight / client.getWindow().getHeight();
                                kitScreen.handleClick(mx, my, event.button());
                            }
                        }
                    );
                    ScreenKeyboardEvents.beforeKeyPress(screen).register(
                        new ScreenKeyboardEvents.BeforeKeyPress() {
                            @Override
                            public void beforeKeyPress(Screen s, KeyInput event) {
                                kitScreen.handleKey(event.key());
                            }
                        }
                    );

                } else if (screen instanceof MarketplaceScreen mScreen) {
                    ScreenMouseEvents.beforeMouseClick(screen).register(
                        new ScreenMouseEvents.BeforeMouseClick() {
                            @Override
                            public void beforeMouseClick(Screen s, MouseInput event) {
                                double mx = client.mouse.getX() * scaledWidth / client.getWindow().getWidth();
                                double my = client.mouse.getY() * scaledHeight / client.getWindow().getHeight();
                                mScreen.handleClick(mx, my);
                            }
                        }
                    );

                } else if (screen instanceof MarketplaceKitDetailScreen dScreen) {
                    ScreenMouseEvents.beforeMouseClick(screen).register(
                        new ScreenMouseEvents.BeforeMouseClick() {
                            @Override
                            public void beforeMouseClick(Screen s, MouseInput event) {
                                double mx = client.mouse.getX() * scaledWidth / client.getWindow().getWidth();
                                double my = client.mouse.getY() * scaledHeight / client.getWindow().getHeight();
                                dScreen.handleClick(mx, my);
                            }
                        }
                    );
                    ScreenKeyboardEvents.beforeKeyPress(screen).register(
                        new ScreenKeyboardEvents.BeforeKeyPress() {
                            @Override
                            public void beforeKeyPress(Screen s, KeyInput event) {
                                dScreen.handleKey(event.key());
                            }
                        }
                    );

                } else if (screen instanceof MarketplaceUploadScreen uScreen) {
                    ScreenMouseEvents.beforeMouseClick(screen).register(
                        new ScreenMouseEvents.BeforeMouseClick() {
                            @Override
                            public void beforeMouseClick(Screen s, MouseInput event) {
                                double mx = client.mouse.getX() * scaledWidth / client.getWindow().getWidth();
                                double my = client.mouse.getY() * scaledHeight / client.getWindow().getHeight();
                                uScreen.handleClick(mx, my);
                            }
                        }
                    );
                    ScreenKeyboardEvents.beforeKeyPress(screen).register(
                        new ScreenKeyboardEvents.BeforeKeyPress() {
                            @Override
                            public void beforeKeyPress(Screen s, KeyInput event) {
                                uScreen.handleKey(event.key());
                            }
                        }
                    );
                }
            }
        });
    }
}
