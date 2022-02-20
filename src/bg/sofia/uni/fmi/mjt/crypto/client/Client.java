package bg.sofia.uni.fmi.mjt.crypto.client;

import bg.sofia.uni.fmi.mjt.crypto.server.util.ErrorLoggerUtil;

import java.io.IOException;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Scanner;

public class Client {
    private static final int SERVER_PORT = 7777;
    private static final String HOST = "localhost";
    private static final int BUFFER_SIZE = 8192;
    private static ByteBuffer buffer = ByteBuffer.allocateDirect(BUFFER_SIZE);

    public static void main(String[] args) {
        try (SocketChannel socketChannel = SocketChannel.open();
             Scanner scanner = new Scanner(System.in)) {
            socketChannel.connect(new InetSocketAddress(HOST, SERVER_PORT));

            while (true) {
                String message = scanner.nextLine(); // read a line from the console

                if ("quit".equals(message)) {
                    break;
                }

                if (message != null && !message.isEmpty()) {
                    writeMessageToChannel(message, socketChannel);
                }

                String reply = getReplay(socketChannel); // buffer drain
                System.out.println(reply);
            }
        } catch (ConnectException e) {
            System.out.println("Something went wrong on out end. " +
                "Try again later or contact administrator by providing the logs in resources/errors.txt");
            ErrorLoggerUtil.logException(e);
        } catch (IOException e) {
            System.out.println("There is a problem with the network communication");
            e.printStackTrace();
            ErrorLoggerUtil.logException(e);
        }
    }

    private static String getReplay(SocketChannel socketChannel) throws IOException {
        buffer.clear(); // switch to writing mode
        socketChannel.read(buffer); // buffer fill
        buffer.flip(); // switch to reading mode

        byte[] byteArray = new byte[buffer.remaining()];
        buffer.get(byteArray);
        String reply = new String(byteArray, "UTF-8"); // buffer drain
        return reply;
    }

    private static void writeMessageToChannel(String message, SocketChannel socketChannel) throws IOException {
        buffer.clear(); // switch to writing mode
        buffer.put(message.getBytes()); // buffer fill
        buffer.flip(); // switch to reading mode
        socketChannel.write(buffer); // buffer drain
    }
}
