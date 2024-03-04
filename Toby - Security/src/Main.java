/****************************************
Security Datagrams

Encryption
 * Encrypted using Long values
 * Long has a size of 64 bits, twice int (32 bits)
 * This is the largest data that ^ can be applied to
 * The longer key means that there are more combinations, therefore more secure

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