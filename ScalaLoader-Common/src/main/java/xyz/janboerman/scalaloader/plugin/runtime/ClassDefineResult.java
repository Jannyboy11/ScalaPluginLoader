package xyz.janboerman.scalaloader.plugin.runtime;

import java.util.Objects;

/**
 * Represents a result of defining a class using an {@link xyz.janboerman.scalaloader.compat.IScalaPluginClassLoader}.
 */
public interface ClassDefineResult /*permits NewClass, OldClass*/ {

    /**
     * Get the Class that was defined.
     * @return the class
     */
    public Class<?> getClassDefinition();

    /**
     * Whether the class was newly-defined.
     * @return true if the class is new, false if it was old
     */
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
