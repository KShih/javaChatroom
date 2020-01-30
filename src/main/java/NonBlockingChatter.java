package edu.nyu.cs9053.homework11;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.*;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * User: blangel
 * Date: 11/23/14
 * Time: 4:32 PM
 */
public class NonBlockingChatter implements Chatter {

    private final SocketChannel chatServerChannel;

    private final Pipe.SourceChannel userInput;

    private static final Charset UTF8 = Charset.forName("UTF-8");

    private static final int MAX_SIZE = 1024;

    private final ByteBuffer writeBuffer;

    private final ByteBuffer readBuffer;

    private final Selector selector;

    public NonBlockingChatter(SocketChannel chatServerChannel,
                              Pipe.SourceChannel userInput) throws IOException{
        this.chatServerChannel = chatServerChannel;
        this.userInput = userInput;
        this.selector = Selector.open();
        // binding the selector to two channel
        this.userInput.register(this.selector, SelectionKey.OP_READ);
        this.chatServerChannel.register(this.selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE);

        writeBuffer = ByteBuffer.allocate(MAX_SIZE);
        readBuffer = ByteBuffer.allocate(MAX_SIZE);
    }

    @Override public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                process();
            } catch (IOException ioe) {
                System.out.printf("Exception - %s%n", ioe.getMessage());
                Thread.currentThread().interrupt();
            }
        }
    }

    private void process() throws IOException{
        int readyChannels = selector.select();
        if (readyChannels < 0){
            return;
        }

        Set<SelectionKey> selectionKeys = selector.selectedKeys(); // get the set of availible key
        Iterator<SelectionKey> keyIterator = selectionKeys.iterator();

        while (keyIterator.hasNext()){
            SelectionKey key = keyIterator.next();
            try {
                if (key.isReadable()) {
                    // Read from server

                    // check this channel is userInput or chatServerChannel
                    if (userInput.equals(key.channel())) { // read from user input and put into writeBuffer

                        writeBuffer.clear();

                        userInput.read(writeBuffer);

                        writeBuffer.flip();

                        readBuffer.put(writeBuffer); // write into readBuffer, and then forward to Writable part to write into socketChannel

                    } else if (chatServerChannel.equals(key.channel())) {

                        SocketChannel socketChannel = (SocketChannel) key.channel();

                        writeBuffer.clear();

                        socketChannel.read(writeBuffer);

                        writeBuffer.flip();

                        CharsetDecoder decoder = UTF8.newDecoder();
                        CharBuffer charBuffer = decoder.decode(writeBuffer);
                        System.out.printf("%s%n", charBuffer.toString());
                    }


                } else if (key.isWritable()) {
                    // Write to channel

                    SocketChannel socketChannel = (SocketChannel) key.channel();

                    readBuffer.flip();

                    socketChannel.write(readBuffer);

                    readBuffer.clear();

                }
            }
            finally{
                keyIterator.remove();
            }

        }

    }

}
