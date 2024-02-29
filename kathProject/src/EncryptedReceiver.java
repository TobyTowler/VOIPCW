import CMPC3M06.AudioPlayer;
import uk.ac.uea.cmp.voip.DatagramSocket2;

import javax.sound.sampled.LineUnavailableException;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.nio.ByteBuffer;

public class EncryptedReceiver implements Runnable{

    static DatagramSocket2 receiving_socket;

    public void start(){
        Thread thread = new Thread(this);
        thread.start();
    }

    public void run (){

        //***************************************************
        //Port to open socket on
        int PORT = 55555;
        //***************************************************

        //***************************************************
        //Open a socket to receive from on port PORT

        //DatagramSocket receiving_socket;
        try{
            receiving_socket = new DatagramSocket2(PORT);
        } catch (SocketException e){
            System.out.println("ERROR: TextReceiver: Could not open UDP socket to receive from.");
            e.printStackTrace();
            System.exit(0);
        }
        //***************************************************

        //***************************************************
        //Main loop.

        boolean running = true;

        AudioPlayer player = null;
        try {
            player = new AudioPlayer();
        } catch (LineUnavailableException e) {
            throw new RuntimeException(e);
        }

        int key = 15;
        while (running){
            byte[] secureBuffer = new byte[514];
            byte[] buffer = new byte[512];
            short authKey = 10;

            DatagramPacket packet = new DatagramPacket(secureBuffer, 0, 514);
            DatagramPacket receivedPacket = new DatagramPacket(buffer, buffer.length);
            for(int i = 0; i < 1000; i++){
                //Allocates two bytes to receiving the ackbuffer in an array.
                short acknowledgement = 1;
                byte[] ackBuffer = ByteBuffer.allocate(2).putShort(acknowledgement).array();
                DatagramPacket ackPacket = new DatagramPacket(ackBuffer, ackBuffer.length, receivedPacket.getAddress(), receivedPacket.getPort());
            }
            //receiving_socket.setSoTimeout(500);
            try {
                receiving_socket.receive(packet);
                receiving_socket.receive(receivedPacket);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            ByteBuffer bb = ByteBuffer.wrap(secureBuffer);
            int headerVal = bb.getShort();
            bb.get(buffer);


            if(headerVal != authKey){
                System.err.println("Bad sender");
            }

            ByteBuffer unwrapDecrypt = ByteBuffer.allocate(buffer.length);
            ByteBuffer cipherText = ByteBuffer.wrap(buffer);
            for(int i = 0; i<buffer.length/4; i++){
                int fourByte = cipherText.getInt();
                fourByte = fourByte ^ key;
                unwrapDecrypt.putInt(fourByte);
            }
            byte[] decryptedBlock = unwrapDecrypt.array();


            try {
                player.playBlock(decryptedBlock);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

        }
        player.close();
        //Close the socket
        receiving_socket.close();
        //***************************************************
    }
}
