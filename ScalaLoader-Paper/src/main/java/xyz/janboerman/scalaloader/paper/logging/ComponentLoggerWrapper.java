package xyz.janboerman.scalaloader.paper.logging;

import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import org.slf4j.spi.LoggingEventBuilder;

import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * Adapter for being able to use {@linkplain ComponentLogger} where a {@linkplain Logger} is needed.
 */
public final class ComponentLoggerWrapper extends Logger {

    private final ComponentLogger componentLogger;

    public ComponentLoggerWrapper(ComponentLogger componentLogger) {
        super(componentLogger.getName(), null);
        this.componentLogger = componentLogger;
        setLevel(Level.ALL);
    }

    @Override
    public void log(LogRecord logRecord) {
        LoggingEventBuilder builder = componentLogger
                .atLevel(adaptLevel(logRecord.getLevel()));
        builder = withMessage(builder, logRecord.getMessage());
        builder = withArguments(builder, logRecord.getParameters());
        builder = withCause(builder, logRecord.getThrown());
        builder.log();
    }

    private static org.slf4j.event.Level adaptLevel(Level level) {
        return org.slf4j.event.Level.intToLevel(level.intValue());
    }

    private static LoggingEventBuilder withMessage(LoggingEventBuilder eventBuilder, String message) {
        return eventBuilder.setMessage(message);
    }

    private static LoggingEventBuilder withArguments(LoggingEventBuilder eventBuilder, Object... arguments) {
        if (arguments != null) {
            for (Object argument : arguments) {
                eventBuilder = eventBuilder.addArgument(argument);
            }
        }
        return eventBuilder;
    }

    private static LoggingEventBuilder withCause(LoggingEventBuilder eventBuilder, Throwable cause) {
        return cause == null ? eventBuilder : eventBuilder.setCause(cause);
    }
}
