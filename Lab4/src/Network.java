import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.time.temporal.ChronoUnit;

public class Network {
    public static class VoipPacket{
        public final short authenticationKey;
        public final byte sequenceNumber;
        public byte[] audio = new byte[512];

        public VoipPacket(short authKey, byte sequenceNumber, byte[] audio){
            for(int i = 0; i < 512; i++){
                this.audio[i] = audio[i];
            }
            this.sequenceNumber = sequenceNumber;
            this.authenticationKey = authKey;
        }

        public VoipPacket(byte[] raw){
            ByteBuffer buffer = ByteBuffer.wrap(raw);
            this.authenticationKey = buffer.getShort(0);
            this.sequenceNumber = buffer.get(2);
            for(int i = 3; i < 515; i++){
                this.audio[i - 3] = buffer.get(i);
            }
        }

        public DatagramPacket datagram(SocketAddress address){
            ByteBuffer raw = ByteBuffer.allocate(512 + 2 + 1);
            raw.putShort(authenticationKey);
            raw.put(sequenceNumber);
            raw.put(audio);
            byte[] bytes = raw.array();
            return new DatagramPacket(bytes,bytes.length, address);
        }
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
