package net.butfly.bus.utils.udp;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

public class UdpClient {

    private final DatagramSocket socket;
    public UdpClient() {
        try {
            socket = new DatagramSocket();
        } catch (SocketException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean send(DatagramPacket packet) {
        try {
            socket.send(packet);
        } catch (IOException e) {
            throw new RuntimeException("send udp packet failed ", e);
        }
        return true;
    }

}
