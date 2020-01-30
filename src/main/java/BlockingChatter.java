package edu.nyu.cs9053.homework11;

import java.io.*;
import java.nio.CharBuffer;
import java.nio.charset.CharsetDecoder;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;

/**
 * User: blangel
 * Date: 11/23/14
 * Time: 4:31 PM
 */
public class BlockingChatter implements Chatter {

    private final InputStream chatServerInput;
    private final OutputStream chatServerOutput;
    private final InputStream userInput;
    private static final String FILE_PATH = "src/main/resources/Moby Dick.txt";
    private static final int MAX_SIZE = 1024;
    private byte[] byteFromBook;
    private final Semaphore semaphore;

    public BlockingChatter(InputStream chatServerInput, OutputStream chatServerOutput, InputStream userInput) {
        this.chatServerInput = chatServerInput;
        this.chatServerOutput = chatServerOutput;
        this.userInput = userInput;
        this.byteFromBook = readFromBook();
        this.semaphore = new Semaphore(1);
    }

    @Override
    public void run() {
        Thread readThread = new Thread(new Runnable() {
            @Override
            public void run() {
                read();
            }
        });

        Thread writeThread = new Thread(new Runnable() {
            @Override
            public void run() {
                write();
            }
        });

        readThread.start();
        writeThread.start();
    }

    public void write(){
        while (!Thread.currentThread().isInterrupted()){
            try {
                BufferedInputStream stream = new BufferedInputStream(userInput);
                int availableAmount = stream.available();
                byte[] into = new byte[availableAmount];
                int readAmount = stream.read(into, 0, into.length);
                if (readAmount == -1){
                    System.out.printf("Stream is closed.%n");
                }
                try {
                    semaphore.acquire();
                } catch(InterruptedException ie){
                    Thread.currentThread().interrupt();
                    throw new RuntimeException(ie); //
                }
                try {
                    // ------------ Block this region -------------- //
                    // because chatServerOutput might be written by write() at the same time
                    if (readAmount != 0){
                        chatServerOutput.write(into);
                        chatServerOutput.flush();
                    }
                    // -------------------------------------------- //
                } finally {
                    semaphore.release();
                }

            } catch (IOException ioe) {
                System.out.printf("Failed to read from userInput - %s%n", ioe.getMessage());
            }
        }
    }

    public void read(){
        while (!Thread.currentThread().isInterrupted()){
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
                        try {
                            semaphore.acquire();
                        } catch(InterruptedException ie){
                            Thread.currentThread().interrupt();
                            throw new RuntimeException(ie); //
                        }
                        try{
                            if (byteFromBook != null) {
                                // ------------ Block this region -------------- //
                                // because chatServerOutput might be written by write() at the same time

                                System.out.printf("Read 'java' from client, sent random text to server.%n");
                                chatServerOutput.write(byteFromBook);
                                chatServerOutput.flush();

                                // --------------------------------------------- //
                            }
                            else{
                                System.out.printf("readString is null.%n");
                            }
                        } finally {
                            semaphore.release();
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
