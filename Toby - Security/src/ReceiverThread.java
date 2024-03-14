import CMPC3M06.AudioPlayer;

import javax.sound.sampled.LineUnavailableException;
import java.io.IOException;
import java.math.BigInteger;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class ReceiverThread implements Runnable{

    static DatagramSocket receiving_socket;
    static DatagramSocket sending_socket;

    public void start(){
        Thread thread = new Thread(this);
        thread.start();
    }

    public void run (){
        int PORT = 55555;
        InetAddress clientIP = null;
        try {
            clientIP = InetAddress.getByName("139.222.98.202");
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
        String authKeyString = "F(D&KL@DLC";

        BigInteger diffieP = new BigInteger("203956878356401977405765866929034577280193993314348263094772646453283062722701277632936616063144088173312372882677123879538709400158306567338328279154499698366071906766440037074217117805690872792848149112022286332144876183376326512083574821647933992961249917319836219304274280243803104015000563790123");
        BigInteger diffieG = new BigInteger("5445489838578453783549853290423473848590832940923504273483290275890324832904823940");
        BigInteger diffieY = new BigInteger("151888487551313212315654435454536446413233415231234642337233432345342503054230523345");
        BigInteger diffieR2 = diffieG.modPow(diffieY, diffieP);


        BigInteger diffieR1 = receiveR1();
        System.out.println("R1 recieved: " + diffieR1);

        try {

            sendR2(diffieR2, clientIP, 1234);
            System.out.println("R2 Send: " + diffieR2);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        sending_socket.close();

        BigInteger diffieK = calculateK(diffieR1, diffieY, diffieP);


        boolean running = true;
        while (running){
            byte[] secureBuffer = new byte[526];
            byte[] buffer = new byte[512];

            DatagramPacket packet = new DatagramPacket(secureBuffer, 0, 526);

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
            int hashed = bb.getInt();
            bb.get(buffer);


            /*
            Authenticating the key by itself and the whole hash of the code and string
            means that we can distinguish between the sender being an imposter or the
            payload being non genuine
             */

            if(!authKeyString.contentEquals(bytes)){
                System.err.println("Packet received from non genuine sender");
                continue;
            }

            if(!authenticateHash(authKeyString, buffer, hashed)){
                System.err.println("Non genuine packet received");
                continue;
            }

            ByteBuffer unwrapDecrypt = ByteBuffer.allocate(buffer.length);
            ByteBuffer cipherText = ByteBuffer.wrap(buffer);

            unwrapDecrypt.put(decrypt(diffieK, buffer));

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


    byte[] decrypt(BigInteger K, byte[] buffer){
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

    void sendR2(BigInteger R2, InetAddress clientIP, int PORT) throws IOException {

        ByteBuffer bb = ByteBuffer.allocate(R2.toByteArray().length + 4);
        bb.putInt(R2.toByteArray().length);  //lenght of R1 is first int in array
        bb.put(R2.toByteArray());

        DatagramPacket packet = new DatagramPacket(bb.array(), bb.array().length, clientIP, PORT);

        sending_socket.send(packet);
    }

    BigInteger receiveR1(){
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

        byte[] r1Packet = new byte[len];
        for(int i = 4; i<len+4; i++){
            r1Packet[i-4] = incomingPacket[i];
        }

        return new BigInteger(r1Packet);
    }

    BigInteger calculateK(BigInteger R2, BigInteger Y, BigInteger P){
        return new BigInteger(String.valueOf(R2.modPow(Y, P)));
    }

    int hashPacket(String key, byte[] buffer){
        byte[] toBeHashed = new byte[key.getBytes().length + buffer.length];
        return Arrays.hashCode(toBeHashed);
    }

    boolean authenticateHash(String key, byte[] buffer, int hashed){
        int hash = hashPacket(key, buffer);
        return hash == hashed;
    }

}
