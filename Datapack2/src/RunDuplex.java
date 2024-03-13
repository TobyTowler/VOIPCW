public class RunDuplex {
    public static void main(String[] args) {
        AudioReciever recieverThread = new AudioReciever();
        EncryptedSender senderThread = new EncryptedSender();

        recieverThread.start();
        senderThread.start();
    }
}
