import uk.ac.uea.cmp.voip.DatagramSocket2;
import uk.ac.uea.cmp.voip.DatagramSocket4;

import java.io.IOException;
import java.net.*;
import java.time.Duration;
import java.time.temporal.ChronoUnit;

public class DatagramSocket4Test{

    public static void main(String[] args) throws IOException {
        int PORT = 55555;
        InetAddress IP = InetAddress.getByName("localhost");
        InetSocketAddress address = new InetSocketAddress(IP, PORT);
        DatagramSocket4 senderReceiver = new DatagramSocket4(null);
        senderReceiver.bind(address);

        final byte[] dummyData = new byte[512];
        Network.VoipPacket packet = new Network.VoipPacket((short) 0, (byte) 0, dummyData);
        DatagramPacket dummyPacket = packet.datagram(address);
        //senderReceiver.send(dummyPacket);
        Duration delay = Network.measureDelay(senderReceiver, senderReceiver, address);
        System.out.println("network has a delay of "  + delay.toMillis() + " ms");
        double plrate = Network.packetloss(senderReceiver, senderReceiver, address);
        System.out.println("network has " + (plrate * 100.0) + "% packet loss" );
        senderReceiver.disconnect();
        senderReceiver.close();
    }

}