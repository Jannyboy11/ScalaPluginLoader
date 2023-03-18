package xyz.janboerman.scalaloader.event.transform;

/**
 * <p>Thrown when an event was used invalidly</p>
 * <p>Example cases where this can happen:</p>
 * <ul>
 *     <li>A JavaPlugin defined an event that extends {@link xyz.janboerman.scalaloader.event.Event}</li>
 *     <li>A JavaPlugin defined an event callback that implements {@link xyz.janboerman.scalaloader.event.EventExecutor}</li>
 *     <li>A JavaPlugin defined an event that implements {@link xyz.janboerman.scalaloader.event.Cancellable}</li>
 *     <li>A ScalaPlugin-defined event that implements {@link xyz.janboerman.scalaloader.event.Cancellable} but only overrides one of its methods</li>
 * </ul>
 */
public class EventError extends Error {

    public EventError() {
    }

    public EventError(String message) {
        super(message);
    }

    public EventError(String message, Throwable cause) {
        super(message, cause);
    }

}
