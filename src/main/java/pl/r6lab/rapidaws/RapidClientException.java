package pl.r6lab.rapidaws;

public class RapidClientException extends RuntimeException {

    public RapidClientException(Throwable cause) {
        super(cause);
    }

    public RapidClientException(String message) {
        super(message);
    }

    public RapidClientException(String message, Throwable cause) {
        super(message, cause);
    }
}
