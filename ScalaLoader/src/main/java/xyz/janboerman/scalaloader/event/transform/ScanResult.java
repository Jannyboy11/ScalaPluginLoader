package xyz.janboerman.scalaloader.event.transform;

import java.util.HashSet;
import java.util.Set;
import java.util.StringJoiner;

class ScanResult {

    String className;
    boolean extendsScalaLoaderEvent;
    boolean implementsScalaLoaderCancellable;
    String staticHandlerListFieldName;
    boolean hasGetHandlers;
    boolean hasGetHandlerList;
    boolean hasValidIsCancelled;
    boolean hasValidSetCancelled;
    Set<String> primaryConstructorDescriptors = new HashSet<>();
    boolean hasClassInitializer;

    @Override
    public String toString() {
        StringJoiner stringJoiner = new StringJoiner(", " + System.lineSeparator());
        stringJoiner.add("className = " + className);
        stringJoiner.add("extends ScalaLoader Event = " + extendsScalaLoaderEvent);
        stringJoiner.add("implements ScalaLoader Cancellable = " + implementsScalaLoaderCancellable);
        stringJoiner.add("static HandlerList field name = " + staticHandlerListFieldName);
        stringJoiner.add("has getHandlers = " + hasGetHandlers);
        stringJoiner.add("has getHandlerList = " + hasGetHandlerList);
        stringJoiner.add("has isCancelled = " + hasValidIsCancelled);
        stringJoiner.add("has setCancelled = " + hasValidSetCancelled);
        stringJoiner.add("primary constructor descriptors = " + primaryConstructorDescriptors);
        stringJoiner.add("has class initializer = " + hasClassInitializer);
        return stringJoiner.toString();
    }

}
