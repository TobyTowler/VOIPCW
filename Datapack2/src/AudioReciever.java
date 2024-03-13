import CMPC3M06.AudioPlayer;

import javax.sound.sampled.LineUnavailableException;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class AudioReciever implements Runnable {

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
        long IDPredicted = 0;

        while (running) {
            byte[] secureBuffer = new byte[522];
            byte[] buffer = new byte[512];
            short authKey = 10;
            //Receive a DatagramPacket (note that the string cant be more than 80 chars)
            DatagramPacket packet = new DatagramPacket(secureBuffer, 0, 522);

            //receiving_socket.setSoTimeout(500);

//
//                //LastReceivedPacketNumber will always be -1 to start since its static to avoid it saying 0 is an error and looping.
//                //It'll also check if CurrentPacketNumber is not equal to the last recieved one +1. If it matches all these, the if statement starts.
//                if (LastReceivedPacketNumber != -1 && CurrentPacketNumber != LastReceivedPacketNumber + 1) {
//                    System.out.println("These packets are missing: ");
//                    //i become the LastReceivedPacketNumber + 1 so if its 28 i would be 29, then it compares it with the currentpacket and goes up.
//                    //This part of the code tells us what packets are missing, all of them.ther
//                    for (int j = LastReceivedPacketNumber + 1; j < CurrentPacketNumber; j++) {
//                        System.out.print(j + " ");
//                        if (LastRecievedAudionumber != null)
//                        //If the next packet that will be recieved is missing, loop the packet until the next packet is found.
//                        {
//                            System.out.println("Repeating audio packet: " + Arrays.toString(LastRecievedAudionumber));
//
//
//                        }

            //receiving_socket.setSoTimeout(500);
            try {
                receiving_socket.receive(packet);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            ByteBuffer bb = ByteBuffer.wrap(secureBuffer);
            short headerVal = bb.getShort();
            long ID = bb.getLong();
            bb.get(buffer);
            if (ID != IDPredicted) {
                System.out.println("packet missing: " + IDPredicted);
//                System.out.println(ID);
//                System.out.println(IDPredicted);
                IDPredicted++;
            }
            IDPredicted++;

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
            //Close the socket
            //***************************************************

        }
        receiving_socket.close();
        player.close();

    }
}