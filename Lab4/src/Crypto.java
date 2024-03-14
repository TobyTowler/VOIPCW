import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Random;

public class Crypto {

    public static byte[] digest(byte[] data, BigInteger key) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(data);
            if (key == null) {
                return md.digest();
            } else {
                return md.digest(key.toByteArray());
            }
        } catch (NoSuchAlgorithmException e) {
        }
        return null;
    }

    public static boolean applyCipher(BigInteger key, byte[] data, boolean strict) {
        if (strict) {
            if (key.toByteArray().length != (DiffieHellmanParameters.MODULUS_BIT_LENGTH) / 8) {
                System.err.println("byte length is " + key.toByteArray().length + " but only " + DiffieHellmanParameters.MODULUS_BIT_LENGTH + " bit keys are supported");
                return false;
            }
        }


        byte[] keyBytes = key.toByteArray();
        final int keyByteCount = strict ? (DiffieHellmanParameters.MODULUS_BIT_LENGTH / 8) : keyBytes.length;
        if (strict) {
            if (data.length < keyByteCount) {
                System.err.println("data length is " + data.length + " but the data must be at least " + keyByteCount + " bytes long");
                return false;
            }
        }

        int keyIndex = 0;

        for (int i = 0; i < data.length; i++) {
            data[i] = (byte) (data[i] ^ keyBytes[keyIndex]);
            keyIndex = (keyIndex + 1) % keyByteCount;
        }

        return true;
    }

    public static class DiffieHellmanKey {

        private BigInteger privateExponent;

        public DiffieHellmanKey() {

        }

        public BigInteger publicDiffieHellmanComponent(DiffieHellmanParameters parameters) {

            SecureRandom rng = new SecureRandom();
            //BigInteger exponent = new BigInteger(DiffieHellmanParameters.MODULUS_BIT_LENGTH, 10, rng);
            BigInteger exponent = BigInteger.probablePrime(DiffieHellmanParameters.MODULUS_BIT_LENGTH, rng);
            this.privateExponent = exponent;
            BigInteger component = parameters.getGenerator().modPow(exponent, parameters.getModulus());
            return component;
        }

        public BigInteger sharedSecretKey(DiffieHellmanParameters parameters, BigInteger publicComponent) {
            return publicComponent.modPow(privateExponent, parameters.getModulus());
        }
    }


    public static class DiffieHellmanParameters {
        public static final int MODULUS_BIT_LENGTH = 2048;
        private final BigInteger modulus;
        private final BigInteger generator;

        public DiffieHellmanParameters() {
            SecureRandom rng = new SecureRandom();
            Random fastRng = new Random();
            this.modulus = BigInteger.probablePrime(MODULUS_BIT_LENGTH - 1, rng);
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

        public ByteBuffer asBytes() {
            System.out.println(" mod byte len: " + modulus.toByteArray().length);
            System.out.println(" generator byte len: " + generator.toByteArray().length);
            ByteBuffer bin = ByteBuffer.allocate(modulus.toByteArray().length + generator.toByteArray().length);
            bin.put(modulus.toByteArray());
            bin.put(generator.toByteArray());
            System.out.println("buffer len: " + bin.array().length);
            return bin;
        }

        public DiffieHellmanParameters(ByteBuffer buf) {
            byte[] modBytes = new byte[MODULUS_BIT_LENGTH / 8];
            byte generatorByte = 0;

            System.out.println("buffer length: " + buf.capacity());
            System.out.println("mod byte len: " + modBytes.length);
            for (int i = 0; i < modBytes.length; i++) {
                modBytes[i] = buf.get(i);
            }
            generatorByte = buf.get(256);

            modulus = new BigInteger(modBytes);
            generator = new BigInteger(new byte[]{generatorByte});
        }
    }

}


