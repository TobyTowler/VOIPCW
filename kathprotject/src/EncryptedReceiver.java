import CMPC3M06.AudioPlayer;

import javax.sound.sampled.LineUnavailableException;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class EncryptedReceiver implements Runnable {

    static int LastReceivedPacketNumber = -1;
    static byte[] LastRecievedAudionumber = null;
    int CurrentPacketNumber = 1;

    static DatagramSocket receiving_socket;

    public void start() {
        Thread thread = new Thread(this);
        thread.start();
    }

    public void run() {

        //***************************************************
        //Port to open socket on
        int PORT = 55555;
        //***************************************************

        //***************************************************
        //Open a socket to receive from on port PORT

        //DatagramSocket receiving_socket;
        try {
            receiving_socket = new DatagramSocket(PORT);
        } catch (SocketException e) {
            System.out.println("ERROR: TextReceiver: Could not open UDP socket to receive from.");
            e.printStackTrace();
            System.exit(0);
        }
        //***************************************************

        //***************************************************
        //Main loop.

        boolean running = true;

        AudioPlayer player;
        try {
            player = new AudioPlayer();
        } catch (LineUnavailableException e) {
            throw new RuntimeException(e);
        }
        int key = 15;
        while (running) {
            byte[] secureBuffer = new byte[514];
            byte[] buffer = new byte[512];
            short authKey = 10;
            try {
                //Receive a DatagramPacket (note that the string cant be more than 80 chars)
                DatagramPacket packet = new DatagramPacket(buffer, 0, 80);

                //receiving_socket.setSoTimeout(500);
                receiving_socket.receive(packet);

                // Get a string from the byte buffer
                String str = new String(buffer);
                //LastReceivedPacketNumber will always be -1 to start since its static to avoid it saying 0 is an error and looping.
                //It'll also check if CurrentPacketNumber is not equal to the last recieved one +1. If it matches all these, the if statement starts.
                if (LastReceivedPacketNumber != -1 && CurrentPacketNumber != LastReceivedPacketNumber + 1) {
                    System.out.println("These packets are missing: ");
                    //i become the LastReceivedPacketNumber + 1 so if its 28 i would be 29, then it compares it with the currentpacket and goes up.
                    //This part of the code tells us what packets are missing, all of them.ther
                    for (int j = LastReceivedPacketNumber + 1; j < CurrentPacketNumber; j++) {
                        System.out.print(j + " ");
                        if (LastRecievedAudionumber != null) {
                            System.out.println("Repeating audio packet: " + Arrays.toString(LastRecievedAudionumber));

                        }

                        //receiving_socket.setSoTimeout(500);
                        try {
                            receiving_socket.receive(packet);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }

                        ByteBuffer bb = ByteBuffer.wrap(secureBuffer);
                        int headerVal = bb.getShort();
                        bb.get(buffer);


                        if (headerVal != authKey) {
                            System.err.println("Bad sender");
                        }

                        ByteBuffer unwrapDecrypt = ByteBuffer.allocate(buffer.length);
                        ByteBuffer cipherText = ByteBuffer.wrap(buffer);
                        for (int i = 0; i < buffer.length / 4; i++) {
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
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}