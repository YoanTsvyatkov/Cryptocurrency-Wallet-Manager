package bg.sofia.uni.fmi.mjt.crypto.server.util;

public class ApiResponse<T> {
    private T data;
    private int statusCode;
    private String message;

    public ApiResponse(T data, int statusCode, String message) {
        this.data = data;
        this.statusCode = statusCode;
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getMessage() {
        return message;
    }
}
