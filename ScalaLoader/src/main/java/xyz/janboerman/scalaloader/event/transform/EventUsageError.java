package xyz.janboerman.scalaloader.event.transform;

public class EventUsageError extends Error {

    public EventUsageError() {
    }

    public EventUsageError(String message) {
        super(message);
    }

    public EventUsageError(String message, Throwable cause) {
        super(message, cause);
    }

}
