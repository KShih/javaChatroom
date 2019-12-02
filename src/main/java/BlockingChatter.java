import java.io.*;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;

public class BlockingChatter implements Chatter {

    private final InputStream chatServerInput;
    private final OutputStream chatServerOutput;
    private final InputStream userInput;
    private static final String FILE_PATH = "src/main/resources/Moby Dick.txt";
    private static final int MAX_SIZE = 1024;

    public BlockingChatter(InputStream chatServerInput, OutputStream chatServerOutput, InputStream userInput) {
        this.chatServerInput = chatServerInput;
        this.chatServerOutput = chatServerOutput;
        this.userInput = userInput;
    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            write();
            read();
        }
    }

    public void write(){
        try {
            BufferedInputStream stream = new BufferedInputStream(userInput);
            int availableAmount = stream.available();
            byte[] into = new byte[availableAmount];
            int readAmount = stream.read(into, 0, into.length);
            if (readAmount == -1){
                System.out.printf("Stream is closed.%n");
            }
            if (readAmount != 0){
                chatServerOutput.write(into);
                chatServerOutput.flush();
            }
        } catch (IOException ioe) {
            System.out.printf("Failed to read from userInput - %s%n", ioe.getMessage());
        }
    }

    public void read(){
        try {
            BufferedInputStream stream = new BufferedInputStream(chatServerInput);
            int availableAmount = stream.available();
            byte[] into = new byte[availableAmount];

            int readAmount = stream.read(into, 0, into.length);
            if (readAmount == -1){
                System.out.printf("Stream is closed.%n");
            }

            if (readAmount != 0){
                String readString = new String(into);

                if (Pattern.matches("\\[.*?] java\n", readString)){

                    byte[] byteFromBook = readFromBook();

                    if (byteFromBook != null) {
                        System.out.printf("Read 'java' from client, sent random text to server.%n");
                        chatServerOutput.write(byteFromBook);
                        chatServerOutput.flush();
                    }
                    else{
                        System.out.printf("readString is null.%n");
                    }

                }
                else{
                    System.out.printf("%s%n", readString);
                }
            }

        } catch (IOException ioe) {
            System.out.printf("Failed to read from chatServerInput- %s%n", ioe.getMessage());
        }
    }

    private byte[] readFromBook(){
        FileInputStream fileStream = null;
        try {
            fileStream = new FileInputStream(FILE_PATH);

            int availableAmount = fileStream.available();
            byte[] into = new byte[availableAmount];

            int readAmount = fileStream.read(into, 0, into.length);
            if (readAmount == -1){
                System.out.printf("Stream is closed.%n");
                return null;
            }

            Random rand = new Random();
            int randInt1 = rand.nextInt(availableAmount);
            int randInt2 = ThreadLocalRandom.current().nextInt(randInt1, Integer.min(randInt1+MAX_SIZE, availableAmount));

            return Arrays.copyOfRange(into, randInt1, randInt2);

        } catch (IOException ioe){
            System.out.printf("Failed to read - %s%n", ioe.getMessage());
            return null;

        } finally {
            if (fileStream != null){
                try{
                    fileStream.close();
                }catch (IOException ioe2){
                    System.out.printf("Failed to close - %s%n", ioe2.getMessage());
                }
            }
        }
    }

}
