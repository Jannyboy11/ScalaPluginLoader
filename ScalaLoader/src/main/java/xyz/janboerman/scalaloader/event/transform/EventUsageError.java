package xyz.janboerman.scalaloader.event.transform;

/**
 * <p>Thrown when an event was used invalidly</p>
 * <p>Example cases where this can happen:</p>
 * <ul>
 *     <li>A JavaPlugin defined an event that extends {@link xyz.janboerman.scalaloader.event.Event}</li>
 *     <li>An even that implements {@link xyz.janboerman.scalaloader.event.Cancellable} but only overrides one of its methods</li>
 * </ul>
 */
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
