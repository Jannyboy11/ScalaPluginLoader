package xyz.janboerman.scalaloader.bytecode;

import org.objectweb.asm.ClassVisitor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * This class is NOT part of the public API!
 */
public class TransformerRegistry {

    public final List<BiFunction<ClassVisitor/*old*/, String/*mainClassName*/, ClassVisitor/*new*/>> mainClassTransformers = new ArrayList<>();
    public final Map<String/*className*/, List<Function<ClassVisitor/*old*/, ClassVisitor/*new*/>>> byClassTransformers = new HashMap<>();
    public final List<Function<ClassVisitor/*old*/, ClassVisitor/*new*/>> unspecificTransformers = new ArrayList<>(0);

    public TransformerRegistry() {
    }

    public void addMainClassTransformer(BiFunction<ClassVisitor, String, ClassVisitor> function) {
        mainClassTransformers.add(function);
    }

    public void addTargetedClassTransformer(String targetClassName, Function<ClassVisitor, ClassVisitor> function) {
        byClassTransformers.computeIfAbsent(targetClassName, k -> new ArrayList<>()).add(function);
    }

    public void addUnspecificTransformer(Function<ClassVisitor, ClassVisitor> function) {
        unspecificTransformers.add(function);
    }

}
