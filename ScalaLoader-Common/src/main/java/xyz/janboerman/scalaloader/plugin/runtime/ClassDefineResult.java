package xyz.janboerman.scalaloader.plugin.runtime;

import java.util.Objects;

public interface ClassDefineResult /*permits NewClass, OldClass*/ {

    public Class<?> getClassDefinition();

    public boolean isNew();

    public static ClassDefineResult newClass(Class<?> clazz) {
        return new NewClass(clazz);
    }

    public static ClassDefineResult oldClass(Class<?> clazz) {
        return new OldClass(clazz);
    }
}

class NewClass implements ClassDefineResult {
    private final Class<?> newlyGeneratedClass;

    NewClass(Class<?> newlyGeneratedClass) {
        this.newlyGeneratedClass = newlyGeneratedClass;
    }

    @Override
    public Class<?> getClassDefinition() {
        return newlyGeneratedClass;
    }

    @Override
    public boolean isNew() {
        return true;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(newlyGeneratedClass);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (!(obj instanceof NewClass)) return false;

        NewClass that = (NewClass) obj;
        return Objects.equals(this.newlyGeneratedClass, that.newlyGeneratedClass);
    }

    @Override
    public String toString() {
        return "NewClass(" + newlyGeneratedClass + ")";
    }
}

class OldClass implements ClassDefineResult {
    private final Class<?> existingClass;

    OldClass(Class<?> existingClass) {
        this.existingClass = existingClass;
    }

    @Override
    public Class<?> getClassDefinition() {
        return existingClass;
    }

    @Override
    public boolean isNew() {
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(existingClass);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (!(obj instanceof OldClass)) return false;

        OldClass that = (OldClass) obj;
        return Objects.equals(this.existingClass, that.existingClass);
    }

    @Override
    public String toString() {
        return "OldClass(" + existingClass + ")";
    }
}
