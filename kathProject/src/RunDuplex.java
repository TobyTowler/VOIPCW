public class RunDuplex {
    public static void main(String[] args) {
        EncryptedReceiver recieverThread = new EncryptedReceiver();
        EncryptedSender senderThread = new EncryptedSender();

        recieverThread.start();
        senderThread.start();
    }
}
