package org.trivait.placechunk.gui;

import me.shedaniel.autoconfig.AutoConfigClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.SpriteIconButton;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.trivait.placechunk.PlaceChunk;
import org.trivait.placechunk.config.PlaceChunkConfig;

public class PCCButton extends SpriteIconButton.CenteredIcon {

    private static final OnPress onPress = (button) -> {
        Screen screen = AutoConfigClient.getConfigScreen(PlaceChunkConfig.class, null).get();
        Minecraft.getInstance().gui.setScreen(screen);
    };
    private static Identifier TEXTURE = Identifier.fromNamespaceAndPath(PlaceChunk.MOD_ID, "icon/icon");

    public PCCButton() {
        super(20, 20, Component.empty(), 16, 16, 0, 0, new WidgetSprites(TEXTURE), onPress, null, Button.DEFAULT_NARRATION, false);
    }
}
