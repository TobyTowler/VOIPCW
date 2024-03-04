import CMPC3M06.AudioRecorder;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class SenderThread implements Runnable{

    static DatagramSocket sending_socket;

    public void start(){
        Thread thread = new Thread(this);
        thread.start();
    }

    public void run (){
        int PORT = 55555;
        InetAddress clientIP = null;
        try {
            clientIP = InetAddress.getByName("localhost");
        } catch (UnknownHostException e) {
            System.err.println("COULD NOT CONNECT TO HOST");
            e.printStackTrace();
            System.exit(0);
        }


        try{
            sending_socket = new DatagramSocket();
        } catch (SocketException e){
            System.out.println("ERROR: TextSender: Could not open UDP socket to send from.");
            e.printStackTrace();
            System.exit(0);
        }

//
//        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));



        AudioRecorder recorder = null;
        try {
            recorder = new AudioRecorder();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        //Keys
        long encryptionKey = 483290423;
//        short authKey = 17594;
        String authKeyString = "XADWDJKVFJ";

        boolean running = true;
        while(running) {
            try {
                byte[] buffer = new byte[512];


                buffer = recorder.getBlock();


                ByteBuffer unwrapEncrypt = ByteBuffer.allocate(buffer.length);
                ByteBuffer plainAudio = ByteBuffer.wrap(buffer);
                for(int i = 0; i<buffer.length/8; i++){
                    long eightByte = plainAudio.getLong();
                    eightByte = (eightByte ^ encryptionKey);
                    unwrapEncrypt.putLong(eightByte);
                }

                byte[] ecnryptedBlock = unwrapEncrypt.array();

                ByteBuffer VOIPpacket = ByteBuffer.allocate(522);
//                VOIPpacket.putShort(authKey);

                byte[] authKeyBytes = authKeyString.getBytes(StandardCharsets.UTF_8);
                VOIPpacket.put(authKeyBytes);


                //System.out.println(authKeyBytes.length);

                VOIPpacket.put(ecnryptedBlock);

                byte[] securePacket = VOIPpacket.array();

                DatagramPacket packet = new DatagramPacket(securePacket, securePacket.length, clientIP, PORT);

                sending_socket.send(packet);



            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        recorder.close();

        sending_socket.close();

    }


}
