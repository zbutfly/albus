package net.butfly.bus.utils.udp;

import net.butfly.albacore.utils.IOs;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.function.Consumer;

public class UdpUtils {

    public static final int UDP_PACKET_SIZE = 1472;
    public static final int UDP_SERVER_TIMEOUT = 1000;

    public static String bytesToHex(byte[] bytes) {
        final char[] hexArray = "0123456789ABCDEF".toCharArray();
        char[] hexChars = new char[bytes.length * 3];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 3] = hexArray[v >> 4];
            hexChars[j * 3 + 1] = hexArray[v & 0x0F];
            hexChars[j * 3 + 2] = ' ';
        }
        return new String(hexChars);
    }

    public static DatagramPacket redirect(DatagramPacket packet, String host, int port) {
        try {
            packet.setAddress(InetAddress.getByName(host));
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
        packet.setPort(port);
        return packet;
    }

    public static boolean request(DatagramPacket packet, Consumer<DatagramPacket> using) {
        new Thread(() -> using.accept(packet)).run();
        return true;
    }

    public static void save(OutputStream out, DatagramPacket packet) {
        byte[] addr = packet.getAddress().getAddress();
        int port = packet.getPort();
        byte[] data = packet.getData();
        try {
            IOs.writeBytes(out, addr);
            IOs.writeInt(out, port);
            IOs.writeBytes(out, data);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static DatagramPacket load(InputStream in) {
        DatagramPacket packet;
        try {
            byte[] addr = IOs.readBytes(in);
            int port = IOs.readInt(in);
            byte[] buf = IOs.readBytes(in);
            packet = new DatagramPacket(buf, buf.length, InetAddress.getByAddress(addr), port);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return packet;
    }
}
