package de.MarkusTieger.Tigxa;

import com.formdev.flatlaf.FlatLightLaf;
import de.MarkusTieger.Tigxa.api.IAPI;
import de.MarkusTieger.Tigxa.api.impl.main.MainAPI;
import de.MarkusTieger.Tigxa.downloads.FileDownloader;
import de.MarkusTieger.Tigxa.extension.IExtension;
import de.MarkusTieger.Tigxa.extensions.impl.ExtensionManager;
import de.MarkusTieger.Tigxa.extensions.impl.internal.AdblockerExtension;
import de.MarkusTieger.Tigxa.extensions.impl.internal.SettingsExtension;
import de.MarkusTieger.Tigxa.gui.screen.InternalScreenRegistry;
import de.MarkusTieger.Tigxa.gui.theme.ThemeManager;
import de.MarkusTieger.Tigxa.gui.window.BrowserWindow;
import de.MarkusTieger.Tigxa.http.cookie.CookieManager;
import de.MarkusTieger.Tigxa.media.MediaUtils;
import de.MarkusTieger.Tigxa.update.Updater;
import de.MarkusTieger.Tigxa.update.Version;
import de.MarkusTieger.Tigxa.web.TrustManager;
import de.MarkusTieger.Tigxa.web.WebUtils;
import lombok.Getter;
import lombok.Setter;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import javax.swing.*;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;


import javax.speech.Central;
import javax.speech.synthesis.Synthesizer;
import javax.speech.synthesis.SynthesizerModeDesc;

public class Browser {

    private static final Logger LOGGER = LogManager.getLogger(Browser.class);

    public static final String DEFAULT_HOMEPAGE = "https://google.com";
    public static final String DEFAULT_SEARCH = "https://google.com/search?q=%s";

    public static final String NAME;
    public static final String FULL_NAME;
    public static final String VERSION;
    public static final String BUILD;
    public static final String FULL_VERSION;
    public static final String COMMIT_HASH;
    public static final String AUTHOR;

    public static String HOMEPAGE;
    public static String SEARCH;
    public static boolean SAVE_COOKIES;
    public static String FONT;

    static {

        String[] args = Browser.class.getPackageName().split("\\.");

        String name = Browser.class.getPackage().getSpecificationTitle();
        if (name == null) {
            name = args[args.length - 1];
        }

        NAME = name;
        FULL_NAME = NAME + " Browser";

        String version = Browser.class.getPackage().getSpecificationVersion();
        if (version == null) {
            version = "0.0.0";
        }

        VERSION = version;

        String build = Browser.class.getPackage().getImplementationVersion();
        if (build == null) {
            build = "-";
        }

        BUILD = build;

        String hash = Browser.class.getPackage().getImplementationTitle();
        if (hash == null) {
            hash = "-";
        }
        COMMIT_HASH = hash;

        FULL_VERSION = VERSION + (BUILD.equalsIgnoreCase("-") ? "" : ("-" + BUILD)) + (COMMIT_HASH.equalsIgnoreCase("-") ? "" : ("-" + COMMIT_HASH));


        String author = Browser.class.getPackage().getImplementationVendor();

        if(author == null){
            author = args[1];
        }

        AUTHOR = author;


    }

    @Getter
    private static final List<BrowserWindow> windows = Collections.synchronizedList(new ArrayList<>());
    @Getter
    private static final List<JFrame> frames = Collections.synchronizedList(new ArrayList<>());


    private static File configRoot;
    private static Properties config;

    @Getter
    private static ExtensionManager extmanager;

    @Getter
    private static IAPI mainAPI;

    @Getter
    private static Updater updater = new Updater();

    @Getter
    private static FileDownloader downloader = new FileDownloader();

    @Getter
    private static List<Consumer<Version>> updateListener = new ArrayList<>();

    @Getter
    private static Version latest = null;

    @Setter
    private static Function<IAPI, IExtension> injectedExtension;

    @Getter
    private static Mode mode;

    public static void start(Mode mode) {

        Browser.mode = mode;

        LOGGER.info("Starting " + FULL_NAME + " v." + FULL_VERSION);

        LOGGER.info("Initializing Web-Engine...");
        WebUtils.initialize(mode);

        LOGGER.info("Initialize Media-Engine...");
        MediaUtils.initialize();

        LOGGER.info("Initialize TrustManager...");
        TrustManager.initialize();

        LOGGER.info("Initialize Config-Root...");
        configRoot = initializeConfigRoot();

        LOGGER.info("Loading Configuration...");
        config = loadConfig();

        LOGGER.info("Loading Configurations into Memory...");
        HOMEPAGE = config.getProperty("homepage", DEFAULT_HOMEPAGE);
        SEARCH = config.getProperty("search", DEFAULT_SEARCH);
        SAVE_COOKIES = !config.getProperty("save_cookies", "true").equalsIgnoreCase("false");
        FONT = config.getProperty("font", "-");
        if(FONT.equalsIgnoreCase("-")) FONT = null;

        LOGGER.info("Applying Theme...");

        if (!ThemeManager.setTheme(config)) {
            ThemeManager.setTheme(FlatLightLaf.class);
        }

        ThemeManager.applyFontByConfig(config);

        if(SAVE_COOKIES) {
            LOGGER.info("Initialize Cookie-Store...");
            CookieManager.initialize(configRoot);
        }

        LOGGER.info("Initializing Extension-API...");

        mainAPI = new MainAPI(configRoot);

        LOGGER.info("Initializing Screens...");

        InternalScreenRegistry registry = new InternalScreenRegistry(mainAPI);
        registry.init();
        registry.apply();

        LOGGER.info("Registring Update-Watcher...");

        updateListener.add((ver) -> latest = ver);

        LOGGER.info("Checking for Updates...");

        checkUpdates();

        LOGGER.info("Initializing Extension-Manager...");

        extmanager = new ExtensionManager();

        LOGGER.info("Loading Extensions...");
        try {
            extmanager.loadExtensions(mainAPI, configRoot);

            if(injectedExtension != null){
                LOGGER.info("Injected-Extension found! Injecting...");
                extmanager.loadExtension(mainAPI, injectedExtension);
            }

            LOGGER.info("Loading Internal-Extensions...");
            extmanager.loadExtension(mainAPI, AdblockerExtension::new);
            extmanager.loadExtension(mainAPI, SettingsExtension::new);
        } catch (IOException e) {
            LOGGER.warn("Extensions can't loaded", e);
        }

        LOGGER.info("Creating Window...");
        createWindowWithDefaultHomePage(mode, mainAPI);

        LOGGER.info("Enabling Extensions...");
        extmanager.enableExtensions();

        LOGGER.info("Store Configurations...");
        storeConfig(config);

    }

    private static void checkUpdates() {

        LOGGER.debug("Checking Update-Conditions...");

        if(!updater.checkJar()) return;
        if(updater.isUpdated()) return;
        if(updater.isDebugBuild()) return;

        LOGGER.debug("Retrieving Latest-Update...");

        Version latest = updater.getLatestVersion();
        if(latest == null){
            LOGGER.debug("Latest Version can't found.");
            return;
        }
        LOGGER.debug("Checking Current-Version...");
        boolean update = false;
        if(!latest.version().equalsIgnoreCase(Browser.VERSION)) update = true;
        if(!latest.build().equalsIgnoreCase(Browser.BUILD)) update = true;
        if(!latest.commit().equalsIgnoreCase(Browser.COMMIT_HASH)) update = true;

        if(update) {
            LOGGER.debug("Newer Update found!");
            updateListener.forEach((listener) -> listener.accept(latest));
        } else LOGGER.debug("Current Version is the latest.");
    }

    public static void updateUI() {
        LOGGER.debug("Updating UI for Windows...");
        synchronized (windows) {
            windows.forEach(BrowserWindow::updateUI);
        }

        LOGGER.debug("Updating UI for Frames...");
        synchronized (frames) {
            frames.forEach(SwingUtilities::updateComponentTreeUI);
        }
    }

    private static Properties loadConfig() {

        LOGGER.debug("Checking Configuration...");

        File file = new File(configRoot, "tigxa.properties");
        if (file.exists()) {
            LOGGER.debug("Loading Configuration...");
            Properties prop = new Properties();
            try {
                FileReader reader = new FileReader(file, StandardCharsets.UTF_8);
                prop.load(reader);
                reader.close();
            } catch (IOException e) {
                LOGGER.warn("Configuration-Loading failed!", e);
            }
            return prop;
        } else {
            LOGGER.debug("Creating File...");
            try {
                file.createNewFile();
            } catch (IOException e) {
                LOGGER.warn("Can't Create File!", e);
            }
        }
        return new Properties();
    }

    private static void storeConfig(Properties prop) {

        LOGGER.debug("Preparing Config-Store...");

        File file = new File(configRoot, "tigxa.properties");
        if (!file.exists()) {
            LOGGER.debug("Creating File...");
            try {
                file.createNewFile();
            } catch (IOException e) {
                LOGGER.warn("Can't Create File!", e);
            }
        }

        LOGGER.debug("Writing to File...");
        try {
            FileWriter writer = new FileWriter(file, StandardCharsets.UTF_8);
            prop.store(writer, "Tigxa Configuration File");
            writer.flush();
            writer.close();
        } catch (IOException e) {
            LOGGER.warn("Can't write to File!", e);
        }
    }

    private static void createWindowWithDefaultHomePage(Mode mode, IAPI api) {
        LOGGER.debug("Initializing Window...");
        BrowserWindow window = new BrowserWindow();
        window.initWindow(mode, api, configRoot);
        LOGGER.debug("Opening Tab...");
        window.newTab((String) null, true);
        if(latest != null) {
            LOGGER.debug("Opening Update-Tab...");
            window.newTab(api.getGUIManager().getScreenRegistry().getRegistredScreen(api.getNamespace(), "update"), true);
        }
    }

    private static File initializeConfigRoot() {
        LOGGER.debug("Initializing Config-Root...");
        File configRoot = new File(System.getProperty("user.home", "."));
        if (!configRoot.exists()) {
            LOGGER.debug("Mkdir...");
            configRoot.mkdirs();
        }
        return configRoot;
    }

    public static void saveConfig() {

        LOGGER.debug("Save Config...");

        LOGGER.debug("Save Theme...");
        ThemeManager.saveConfig(config);

        LOGGER.debug("Save Memory...");
        config.setProperty("homepage", HOMEPAGE);
        config.setProperty("search", SEARCH);
        config.setProperty("save_cookies", SAVE_COOKIES ? "true" : "false");
        config.setProperty("font", FONT == null ? "-" : FONT);

        LOGGER.debug("Save-Config...");
        storeConfig(config);
    }


    public static enum Mode {

        JAVAFX, SWT, SWING, DJ_NATIVE_SWT, NONE;

    }

}
