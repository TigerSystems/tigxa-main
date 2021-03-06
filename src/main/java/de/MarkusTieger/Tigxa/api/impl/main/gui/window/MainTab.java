package de.MarkusTieger.Tigxa.api.impl.main.gui.window;

import de.MarkusTieger.Tigxa.api.window.ITab;
import de.MarkusTieger.Tigxa.api.window.IWindow;
import de.MarkusTieger.Tigxa.api.window.TabType;
import de.MarkusTieger.Tigxa.web.WebUtils;
import lombok.Getter;

import javax.swing.*;
import java.awt.*;

public class MainTab implements ITab {

    private final MainWindow window;
    private final TabType type;

    @Getter
    private final Component comp;

    public MainTab(MainWindow window, TabType type, Component comp) {
        this.comp = comp;
        this.window = window;
        this.type = type;
    }

    @Override
    public String getTitle() {
        return window.window.tabs.getTitleAt(window.window.tabs.indexOfComponent(comp));
    }

    @Override
    public Icon getIcon() {
        return window.window.tabs.getIconAt(window.window.tabs.indexOfComponent(comp));
    }

    @Override
    public void remove() {
        if (isActive()) {
            window.map.remove(window, this);
            window.window.tabs.remove(window.window.tabs.indexOfComponent(comp));
            WebUtils.unloadTab(window.window, comp);
            window.window.update();
        }
    }

    @Override
    public TabType getType() {
        return type;
    }

    @Override
    public IWindow getWindow() {
        return window;
    }

    @Override
    public boolean isActive() {
        return window.map.containsValue(this);
    }
}
