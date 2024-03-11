import CMPC3M06.AudioPlayer;
import CMPC3M06.AudioRecorder;
import uk.ac.uea.cmp.voip.DatagramSocket4;

import javax.sound.sampled.LineUnavailableException;
import java.io.IOException;
import java.net.*;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Scanner;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

public class DS4Configuration {
    //public static final int TIMEOUT = 100;
    public static AtomicBoolean quitFlag = new AtomicBoolean(false);

    public static void main(String[] args) throws SocketException, UnknownHostException, InterruptedException, ExecutionException {
        int PORT = 55555;
        InetAddress IP = InetAddress.getByName("localhost");
        InetSocketAddress address = new InetSocketAddress(IP, PORT);

        DatagramSocket4 socket = new DatagramSocket4(null);
        socket.bind(address);
        //socket.setSoTimeout(TIMEOUT);
        try (ExecutorService pool = Executors.newFixedThreadPool(2)) {
            Future<?> threadA = pool.submit(() -> {
                try {
                    //DatagramSocket4 socket = new DatagramSocket4(null);
                    //socket.bind(address);
                    //socket.setSoTimeout(TIMEOUT);
                    send(socket, address);
                } catch (IOException | LineUnavailableException e) {
                    throw new RuntimeException(e);
                }
            });

            Future<?> threadB = pool.submit(() -> {
                try {
                    //DatagramSocket4 socket = new DatagramSocket4(null);
                    //socket.bind(address);
                    //socket.setSoTimeout(TIMEOUT);
                    receive(socket);
                } catch (LineUnavailableException e) {
                    e.printStackTrace();
                }
            });
            //pool.awaitTermination()
            threadA.get();
            threadB.get();
        } catch (ExecutionException e) {
            socket.close();
            throw new ExecutionException(e);
        }

        socket.close();
    }

    public static void send(DatagramSocket4 socket , SocketAddress receiverAddress) throws IOException, LineUnavailableException {
        int[] schedule = Network.interleaveOrder(4);
        Network.VoipPacket[] packetBuffer = new Network.VoipPacket[16];
        AudioRecorder recorder = new AudioRecorder();
        while(getQuit() == false){
            for(int i = 0; i < 16; i++){
                int bufferIndex = schedule[i];
                byte[] block = recorder.getBlock();
                packetBuffer[bufferIndex] = new Network.VoipPacket(getAuthKey(), (byte)i, block);
            }

            for(int i=0; i < 16; i++){
                DatagramPacket packet = packetBuffer[i].datagram(receiverAddress);
                socket.send(packet);
                //System.out.println("sent SN " + packetBuffer[i].sequenceNumber);
            }
        }

        recorder.close();
        socket.close();
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

    public static void receive(DatagramSocket4 socket) throws  LineUnavailableException {
        AudioPlayer player = new AudioPlayer();
        //Network.VoipPacket receivedPacket = new Network.VoipPacket((short) 0, (byte) 0, new byte[512]);
        System.out.println("recvr running");
        byte[] raw = new byte[Network.VoipPacket.PACKET_LENGTH_BYTES];
        DatagramPacket packet = new DatagramPacket(raw, Network.VoipPacket.PACKET_LENGTH_BYTES);
        int[] schedule = Network.interleaveOrder(4);
        byte[] lastBuffer = new byte[512];
        int next = 0;
        Network.VoipPacket[] audioBuffer = new Network.VoipPacket[16];
        while(getQuit() == false){
            try {
                socket.receive(packet);
                Network.VoipPacket receivedPacket = new Network.VoipPacket(packet.getData());
                if (receivedPacket.sequenceNumber != schedule[next]){
                    System.out.println("SN " + receivedPacket.sequenceNumber + " != "  + schedule[next] + " packet lost detected");
                }
                if(!Arrays.equals(receivedPacket.calculateDigest(),receivedPacket.msgDigest)){
                    Network.VoipPacket nextPacket = new Network.VoipPacket((short) 0, (byte) schedule[Math.floorMod(next - 1, 16)], lastBuffer);
                    audioBuffer[next] = nextPacket;
                }else{
                    audioBuffer[next] = receivedPacket;
                    for(int i = 0; i < 512; i++){
                        lastBuffer[i] = receivedPacket.audio[i];
                    }
                }

                if ((next + 1) == 16){
                    Arrays.sort(audioBuffer, Comparator.comparingInt(a -> a.sequenceNumber));
                    for(int i = 0; i < 16; i++){
                        player.playBlock(audioBuffer[i].audio);
                        //System.out.println("===played===");
                    }
                }
                next = (next + 1) % 16;
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
