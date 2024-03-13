import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Random;

public class Crypto {

    public static byte[] digest(byte[] data, BigInteger key){
        try{
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(data);
            if (key == null) {
                return md.digest();
            }else{
                return md.digest(key.toByteArray());
            }
        }catch(NoSuchAlgorithmException e){
        }
        return null;
    }
    public static boolean applyCipher(BigInteger key, byte[] data, boolean strict){
        if (strict) {
            if (key.toByteArray().length != (DiffieHellmanParameters.MODULUS_BIT_LENGTH) / 8) {
                System.err.println("byte length is " + key.toByteArray().length + " but only " + DiffieHellmanParameters.MODULUS_BIT_LENGTH + " bit keys are supported");
                return false;
            }
        }


        byte[] keyBytes = key.toByteArray();
        final int keyByteCount = strict ?  (DiffieHellmanParameters.MODULUS_BIT_LENGTH / 8): keyBytes.length;
        if(strict) {
            if (data.length < keyByteCount) {
                System.err.println("data length is " + data.length + " but the data must be at least " + keyByteCount + " bytes long");
                return false;
            }
        }

        int keyIndex = 0;

        for(int i = 0; i < data.length; i++){
            data[i] = (byte)(data[i] ^ keyBytes[keyIndex]);
            keyIndex = (keyIndex + 1 ) % keyByteCount;
        }

        return true;
    }

    public static class DiffieHellmanKey{

        private BigInteger privateExponent;

        public DiffieHellmanKey(){

        }

        public BigInteger publicDiffieHellmanComponent(DiffieHellmanParameters parameters){

            SecureRandom rng = new SecureRandom();
            //BigInteger exponent = new BigInteger(DiffieHellmanParameters.MODULUS_BIT_LENGTH, 10, rng);
            BigInteger exponent = BigInteger.probablePrime(DiffieHellmanParameters.MODULUS_BIT_LENGTH, rng);
            this.privateExponent = exponent;
            BigInteger component = parameters.getGenerator().modPow(exponent, parameters.getModulus());
            return component;
        }

        public BigInteger sharedSecretKey(DiffieHellmanParameters parameters, BigInteger publicComponent){
            return publicComponent.modPow(privateExponent,parameters.getModulus());
        }
    }


    public static class DiffieHellmanParameters{
        public static final int MODULUS_BIT_LENGTH = 2048;
        private final BigInteger modulus;
        private final BigInteger generator;

        public DiffieHellmanParameters(){
            SecureRandom rng = new SecureRandom();
            Random fastRng = new Random();
            this.modulus = BigInteger.probablePrime(MODULUS_BIT_LENGTH - 1,  rng);
            System.out.println(" org mod byte len: " + modulus.toByteArray().length);
            //BigInteger tmp = new BigInteger(MODULUS_BIT_LENGTH - 1, fastRng);
            this.generator = BigInteger.TWO;
        }

        public BigInteger getModulus() {
            return modulus;
        }

        public BigInteger getGenerator() {
            return generator;
        }

        public ByteBuffer asBytes(){
            System.out.println(" mod byte len: " + modulus.toByteArray().length);
            System.out.println(" generator byte len: " + generator.toByteArray().length);
            ByteBuffer bin = ByteBuffer.allocate(modulus.toByteArray().length + generator.toByteArray().length);
            bin.put(modulus.toByteArray());
            bin.put(generator.toByteArray());
            System.out.println("buffer len: " + bin.array().length);
            return bin;
        }

        public DiffieHellmanParameters (ByteBuffer buf){
            byte[] modBytes = new byte[MODULUS_BIT_LENGTH / 8];
            byte generatorByte = 0;

            System.out.println("buffer length: " + buf.capacity());
            System.out.println("mod byte len: " + modBytes.length);
            for (int i = 0; i < modBytes.length; i++){
                modBytes[i] = buf.get(i);
            }
            generatorByte = buf.get(256);

            modulus = new BigInteger(modBytes);
            generator = new BigInteger(new byte[]{generatorByte});
        }
    }
    /*
    public byte[] md5(byte[] message){

    }*/


    public byte[] SHA256(byte [] message){
        ArrayList<Byte> msg = padMessage(message);
        return null;
    }

    public byte[] md5(byte[] message){
        ArrayList<Byte> msg = padMessage(message);
        int a = 0x01234567;
        int b = 0x89ABCDEF;
        int c = 0xFEBCDA98;
        int d = 0X76543210;

        int counter = 0;
        while (counter < msg.size()){
            byte[] shrt = new byte[2];
            //shrt[0] =
        }
        return null;
    }
    private int f(int x, int y, int z){
        return (x & y) | (~x & z);
    }

    private int g(int x, int y, int z){
        return (x & z) | (y & ~z);
    }

    private int h(int x, int y, int z){
        return (x ^ y ^ z);
    }

    private int i(int x, int y, int z){
       return (y ^ (x | ~z));
    }

    public ArrayList<Byte> padMessage(byte[] message){
        long last = lastSetBit(message);
        long originalLen = last + 1;
        long offset = last % 8;
        ArrayList<Byte> data = new ArrayList<Byte>();
        for (int i = 0; i < message.length; i++){
            data.add(message[i]);
        }
        if (offset == 0){
            data.add((byte)1);
        }else{
            data.set(data.size() - 1, (byte)(data.getLast() | (1 << offset + 1)));
        }

        last = lastSetBit(data.getLast()) + (8 * (data.size() - 1));
        long padAmmount = padAmmount(last);
        for(long i = 0; i < padAmmount; i++){
            data.add((byte)0);
        }
        ByteBuffer lengthEncoded = ByteBuffer.allocate(Long.BYTES);
        lengthEncoded.putLong(originalLen);
        byte[] lengthEncodedBytes = lengthEncoded.array();
        for(int i = 0; i < lengthEncodedBytes.length; i++){
            data.add(lengthEncodedBytes[i]);
        }

        return data;
    }

    public long padAmmount(long lastSetBit){
        long rem = lastSetBit % 8;
        long full = lastSetBit + (8 - rem);
        long bytes = full / 8;
        long padAmmount = 56 - (bytes % 56);
        return padAmmount;
    }
    public long lastSetBit(byte [] message){
        int nbits = message.length * 8;
        for(int i = nbits; i >=0; i++){
            int offset = i % 8;
            int block = nbits / 8;
            if(((message[block]) & ( 1 << offset)) > 0 ){
                return i;
            }
        }
        return -1;
    }
    public int lastSetBit(byte message){
        for(int i = 7; i > -1; i--){
            if ((message & (1 << i)) > 0 ){
                return i;
            }
        }
        return -1;
    }

}
