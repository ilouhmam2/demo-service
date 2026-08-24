package fr.francetv.demo.domain.exception;

public class ExternalServiceException extends RuntimeException {

    public enum Kind {
        BAD_GATEWAY,
        SERVICE_UNAVAILABLE
    }

    private final Kind kind;

    public ExternalServiceException(Kind kind, String message) {
        super(message);
        this.kind = kind;
    }

    public ExternalServiceException(Kind kind, String message, Throwable cause) {
        super(message, cause);
        this.kind = kind;
    }

    public Kind getKind() {
        return kind;
    }
}