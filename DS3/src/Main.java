public class Main {
    public static void main(String[] args) {

        DS3Receiver recieverThread = new DS3Receiver();
        DS3Sender senderThread = new DS3Sender();

        recieverThread.start();
        senderThread.start();
    }
}