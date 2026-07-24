package com.kitmod;

import com.kitmod.client.gui.KitManagerScreen;
import com.kitmod.client.keybind.KitKeybind;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;

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
    }
}
