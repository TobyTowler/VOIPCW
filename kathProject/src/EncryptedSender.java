import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.*;
import java.nio.ByteBuffer;

import CMPC3M06.AudioRecorder;
import uk.ac.uea.cmp.voip.DatagramSocket2;

import javax.sound.sampled.LineUnavailableException;

public class EncryptedSender implements Runnable{

    static DatagramSocket2 sending_socket;

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
            sending_socket = new DatagramSocket2();
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

        // Send 1000 dummy packets
        for (int i = 0; i < 1000; i++) {
            try {
                byte[] buffer = recorder.getBlock();

                ByteBuffer unwrapEncrypt = ByteBuffer.allocate(buffer.length);
                ByteBuffer plainAudio = ByteBuffer.wrap(buffer);
                for (int j = 0; j < buffer.length / 4; j++) {
                    int fourByte = plainAudio.getInt();
                    fourByte = fourByte ^ key;
                    unwrapEncrypt.putInt(fourByte);
                }
                byte[] encryptedBlock = unwrapEncrypt.array();

                ByteBuffer VOIPpacket = ByteBuffer.allocate(514);
                short authKey = 10;
                VOIPpacket.putShort(authKey);
                VOIPpacket.put(encryptedBlock);

                byte[] securePacket = VOIPpacket.array();

                DatagramPacket sendPacket = new DatagramPacket(securePacket, securePacket.length, clientIP, PORT);
                sending_socket.send(sendPacket);

                //Verifies whether the packets got through by printing each packet number if it gets through.
                byte[] AckBuffer = new byte[2];
                DatagramPacket AckPacket = new DatagramPacket(AckBuffer, AckBuffer.length, clientIP, PORT);
                sending_socket.receive(AckPacket);

                short acknowledgment = ByteBuffer.wrap(AckBuffer).getShort();
                if (acknowledgment == 1) {
                    System.out.println("Packet " + i + " acknowledged");
                } else {
                    System.out.println("Packet " + i + " not acknowledged");
                }


            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        recorder.close();
        // Close the socket
        sending_socket.close();
    }
}