package bg.sofia.uni.fmi.mjt.crypto.server.exception;

public class UserAlreadyExistException extends Exception {
    public UserAlreadyExistException(String message) {
        super(message);
    }
}
