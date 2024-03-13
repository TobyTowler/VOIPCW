import CMPC3M06.AudioPlayer;
import CMPC3M06.AudioRecorder;
import uk.ac.uea.cmp.voip.DatagramSocket4;

import javax.sound.sampled.LineUnavailableException;
import java.io.IOException;
import java.math.BigInteger;
import java.net.*;
import java.util.Arrays;
import java.util.Base64;
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
        Crypto.DiffieHellmanParameters diffieHellmanParameters = new Crypto.DiffieHellmanParameters();
        BigInteger sharedSecret = new BigInteger("5445489838578453783549853290423473848590832940923504273483290275890324832904823940");
        try (ExecutorService pool = Executors.newFixedThreadPool(2)) {
            Future<?> threadA = pool.submit(() -> {
                try {
                    //DatagramSocket4 socket = new DatagramSocket4(null);
                    //socket.bind(address);
                    //socket.setSoTimeout(TIMEOUT);
                    /*
                    BigInteger receivedComponent = null;
                    Crypto.DiffieHellmanKey key = new Crypto.DiffieHellmanKey();
                    BigInteger pubComp = key.publicDiffieHellmanComponent(diffieHellmanParameters);
                    byte[] rawComp = Network.VoipPacket.diffieHellmanPublicComponentPacket(Network.VoipPacket.SENDER_DHPC_TYPE,pubComp, null);

                    DatagramPacket packet = new DatagramPacket(rawComp, rawComp.length, address);
                    while(receivedComponent == null) {
                        System.out.println("sendr started waiting");
                        socket.send(packet);
                        Thread.sleep(200);
                        socket.receive(packet);
                        System.out.println("sendr finished waiting");
                        receivedComponent = Network.VoipPacket.readValidDHPC(packet.getData(), Network.VoipPacket.RECVR_DHPC_TYPE);
                        if(receivedComponent == null){
                            System.out.println("could not complete key exchange retrying");
                        }
                    }
                    BigInteger sharedSecret = key.sharedSecretKey(diffieHellmanParameters, receivedComponent);

                    System.out.println("sender secret: " + new String(Base64.getEncoder().encode(sharedSecret.toByteArray())));
                    //while(!getQuit()){}
                    */
                    send(socket, address, sharedSecret);
                } catch (IOException | LineUnavailableException e) {
                    e.printStackTrace();
                }
            });

            Future<?> threadB = pool.submit(() -> {
                try {
                    //DatagramSocket4 socket = new DatagramSocket4(null);
                    //socket.bind(address);
                    //socket.setSoTimeout(TIMEOUT);
                    /*
                    Crypto.DiffieHellmanKey key = new Crypto.DiffieHellmanKey();
                    BigInteger pubComp = key.publicDiffieHellmanComponent(diffieHellmanParameters);

                    byte[] dummyBytes = new byte[2 + 32 + (Crypto.DiffieHellmanParameters.MODULUS_BIT_LENGTH / 8)];
                    DatagramPacket dummy = new DatagramPacket(dummyBytes, dummyBytes.length, address);
                    BigInteger receivedComponent = null;
                    while(receivedComponent == null) {
                        socket.receive(dummy);
                        receivedComponent = Network.VoipPacket.readValidDHPC(dummy.getData(), Network.VoipPacket.SENDER_DHPC_TYPE);
                    }

                    byte[] rawComp = Network.VoipPacket.diffieHellmanPublicComponentPacket(Network.VoipPacket.RECVR_DHPC_TYPE,pubComp, null);
                    DatagramPacket packet = new DatagramPacket(rawComp, rawComp.length, address);
                    socket.send(packet);
                    BigInteger sharedSecret = key.sharedSecretKey(diffieHellmanParameters, receivedComponent);

                    System.out.println("receiver secret: " + new String(Base64.getEncoder().encode(sharedSecret.toByteArray())));
                    //while(!getQuit()){}
                     */
                    receive(socket,sharedSecret);
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

    public static void send(DatagramSocket4 socket , SocketAddress receiverAddress, BigInteger encryptionKey) throws IOException, LineUnavailableException {
        System.out.println("sender running");
        int[] schedule = Network.interleaveOrder(4);
        Network.VoipPacket[] packetBuffer = new Network.VoipPacket[16];
        AudioRecorder recorder = new AudioRecorder();
        while(getQuit() == false){
            //System.out.println("sender in loop");
            for(int i = 0; i < 16; i++){
                int bufferIndex = schedule[i];
                byte[] block = recorder.getBlock();
                packetBuffer[bufferIndex] = new Network.VoipPacket(getAuthKey(), (byte)i, block);
                //System.out.println("applying encryption");
                Crypto.applyCipher(encryptionKey,packetBuffer[bufferIndex].audio,false);
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

    public static void receive(DatagramSocket4 socket, BigInteger encryptionKey) throws  LineUnavailableException {
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
            //System.out.println("recvr in loop");
            try {
                socket.receive(packet);
                Network.VoipPacket receivedPacket = new Network.VoipPacket(packet.getData());
                if (receivedPacket.sequenceNumber != schedule[next]){
                    //System.out.println("SN " + receivedPacket.sequenceNumber + " != "  + schedule[next] + " packet lost detected");
                }
                //System.out.println("applying encryption");
                if (encryptionKey != null){
                    Crypto.applyCipher(encryptionKey,receivedPacket.audio, false);
                }
                //System.out.println("applied encryption");
                if(!Arrays.equals(receivedPacket.calculateDigest(),receivedPacket.msgDigest) || receivedPacket.sequenceNumber != schedule[next]){
                    //System.out.println("packet " + receivedPacket.sequenceNumber + " is probably corrupted");
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
                    //System.out.print("deinterleaved: ");
                    for(int i = 0; i < 16; i++){
                        //System.out.print(" p" + i);
                        player.playBlock(audioBuffer[i].audio);
                        //System.out.println("===played===");
                    }
                    //System.out.println("");
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
