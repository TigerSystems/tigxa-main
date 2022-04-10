package de.markustieger.tigxa.extension;

import de.markustieger.tigxa.extension.api.gui.IGUIWindow;

import java.awt.*;

public interface IExtension {

    String getName();

    String getVersion();

    String[] getAuthors();

    Image getIcon();
    Image getDarkIcon();

    void onLoad();

    void onEnable();

    void onDisable();

    void onAction(IGUIWindow window, int relativeX, int relativeY, int absoluteX, int absoluteY);

    void onAction(IGUIWindow window, String id);

}
