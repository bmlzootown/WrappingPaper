package ml.bmlzootown;

import com.google.common.io.Files;

import java.io.*;
import java.util.*;

public class Server {
    //Thread variables
    private static Runtime runtime = Runtime.getRuntime();
    private static Process runServer;
    private static Thread serverLogThread;
    private static Thread serverCmdThread;
    private static PrintStream cmdSender;

    //Restart variables
    public static Timer timeRestart;
    public static int h = WrappingPaper.h;
    public static int m = WrappingPaper.m;
    public static int s = WrappingPaper.s;
    private static Calendar restartCal = Calendar.getInstance();
    public static Date restartDate;

    //Warning variables
    public static List<Integer> warnings = WrappingPaper.warnings;
    public static Timer[] warnTimers = new Timer[warnings.size()];

    //Wrapper commands
    private static String stopCommand = WrappingPaper.stopCommand;
    private static String warnCommand = WrappingPaper.warnCommand;

    public static boolean willStop;
    public static boolean willRestart;

    public static void setupRestart() {
        if (timeRestart != null) {
            timeRestart.cancel();
            timeRestart = null;
        }

        int sec = ((h*60)*60) + (m*60) + s;

        restartCal = Calendar.getInstance();
        restartCal.add(Calendar.SECOND, sec);
        restartDate = restartCal.getTime();

        timeRestart = new Timer();
        timeRestart.schedule(new TimerTask() {
            @Override
            public void run() {
                willRestart = true;
                try {
                    willRestart = true;
                    cmdSender.println(warnCommand + " Restarting!");
                    cmdSender.println(stopCommand);
                    cmdSender.flush();
                    cmdSender.close();
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        }, restartDate);
    }

    public static void setupWarnings() {
        if (warnTimers.length != 0) {
            for (int i = 0; i < warnTimers.length; i++) {
                if (warnTimers[i] != null) {
                    warnTimers[i].cancel();
                    warnTimers[i] = null;
                }
            }
        }
        int sec = ((h*60)*60) + (m*60) + s;
        for (int i = 0; i < warnings.size(); i++) {
            int t = warnings.get(i);
            if (sec > t) {
                Calendar warningCal = Calendar.getInstance();
                warningCal.add(Calendar.SECOND, (sec - t));
                Date warnDate = warningCal.getTime();

                warnTimers[i] = new Timer();
                warnTimers[i].schedule(new TimerTask() {
                    @Override
                    public void run() {
                        int second = t;
                        if (second > 59) {
                            int minute = (second % 3600) / 60;
                            if (minute > 59) {
                                int hour = second / 3600;
                                cmdSender.println(warnCommand + " Restarting server in " + hour + " hours!");
                                cmdSender.flush();
                            } else {
                                cmdSender.println(warnCommand + " Restarting server in " + minute + " minutes!");
                                cmdSender.flush();
                            }
                        } else {
                            cmdSender.println(warnCommand + " Restarting server in " + second + " seconds!");
                            cmdSender.flush();
                        }
                    }
                }, warnDate);
            }
        }
    }

    public static void stopWrapper() {
        willStop = true;
        willRestart = false;
        try {
            cmdSender.println(WrappingPaper.stopCommand);
            cmdSender.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void startServer(){
        willStop = false;
        willRestart = false;
        if(!new File(WrappingPaper.jarFilePath).exists()){
            WrappingPaper.log("Server jar not found!");
            try {
                Thread.sleep(3000);
            }catch (Exception e){/*Nothing to do.*/}
            System.exit(0);
        }
        if (WrappingPaper.timeEnabled) {
            setupRestart();
        }
        if (WrappingPaper.warn) {
            setupWarnings();
        }
        try {
            runServer = runtime.exec("java " + WrappingPaper.jvmOptions + " -jar " + WrappingPaper.jarFilePath + " " + WrappingPaper.launchOptions);
            InputStream is = runServer.getInputStream();
            serverLogThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try{
                        BufferedReader br = new BufferedReader(new InputStreamReader(is));
                        String line;
                        while ((line = br.readLine()) != null) {
                            System.out.println(line);
                        }
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                }
            });
            serverLogThread.start();
            serverCmdThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try{
                        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
                        cmdSender = new PrintStream(runServer.getOutputStream());
                        String line;
                        while ((line = br.readLine()) != null) {
                            String[] args = line.split(" ");
                            if(args[0].equalsIgnoreCase(".stop")){
                                willStop = true;
                                willRestart = false;
                                try {
                                    cmdSender.println(WrappingPaper.stopCommand);
                                    cmdSender.flush();
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            } else if(args[0].equalsIgnoreCase(".restart")){
                                willRestart = true;
                                try {
                                    cmdSender.println(WrappingPaper.warnCommand + " Restarting!");
                                    cmdSender.println(WrappingPaper.stopCommand);
                                    cmdSender.flush();
                                    break;
                                }catch (Exception e){
                                    e.printStackTrace();
                                }
                            } else if (args[0].equalsIgnoreCase(".cancel")) {
                                willRestart = false;
                                willStop = false;
                                for (int i = 0; i < warnTimers.length; i++) {
                                    if (warnTimers[i] != null) {
                                        warnTimers[i].cancel();
                                        warnTimers[i].purge();
                                        warnTimers[i] = null;
                                    }
                                }
                                if (timeRestart != null) {
                                    timeRestart.cancel();
                                    timeRestart.purge();
                                    timeRestart = null;
                                }
                            } else if (args[0].equalsIgnoreCase(".status")) {
                                if (timeRestart == null) {
                                    WrappingPaper.log("No restart scheduled");
                                } else {
                                    int diff = (int) (restartDate.getTime() - (new Date()).getTime()) / 1000;
                                    int hours = diff / 3600;
                                    int minutes = (diff % 3600) / 60;
                                    int seconds = diff % 60;
                                    cmdSender.flush();
                                    WrappingPaper.log("Remaining time: " + hours + "h " + minutes + "m " + seconds + "s");
                                }
                            } else if (args[0].equalsIgnoreCase(".reschedule") || args[0].equalsIgnoreCase(".set")) {
                                if (args.length < 4) {
                                    cmdSender.flush();
                                    WrappingPaper.log("Syntax Error: .reschedule [h|m|s]");
                                } else {
                                    if (timeRestart != null) {
                                        timeRestart.cancel();
                                        timeRestart = null;
                                    }
                                    h = Integer.parseInt(args[1]);
                                    m = Integer.parseInt(args[2]);
                                    s = Integer.parseInt(args[3]);

                                    setupRestart();
                                    if (WrappingPaper.warn) {
                                        setupWarnings();
                                    }
                                    WrappingPaper.log("Restart scheduled for " + h + "h " + m + "m " + s + "s");
                                }
                            } else if (args[0].equalsIgnoreCase(".enable")) {
                                if (args.length == 2) {
                                    String plugin = args[1];
                                    if (args[1].contains(".jar")) {
                                        plugin = plugin.substring(0, plugin.length() - 4);
                                    }
                                    File pl = new File("./plugins/" + plugin + ".disabled");
                                    if (pl.exists()) {
                                        File pl2 = new File("./wrapper/updates/" + plugin + ".enable");
                                        pl2.createNewFile();
                                    }
                                }
                            } else if (args[0].equalsIgnoreCase(".disable")) {
                                if (args.length == 2) {
                                    String plugin = args[1];
                                    if (args[1].contains(".jar")) {
                                        plugin = plugin.substring(0, plugin.length() - 4);
                                    }
                                    File pl = new File("./plugins/" + plugin + ".jar");
                                    if (pl.exists()) {
                                        File pl2 = new File("./wrapper/updates/" + plugin + ".disable");
                                        pl2.createNewFile();
                                    }
                                }
                            } else if (args[0].equalsIgnoreCase(".remove")) {
                                if (args.length == 2) {
                                    String plugin = args[1];
                                    if (args[1].contains(".jar")) {
                                        plugin = plugin.substring(0, plugin.length() - 4);
                                    }
                                    File pl = new File("./plugins/" + plugin + ".jar");
                                    File pl2 = new File("./plugins/" + plugin + ".disabled");
                                    if (pl.exists()) {
                                        (new File("./wrapper/updates/" + plugin + ".remove")).createNewFile();
                                    }
                                    if (pl2.exists()) {
                                        pl2.delete();
                                    }
                                }
                            } else if(args[0].equalsIgnoreCase(".help")){
                                cmdSender.flush();
                                Thread.sleep(100);
                                WrappingPaper.log(".status - Shows time remaining until next restart");
                                WrappingPaper.log(".restart - Restarts server");
                                WrappingPaper.log(".reschedule [h|m|s] - Restarts server");
                                WrappingPaper.log(".cancel - Cancels restart");
                                WrappingPaper.log(".stop - Stops server/wrapper");
                                WrappingPaper.log(".enable - Enables a disabled plugin on next restart");
                                WrappingPaper.log(".disable - Disables plugin on next restart");
                                WrappingPaper.log(".remove - Removes plugin on next restart");
                                WrappingPaper.log(".help - Shows all commands");
                            } else {
                                cmdSender.println(line);
                                cmdSender.flush();
                            }
                        }
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                }
            });
            serverCmdThread.start();
            runServer.waitFor();
            cmdSender = null;
            serverCmdThread.stop();
            serverCmdThread = null;
            serverLogThread.stop();
            serverLogThread = null;
            if(timeRestart != null) {
                timeRestart.cancel();
                timeRestart = null;
            }

            //Plugin updates and/or removal
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
            //---------------

            if(willStop || !WrappingPaper.crashEnabled && !willRestart) {
                System.exit(0);
            }
            WrappingPaper.log("Restarting in...");
            int wait = WrappingPaper.restartWait;
            while(wait > 0){
                WrappingPaper.log(wait + "");
                Thread.sleep(1000);
                wait--;
            }
            startServer();
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
