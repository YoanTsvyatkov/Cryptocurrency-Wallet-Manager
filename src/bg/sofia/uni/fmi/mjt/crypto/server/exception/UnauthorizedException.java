package bg.sofia.uni.fmi.mjt.crypto.server.exception;

public class UnauthorizedException extends HttpException {

    public UnauthorizedException(String message) {
        super(message);
    }
}
