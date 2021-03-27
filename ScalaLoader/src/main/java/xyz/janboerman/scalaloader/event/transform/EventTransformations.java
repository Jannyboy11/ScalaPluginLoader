package xyz.janboerman.scalaloader.event.transform;

import org.bukkit.event.Event;
import org.objectweb.asm.*;
import xyz.janboerman.scalaloader.bytecode.AsmConstants;

/**
 * This class is NOT part of the public API!
 */
public class EventTransformations {

    static final int ASM_API = AsmConstants.ASM_API;

    static final String BUKKIT_EVENT_DESCRIPTOR = "Lorg/bukkit/event/Event;";
    static final String BUKKIT_CANCELLABLE_DESCRIPTOR = "Lorg/bukkit/event/Cancellable;";
    static final String BUKKIT_EVENTEXECUTOR_DESCRIPTOR = "Lorg/bukkit/plugin/EventExecutor;";
    static final String BUKKIT_EVENT_NAME = "org/bukkit/event/Event";
    static final String BUKKIT_CANCELLABLE_NAME = "org/bukkit/event/Cancellable";
    static final String BUKKIT_EVENTEXECUTOR_NAME = "org/bukkit/plugin/EventExecutor";
    static final String BUKKIT_EVENT_CLASS = "org.bukkit.event.Event";
    static final String BUKKIT_CANCELLABLE_CLASS = "org.bukkit.event.Cancellable";
    static final String BUKKIT_EVENTEXECUTOR_CLASS = "org.bukkit.plugin.EventExecutor";

    static final String HANDLERLIST_DESCRIPTOR = "Lorg/bukkit/event/HandlerList;";
    static final String GETHANDLERLIST_DESCRIPTOR = "()Lorg/bukkit/event/HandlerList;";
    static final String GETHANDLERS_DESCRIPTOR = "()Lorg/bukkit/event/HandlerList;";
    static final String HANDLERLIST_NAME = "org/bukkit/event/HandlerList";
    static final String GETHANDLERLIST_METHODNAME = "getHandlerList";
    static final String GETHANDLERS_METHODNAME = "getHandlers";

    static final String SCALALOADER_EVENT_DESCRIPTOR = "Lxyz/janboerman/scalaloader/event/Event;";
    static final String SCALALOADER_CANCELLABLE_DESCRIPTOR = "Lxyz/janboerman/scalaloader/event/Cancellable;";
    static final String SCALALOADER_EVENTEXECUTOR_DESCRIPTOR = "Lxyz/janboerman/scalaloader/event/EventExecutor;";
    static final String SCALALOADER_EVENT_NAME = "xyz/janboerman/scalaloader/event/Event";
    static final String SCALALOADER_CANCELLABLE_NAME = "xyz/janboerman/scalaloader/event/Cancellable";
    static final String SCALALOADER_EVENTEXECUTOR_NAME = "xyz/janboerman/scalaloader/event/EventExecutor";
    static final String SCALALOADER_EVENT_CLASS = "xyz.janboerman.scalaloader.event.Event";
    static final String SCALALOADER_CANCELLABLE_CLASS = "xyz.janboerman.scalaloader.event.Cancellable";
    static final String SCALALOADER_EVENTEXECUTOR_CLASS = "xyz.janboerman.scalaloader.event.EventExecutor";

    static final String SETCANCELLED_NAME = "setCancelled";
    static final String ISCANCELLED_NAME = "isCancelled";
    static final String EXECUTE_NAME = "execute";
    static final String SETCANCELLED_DESCRIPTOR = "(Z)V";
    static final String ISCANCELLED_DESCRIPTOR = "()Z";
    static final String SCALALOADER_EXECUTE_DESCRIPTOR = "(Lorg/bukkit/event/Listener;Ljava/lang/Object;)V";
    static final String BUKKIT_EXECUTE_DESCRIPTOR = "(Lorg/bukkit/event/Listener;Lorg/bukkit/event/Event;)V";

    static final String FALLBACK_HANDLERLIST_FIELD_NAME = "$HANDLERS";
    static final String FALLBACK_CANCEL_FIELD_NAME = "$cancel";

    private EventTransformations() {
    }

    /**
     * <p>The following transformations are applied:</p>
     *
     * <p>
     *      1:
     *      Make the class extend org.bukkit.event.Event if the superclass is xyz.janboerman.scalaloader.event.Event.
     *      If that transformation is applied, then also the following transformations are applied:
     * </p>
     * <ul>
     *     <li>inject static field of type HandlerList (if absent)</li>
     *     <li>inject static method "static getHandlerList()" (if absent), make it public</li>
     *     <li>inject virtual method "HandlerList getHandlers()" (if absent), make it public</li>
     * </ul>
     * <p>
     *      2:
     *      If the class implements the xyz.janboerman.scalaloader.event.Cancellable interface directly, it is replaced by the org.bukkit.event.Cancellable interface
     *      and the isCancelled and setCancelled methods are added as virtual methods to the event's class.
     * </p>
     * <p>
     *      3:
     *      If a class implements xyz.janboerman.scalaloader.event.EventExecutor, it is replaced by org.bukkit.plugin.EventExecutor.
     *      The descriptor of the functional method "execute" will be be updated to accept an org.bukkit.event.Event instead.
     * </p>
     * <p>
     *      4:
     *      Every class that is using xyz.janboerman.scalaloader.event.Event or xyz.janboerman.scalaloder.event.Cancellable
     *      will be transformed to use their Bukkit counterparts instead.
     * </p>
     * <p>
     *      5:
     *      Calls to {@link xyz.janboerman.scalaloader.event.EventBus#callEvent(Object)} are replaced by calls to {@link xyz.janboerman.scalaloader.event.EventBus#callEvent(Event)}
     * </p>
     *
     *
     * @param clazz the class to be transformed
     * @param pluginClassLoader the classloader that ASM uses to compute the least upper bound for the StackMapTable
     * @return the transformed class bytes
     */
    //TODO write some tests for this method
    public static byte[] transform(byte[] clazz, ClassLoader pluginClassLoader) throws EventError {
        ScanResult eventResult = new EventScanner().scan(new ClassReader(clazz));

        ClassWriter classWriter = new ClassWriter( 0 /*ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS*/) {
            @Override
            protected ClassLoader getClassLoader() {
                return pluginClassLoader;
            }
        };

        ClassVisitor combinedTransformer = classWriter;
        if (eventResult.extendsScalaLoaderEvent)
            combinedTransformer = new EventTransformer(eventResult, combinedTransformer);
        if (eventResult.implementsScalaLoaderCancellable)
            combinedTransformer = new CancellableTransformer(eventResult, combinedTransformer);
        if (eventResult.implementsScalaLoaderEventExecutor)
            combinedTransformer = new EventExecutorTransformer(combinedTransformer);
        combinedTransformer = new EventBusUserTransformer(combinedTransformer);
        combinedTransformer = new EventUserTransformer(combinedTransformer);

        new ClassReader(clazz).accept(combinedTransformer, ClassReader.EXPAND_FRAMES);

        return classWriter.toByteArray();
    }

}
