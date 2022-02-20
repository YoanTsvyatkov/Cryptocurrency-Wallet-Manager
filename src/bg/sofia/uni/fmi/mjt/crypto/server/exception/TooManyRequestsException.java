package bg.sofia.uni.fmi.mjt.crypto.server.exception;

public class TooManyRequestsException extends HttpException {
    public TooManyRequestsException(String message) {
        super(message);
    }
}
