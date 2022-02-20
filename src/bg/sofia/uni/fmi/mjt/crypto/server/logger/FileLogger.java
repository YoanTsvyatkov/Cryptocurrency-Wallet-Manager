package bg.sofia.uni.fmi.mjt.crypto.server.logger;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Writer;

public class FileLogger implements Logger {
    private Writer writer;

    public FileLogger(Writer writer) {
        this.writer = writer;
    }

    @Override
    public void logMessage(String message) {
        try (var bufferedWriter = new BufferedWriter((writer))) {
            bufferedWriter.write(message);
        } catch (IOException exception) {
            System.out.println("Something went wrong...");
        }
    }
}
