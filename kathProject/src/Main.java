import CMPC3M06.AudioPlayer;
import CMPC3M06.AudioRecorder;

import java.nio.ByteBuffer;
import java.util.*;

public class Main {
    public static void main(String args[]) throws Exception {
        //Vector used to store audio blocks (32ms/512bytes each)
        Vector<byte[]> voiceVector = new Vector<byte[]>();

        //Initialise AudioPlayer and AudioRecorder objects
        AudioRecorder recorder = new AudioRecorder();
        AudioPlayer player = new AudioPlayer();

        //Recording time in seconds
        int recordTime = 2;

        //Capture audio data and add to voiceVector
        System.out.println("Recording Audio...");

        //keylist
        //15 - still somewhat understanable, lots of background noise
        //0 - no ecryption
        //9999 - very distored possible still removable
        //200 loud distortion, can still kind of makeout words
        //2131283743 louder distortion
        //higher generally better
        int key = 9999;

        for (int i = 0; i < Math.ceil(recordTime / 0.032); i++) {
            byte[] block = recorder.getBlock();
            ByteBuffer unwrapEncrypt = ByteBuffer.allocate(block.length);
            ByteBuffer plainText = ByteBuffer.wrap(block);
            for(int j = 0; j<block.length/4; j++){
                int fourByte = plainText.getInt();
                fourByte = fourByte ^ key;
                unwrapEncrypt.putInt(fourByte);

            }
            //player.playBlock(block);
            //voiceVector.add(block);
            voiceVector.add(unwrapEncrypt.array());
            //byte[] encryptedBlock = unwrapEncrypt.array();
        }

        //Close audio input
        recorder.close();

        //Iterate through voiceVector and play out each audio block
        System.out.println("Playing Audio...");

        Iterator<byte[]> voiceItr = voiceVector.iterator();
        Vector<byte[]> playable = new Vector<byte[]>();
//        System.out.println(voiceItr.next().length);

        while (voiceItr.hasNext()) {
            ByteBuffer unwrapDecrypt = ByteBuffer.allocate(voiceItr.next().length);
            System.out.println("voice length : " + voiceItr.next().length);
            ByteBuffer cipherText = ByteBuffer.wrap(voiceItr.next());
            for(int i = 0; i < 512/4; i++){
                int fourByte = cipherText.getInt();
                fourByte = fourByte ^ key;
                System.out.println("i: " + i);
                unwrapDecrypt.putInt(fourByte);
            }
            playable.add(unwrapDecrypt.array());
            //player.playBlock(unwrapDecrypt.array());
        }
        for(byte[] b : playable) {
            player.playBlock(b);
        }
        //Close audio output
        player.close();
    }
}