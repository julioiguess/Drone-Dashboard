package drone;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

/**
 * UDP Controller for communicating with the physical Tello drone (TLW004).
 */
public class TelloController {
    private static final String TELLO_IP = "192.168.10.1";
    private static final int TELLO_PORT = 8889;
    private DatagramSocket socket;
    private InetAddress address;

    public TelloController() {
        try {
            socket = new DatagramSocket();
            address = InetAddress.getByName(TELLO_IP);
            sendCommand("command"); // Initialize SDK mode
        } catch (Exception e) {
            System.err.println("Could not initialize Tello UDP connection: " + e.getMessage());
        }
    }

    public void sendCommand(String command) {
        try {
            byte[] buf = command.getBytes();
            DatagramPacket packet = new DatagramPacket(buf, buf.length, address, TELLO_PORT);
            socket.send(packet);
            System.out.println("Sent Tello Command: " + command);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void takeoff() {
        sendCommand("takeoff");
    }

    public void land() {
        sendCommand("land");
    }

    public void moveForward(int cm) {
        sendCommand("forward " + cm);
    }

    public void rotateClockwise(int degrees) {
        sendCommand("cw " + degrees);
    }
}
