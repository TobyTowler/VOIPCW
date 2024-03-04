import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;

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

        public DatagramPacket datagram(){
            ByteBuffer raw = ByteBuffer.allocate(512);
            raw.putShort(authenticationKey);
            raw.put(sequenceNumber);
            raw.put(audio);
            byte[] bytes = raw.array();
            return new DatagramPacket(bytes,bytes.length);
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

    public static Duration measureDelay(DatagramSocket sender,DatagramSocket receiver) throws IOException {
        final long rounds = 1000;
        final byte[] dummyData = new byte[512];
        long startTime = 0;
        VoipPacket packet = new VoipPacket((short) 0, (byte) 0, dummyData);
        DatagramPacket dummyPacket = packet.datagram();
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

    public double packetloss(DatagramSocket sender, DatagramSocket receiver) throws IOException {
        final long rounds = 1000;
        final byte[] dummyData = new byte[512];
        long startTime = 0;
        short sentPacket = 0;
        long lost = 0;
        for(int i = 0; i < rounds; i++){
            VoipPacket packet = new VoipPacket(sentPacket, (byte) 0, dummyData);
            DatagramPacket dummyPacket = packet.datagram();
            sender.send(dummyPacket);
            try{
                receiver.receive(dummyPacket);
                VoipPacket receivedPacket = new VoipPacket(dummyPacket.getData());
                if (receivedPacket.sequenceNumber != sentPacket){
                    lost += 1;
                }
            }catch(IOException e){
                // timed out
                lost += 1;
            }
            sentPacket = (short)((sentPacket + 1) % 16);
        }
        return (double)lost/(double)rounds;
    }

    public long bitrate(DatagramSocket sender)throws IOException{
        final byte[] dummyData = new byte[512];
        VoipPacket packet = new VoipPacket((short) 0, (byte) 0, dummyData);
        DatagramPacket dummyPacket = packet.datagram();
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
