package ml.bmlzootown.udp;

import ml.bmlzootown.Server;
import ml.bmlzootown.WrappingPaper;

import java.io.File;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.Date;
import java.util.List;

public class Responder implements Runnable {
    DatagramSocket socket = null;
    DatagramPacket packet = null;

    // Restart Timer Variables
    private static int h = WrappingPaper.h;
    private static int m = WrappingPaper.m;
    private static int s = WrappingPaper.s;
    private static Date restartDate = Server.restartDate;

    // Warning Variables
    private static List<Integer> warnings = WrappingPaper.warnings;

    public Responder(DatagramSocket socket, DatagramPacket packet) {
        this.socket = socket;
        this.packet = packet;
    }

    public void run() {
        byte[] receivedBytes = packet.getData();
        if(receivedBytes == null || packet.getLength() == 0) {
            return;
        }
        String request = new String(packet.getData(), 0, packet.getLength());
        if (WrappingPaper.debug) WrappingPaper.log("[SpigotWrapper] Packet get! " + request);
        String[] args = request.split(":");
        if (args[0].equalsIgnoreCase("status")) {
            if (Server.timeRestart == null) {
                String reply = "[NULL]";
                DatagramPacket response = getResponse(reply.getBytes(), reply.length());
                sendResponse(response);
            } else {
                int diff = (int) (Server.restartDate.getTime() - (new Date()).getTime()) / 1000;

                long h = diff / 3600;
                long m = (diff % 3600) / 60;
                long s = diff % 60;

                String reply = String.format("%02d:%02d:%02d", h, m, s);
                DatagramPacket response = getResponse(reply.getBytes(), reply.length());
                sendResponse(response);
            }
        }
        if (args[0].equalsIgnoreCase("reschedule")) {
            if (args.length == 4) {
                if (Server.timeRestart != null) {
                    Server.timeRestart.cancel();
                    Server.timeRestart = null;
                }
                Server.h = Integer.parseInt(args[1]);
                Server.m = Integer.parseInt(args[2]);
                Server.s = Integer.parseInt(args[3]);
                Server.setupRestart();
                Server.setupWarnings();
            }
        }
        if (args[0].equalsIgnoreCase("stop")) {
            Server.stopWrapper();
        }
        if (args[0].equalsIgnoreCase("cancel")) {
            Server.willRestart = false;
            Server.willStop = false;
            for (int i = 0; i < Server.warnTimers.length; i++) {
                if (Server.warnTimers[i] != null) {
                    Server.warnTimers[i].cancel();
                    Server.warnTimers[i].purge();
                    Server.warnTimers[i] = null;
                }
            }
            if (Server.timeRestart != null) {
                Server.timeRestart.cancel();
                Server.timeRestart.purge();
                Server.timeRestart = null;
            }
        }
        if (args[0].equalsIgnoreCase("enable")) {
            if (args.length == 2) {
                String plugin = args[1];
                if (args[1].contains(".jar")) {
                    plugin = plugin.substring(0, plugin.length() - 4);
                }
                File pl = new File("./plugins/" + plugin + ".disabled");
                if (pl.exists()) {
                    File pl2 = new File("./wrapper/updates/" + plugin + ".enable");
                    try {
                        pl2.createNewFile();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        if (args[0].equalsIgnoreCase("disable")) {
            if (args.length == 2) {
                String plugin = args[1];
                if (args[1].contains(".jar")) {
                    plugin = plugin.substring(0, plugin.length() - 4);
                }
                File pl = new File("./plugins/" + plugin + ".jar");
                if (pl.exists()) {
                    File pl2 = new File("./wrapper/updates/" + plugin + ".disable");
                    try {
                        pl2.createNewFile();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        if (args[0].equalsIgnoreCase("remove")) {
            if (args.length == 2) {
                String plugin = args[1];
                if (args[1].contains(".jar")) {
                    plugin = plugin.substring(0, plugin.length() - 4);
                }
                File pl = new File("./plugins/" + plugin + ".jar");
                File pl2 = new File("./plugins/" + plugin + ".disabled");
                if (pl.exists()) {
                    try {
                        (new File("./wrapper/updates/" + plugin + ".remove")).createNewFile();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                if (pl2.exists()) {
                    pl2.delete();
                }
            }
        }
    }

    private void sendResponse(DatagramPacket response) {
        try {
            socket.send(response);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private DatagramPacket getResponse(byte[] data, int length) {
        return new DatagramPacket(data, data.length,
                packet.getAddress(), packet.getPort());
    }
}
