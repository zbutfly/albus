package net.butfly.bus.utils.udp;

import java.io.IOException;
import java.net.*;
import java.util.function.Consumer;

public class UdpServer {
    private DatagramSocket socket;
    private Consumer<DatagramPacket> handler;

    public UdpServer(String host, int port) throws SocketException, UnknownHostException {
        this(host, port, 0);
    }

    public UdpServer(String host, int port, int timeout) throws UnknownHostException, SocketException {
        socket = new DatagramSocket(port, InetAddress.getByName(host));
        socket.setSoTimeout(timeout);
    }

    public UdpServer setHandler(Consumer<DatagramPacket> handler) {
        this.handler = handler;
        return this;
    }

    public void start() {
        Thread t = new Thread(() -> {
            while (true) {
                byte[] buf = new byte[UdpUtils.UDP_PACKET_SIZE];
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                try {
                    socket.receive(packet);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                handler.accept(packet);
            }
        });
        t.start();
    }

    public void send(DatagramPacket packet) {
        try {
            socket.send(packet);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void receive(DatagramPacket packet) throws SocketTimeoutException {
        try {
            socket.receive(packet);
        } catch (SocketTimeoutException e) {
            throw e;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
