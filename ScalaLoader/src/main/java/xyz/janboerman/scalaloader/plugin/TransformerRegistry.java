package xyz.janboerman.scalaloader.plugin;

import org.objectweb.asm.ClassVisitor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

public class TransformerRegistry {

    Map<String/*className*/, List<BiFunction<ClassVisitor/*old*/, String/*mainClassName*/, ClassVisitor/*new*/>>> byClassTransformers = new HashMap<>();
    List<BiFunction<ClassVisitor/*old*/, String/*mainClassName*/, ClassVisitor/*new*/>> mainClassTransformers = new ArrayList<>();

    TransformerRegistry() {
    }

    public void addMainClassTransformer(BiFunction<ClassVisitor, String , ClassVisitor> function) {
        this.mainClassTransformers.add(function);
    }

    public void addClassTransformer(String targetClassName, BiFunction<ClassVisitor, String, ClassVisitor> function) {
        byClassTransformers.computeIfAbsent(targetClassName, k -> new ArrayList<>())
                .add(function);
    }

}
