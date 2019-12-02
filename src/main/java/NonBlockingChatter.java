import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.*;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.util.*;

public class NonBlockingChatter implements Chatter {

    private final SocketChannel chatServerChannel;

    private final Pipe.SourceChannel userInput;

    private static final Charset UTF8 = Charset.forName("UTF-8");

    private static final int MAX_SIZE = 1024;

    private ByteBuffer writeBuffer = ByteBuffer.allocate(MAX_SIZE);

    private ByteBuffer readBuffer = ByteBuffer.allocate(MAX_SIZE);

    private final Selector selector;

    public NonBlockingChatter(SocketChannel chatServerChannel,
                              Pipe.SourceChannel userInput) throws IOException{
        this.chatServerChannel = chatServerChannel;
        this.userInput = userInput;
        this.selector = Selector.open();
        chatServerChannel.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE);
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
        if (readyChannels == 0){
            return;
        }

        Set<SelectionKey> selectionKeys = selector.selectedKeys();
        Iterator<SelectionKey> keyIterator = selectionKeys.iterator();

        while (keyIterator.hasNext()){
            SelectionKey key = keyIterator.next();
            SelectableChannel channel = key.channel();

            if (key.isReadable()) {
                // Read from server

                readBuffer.clear();

                int result = chatServerChannel.read(readBuffer);

                if (result == -1){
                    key.cancel();
                    continue;
                }

                readBuffer.flip();

                CharsetDecoder decoder = UTF8.newDecoder();
                CharBuffer charBuffer = decoder.decode(readBuffer);
                System.out.printf("%s%n", charBuffer.toString());

                readBuffer.clear();

            } else if (key.isWritable()) {
                // Write to channel

                writeBuffer.clear();

                int result = userInput.read(writeBuffer);

                if (result == -1){
                    key.cancel();
                    continue;
                }

                writeBuffer.flip();

                chatServerChannel.write(writeBuffer);

                writeBuffer.clear();

            }
            keyIterator.remove();
        }

    }

}
