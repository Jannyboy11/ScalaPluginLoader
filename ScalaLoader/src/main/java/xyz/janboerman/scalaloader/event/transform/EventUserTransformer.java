package xyz.janboerman.scalaloader.event.transform;

import org.objectweb.asm.*;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.SimpleRemapper;

import java.util.Map;
import static xyz.janboerman.scalaloader.compat.Compat.*;

import static xyz.janboerman.scalaloader.event.transform.EventTransformations.*;

class EventUserTransformer extends ClassRemapper {

    private static final Map<String, String> MAPPINGS = mapOf(
            mapEntry(SCALALOADER_EVENT_DESCRIPTOR, BUKKIT_EVENT_DESCRIPTOR),
            mapEntry(SCALALOADER_EVENT_NAME, BUKKIT_EVENT_NAME),
            mapEntry(SCALALOADER_EVENT_CLASS, BUKKIT_EVENT_CLASS),

            mapEntry(SCALALOADER_CANCELLABLE_DESCRIPTOR, BUKKIT_CANCELLABLE_DESCRIPTOR),
            mapEntry(SCALALOADER_CANCELLABLE_NAME, BUKKIT_CANCELLABLE_NAME),
            mapEntry(SCALALOADER_CANCELLABLE_CLASS, BUKKIT_CANCELLABLE_CLASS),

            mapEntry(SCALALOADER_EVENTEXECUTOR_DESCRIPTOR, BUKKIT_EVENTEXECUTOR_DESCRIPTOR),
            mapEntry(SCALALOADER_EVENTEXECUTOR_NAME, BUKKIT_EVENTEXECUTOR_NAME),
            mapEntry(SCALALOADER_EVENTEXECUTOR_CLASS, BUKKIT_EVENTEXECUTOR_CLASS),
            mapEntry(SCALALOADER_EXECUTE_DESCRIPTOR, BUKKIT_EXECUTE_DESCRIPTOR)
    );

    EventUserTransformer(ClassVisitor delegate) {
        super(ASM_API, delegate, new SimpleRemapper(MAPPINGS));
        //no need to transform the signature of EventExecutor, because method resolution is purely descriptor-based!
        //see: https://docs.oracle.com/javase/specs/jvms/se13/html/jvms-5.html#jvms-5.4.3.3
        //it is kind of weird though to have types in the signature not match types in the descriptor,
        //but apparently that is not illegal.
    }

}

