package bg.sofia.uni.fmi.mjt.crypto.server.exception;

public class ForbiddenException extends HttpException {

    public ForbiddenException(String message) {
        super(message);
    }
}
