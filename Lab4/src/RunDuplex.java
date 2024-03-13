public class RunDuplex {
    public static void main(String[] args) {
        EncryptedReceiver recieverThread = new EncryptedReceiver();
        EncryptedSender senderThread = new EncryptedSender();

        Thread recvr = recieverThread.start();
        Thread sendr = senderThread.start();
    }
}
