package ml.bmlzootown;

import com.google.common.io.Files;
import ml.bmlzootown.config.ConfigManager;
import ml.bmlzootown.udp.UDPServer;
import net.md_5.bungee.config.Configuration;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

public class WrappingPaper {
    //Config files
    private static File configFile = new File("./wrapper/wrapper.yml");

    //Configs
    private static Configuration config;
    public static boolean debug = false;

    //Time variables
    public static int h;
    public static int m;
    public static int s;

    //Warning variables
    public static boolean warn;
    public static List<Integer> warnings;
    public static String warnCommand;

    public static boolean timeEnabled;
    public static boolean crashEnabled;

    public static String stopCommand;
    public static String jarFilePath;
    public static String jvmOptions;
    public static String launchOptions;
    public static int restartWait;

    public static void main(String[] args)throws InterruptedException {
        log("WrappingPaper by bmlzootown");
        Thread.sleep(1000);
        initConfig();
        Thread.sleep(1000);
        try {
            UDPServer udp = new UDPServer(25569);
            new Thread(udp).start();
        } catch (Exception e) {
            e.printStackTrace();
        }
        Thread.sleep(1000);
        Server.startServer();
    }

    private static void initConfig() throws InterruptedException {
        //Plugin updates and/or removal
        File wrapperDir = new File("./wrapper");
        if (!wrapperDir.exists()) {
            wrapperDir.mkdirs();
        }
        try {
            File updateDir = new File("./wrapper/updates");
            File pluginFolder = new File("./plugins");
            if (updateDir.exists() && pluginFolder.exists()) {
                File[] files = updateDir.listFiles();
                for (int i = 0; i < files.length; i++) {
                    String ext = Files.getFileExtension(files[i].getPath());
                    String name = Files.getNameWithoutExtension(files[i].getPath());
                    if (ext.equalsIgnoreCase("remove")) {
                        File pl = new File(pluginFolder + File.separator + name + ".jar");
                        if (pl.exists()) {
                            pl.delete();
                        }
                        files[i].delete();
                    } else if (ext.equalsIgnoreCase("disable")) {
                        File pl = new File(pluginFolder + File.separator + name + ".jar");
                        if (pl.exists()) {
                            Files.move(pl, new File(pluginFolder + File.separator + name + ".disabled"));
                        }
                        files[i].delete();
                    } else if (ext.equalsIgnoreCase("enable")) {
                        File pl = new File(pluginFolder + File.separator + name + ".disabled");
                        if (pl.exists()) {
                            Files.move(pl, new File(pluginFolder + File.separator + name + ".jar"));
                        }
                        files[i].delete();
                    } else if (ext.equalsIgnoreCase("jar")) {
                        File from = files[i];
                        File to = new File(pluginFolder + File.separator + name + "." + ext);
                        Files.move(from, to);
                        from.delete();
                    }
                }
            } else {
                updateDir.mkdirs();
                pluginFolder.mkdirs();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        //---------------

        if(!configFile.exists()){
            log("Config not found... Creating!");
            Thread.sleep(500);
            try {
                configFile.createNewFile();
                config = ConfigManager.getConfig(configFile);
                List<Integer> warnings = Arrays.asList(1800, 600, 300, 60, 30);
                config.set("restart.enable", true);
                config.set("restart.h", 4);
                config.set("restart.m", 0);
                config.set("restart.s", 0);
                config.set("restart.wait",5);
                config.set("restart.warn.enable", true);
                config.set("restart.warn.command", "say");
                config.set("restart.warn.timings", warnings);
                config.set("restart.crash.enable", true);
                config.set("stop-command","stop");
                config.set("server-jar","paperclip.jar");
                config.set("jvm-options","-Xmx1024M -Xms1024M");
                config.set("launch-options","nogui");
                config.set("debug", false);
                ConfigManager.saveConfig(config,configFile);
                Thread.sleep(500);
                log("Config created!");
                Thread.sleep(3000);
                System.exit(0);
            }catch (Exception e) {
                error(e.getMessage());
                Thread.sleep(3000);
                System.exit(0);
            }
        }
        config = ConfigManager.getConfig(configFile);
        try {
            timeEnabled = config.getBoolean("restart.enable");
            crashEnabled = config.getBoolean("restart.crash.enable");
            stopCommand = config.getString("stop-command");
            h = config.getInt("restart.h");
            m = config.getInt("restart.m");
            s = config.getInt("restart.s");
            warn = config.getBoolean("restart.warn.enable");
            warnCommand = config.getString("restart.warn.command");
            warnings = config.getIntList("restart.warn.timings");
            jarFilePath = config.getString("server-jar");
            jvmOptions = config.getString("jvm-options");
            launchOptions = config.getString("launch-options");
            restartWait = config.getInt("restart.wait");
            debug = config.getBoolean("debug");
        }catch (Exception e) {
            error(e.getMessage());
            System.exit(-1);
        }
        log("Config loaded!");
    }

    public static void reloadConfig() {
        config = ConfigManager.getConfig(configFile);
        try {
            timeEnabled = config.getBoolean("restart.enable");
            crashEnabled = config.getBoolean("restart.crash.enable");
            stopCommand = config.getString("stop-command");
            h = config.getInt("restart.h");
            m = config.getInt("restart.m");
            s = config.getInt("restart.s");
            warn = config.getBoolean("restart.warn.enable");
            warnCommand = config.getString("restart.warn.command");
            warnings = config.getIntList("restart.warn.timings");
            jarFilePath = config.getString("server-jar");
            jvmOptions = config.getString("jvm-options");
            launchOptions = config.getString("launch-options");
            restartWait = config.getInt("restart.wait");
            debug = config.getBoolean("debug");
        }catch (Exception e) {
            error(e.getMessage());
        }
    }

    public static void log(String string){
        Date date = new Date();
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("[HH:mm:ss");
        System.out.println(simpleDateFormat.format(date)+" INFO]: " + string);
    }

    public static void error(String string){
        Date date = new Date();
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("[HH:mm:ss");
        System.out.println(simpleDateFormat.format(date)+" ERROR]: " + string);
    }
}
