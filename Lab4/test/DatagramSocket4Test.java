import uk.ac.uea.cmp.voip.DatagramSocket4;

import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.time.Duration;
import java.time.temporal.ChronoUnit;

public class DatagramSocket4Test{

    public static void main(String[] args) throws IOException {
        int PORT = 55555;
        InetAddress IP = InetAddress.getByName("localhost");
        DatagramSocket4 sender = new DatagramSocket4(PORT,IP);
        DatagramSocket4 receiver = new DatagramSocket4(PORT,IP);

        Duration delay = Network.measureDelay(sender, receiver);
        System.out.println("network has a delay of "  + delay.get(ChronoUnit.MILLIS) + "ms");
        sender.disconnect();
        sender.close();
        receiver.disconnect();
        receiver.close();
    }

}