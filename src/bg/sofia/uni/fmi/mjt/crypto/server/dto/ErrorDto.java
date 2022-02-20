package bg.sofia.uni.fmi.mjt.crypto.server.dto;

public class ErrorDto {
    private String error;

    public ErrorDto(String error) {
        this.error = error;
    }

    public String getMessage() {
        return error;
    }

    public void setMessage(String error) {
        this.error = error;
    }
}
