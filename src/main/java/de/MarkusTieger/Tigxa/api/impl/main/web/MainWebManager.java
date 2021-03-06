package de.MarkusTieger.Tigxa.api.impl.main.web;

import de.MarkusTieger.Tigxa.api.IAPI;
import de.MarkusTieger.Tigxa.api.engine.IEngine;
import de.MarkusTieger.Tigxa.api.impl.main.gui.window.MainTab;
import de.MarkusTieger.Tigxa.api.impl.main.gui.window.MainWindow;
import de.MarkusTieger.Tigxa.api.web.IWebEngine;
import de.MarkusTieger.Tigxa.api.web.IWebManager;
import de.MarkusTieger.Tigxa.api.window.ITab;
import de.MarkusTieger.Tigxa.api.window.IWindow;
import de.MarkusTieger.Tigxa.api.window.TabType;
import javafx.scene.web.WebView;

import java.awt.*;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class MainWebManager implements IWebManager {

    private final IAPI api;

    public MainWebManager(IAPI api) {
        this.api = api;
    }

    private final Map<WebView, IWebEngine> map = Collections.synchronizedMap(new HashMap<>());

    @Override
    public IWebEngine getEngineByTab(ITab iTab) {
        if (iTab.getType() != TabType.WEB) return null;

        IWindow window = iTab.getWindow();
        if (!api.getWindowManager().listWindows().contains(window)) return null;
        if (!window.listTabs().contains(iTab)) return null;

        Map<Component, IEngine> map = ((MainWindow)window).window.getTabLinks();
        synchronized (map){
            return (IWebEngine) map.get(((MainTab)iTab).getComp());
        }
    }

    @Override
    public IWebEngine getEngineFromCurrentTab(IWindow iWindow) {
        return getEngineByTab(iWindow.getSelectedTab());
    }

}
