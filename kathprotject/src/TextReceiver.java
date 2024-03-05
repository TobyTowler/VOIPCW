/**
 *
 * @author  abj
 */
import uk.ac.uea.cmp.voip.DatagramSocket2;

import java.net.*;
import java.io.*;

public class TextReceiver {

    static DatagramSocket2 receiving_socket;

    //This LastRecievedPacketNumber
    static int LastReceivedPacketNumber = -1;

    public static void main(String[] args) {

        //***************************************************
        //Port to open socket on
        int PORT = 55555;
        //***************************************************

        //***************************************************
        //Open a socket to receive from on PORT

        //DatagramSocket receiving_socket;
        try {
            receiving_socket = new DatagramSocket2(PORT);
        } catch (SocketException e) {
            System.out.println("ERROR: TextReceiver: Could not open UDP socket to receive from.");
            e.printStackTrace();
            System.exit(0);
        }
        //***************************************************

        //***************************************************
        //Main loop.

        boolean running = true;

        while (running) {

            try {
                //Receive a DatagramPacket (note that the string can't be more than 80 chars)
                byte[] buffer = new byte[80];
                DatagramPacket packet = new DatagramPacket(buffer, 0, 80);
                //receiving_socket.setSoTimeout(500);

                receiving_socket.receive(packet);

                // Get a string from the byte buffer
                String str = new String(buffer);
                //Get the packet number by parsing the current string (doing trim removes the whitespace)
                int CurrentPacketNumber = Integer.parseInt(str.trim());
                //LastRecievedPacketNumber will always be -1 to start since its static to avoid it saying 0 is an error and looping.
                //It'll also check if CurrentPacketNumber is not equal to the last recieved one +1. If it matches all these, the if statement starts.
                if (LastReceivedPacketNumber != -1 && CurrentPacketNumber != LastReceivedPacketNumber + 1) {
                    System.out.println("These packets are missing: ");
                    //i becomes the LastRecievedPacketNumber + 1 so if its 28 i would be 29, then it compares it with the currentpacket and goes up.
                    //This part of the code tells us what packets are missing, all of them.ther
                    for (int i = LastReceivedPacketNumber + 1; i < CurrentPacketNumber; i++) {
                        System.out.print(i + " ");
                    }
                    System.out.println();
                }
                System.out.print(CurrentPacketNumber + "\n");
                LastReceivedPacketNumber = CurrentPacketNumber;

                // The user can type EXIT to quit
                if (str.substring(0, 4).equals("EXIT")) {
                    running = false;
                }
            } catch (SocketTimeoutException e) {
                System.err.println("Data missing");
            } catch (IOException e) {
                System.out.println("ERROR: TextReceiver: Some random IO error occurred!");
                e.printStackTrace();
            }
        }
        // Close the socket
        receiving_socket.close();
        //***************************************************
    }
}
