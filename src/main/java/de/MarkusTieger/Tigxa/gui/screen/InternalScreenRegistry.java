package de.MarkusTieger.Tigxa.gui.screen;

import com.formdev.flatlaf.FlatLightLaf;
import com.yubico.client.v2.VerificationResponse;
import de.MarkusTieger.Tigxa.Browser;
import de.MarkusTieger.Tigxa.api.IAPI;
import de.MarkusTieger.Tigxa.api.gui.IScreen;
import de.MarkusTieger.Tigxa.api.gui.registry.IScreenRegistry;
import de.MarkusTieger.Tigxa.api.impl.main.gui.screen.MainScreenRegistry;
import de.MarkusTieger.Tigxa.gui.theme.Theme;
import de.MarkusTieger.Tigxa.gui.theme.ThemeCategory;
import de.MarkusTieger.Tigxa.gui.theme.ThemeManager;
import de.MarkusTieger.Tigxa.gui.window.PasswordWindow;
import de.MarkusTieger.Tigxa.http.cookie.CookieManager;
import de.MarkusTieger.Tigxa.lang.Translator;
import de.MarkusTieger.Tigxa.update.Updater;
import de.MarkusTieger.Tigxa.update.Version;
import lombok.Getter;
import org.chromium.userinterface.GameScreen;

import javax.speech.Central;
import javax.speech.synthesis.Synthesizer;
import javax.speech.synthesis.SynthesizerModeDesc;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.*;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class InternalScreenRegistry {

    private final HashMap<String, IScreen> map = new HashMap<>();
    private final IAPI api;

    private IScreen about;
    private IScreen settings;
    private IScreen update;
    private IScreen chromeDino;

    public InternalScreenRegistry(IAPI api){
        this.api = api;
    }

    public void init(){

        initAbout();
        initSettings();
        initUpdate();
        initChromeDino();

    }

    public void apply(){
        api.getGUIManager().getScreenRegistry().registerScreen(about, "about");
        api.getGUIManager().getScreenRegistry().registerScreen(settings, "settings");
        api.getGUIManager().getScreenRegistry().registerScreen(update, "update");
        ((MainScreenRegistry)api.getGUIManager().getScreenRegistry()).registerScreen(chromeDino, "chrome", "dino");
    }

    private void initChromeDino(){
        chromeDino = api.getGUIManager().createScreen(Translator.translate(26), "chrome://dino");

        GameScreen gameScreen = new GameScreen(chromeDino.getContentPane());
        chromeDino.getContentPane().addKeyListener(gameScreen);
        chromeDino.getContentPane().addMouseListener(gameScreen);
        chromeDino.getContentPane().add(gameScreen);

        gameScreen.startGame();

    }

    private void initUpdate(){
        update = api.getGUIManager().createScreen(Translator.translate(27), api.getNamespace() + "://update");

        update.getContentPane().setLayout(null);

        JLabel label = new JLabel();
        label.setBounds(25, 25, 500, 50);
        update.getContentPane().add(label);

        JProgressBar bar = new JProgressBar();
        bar.setMinimum(0);
        bar.setMaximum(10000);
        bar.setBounds(25, 75, 500, 10);
        update.getContentPane().add(bar);

        JButton btn = new JButton(Translator.translate(27));
        btn.setBounds(25, 125, 100, 25);
        btn.setEnabled(false);
        update.getContentPane().add(btn);

        Browser.getUpdateListener().add((latest) -> {

            btn.setEnabled(true);
            String txt = Translator.translate(28, Browser.FULL_NAME, latest.version(), latest.build(), latest.commit());
            System.out.println(txt);
            label.setText(txt);
            btn.addActionListener((e) -> {

                btn.setEnabled(false);

                new Thread(() -> {
                    Browser.getUpdater().update(latest, (percend) -> {

                        if(percend == -1D){
                            bar.setValue(10000);
                            bar.setForeground(Color.GREEN);
                        } else {
                            bar.setValue((int) (percend * 100D));
                        }

                    });
                }, Translator.translate(29)).start();

            });

        });
    }

    private void initAbout(){

        about = api.getGUIManager().createScreen(Translator.translate(30), api.getNamespace() + "://about");

        about.getContentPane().setLayout(null);

        JLabel label = new JLabel() {

            private boolean rendered = false;

            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);

                if(rendered) return;
                rendered = true;
                new Thread(this::start, "Starter").start();
            }

            private void start(){
                try
                {

                    InputStream in = Browser.class.getResourceAsStream("/res/gui/about/text");
                    String text = new String(in.readAllBytes(), StandardCharsets.UTF_8);
                    in.close();

                    System.setProperty("freetts.voices", "com.sun.speech.freetts.en.us" + ".cmu_us_kal.KevinVoiceDirectory");
                    Central.registerEngineCentral("com.sun.speech.freetts" + ".jsapi.FreeTTSEngineCentral");
                    Synthesizer synthesizer = Central.createSynthesizer(new SynthesizerModeDesc(Locale.US));
                    synthesizer.allocate();
                    synthesizer.resume();

                    while (true){
                        synthesizer.speakPlainText(text, null);
                        synthesizer.waitEngineState(Synthesizer.QUEUE_EMPTY);
                    }

                    // synthesizer.deallocate();
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
            }
        };
        label.setBounds(0, 0, 20, 20);
        about.getContentPane().add(label);
    }

    private void initSettings() {
        settings = api.getGUIManager().createScreen(Translator.translate(31), api.getNamespace() + "://settings");

        JPanel frame = settings.getContentPane();
        frame.setLayout(null);



        Class<?> current = ThemeManager.getTheme();
        ThemeCategory current_category = ThemeManager.getCategory(current);

        if (current_category == null || current == null) {
            current_category = ThemeCategory.FLATLAF;
            current = FlatLightLaf.class;
        }

        JComboBox<ThemeCategory> categoryCombo = new JComboBox<>();
        categoryCombo.setBounds(25, 25, 150, 25);

        for (ThemeCategory category : ThemeCategory.values()) {
            categoryCombo.addItem(category);
        }
        categoryCombo.setSelectedItem(current_category);

        frame.add(categoryCombo);

        JComboBox<Theme> themeCombo = new JComboBox<>();
        themeCombo.setBounds(25, 75, 150, 25);

        Theme selected = null;
        for (Map.Entry<String, Class<?>> e : ThemeManager.getThemesByCategory(current_category).entrySet()) {
            if (e.getValue() == current) {
                themeCombo.addItem(selected = new Theme(e.getKey(), e.getValue()));
            } else {
                themeCombo.addItem(new Theme(e.getKey(), e.getValue()));
            }
        }
        if (selected != null) themeCombo.setSelectedItem(selected);

        frame.add(themeCombo);


        categoryCombo.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent event) {
                themeCombo.removeAllItems();
                for (Map.Entry<String, Class<?>> e : ThemeManager.getThemesByCategory((ThemeCategory) categoryCombo.getSelectedItem()).entrySet()) {
                    themeCombo.addItem(new Theme(e.getKey(), e.getValue()));
                }
            }
        });

        JButton btn = new JButton(Translator.translate(32));
        btn.setBounds(25, 125, 150, 25);
        btn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ThemeManager.setTheme(((Theme) themeCombo.getSelectedItem()).clazz());
                Browser.updateUI();
                Browser.saveConfig();
            }
        });
        frame.add(btn);

        JButton pwd = new JButton(Translator.translate(33));
        pwd.setEnabled(Browser.SAVE_COOKIES);
        pwd.setBounds(25, 175, 150, 25);
        pwd.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

                new Thread(() -> {
                    char[] pwd = PasswordWindow.requestPWD(Translator.translate(34), (value) -> {
                        return value;
                    });

                    if (pwd != null) {
                        if (pwd.length == 0) {
                            CookieManager.getDef().getStore().setPwd(null);
                        } else {
                            CookieManager.getDef().getStore().setPwd(pwd);
                        }
                    }

                    VerificationResponse yubi = PasswordWindow.requestYUBI(Translator.translate(35), (value) -> {
                        return value;
                    });

                    if (yubi != null) {
                        if (yubi.isOk()) {
                            CookieManager.getDef().getStore().setYubi(yubi);
                        } else {
                            CookieManager.getDef().getStore().setYubi(null);
                        }
                    }

                    CookieManager.getDef().getStore().save();
                }, "Cookie-Change-Window-Thread").start();
            }
        });
        frame.add(pwd);

        JCheckBox saveCookies = new JCheckBox(Translator.translate(36));
        saveCookies.setBounds(25, 225, 150, 25);
        saveCookies.setSelected(Browser.SAVE_COOKIES);
        saveCookies.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                Browser.SAVE_COOKIES = saveCookies.isSelected();
                Browser.saveConfig();
            }
        });
        frame.add(saveCookies);

        JButton erease = new JButton(Translator.translate(27));
        erease.setBounds(25, 275, 150, 25);
        erease.addActionListener((e) -> {

            CookieManager.getDef().getStore().erease();

        });
        frame.add(erease);

        JTextField homepage = new JTextField();
        homepage.setText(Browser.HOMEPAGE);
        homepage.setBounds(25, 325, 200, 25);
        homepage.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                update();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                update();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                update();
            }

            private void update(){
                Browser.HOMEPAGE = homepage.getText();
                Browser.saveConfig();
            }
        });
        frame.add(homepage);


        JTextField search = new JTextField();
        search.setText(Browser.SEARCH);
        search.setBounds(25, 375, 200, 25);
        search.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                update();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                update();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                update();
            }

            private void update(){
                Browser.SEARCH = search.getText();
                Browser.saveConfig();
            }
        });
        frame.add(search);

        FontItem default_font = new FontItem(null);

        JComboBox<FontItem> item = new JComboBox<FontItem>();
        item.setBounds(25, 425, 150, 25);
        item.addItem(default_font);

        GraphicsEnvironment env = GraphicsEnvironment.getLocalGraphicsEnvironment();

        Map<String, FontItem> map = new HashMap<>();

        for(Font f : env.getAllFonts()){
            FontItem fi = new FontItem(f);
            item.addItem(fi);
            map.put(f.getName(), fi);
        }

        item.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                if(((FontItem)item.getSelectedItem()).font == null) {
                    Browser.FONT = null;
                    Browser.saveConfig();
                    return;
                }
                for(Map.Entry<String, FontItem> i : map.entrySet()){
                    if(i.getValue() == item.getSelectedItem()){
                        Browser.FONT = i.getKey();
                        Browser.saveConfig();
                    }
                }
            }
        });

        if(Browser.FONT != null){
            InternalScreenRegistry.FontItem i = map.get(Browser.FONT);
            if(i != null) {
                item.setSelectedItem(i);
            }
        }

        frame.add(item);


    }

    public static class FontItem {

        @Getter
        private final Font font;

        public FontItem(Font font){
            this.font = font;
        }

        @Override
        public String toString() {
            return font == null ? Translator.translate(38) : font.getName();
        }
    }

}
