import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;

import CMPC3M06.AudioRecorder;
import uk.ac.uea.cmp.voip.DatagramSocket2;

import javax.sound.sampled.LineUnavailableException;

public class EncryptedSender implements Runnable {

    static DatagramSocket sending_socket;

    public void start() {
        Thread thread = new Thread(this);
        thread.start();
    }

    public void run() {

        //***************************************************
        //Port to send to
        int PORT = 55555;
        //IP ADDRESS to send to
        InetAddress clientIP = null;
        try {
            clientIP = InetAddress.getByName("localhost");  //CHANGE localhost to IP or NAME of client machine
        } catch (UnknownHostException e) {
            System.out.println("ERROR: TextSender: Could not find client IP");
            e.printStackTrace();
            System.exit(0);
        }
        //***************************************************

        //***************************************************
        //Open a socket to send from
        //We dont need to know its port number as we never send anything to it.
        //We need the try and catch block to make sure no errors occur.

        //DatagramSocket sending_socket;
        try {
            sending_socket = new DatagramSocket2();
        } catch (SocketException e) {
            System.out.println("ERROR: TextSender: Could not open UDP socket to send from.");
            e.printStackTrace();
            System.exit(0);
        }
        //***************************************************

        //***************************************************

        //***************************************************

        //***************************************************
        //Main loop.

        boolean running = true;

        AudioRecorder recorder = null;
        try {
            recorder = new AudioRecorder();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        int key = 15;
        //higher is better
        //0 is nothing
        long ID = 0;
        while (running) {
            //                for (int j = 0; j < 1000; j++) {
            try {
                //Read in a string from the standard input
                //String str = in.readLine();

//                String str = "";
//                str = "";

                //Convert it to an array of bytes
                byte[] buffer = recorder.getBlock();

                //Make a DatagramPacket from it, with client address and port number
                //Vector used to store audio blocks (32ms/512bytes each)
                //Vector<byte[]> voiceVector = new Vector<byte[]>();

                //Initialise AudioPlayer and AudioRecorder objects
//                buffer = recorder.getBlock();
                //Make a DatagramPacket from it, with client address and port number


                ByteBuffer unwrapEncrypt = ByteBuffer.allocate(buffer.length);
                ByteBuffer plainAudio = ByteBuffer.wrap(buffer);

                for (int i = 0; i < buffer.length / 4; i++) {
                    int fourByte = plainAudio.getInt();
                    fourByte = fourByte ^ key;
                    unwrapEncrypt.putInt(fourByte);
                }
                byte[] encryptedBlock = unwrapEncrypt.array();


                short authKey = 10;
                ByteBuffer VOIPpacket = ByteBuffer.allocate(512 + 2 + 8);
                VOIPpacket.putShort(authKey);
                VOIPpacket.putLong(ID);
                VOIPpacket.put(encryptedBlock);
                ID++;


                byte[] securePacket = VOIPpacket.array();

                DatagramPacket packet = new DatagramPacket(securePacket, securePacket.length, clientIP, PORT);

                //Send it
                sending_socket.send(packet);


            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            //Close the socket
            //***************************************************
        }
        sending_socket.close();
        recorder.close();

    }
}
