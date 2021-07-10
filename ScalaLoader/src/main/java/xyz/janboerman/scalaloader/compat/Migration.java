package xyz.janboerman.scalaloader.compat;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.SimpleRemapper;
import xyz.janboerman.scalaloader.bytecode.AsmConstants;
import static xyz.janboerman.scalaloader.compat.Compat.*;

/**
 * This class is NOT part of the public API!
 */
public class Migration {

    private Migration() {}

    public static byte[] transform(byte[] byteCode, ClassLoader pluginClassLoader) {
        ClassWriter classWriter = new ClassWriter(0) {
            @Override
            protected ClassLoader getClassLoader() {
                return pluginClassLoader;
            }
        };

        ClassVisitor combinedTransformer = classWriter;

        combinedTransformer = new NumericRangeUpdater(combinedTransformer);
        //there is room for more here.

        new ClassReader(byteCode).accept(combinedTransformer, ClassReader.EXPAND_FRAMES);
        return classWriter.toByteArray();
    }

}

class NumericRangeUpdater extends ClassRemapper {

    private static final NumericRangeRemapper NUMERIC_RANGE_MIGRATION = new NumericRangeRemapper();

    NumericRangeUpdater(ClassVisitor delegate) {
        super(AsmConstants.ASM_API, delegate, NUMERIC_RANGE_MIGRATION);
    }

    private static final class NumericRangeRemapper extends SimpleRemapper {

        private static final String CONFIGURATION_SERIALIZABLE_RUNTIME_PACKAGE = "xyz/janboerman/scalaloader/configurationserializable/runtime";
        private static final String TYPES_PACKAGE = CONFIGURATION_SERIALIZABLE_RUNTIME_PACKAGE + "/types";

        private static final String OLD_NUMERIC_RANGE = CONFIGURATION_SERIALIZABLE_RUNTIME_PACKAGE + "/NumericRange";
        private static final String NEW_NUMERIC_RANGE = TYPES_PACKAGE + "/NumericRange";
        private static final String OLD_BYTE_RANGE = OLD_NUMERIC_RANGE + "$OfByte";
        private static final String NEW_BYTE_RANGE = NEW_NUMERIC_RANGE + "$OfByte";
        private static final String OLD_SHORT_RANGE = OLD_NUMERIC_RANGE + "$OfShort";
        private static final String NEW_SHORT_RANGE = NEW_NUMERIC_RANGE + "$OfShort";
        private static final String OLD_INT_RANGE = OLD_NUMERIC_RANGE + "$OfInteger";
        private static final String NEW_INT_RANGE = NEW_NUMERIC_RANGE + "$OfInteger";
        private static final String OLD_LONG_RANGE = OLD_NUMERIC_RANGE + "$OfLong";
        private static final String NEW_LONG_RANGE = NEW_NUMERIC_RANGE + "$OfLong";
        private static final String OLD_BIG_INTEGER_RANGE = OLD_NUMERIC_RANGE + "$OfBigInteger";
        private static final String NEW_BIG_INTEGER_RANGE = NEW_NUMERIC_RANGE + "$OfBigInteger";

        private NumericRangeRemapper() {
            super(mapOf(
                    mapEntry(OLD_NUMERIC_RANGE, NEW_NUMERIC_RANGE),
                    mapEntry(OLD_BYTE_RANGE, NEW_BYTE_RANGE),
                    mapEntry(OLD_SHORT_RANGE, NEW_SHORT_RANGE),
                    mapEntry(OLD_INT_RANGE, NEW_INT_RANGE),
                    mapEntry(OLD_LONG_RANGE, NEW_LONG_RANGE),
                    mapEntry(OLD_BIG_INTEGER_RANGE, NEW_BIG_INTEGER_RANGE)
            ));
        }
    }

}
