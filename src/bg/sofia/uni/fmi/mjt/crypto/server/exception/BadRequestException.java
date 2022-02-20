package bg.sofia.uni.fmi.mjt.crypto.server.exception;

public class BadRequestException extends HttpException {

    public BadRequestException(String message) {
        super(message);
    }
}
