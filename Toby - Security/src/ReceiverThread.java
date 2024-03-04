import CMPC3M06.AudioPlayer;

import javax.sound.sampled.LineUnavailableException;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class ReceiverThread implements Runnable{

    static DatagramSocket receiving_socket;

    public void start(){
        Thread thread = new Thread(this);
        thread.start();
    }

    public void run (){


        int PORT = 55555;

        try{
            receiving_socket = new DatagramSocket(PORT);
        } catch (SocketException e){
            System.out.println("ERROR: TextReceiver: Could not open UDP socket to receive from.");
            e.printStackTrace();
            System.exit(0);
        }


        AudioPlayer player = null;
        try {
            player = new AudioPlayer();
        } catch (LineUnavailableException e) {
            throw new RuntimeException(e);
        }

        //keys
        long encryptionKey = 483290423;
        String authKeyString = "XADWDJKVFJ";



        boolean running = true;
        while (running){
            byte[] secureBuffer = new byte[522];
            byte[] buffer = new byte[512];
            //short authKey = 17594;

            DatagramPacket packet = new DatagramPacket(secureBuffer, 0, 522);

            //receiving_socket.setSoTimeout(500);
            try {
                receiving_socket.receive(packet);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            ByteBuffer bb = ByteBuffer.wrap(secureBuffer);

            //Getting authKey
            byte[] authKeyBytes = new byte[10];
            bb.get(authKeyBytes);
            String bytes = new String(authKeyBytes, StandardCharsets.UTF_8);
            bb.get(buffer);


            if(!authKeyString.contentEquals(bytes)){
                System.err.println("Packet received from non genuine sender");
                continue;
            }

            ByteBuffer unwrapDecrypt = ByteBuffer.allocate(buffer.length);
            ByteBuffer cipherText = ByteBuffer.wrap(buffer);

            for(int i = 0; i<buffer.length/8; i++){
                long eightByte = cipherText.getLong();
                eightByte = (eightByte ^ encryptionKey);
                unwrapDecrypt.putLong(eightByte);
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
