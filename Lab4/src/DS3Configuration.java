import CMPC3M06.AudioPlayer;
import CMPC3M06.AudioRecorder;
import uk.ac.uea.cmp.voip.DatagramSocket3;
import uk.ac.uea.cmp.voip.DatagramSocket4;

import javax.sound.sampled.LineUnavailableException;
import java.io.IOException;
import java.math.BigInteger;
import java.net.*;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

public class DS3Configuration {
    //public static final int TIMEOUT = 100;
    public static AtomicBoolean quitFlag = new AtomicBoolean(false);
    public static final boolean INTERLEAVE_MODE = false;
    public static final boolean REPEATER_MODE  = false;

    public static void main(String[] args) throws SocketException, UnknownHostException, InterruptedException, ExecutionException {
        int PORT = 55555;

        InetSocketAddress address = new InetSocketAddress(PORT);



        //InetSocketAddress target = new InetSocketAddress("139.222.97.107", PORT);
        InetSocketAddress target = new InetSocketAddress("localhost", PORT);

        //socket.setSoTimeout(TIMEOUT);
        DatagramSocket3 socket = new DatagramSocket3(null);
        socket.setSoTimeout(500);
        socket.bind(address);

        Crypto.DiffieHellmanParameters diffieHellmanParameters = new Crypto.DiffieHellmanParameters();
        BigInteger sharedSecret = new BigInteger("5445489838578453783549853290423473848590832940923504273483290275890324832904823940");
        ExecutorService pool = Executors.newFixedThreadPool(2);
        Future<?> threadA = pool.submit(() -> {
            try {

                send(socket, target, sharedSecret);
            } catch (IOException | LineUnavailableException e) {
                e.printStackTrace();
            }
        });

        Future<?> threadB = pool.submit(() -> {
            try {

                receive(socket,sharedSecret);
            } catch (LineUnavailableException e) {
                e.printStackTrace();
            }
        });
        //pool.awaitTermination()
        threadA.get();
        threadB.get();
    }



    public static void interleaveSend(DatagramSocket3 socket, SocketAddress receiver, BigInteger encryptionKey) throws LineUnavailableException, IOException {
        System.out.println("sender running");
        int[] schedule = Network.interleaveOrder(4);
        Network.VoipPacket[] packetBuffer = new Network.VoipPacket[16];
        AudioRecorder recorder = new AudioRecorder();
        while(getQuit() == false){
            //System.out.println("sender in loop");
            for(int i = 0; i < schedule.length; i++){
                byte[] block = recorder.getBlock();
                Network.VoipPacket packet = new Network.VoipPacket(getAuthKey(), (byte) schedule[i], block);
                //System.out.println("applying encryption");
                Crypto.applyCipher(encryptionKey,packet.audio,false);
                packetBuffer[schedule[i]] = packet;
            }

            for(int i = 0; i < schedule.length; i++){
                socket.send(packetBuffer[i].datagram(receiver));
                System.out.println("sent " + i);
            }

        }

        recorder.close();
        socket.close();
    }

    public static void normalSend(DatagramSocket3 socket, SocketAddress receiverAddress, BigInteger encryptionKey) throws LineUnavailableException, IOException {
        System.out.println("sender running");
        //int[] schedule = Network.interleaveOrder(4);
        Network.VoipPacket[] packetBuffer = new Network.VoipPacket[16];
        AudioRecorder recorder = new AudioRecorder();
        while(getQuit() == false){
            //System.out.println("sender in loop");
            for(int i = 0; i < 16; i++){
                byte[] block = recorder.getBlock();
                Network.VoipPacket packet = new Network.VoipPacket(getAuthKey(), (byte) i, block);
                //System.out.println("applying encryption");
                Crypto.applyCipher(encryptionKey,packet.audio,false);
                socket.send(packet.datagram(receiverAddress));
                System.out.println("sent " + i);
            }

        }

        recorder.close();
        socket.close();
    }


    public static void send(DatagramSocket3 socket , SocketAddress receiverAddress, BigInteger encryptionKey) throws IOException, LineUnavailableException {
        if (INTERLEAVE_MODE){
            interleaveSend(socket,receiverAddress,encryptionKey);
        }else {
            normalSend(socket, receiverAddress, encryptionKey);
        }
    }

    public static boolean getQuit(){
        return quitFlag.get();
    }

    public static void waitForQuit(){
        Scanner io = new Scanner(System.in);
        boolean quit = false;
        while(!quit){
            if (io.nextLine().trim().equals("q")){
                quitFlag.compareAndSet(false,true);
                quit = true;
            }
        }
    }

    public static void receive(DatagramSocket3 socket, BigInteger encryptionKey) throws  LineUnavailableException {
        AudioPlayer player = new AudioPlayer();
        //Network.VoipPacket receivedPacket = new Network.VoipPacket((short) 0, (byte) 0, new byte[512]);
        System.out.println("recvr running");
        byte[] raw = new byte[Network.VoipPacket.PACKET_LENGTH_BYTES];
        DatagramPacket packet = new DatagramPacket(raw, Network.VoipPacket.PACKET_LENGTH_BYTES);
        int[] schedule = Network.interleaveOrder(4);

        HashMap<Byte, byte[]> outOfOrder = new HashMap<>();
        int next = 0;

        int latency = 16;
        while(getQuit() == false){
            //System.out.println("recvr in loop");
            try {
                System.out.println("recvr waiting");
                socket.receive(packet);
                System.out.println("recvr done waiting");

                Network.VoipPacket receivedPacket = new Network.VoipPacket(packet.getData());
                if (receivedPacket.sequenceNumber != schedule[next]){
                    //System.out.println("SN " + receivedPacket.sequenceNumber + " != "  + schedule[next] + " packet lost detected");
                }
                //System.out.println("applying encryption");
                if (encryptionKey != null){
                    Crypto.applyCipher(encryptionKey,receivedPacket.audio, false);
                }

                //player.playBlock(receivedPacket.audio);


                if(outOfOrder.size() != latency){
                    outOfOrder.put(receivedPacket.sequenceNumber,receivedPacket.audio);
                }
                System.out.println("received " + receivedPacket.sequenceNumber);
                System.out.println("receiver buffer fill : " + outOfOrder.size());
                if(outOfOrder.size()  == latency){
                    System.out.println("emptying buffer");
                    byte[] lastPlayed = new byte[512];
                    for(int i = 0; i < 16; i++){
                        byte[] maybeNext = outOfOrder.get((byte)i);
                        if(maybeNext != null){
                            player.playBlock(maybeNext);
                            if(REPEATER_MODE == true){
                                System.arraycopy(maybeNext, 0, lastPlayed, 0, 512);
                            }
                            outOfOrder.remove((byte)i);
                        }else{
                            player.playBlock(lastPlayed);
                        }
                    }
                }else{
                    System.out.println("waiting for buffer to fill");
                }




            } catch (IOException e) {
                if (e instanceof SocketTimeoutException ){
                    System.out.println("read timed out");
                }else{
                    e.printStackTrace();
                }
            }
        }
        socket.close();
        player.close();
    }

    static short getAuthKey(){
        return 17;
    }
}

