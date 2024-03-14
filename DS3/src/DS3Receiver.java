import CMPC3M06.AudioPlayer;
import uk.ac.uea.cmp.voip.DatagramSocket3;

import javax.sound.sampled.LineUnavailableException;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.HashMap;

public class DS3Receiver implements Runnable{

    static DatagramSocket3 receiving_socket;

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
            receiving_socket = new DatagramSocket3(PORT);
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
        short lastPlayed = 0;
        int key = 15;
        HashMap<Short, byte[]> map = new HashMap<>();
        while (running){
            byte[] secureBuffer = new byte[516];
            byte[] buffer = new byte[512];
            short authKey = 10;

            DatagramPacket packet = new DatagramPacket(secureBuffer, 0, 516);

            //receiving_socket.setSoTimeout(500);
            try {
                receiving_socket.receive(packet);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            ByteBuffer bb = ByteBuffer.wrap(secureBuffer);
            short headerVal = bb.getShort();
            short sequnceNumber = bb.getShort();
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

//            System.out.println("RECEIVED : " + sequnceNumber);
            map.put(sequnceNumber, decryptedBlock);


            System.out.println("size: " + map.size());
            System.out.println("L " + lastPlayed);


            if(map.size() > 16){ //buffer size of 17 packets (16+1)
                for(short i = (short) (lastPlayed+1); i<Short.MAX_VALUE; i++){
                    if(map.get(i) != null){
                        try {
                            player.playBlock(map.get(i));
                            System.out.println("Playing " + i);
                            map.remove(i);
                            lastPlayed = i;
                            break;
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
            }



        }
        player.close();
        //Close the socket
        receiving_socket.close();
        //***************************************************
    }
}
