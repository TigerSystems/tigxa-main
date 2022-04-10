package de.markustieger.tigxa.extension.api.impl.main.gui.context;

import de.markustieger.tigxa.extension.api.action.IActionHandler;
import de.markustieger.tigxa.extension.api.gui.context.IContextEntry;
import javafx.application.Platform;

import javax.swing.*;

public class MainContextEntry implements IContextEntry {

    private final IActionHandler action;
    private final JMenu handler;
    private final MainContextMenu main;

    public MainContextEntry(MainContextMenu main, IActionHandler action, JMenu handler) {
        this.action = action;
        this.handler = handler;
        this.main = main;
    }

    @Override
    public IContextEntry addEntry(String name, String actionId, boolean allowSubs) {
        JMenuItem item = allowSubs ? new JMenu(name) : new JMenuItem(name);
        item.addActionListener((e) -> {
            if (main.isFxthread()) {
                Platform.runLater(() -> action.onAction(main.getLastWindow(), actionId));
            } else {
                action.onAction(main.getLastWindow(), actionId);
            }
        });
        handler.add(item);
        return allowSubs ? new MainContextEntry(main, action, (JMenu) item) : null;
    }

    @Override
    public void addSeperator() {
        handler.addSeparator();
    }
}
