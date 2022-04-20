package de.MarkusTieger.Tigxa.bootstrap;

import com.formdev.flatlaf.FlatLightLaf;
import de.MarkusTieger.Tigxa.Browser;
import de.MarkusTieger.Tigxa.update.Updater;
import de.MarkusTieger.Tigxa.update.Version;
import org.apache.log4j.*;

import javax.swing.*;
import java.io.IOException;
import java.lang.reflect.Method;

public class Bootstrap {

    private static final Logger LOGGER = Logger.getLogger(Bootstrap.class);

    public static void main(String[] args) throws Throwable {

        if(args.length > 0){
            if(args[0].equalsIgnoreCase("first-exec-recovery")){
                ErrorLauncher.startUpdater();
                return;
            }
            if(args[0].equalsIgnoreCase("version")){
                System.out.println(Browser.FULL_NAME);
                System.out.println(Browser.FULL_VERSION);
                System.out.println("Made by " + Browser.AUTHOR);
                return;
            }
        }

        Logger root = Logger.getRootLogger();

        Layout layout = new PatternLayout("[%d{HH:mm:ss}] [ %t/%-5p] [%l] %m%n");

        Appender console = new ConsoleAppender(layout, ConsoleAppender.SYSTEM_OUT);
        root.addAppender(console);

        try {
            RollingFileAppender file = new RollingFileAppender(layout, "logs/latest.log", true);
            root.addAppender(file);
        } catch (IOException e) {
            LOGGER.warn("FileAppender can't set.", e);
        }

        LOGGER.info("----------------------------------------");

        LOGGER.info("Bootstrap is launching Application...");

        LOGGER.info("Initializing Default Look and Feel of this Application...");
        FlatLightLaf.setup();

        if(args.length > 0){
            if(args[0].equalsIgnoreCase("recovery")){
                LOGGER.info("Starting Recovery...");
                ErrorLauncher.launch(LaunchError.ALL_FAILED);
                return;
            }
            if(args[0].equalsIgnoreCase("discord-rpc")){

                update();

                LOGGER.info("Starting Discord-RPC Background-Worker...");
                Browser.initializeRPC();
                return;
            }
        }

        String force_launch = System.getProperty("tigxa.force_launch");

        if(force_launch == null || force_launch.equalsIgnoreCase("-")){
            try {
                LOGGER.info("Checking JavaFX integrity using \"" + Method.class.getPackage().getName() + "\"...");
                Class<?> clazz = Class.forName("javafx.application.Application");
                LOGGER.info("Launching...");
                try {
                    FXLauncher.main(args);
                } catch (Throwable e){
                    LOGGER.warn("Launch Failed.", e);
                    ErrorLauncher.launch(LaunchError.FX_NOT_LOADED);
                }
            } catch (ClassNotFoundException e) {
                LOGGER.warn("JavaFX was not found!", e);
                ErrorLauncher.launch(LaunchError.FX_NOT_FOUND);
            }
        } else {

            LOGGER.info("Using Force-Launch...");

            Browser.Mode mode = Browser.Mode.valueOf(force_launch.toUpperCase());
            if(mode == null){

                LOGGER.warn("Invalid Force-Launch-Property! Requesting Normal-Start...");

                int option = JOptionPane.showConfirmDialog(null, "Force-Launch failed. Would you like to Launch Normal?", "Force-Launch failed.", JOptionPane.YES_NO_OPTION);
                if(option == JOptionPane.YES_OPTION){
                    LOGGER.info("Restarting Normal...");
                    System.setProperty("tigxa.force_launch", "-");
                    main(args);
                } else {
                    LOGGER.info("Stopping Bootstrap...");
                    System.exit(0);
                }
            } else {
                if(mode == Browser.Mode.JAVAFX){
                    LOGGER.info("Force-Launch-Property is set to Default! Restarting...");
                    System.setProperty("tigxa.force_launch", "-");
                    main(args);
                } else {
                    LOGGER.info("Launching using Engine \"" + mode.name() + "\"...");
                    try {
                        Browser.start(mode);
                    } catch(Throwable e){
                        LOGGER.error("Launch Failed! Force-Launch is aktiv. Do nothing...", e);
                    }
                }
            }

        }
    }

    private static final Updater updater = new Updater();

    private static void update(){
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
            updater.update(latest, (e) -> {});
        } else LOGGER.debug("Current Version is the latest.");
    }

}
