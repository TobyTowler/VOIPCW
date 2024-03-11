import uk.ac.uea.cmp.voip.DatagramSocket2;
import uk.ac.uea.cmp.voip.DatagramSocket3;
import uk.ac.uea.cmp.voip.DatagramSocket4;

import java.io.IOException;
import java.net.*;
import java.time.Duration;
import java.util.Arrays;

public class DatagramSocket4Test{

    public static void main(String[] args) throws IOException {
        if(!Arrays.equals(new int[]{0,4,8,12,1,5,9,13,2,6,10,14,3,7,11,15},Network.interleaveOrder(4))){
           System.out.println("Interleave order is incorrect");
           System.exit(-1);
        }

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

        Network.CorruptionStats corrupted = Network.packetsCorrupted(senderReceiver, senderReceiver, address);
        corrupted.display();


        senderReceiver.disconnect();
        senderReceiver.close();
    }

}