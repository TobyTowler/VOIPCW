import CMPC3M06.AudioRecorder;

import java.io.IOException;
import java.math.BigInteger;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Scanner;

public class SenderThread implements Runnable{

    static DatagramSocket sending_socket;
    static DatagramSocket receiving_socket;

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
            System.out.println("ERROR: SenderThread could not open.");
            e.printStackTrace();
            System.exit(0);
        }

        try{
            receiving_socket = new DatagramSocket(1234);
        } catch (SocketException e){
            System.out.println("ERROR: Could not receive R2 from receiver");
            e.printStackTrace();
            System.exit(0);
        }



        AudioRecorder recorder = null;
        try {
            recorder = new AudioRecorder();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        //Keys
        String authKeyString = "F(D&KL@DLC";

        BigInteger diffieP = new BigInteger("203956878356401977405765866929034577280193993314348263094772646453283062722701277632936616063144088173312372882677123879538709400158306567338328279154499698366071906766440037074217117805690872792848149112022286332144876183376326512083574821647933992961249917319836219304274280243803104015000563790123");
        BigInteger diffieG = new BigInteger("5445489838578453783549853290423473848590832940923504273483290275890324832904823940");
        BigInteger diffieX = new BigInteger("750943811548649846513124564987884561373962357253753752375238523782382352387");
        BigInteger diffieR1 = diffieG.modPow(diffieX, diffieP);

        try {
            sendR1(diffieR1, clientIP, PORT);
            System.out.println("Send R1 : " + diffieR1);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        BigInteger diffieR2 = receiveR2();
        System.out.println("R2 received: " + diffieR2);


        receiving_socket.close();

        BigInteger diffieK = calculateK(diffieR2, diffieX, diffieP);


        boolean running = true;
        while(running) {
            try {
                byte[] buffer = new byte[512];


                buffer = recorder.getBlock();


                ByteBuffer unwrapEncrypt = ByteBuffer.allocate(buffer.length);
                ByteBuffer plainAudio = ByteBuffer.wrap(buffer);

                unwrapEncrypt.put(encrypt(diffieK, buffer));

                byte[] ecnryptedBlock = unwrapEncrypt.array();

                ByteBuffer VOIPpacket = ByteBuffer.allocate(512 + authKeyString.length() + 4);
                //buffer size + key size + int size`


                byte[] authKeyBytes = authKeyString.getBytes(StandardCharsets.UTF_8);

                int hashCode = hashPacket(authKeyString, buffer);

                VOIPpacket.put(authKeyBytes);
                VOIPpacket.putInt(hashCode);
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


    byte[] encrypt(BigInteger K, byte[] buffer){
        byte[] result = new byte[512];
        byte[] kBytes = K.toByteArray();
        int o = 0;
        for(int i = 0; i<buffer.length; i++){
            if(i > kBytes.length-1) o = 0;
            result[i] = (byte) (buffer[i] ^ kBytes[o]);
            o++;
        }

        return result;
    }

    void sendR1(BigInteger R1, InetAddress clientIP, int PORT) throws IOException {

        ByteBuffer bb = ByteBuffer.allocate(R1.toByteArray().length + 4);
        bb.putInt(R1.toByteArray().length);  //lenght of R1 is first int in array
        bb.put(R1.toByteArray());

        DatagramPacket packet = new DatagramPacket(bb.array(), bb.array().length, clientIP, PORT);

        sending_socket.send(packet);
    }

    BigInteger receiveR2(){
        byte[] incomingPacket = new byte[512];
        //byte[] len = new byte[4];
        //DatagramPacket length = new DatagramPacket(len, 0, 4);
        DatagramPacket packet = new DatagramPacket(incomingPacket, 0, 512);
        try {
            receiving_socket.receive(packet);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        ByteBuffer bb = ByteBuffer.wrap(incomingPacket);
        int len = bb.getInt();

        byte[] r2Packet = new byte[len];
        for(int i = 4; i<len+4; i++){
            r2Packet[i-4] = incomingPacket[i];
        }

        return new BigInteger(r2Packet);
    }

    BigInteger calculateK(BigInteger R2, BigInteger X, BigInteger P){
        return new BigInteger(String.valueOf(R2.modPow(X, P)));
    }

    int hashPacket(String key, byte[] buffer){
        byte[] toBeHashed = new byte[key.getBytes().length + buffer.length];
        return Arrays.hashCode(toBeHashed);
    }

}
