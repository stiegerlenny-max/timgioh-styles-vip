package de.timgioh.styles;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.pack.PackScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TimgiohStylesMod implements ClientModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("timgioh styles");

    private static boolean cleaned = false;

    @Override
    public void onInitializeClient() {
        StyleManager.init();
        ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
            if (screen instanceof TitleScreen || screen instanceof GameMenuScreen || screen instanceof PackScreen) {
                if (!cleaned && screen instanceof TitleScreen) {
                    cleaned = true;
                    StyleManager.cleanup();
                }
                ButtonWidget button = ButtonWidget.builder(Text.literal("timgioh styles"),
                        b -> client.setScreen(new StylesScreen(screen)))
                    .dimensions(6, 6, 100, 20)
                    .build();
                Screens.getButtons(screen).add(button);
            }
        });
    }
}
