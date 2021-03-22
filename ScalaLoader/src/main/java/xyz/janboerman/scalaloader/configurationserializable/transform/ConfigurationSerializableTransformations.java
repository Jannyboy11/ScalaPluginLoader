package xyz.janboerman.scalaloader.configurationserializable.transform;

import org.bukkit.configuration.serialization.SerializableAs;
import org.objectweb.asm.*;

import xyz.janboerman.scalaloader.bytecode.AsmConstants;
import xyz.janboerman.scalaloader.configurationserializable.*;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;

/**
 * This class is NOT part of the public API!
 */
public class ConfigurationSerializableTransformations {

    static final int ASM_API = AsmConstants.ASM_API;

    static final String BUKKIT_CONFIGURATIONSERIALIZABLE_NAME = Type.getInternalName(org.bukkit.configuration.serialization.ConfigurationSerializable.class);
    static final String BUKKIT_SERIALIZABLEAS_DESCRIPTOR = Type.getDescriptor(SerializableAs.class);

    static final String SERIALIZE_NAME = "serialize";
    static final String SERIALIZE_DESCRIPTOR = "()Ljava/util/Map;";
    static final String SERIALIZE_SIGNATURE = "()Ljava/util/Map<Ljava/lang/String;Ljava/lang/Object;>;";
    static final String DESERIALIZE_NAME = "deserialize";
    static final String VALUEOF_NAME = "valueOf";
    static final String CONSTRUCTOR_NAME = "<init>";
    static final String CLASS_INIT_NAME = "<clinit>";
    static final String REGISTER_NAME = "$registerWithConfigurationSerialization";
    static final String REGISTER_DESCRIPTOR = "()V";
    static final String DESERIALIZATION_CONSTRUCTOR_DESCRIPTOR = deserializationDescriptor("V");
    static final String DESERIALIZATION_CONSTRUCTOR_SIGNATURE = deserializationSignature("V");
    static final String SCAN_NAME = "scan";
    static final String CONSTRUCTUSING_NAME = "constructUsing";
    static final String REGISTERAT_NAME = "registerAt";
    static final String ADAPT_NAME = "adapt";
    static final String AS_NAME = "as";
    static final String VARIANT_NAME = "$variant";

    static final String SCALALOADER_CONFIGURATIONSERIALIZABLE_DESCRIPTOR = Type.getDescriptor(ConfigurationSerializable.class);
    static final String SCALALOADER_DELEGATESERIALIZATION_DESCRIPTOR = Type.getDescriptor(DelegateSerialization.class);
    static final String SCALALOADER_SCAN_DESCRIPTOR = Type.getDescriptor(Scan.class);
    static final String SCALALAODER_SCANTYPE_DESCRIPTOR = Type.getDescriptor(Scan.Type.class);
    static final String SCALALOADER_PROPERTYINCLUDE_DESCRIPTOR = Type.getDescriptor(Scan.IncludeProperty.class);
    static final String SCALALOADER_PROPERTYEXCLUDE_DESCRIPTOR = Type.getDescriptor(Scan.ExcludeProperty.class);
    static final String SCALALOADER_INJECTIONPOINT_DESCRIPTOR = Type.getDescriptor(InjectionPoint.class);
    static final String SCALALOADER_DESERIALIZATIONMETHOD_DESCRIPTOR = Type.getDescriptor(DeserializationMethod.class);

    static final String MAP_NAME = "java/util/Map";
    static final String MAP_DESCRIPTOR = "Ljava/util/Map;";
    static final String MAP_SIGNATURE = "Ljava/util/Map<Ljava/lang/String;Ljava/lang/Object;>;";
    static final String MAP_PUT_NAME = "put";
    static final String MAP_PUT_DESCRIPTOR = "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;";
    static final String MAP_GET_NAME = "get";
    static final String MAP_GET_DESCRIPTOR = "(Ljava/lang/Object;)Ljava/lang/Object;";
    static final String MAP_PUTALL_NAME = "putAll";
    static final String MAP_PUTALL_DESCRIPTOR = "(Ljava/util/Map;)V";
    static final String HASHMAP_NAME = "java/util/HashMap";
    static final String HASHMAP_DESCRIPTOR = "Ljava/util/HashMap;";

    static final String OPTION_NAME = "scala/Option";
    static final String OPTION_DESCRIPTOR = "Lscala/Option;";

    static final Type MAP_TYPE = Type.getType(Map.class);
    static final Type HASHMAP_TYPE = Type.getType(HashMap.class);
    static final Type STRING_TYPE = Type.getType(String.class);
    static final Type OBJECT_TYPE = Type.getType(Object.class);
    static final Type BOOLEAN_TYPE = Type.BOOLEAN_TYPE;
    static final Type OPTION_TYPE = Type.getType(OPTION_DESCRIPTOR);
    static final Type LIST_TYPE = Type.getType(List.class);
    static final Type ARRAYLIST_TYPE = Type.getType(ArrayList.class);
    static final Type SET_TYPE = Type.getType(Set.class);
    static final Type BIGINTEGER_TYPE = Type.getType(BigInteger.class);
    static final Type BIGDECIMAL_TYPE = Type.getType(BigDecimal.class);
    static final Type UUID_TYPE = Type.getType(UUID.class);

    static final Type Byte_TYPE = Type.getType(Byte.class);
    static final Type Short_TYPE = Type.getType(Short.class);
    static final Type Integer_TYPE = Type.getType(Integer.class);
    static final Type Long_TYPE = Type.getType(Long.class);
    static final Type Float_TYPE = Type.getType(Float.class);
    static final Type Double_TYPE = Type.getType(Double.class);
    static final Type Boolean_TYPE = Type.getType(Boolean.class);
    static final Type Character_TYPE = Type.getType(Character.class);
    static final Type Void_TYPE = Type.getType(Void.class);

    static String deserializationDescriptor(String returnTypeDescriptor) {
        return "(Ljava/util/Map;)" + returnTypeDescriptor;
    }

    static String deserializationSignature(String returnTypeDescriptor) {
        return "(Ljava/util/Map<Ljava/lang/String;Ljava/lang/Object;>;)" + returnTypeDescriptor;
    }


    //TODO Ideally I want to register some types from the scala standard library. e.g:
    //TODO RichInt, RichFloat, RichDouble, RichLong, RichByte (take Boxing into account in (de)serialization methods!)
    //TODO Option, Some, None, Either, Left, Right, and also Try, Success, Fail?
    //TODO List, Seq, Set, Map
    //TODO mutable collections?
    //TODO what about Tuples and Unit?
    //TODO what about Null and Nothing? Null should! be registered, Nothing should not because it has no inhabitants!
    //TODO and Ideally, these types are only registered ONCE per scala version!

    //TODO I can achieve this by transforming the classes from the scala standard lib themselves and add ConfigurationSerializable as an interface
    //TODO one other possibility is to generate wrapper classes!
    //TODO but the most portable solution would be to generate the wrapping-and unwrapping code at the use-site.

    //TODO should these scala standard library classes be registered using an alias that includes the scala version string (not minor version)! (in order to disambiguate!)
    //TODO yes I think so. I need to test if there is a problem when a plugin upgrades its scala version and tries to deserialize a serialized object that used the older scala library.
    //TODO This is going to cause problems for sure, but registering them without an alias could cause multiple versions of the 'same' class to get registered.
    //TODO actually, *maybe* the best solution to do this is to *not* do it so that I won't run into these extra problems! (like java modules don't have support for versions!)
    //TODO but not registering scala stdlib classes and generating the transformation bytecode as use-site would still work, so I'm going with that solution when the time comes!

    //TODO also do not forget about 'immutable' java types such as BigDecimal, BigInteger, UUID TODO and maybe enums?
    //TODO I don't think I can detect whether a class is an enum without loading it if it is not in the same jar file

    private ConfigurationSerializableTransformations() {}


    public static byte[] transform(byte[] clazz, ClassLoader pluginClassLoader) throws ConfigurationSerializableError {
        LocalScanResult localResult = new LocalScanner().scan(new ClassReader(clazz));

        ClassWriter classWriter = new ClassWriter(0) {
            @Override
            protected ClassLoader getClassLoader() {
                return pluginClassLoader;
            }
        };

        ClassVisitor combinedTransformer = classWriter;
        combinedTransformer = new DelegateTransformer(combinedTransformer, localResult);
        combinedTransformer = new SerializableTransformer(combinedTransformer, localResult, pluginClassLoader);

        new ClassReader(clazz).accept(combinedTransformer, 0);

        return classWriter.toByteArray();
    }

}
