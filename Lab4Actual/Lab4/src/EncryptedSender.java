import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.*;
import java.nio.ByteBuffer;

import CMPC3M06.AudioRecorder;

import javax.sound.sampled.LineUnavailableException;

public class EncryptedSender implements Runnable{

    static DatagramSocket sending_socket;

    public void start(){
        Thread thread = new Thread(this);
        thread.start();
    }

    public void run (){

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
        try{
            sending_socket = new DatagramSocket();
        } catch (SocketException e){
            System.out.println("ERROR: TextSender: Could not open UDP socket to send from.");
            e.printStackTrace();
            System.exit(0);
        }
        //***************************************************

        //***************************************************
        //Get a handle to the Standard Input (console) so we can read user input

        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        //***************************************************

        //***************************************************
        //Main loop.

        boolean running = true;

        AudioRecorder recorder = null;
        try {
            recorder = new AudioRecorder();
        } catch (LineUnavailableException e) {
            throw new RuntimeException(e);
        }
        int key = 15;
        //higher is better
        //0 is nothing
        while (running) {
            try {
                byte[] buffer = new byte[512];
                //Vector used to store audio blocks (32ms/512bytes each)
                //Vector<byte[]> voiceVector = new Vector<byte[]>();

                //Initialise AudioPlayer and AudioRecorder objects
                buffer = recorder.getBlock();
                //Make a DatagramPacket from it, with client address and port number



                ByteBuffer unwrapEncrypt = ByteBuffer.allocate(buffer.length);
                ByteBuffer plainAudio = ByteBuffer.wrap(buffer);
                for(int i = 0; i<buffer.length/4; i++){
                    int fourByte = plainAudio.getInt();
                    fourByte = fourByte ^ key;
                    unwrapEncrypt.putInt(fourByte);
                }
                byte[] ecnryptedBlock = unwrapEncrypt.array();

                ByteBuffer VOIPpacket = ByteBuffer.allocate(514);
                short authKey = 10;
                VOIPpacket.putShort(authKey);
                VOIPpacket.put(ecnryptedBlock);

                byte[] securePacket = VOIPpacket.array();

                DatagramPacket packet = new DatagramPacket(securePacket, securePacket.length, clientIP, PORT);

                //Send it
                sending_socket.send(packet);



            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        recorder.close();
        //Close the socket
        sending_socket.close();
        //***************************************************
    }
}
