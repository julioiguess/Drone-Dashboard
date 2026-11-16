package Drone;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;

public class TelloController {
    private static final String TELLO_IP = "192.168.10.1";
    private static final int TELLO_PORT = 8889;
    private DatagramSocket socket;
    private InetAddress address;

    public TelloController() {
        try {
            socket = new DatagramSocket();
            socket.setSoTimeout(7000);
            address = InetAddress.getByName(TELLO_IP);
        } catch (Exception e) {
            System.err.println("Could not initialize Tello UDP socket: " + e.getMessage());
        }
    }

    public synchronized String sendCommand(String command) {
        if (socket == null || address == null) return "NO_SOCKET";
        try {
            byte[] buf = command.getBytes(StandardCharsets.UTF_8);
            DatagramPacket packet = new DatagramPacket(buf, buf.length, address, TELLO_PORT);
            socket.send(packet);
            System.out.println("Sent Tello Command: " + command);

            if ("emergency".equalsIgnoreCase(command)) return "EMERGENCY_SENT";

            byte[] receiveBuf = new byte[256];
            DatagramPacket responsePacket = new DatagramPacket(receiveBuf, receiveBuf.length);
            socket.receive(responsePacket);
            String reply = new String(responsePacket.getData(), 0, responsePacket.getLength(), StandardCharsets.UTF_8).trim();
            System.out.println("Tello Reply: " + reply);
            return reply;
        } catch (SocketTimeoutException e) {
            System.err.println("Tello timeout waiting for reply to: " + command);
            return "TIMEOUT";
        } catch (Exception e) {
            e.printStackTrace();
            return "ERROR: " + e.getMessage();
        }
    }

    public String connectSdk() { return sendCommand("command"); }
    public String takeoff() { return sendCommand("takeoff"); }
    public String land() { return sendCommand("land"); }
    public String emergency() { return sendCommand("emergency"); }
    public String moveForward(int cm) { return sendCommand("forward " + cm); }
    public String rotateClockwise(int degrees) { return sendCommand("cw " + degrees); }
    public String getBattery() { return sendCommand("battery?"); }
}
