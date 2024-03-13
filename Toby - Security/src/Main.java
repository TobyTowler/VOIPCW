/****************************************
Security Datagrams

Encryption
 * Diffie-Hellman
 * R1 and R2 are sent before communication starts
 * K is also calculated before communication starts
 * This means both the sender and receiver are ready when voice communication starts

Sender Key
 * Used String
 * Size is changeable - would need to change total packet size and key size
 * Currently set to 10 characters
 * Longer key and more characters means more security
 * If sender key does not match, packet is discarded
****************************************/


public class Main {
    public static void main(String[] args) {
        SenderThread sender = new SenderThread();
        ReceiverThread receiver = new ReceiverThread();

        sender.start();
        receiver.start();
    }
}
