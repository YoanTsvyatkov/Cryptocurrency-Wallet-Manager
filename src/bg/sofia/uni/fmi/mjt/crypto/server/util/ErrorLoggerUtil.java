package bg.sofia.uni.fmi.mjt.crypto.server.util;

import bg.sofia.uni.fmi.mjt.crypto.server.logger.FileLogger;
import bg.sofia.uni.fmi.mjt.crypto.server.logger.Logger;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

public class ErrorLoggerUtil {
    public static void logException(Exception e) {
        try (var fileWriter = new FileWriter(FileConstants.ERRORS_FILE)) {
            Logger logger = new FileLogger(fileWriter);
            logger.logMessage(String.format("%s\n%s\n", e.getMessage(), getStacktraceOfException(e)));
        } catch (IOException exception) {
            System.out.println("Something went wrong on out end. ");
        }
    }

    private static String getStacktraceOfException(Exception e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);

        return sw.toString();
    }
}
