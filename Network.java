import java.io.IOException;
import java.math.BigInteger;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;

public class Network {

    public static int [] interleaveOrder(int matrixRowLength){
        int len = matrixRowLength * matrixRowLength;
        int[] schedule = new int[len];
        int blockOffset = 0;
        int index = 0;
        for(int i = 0; i < len; i++){
            schedule[i] = index;
            if(index + matrixRowLength >= len){
                blockOffset += 1;
                index = blockOffset;
            }else{
                index += matrixRowLength;
            }
        }
        return schedule;
    }

    public static class VoipPacket{
        public static final short SENDER_DHPC_TYPE = 0x10AA;
        public static final short RECVR_DHPC_TYPE = 0x20BB;
        public final short authenticationKey;
        public final byte sequenceNumber;
        public byte[] msgDigest = new byte[32];
        public byte[] audio = new byte[512];

        public static final int PACKET_LENGTH_BYTES = 512 + 32 + 2 + 1;
        public VoipPacket(short authKey, byte sequenceNumber, byte[] audio){
            for(int i = 0; i < 512; i++){
                this.audio[i] = audio[i];
            }
            this.sequenceNumber = sequenceNumber;
            this.authenticationKey = authKey;
            msgDigest = calculateDigest();
        }

        public static byte[] diffieHellmanPublicComponentPacket(short type,BigInteger pubComponent, BigInteger authKey){

            ByteBuffer buf = ByteBuffer.allocate((Crypto.DiffieHellmanParameters.MODULUS_BIT_LENGTH / 8) + 2);
            buf.putShort(type);
            for (byte b : pubComponent.toByteArray()){
               buf.put(b);
            }
            //buf.put(pubComponent.toByteArray());
            byte[] digest = Crypto.digest(buf.array(), null);
            ByteBuffer finalBuf = ByteBuffer.allocate((Crypto.DiffieHellmanParameters.MODULUS_BIT_LENGTH / 8) + 2 + 32);
            finalBuf.putShort(type);
            for(byte b : digest){
               finalBuf.put(b);
            }
            for (byte b : pubComponent.toByteArray()){
                finalBuf.put(b);
            }
            //finalBuf.put(digest);
            return finalBuf.array();
        }


        public static BigInteger readValidDHPC(byte[] raw,short type){
            ByteBuffer buffer = ByteBuffer.wrap(raw);
            if (buffer.getShort() != type){
                System.err.println("packet is not a DHPC type packet");
                return null;
            }
            //ByteBuffer tmp = ByteBuffer.allocate((Crypto.DiffieHellmanParameters.MODULUS_BIT_LENGTH / 8) + 2);
            //tmp.get(1,new byte [], 2, 8);
            byte[] data = new byte[(Crypto.DiffieHellmanParameters.MODULUS_BIT_LENGTH / 8) + 2];
            buffer.get(0,data,0,2);;
            buffer.get(2 + 32,data,2, Crypto.DiffieHellmanParameters.MODULUS_BIT_LENGTH/8);

            byte[] receivedDigest = new byte[32];
            buffer.get(2,receivedDigest,0,32);
            byte[] computedDigest = Crypto.digest(data, null);
            if(!Arrays.equals(computedDigest,receivedDigest)){
               System.out.println("digest does not match");
               return null;
            }
            return new BigInteger(data,2,Crypto.DiffieHellmanParameters.MODULUS_BIT_LENGTH / 8);
        }

        public byte[] calculateDigest(){
            ByteBuffer raw = ByteBuffer.allocate(512 + 2 + 1);
            raw.putShort(authenticationKey);
            raw.put(sequenceNumber);
            raw.put(audio);
            return Crypto.digest(raw.array(),null);
        }

        public VoipPacket(byte[] raw){
            ByteBuffer buffer = ByteBuffer.wrap(raw);
            this.authenticationKey = buffer.getShort(0);
            this.sequenceNumber = buffer.get(2);
            for(int i = 3; i < 35; i++){
                this.msgDigest[i - 3] = buffer.get(i);
            }
            for(int i = 35; i < (35  + 512); i++){
                this.audio[i - 35] = buffer.get(i);
            }
        }

        public DatagramPacket datagram(InetAddress address, int port){
            ByteBuffer raw = ByteBuffer.allocate(PACKET_LENGTH_BYTES);
            //System.out.println("digest bytes: " + msgDigest.length);
            raw.putShort(authenticationKey);
            raw.put(sequenceNumber);
            raw.put(msgDigest);
            raw.put(audio);
            byte[] bytes = raw.array();
            return new DatagramPacket(bytes,bytes.length, address,port);
        }
        public DatagramPacket datagram(SocketAddress address){
            ByteBuffer raw = ByteBuffer.allocate(PACKET_LENGTH_BYTES);
            //System.out.println("digest bytes: " + msgDigest.length);
            raw.putShort(authenticationKey);
            raw.put(sequenceNumber);
            raw.put(msgDigest);
            raw.put(audio);
            byte[] bytes = raw.array();
            return new DatagramPacket(bytes,bytes.length, address);
        }
    }

    public static short crc(byte a1, byte a2){
        return 0;
    }

    /*
    public static ArrayList<VoipPacket> interleave(short authKey, ArrayList<byte[]> blocks){
        
        for (int blockOffset = 0; blockOffset < 4; blockOffset++){
            int index = blockOffset;
            for (int i = 0; i < 4; i++){
                
            }
        }
    }
    */

    public static Duration measureDelay(DatagramSocket sender,DatagramSocket receiver,SocketAddress address) throws IOException {
        final long rounds = 1000;
        final byte[] dummyData = new byte[512];
        long startTime = 0;
        VoipPacket packet = new VoipPacket((short) 0, (byte) 0, dummyData);
        DatagramPacket dummyPacket = packet.datagram(address);
        receiver.setSoTimeout(1000);
        for(int i = 0; i < rounds; i++){
            sender.send(dummyPacket);
            if(i == 0){
                startTime = System.nanoTime();
            }
            try {
                receiver.receive(dummyPacket);
            }catch(IOException ignored){}
        }
        long endTime = System.nanoTime();
        long elapsed = endTime - startTime;
        long averageNS = elapsed / rounds;
        return Duration.of(averageNS, ChronoUnit.NANOS);
    }

    public static class CorruptionStats{
       public final int packetsSent;
       public final int packetsCorrupted;
       public final int maxBitsCorrupted;
       public final int maxBurstLength;

       public CorruptionStats(int packetsSent, int packetsCorrupted, int maxBitsCorrupted, int maxBurstLength ){
           this.packetsSent = packetsSent;
           this.packetsCorrupted = packetsCorrupted;
           this.maxBitsCorrupted = maxBitsCorrupted;
           this.maxBurstLength = maxBurstLength;
       }

       public void display(){
           System.out.println("network had corrupted: " + packetsCorrupted + "/" + packetsSent + " max bits corrupted: " + maxBitsCorrupted + " max corruption burst length: " + maxBurstLength);
       }
    }

    public static CorruptionStats packetsCorrupted(DatagramSocket sender, DatagramSocket receiver, SocketAddress address) throws IOException {
        final int rounds = 1000;
        final byte[] dummyData = new byte[512];
        int corrupted = 0;
        byte sentPacket = 0;
        int maxBitsChanged = 0;
        int maxBurst = 0;
        int maxPacketBurst = 0;
        int packetBurst = 0;
        int lastPacketCorrupted = 0;
        int packetCount = 0;
        for(int i = 0; i < rounds; i++){
            VoipPacket packet = new VoipPacket((short)0, sentPacket, dummyData);
            DatagramPacket dummyPacket = packet.datagram(address);
            sender.send(dummyPacket);
            try{
                receiver.receive(dummyPacket);
                VoipPacket receivedPacket = new VoipPacket(dummyPacket.getData());
                if (receivedPacket.sequenceNumber != sentPacket){
                    System.out.println("sequence number mismatch: " + sentPacket + "!=" + receivedPacket.sequenceNumber );
                }

                byte[] receivedDigest = receivedPacket.msgDigest;
                if(!Arrays.equals(receivedDigest,receivedPacket.calculateDigest())){
                    System.out.println("packet " + packetCount + " digest mismatch");
                }

                int bitsChanged = 0;
                int burst = 0;
                int lastBitChanged = 0;
                for(int j = 0; j < 512; j++){
                    if(receivedPacket.audio[j] != 0){
                        if (bitsChanged == 0){
                            System.out.print("packet " + packetCount + " corrupted payload bits: ");
                        }else{
                            System.out.print(j + " ");
                        }
                        if (lastBitChanged == (j - 1)){
                            burst += 1;
                        }else{
                            maxBurst = Math.max(burst,maxBurst);
                            System.out.println("burst ended");
                            burst = 0;
                        }

                        bitsChanged += 1;
                        lastBitChanged = j;
                    }
                }
                if (lastPacketCorrupted == (packetCount -1 ) ){
                    packetBurst += 1;
                }else{
                    maxPacketBurst = Math.max(maxPacketBurst,packetBurst);
                    packetBurst = 0;
                }

                maxBurst = Math.max(burst,maxBurst);
                if (bitsChanged > 0){
                    System.out.println("");
                }
                if (bitsChanged != 0){
                    System.out.println(bitsChanged + " bits flipped");
                    lastPacketCorrupted = packetCount;
                    corrupted += 1;
                }
                maxBitsChanged = Math.max(maxBitsChanged,bitsChanged);
            }catch(IOException e){
                System.out.println("timed out");
                // timed out
            }
            sentPacket = (byte)((sentPacket + 1) % 16);
            packetCount += 1;
        }

        System.out.println("max bit errors: "+maxBitsChanged);
        System.out.println("max packet burst: " + maxPacketBurst);
        return new CorruptionStats(rounds,corrupted, maxBitsChanged, maxBurst);
    }

    //public static double simple
    public static double packetloss(DatagramSocket sender, DatagramSocket receiver, SocketAddress address) throws IOException {
        final long rounds = 1000;
        final byte[] dummyData = new byte[512];
        long startTime = 0;
        byte sentPacket = 0;
        long lost = 0;
        for(int i = 0; i < rounds; i++){
            VoipPacket packet = new VoipPacket((short)0, sentPacket, dummyData);
            DatagramPacket dummyPacket = packet.datagram(address);
            sender.send(dummyPacket);
            try{
                receiver.receive(dummyPacket);
                VoipPacket receivedPacket = new VoipPacket(dummyPacket.getData());
                if (receivedPacket.sequenceNumber != sentPacket){
                    System.out.println("sequence number mismatch: " + sentPacket + "!=" + receivedPacket.sequenceNumber );
                    lost += 1;
                }
            }catch(IOException e){
                System.out.println("timed out");
                // timed out
                lost += 1;
            }
            sentPacket = (byte)((sentPacket + 1) % 16);
        }
        System.out.println("lost " + lost + " packets");
        return (double)lost/(double)rounds;
    }


    public long bitrate(DatagramSocket sender, SocketAddress address)throws IOException{
        final byte[] dummyData = new byte[512];
        VoipPacket packet = new VoipPacket((short) 0, (byte) 0, dummyData);
        DatagramPacket dummyPacket = packet.datagram(address);
        long start = System.currentTimeMillis();
        long packetsSent = 0l;
        long lastPing = 0l;
        final long second = 1000L;
        while((lastPing - start) <= 3 * second){
            sender.send(dummyPacket);
            packetsSent += 1;
            lastPing = System.currentTimeMillis();
        }
        long avgSentPerSecond = packetsSent / 3;
        return (long)dummyPacket.getData().length * avgSentPerSecond;
    }

}
