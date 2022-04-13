package de.MarkusTieger.Tigxa.extensions.impl.internal;

import de.MarkusTieger.Tigxa.Browser;
import de.MarkusTieger.Tigxa.api.IAPI;
import de.MarkusTieger.Tigxa.api.event.IEvent;
import de.MarkusTieger.Tigxa.api.gui.context.IContextEntry;
import de.MarkusTieger.Tigxa.api.gui.context.IContextMenu;
import de.MarkusTieger.Tigxa.api.permission.IPermissionResult;
import de.MarkusTieger.Tigxa.api.permission.Permission;
import de.MarkusTieger.Tigxa.api.web.IWebEngine;
import de.MarkusTieger.Tigxa.api.window.ITab;
import de.MarkusTieger.Tigxa.api.window.IWindow;
import de.MarkusTieger.Tigxa.api.window.TabType;
import de.MarkusTieger.Tigxa.extension.impl.BasicExtension;
import de.MarkusTieger.Tigxa.gui.window.ConfigWindow;

public class SettingsExtension extends BasicExtension {

    private final IAPI api;

    public SettingsExtension(IAPI api) {
        super(api.getPermissionManager(), "Settings", Browser.VERSION, new String[]{"MarkusTieger"}, Browser.class.getResource("/res/gui/extensions/settings.png"));
        this.api = api;
    }

    @Override
    public void onLoad() {

    }

    @Override
    public void onEnable() {

    }

    @Override
    public void onDisable() {

    }

    @Override
    public void onAction(IWindow window, int relativeX, int relativeY, int absoluteX, int absoluteY) {

        IContextMenu menu = api.getGUIManager().createContextMenu(true, api.getActionHandler());

        menu.addEntry("New Tab", "new_tab", false);
        menu.addEntry("New Window", "new_window", false);

        menu.addSeperator();

        menu.addEntry("Downloads", "downloads", false);
        menu.addEntry("Passwords", "passwords", false);
        menu.addEntry("Extensions", "extensions", false);

        menu.addSeperator();

        menu.addEntry("Print", "print", false);
        // menu.addEntry("Search", "search", false);

        IContextEntry entry = menu.addEntry("Zoom", "", true);
        entry.addEntry("200%", "zoom_200", false);
        entry.addEntry("100%", "zoom_100", false);
        entry.addEntry("75%", "zoom_75", false);
        entry.addEntry("50%", "zoom_50", false);
        entry.addEntry("25%", "zoom_25", false);

        menu.addSeperator();

        menu.addEntry("Settings", "settings", false);

        entry = menu.addEntry("More Tools", "", true);
        entry.addEntry("Open Web-Terminal", "terminal", false);
        entry.addEntry("Show Source-Code", "source", false);

        entry = menu.addEntry("Help", "", true);
        entry.addEntry("Get Help", "help", false);
        entry.addEntry("Send Feedback", "feedback", false);
        entry.addEntry("Check for Updates", "update", false);
        entry.addEntry("About " + Browser.NAME, "about", false);

        menu.addSeperator();

        menu.addEntry("Exit", "exit", false);

        menu.show(window, relativeX, relativeY);

    }

    @Override
    public void onAction(IWindow window, String id) {

        if (id.equalsIgnoreCase("new_tab")) {
            if (window == null) return;
            window.add(null, true);
        }

        if (id.equalsIgnoreCase("new_window")) {
            api.getWindowManager().addWindow().add(null, true);
        }

        if (id.equalsIgnoreCase("print")) {
            if (window == null) return;

            ITab tab = window.getSelectedTab();
            if (tab == null) return;
            if (tab.getType() != TabType.WEB) return;

            IPermissionResult result = api.getPermissionManager().requestPermissions(new Permission[]{Permission.WEB});
            if (result.getDisallowed().size() > 0) return;

            IWebEngine engine = api.getWebManager().getEngineByTab(tab);
            if (engine == null) return;

            engine.print();
        }

        if (id.toLowerCase().startsWith("zoom_".toLowerCase())) {
            String data = id.substring(5);
            try {
                int value = Integer.parseInt(data);
                double factor = (((double) value) / 100D);

                if (window == null) return;

                ITab tab = window.getSelectedTab();
                if (tab == null) return;
                if (tab.getType() != TabType.WEB) return;

                IPermissionResult result = api.getPermissionManager().requestPermissions(new Permission[]{Permission.WEB});
                if (result.getDisallowed().size() > 0) return;

                IWebEngine engine = api.getWebManager().getEngineByTab(tab);
                if (engine == null) return;

                engine.setZoom(factor);
            } catch (NumberFormatException e) {
            }
        }

        if (id.equalsIgnoreCase("settings")) {
            ConfigWindow.create(); // TODO: Settings-Screen
        }

        if (id.equalsIgnoreCase("exit")) {
            System.exit(0);
        }

    }

    @Override
    public void onEvent(IEvent iEvent) {
    }
}
