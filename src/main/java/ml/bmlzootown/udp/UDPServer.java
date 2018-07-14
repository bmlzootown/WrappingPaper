package ml.bmlzootown.udp;

import ml.bmlzootown.WrappingPaper;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

public class UDPServer implements Runnable {
    DatagramSocket socket = null;
    byte[] receiveData = new byte[1024];

    public UDPServer(int port) {
        try {
            socket = new DatagramSocket(port);
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }

    public void run() {
        WrappingPaper.log("Listening on port 25569 (UDP)...");
        while (true) {
            DatagramPacket packet = new DatagramPacket(receiveData,
                    receiveData.length);
            try {
                socket.receive(packet);
                new Thread(new Responder(socket, packet)).start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
