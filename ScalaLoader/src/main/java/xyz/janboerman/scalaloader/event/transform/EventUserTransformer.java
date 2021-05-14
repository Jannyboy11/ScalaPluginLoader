package xyz.janboerman.scalaloader.event.transform;

import org.objectweb.asm.*;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.SimpleRemapper;

import java.util.HashMap;
import java.util.Map;

import static xyz.janboerman.scalaloader.event.transform.EventTransformations.*;

class EventUserTransformer extends ClassRemapper {

    private static final Map<String, String> MAPPINGS = new HashMap<String, String>() {
        {
            put(SCALALOADER_EVENT_DESCRIPTOR, BUKKIT_EVENT_DESCRIPTOR);
            put(SCALALOADER_EVENT_NAME, BUKKIT_EVENT_NAME);
            put(SCALALOADER_EVENT_CLASS, BUKKIT_EVENT_CLASS);

            put(SCALALOADER_CANCELLABLE_DESCRIPTOR, BUKKIT_CANCELLABLE_DESCRIPTOR);
            put(SCALALOADER_CANCELLABLE_NAME, BUKKIT_CANCELLABLE_NAME);
            put(SCALALOADER_CANCELLABLE_CLASS, BUKKIT_CANCELLABLE_CLASS);

            put(SCALALOADER_EVENTEXECUTOR_DESCRIPTOR, BUKKIT_EVENTEXECUTOR_DESCRIPTOR);
            put(SCALALOADER_EVENTEXECUTOR_NAME, BUKKIT_EVENTEXECUTOR_NAME);
            put(SCALALOADER_EVENTEXECUTOR_CLASS, BUKKIT_EVENTEXECUTOR_CLASS);
            put(SCALALOADER_EXECUTE_DESCRIPTOR, BUKKIT_EXECUTE_DESCRIPTOR);
        }
    }; //can use Map.ofEntries in Java 11+

    EventUserTransformer(ClassVisitor delegate) {
        super(ASM_API, delegate, new SimpleRemapper(MAPPINGS));
        //no need to transform the signature of EventExecutor, because method resolution is purely descriptor-based!
        //see: https://docs.oracle.com/javase/specs/jvms/se13/html/jvms-5.html#jvms-5.4.3.3
        //it is kind of weird though to have types in the signature not match types in the descriptor,
        //but apparently that is not illegal.
    }

}

