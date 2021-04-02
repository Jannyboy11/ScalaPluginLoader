package xyz.janboerman.scalaloader.configurationserializable.runtime;

import org.bukkit.configuration.serialization.ConfigurationSerializable;
import xyz.janboerman.scalaloader.bytecode.Called;
import xyz.janboerman.scalaloader.configurationserializable.transform.ConfigurationSerializableError;
import xyz.janboerman.scalaloader.plugin.ScalaPluginClassLoader;
import xyz.janboerman.scalaloader.util.Maybe;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.logging.Logger;

/**
 * <p>
 *     This class servers as a fallback for the configuration serialization framework.
 *     Generated serialize and deserializion methods may emit calls to {@link #serialize(Object, ParameterType, ScalaPluginClassLoader)}
 *     and {@link #deserialize(Object, ParameterType, ScalaPluginClassLoader)} if at classload-time it could not be established how an object should be (de)serialized.
 * </p>
 * <p>
 *     To adjust the runtime serialization and deserialization behaviour, you can register a {@link Codec}
 * </p>
 *
 * @see #registerCodec(ScalaPluginClassLoader, Class, Codec)
 * @see xyz.janboerman.scalaloader.configurationserializable.ConfigurationSerializable
 * @see xyz.janboerman.scalaloader.configurationserializable.DelegateSerialization
 * @see Codec
 */
public class RuntimeConversions {

    private static final Map<ScalaPluginClassLoader, Registrations> registrations = new HashMap<>();

    private RuntimeConversions() {
    }

    /**
     * Register a codec that will be used by {@link #serialize(Object, ParameterType, ScalaPluginClassLoader)} and {@link #deserialize(Object, ParameterType, ScalaPluginClassLoader)}.
     * @param pluginClassLoader the classloader of your ScalaPlugin
     * @param type the live type of the objects the codec should work with
     * @param codec the serializer and deserializer
     * @return true if the codec was registered, otherwise false
     */
    public static boolean registerCodec(ScalaPluginClassLoader pluginClassLoader, ParameterType type, Codec<?, ?> codec) {
        Objects.requireNonNull(pluginClassLoader, "plugin classloader cannot be null!");
        return registrations.computeIfAbsent(pluginClassLoader, Registrations::new).register(type, codec);
    }

    /**
     * Register a codec that will be used by {@link #serialize(Object, ParameterType, ScalaPluginClassLoader)} and {@link #deserialize(Object, ParameterType, ScalaPluginClassLoader)}.
     * @param pluginClassLoader the classloader of your ScalaPlugin
     * @param whenToUse a predicate that will be tested once an object is serialized or deserialized
     * @param codecFactory a generator for a codec that is called once the predicate test succeeds.
     * @return true if the codec factory was registered, otherwise false
     */
    public static boolean registerCodec(ScalaPluginClassLoader pluginClassLoader, Predicate<? super ParameterType> whenToUse, Function<? super ParameterType, ? extends Codec<?, ?>> codecFactory) {
        Objects.requireNonNull(pluginClassLoader, "plugin classloader cannot be null!");
        return registrations.computeIfAbsent(pluginClassLoader, Registrations::new).register(whenToUse, codecFactory);
    }

    /**
     * Register a codec that will be used by {@link #serialize(Object, ParameterType, ScalaPluginClassLoader)} and {@link #deserialize(Object, ParameterType, ScalaPluginClassLoader)}.
     * @param pluginClassLoader the classloader of your ScalaPlugin
     * @param clazz the live type of objects your codec should work with
     * @param codec the codec
     * @param <T> the type of the objects in their live form.
     * @return true if the codec was registered, otherwise false
     */
    public static <T> boolean registerCodec(ScalaPluginClassLoader pluginClassLoader, Class<T> clazz, Codec<T, ?> codec) {
        return registerCodec(pluginClassLoader, ParameterType.from(clazz), codec);
    }

    /**
     * @deprecated internal use only.
     */
    @Deprecated
    public static void clearCodecs(ScalaPluginClassLoader scalaPlugin) {
        registrations.remove(scalaPlugin);
    }

    // ==================== serialize ====================

    /**
     * This method will be called by configuration serializable types for which ScalaLoader does not know how to handle them out of the box.
     * You can register a {@link Codec} using {@link #registerCodec(ScalaPluginClassLoader, ParameterType, Codec)} or its overloads so you can specify the behaviour at runtime.
     * @param live the object that is going to be serialized
     * @param type the type of the live object
     * @param pluginClassLoader the classloader of your plugin
     * @return the object in its serialized form
     */
    @Called
    public static Object serialize(Object live, ParameterType type, ScalaPluginClassLoader pluginClassLoader) {
        Class<?> rawType = type.getRawType();
        assert rawType.isInstance(live) : "live object is not an instance of " + type;

        //out-of-the-box supported
        if (ConfigurationSerializable.class.isAssignableFrom(rawType)
            || rawType == String.class
            || rawType == Integer.class || rawType == int.class
            || rawType == Double.class || rawType == double.class
            || rawType == Boolean.class || rawType == boolean.class) {
            return live;
        }

        //other primitives
        else if (rawType == Byte.class || rawType == byte.class) {
            return ((Byte) live).intValue();
        } else if (rawType == Short.class || rawType == short.class) {
            return ((Short) live).intValue();
        } else if (rawType == Long.class || rawType == long.class) {
            return ((Long) live).toString();
        } else if (rawType == Float.class || rawType == float.class) {
            return ((Float) live).doubleValue();
        } else if (rawType == Character.class || rawType == char.class) {
            return ((Character) live).toString();
        } else if (rawType == Void.class || rawType == void.class) {
            return null;
        }

        //built-ins
        else if (rawType == UUID.class) {
            return ((UUID) live).toString();
        } else if (rawType == BigInteger.class) {
            return ((BigInteger) live).toString();
        } else if (rawType == BigDecimal.class) {
            return ((BigDecimal) live).toString();
        } else if (rawType.isEnum()) {
            return ((Enum<?>) live).name();
        }

        //collections
        else if (type instanceof ArrayParameterType) {
            return serializeArray(live, (ArrayParameterType) type, pluginClassLoader);
        } else if (type instanceof ParameterizedParameterType && live instanceof Collection) {
            return serializeCollection(live, (ParameterizedParameterType) type, pluginClassLoader);
        } else if (type instanceof ParameterizedParameterType && live instanceof Map) {
            return serializeMap(live, (ParameterizedParameterType) type, pluginClassLoader);
        }

        //fallback
        Registrations registrations = RuntimeConversions.registrations.get(pluginClassLoader);
        if (registrations != null) {
            Maybe<Object> maybe = registrations.serialize(type, live);
            if (maybe.isPresent()) {
                Object serializedInstance = maybe.get();
                if (!(serializedInstance instanceof ConfigurationSerializable)
                        && !(serializedInstance instanceof String)
                        && !(serializedInstance instanceof Integer)
                        && !(serializedInstance instanceof Double)
                        && !(serializedInstance instanceof Boolean)
                        && !(serializedInstance instanceof List)
                        && !(serializedInstance instanceof Map)
                        && !(serializedInstance instanceof Set)) {
                    Logger logger = pluginClassLoader.getPlugin().getLogger();
                    logger.warning("Serialized type " + serializedInstance.getClass().getName() + " is not supported out of the box by Bukkit's configuration serialization api.");
                    logger.warning("Please let your Codec serialize to a type that implements org.bukkit.configuration.serialization.ConfigurationSerializable,");
                    logger.warning("or one of the supported types out of Java's standard library:");
                    logger.warning("java.lang.String, java.lang.Integer, java.lang.Double, java.lang.Boolean, java.util.List, java.util.Set or java.util.Map");
                }
                return serializedInstance;
            }
        }

        pluginClassLoader.getPlugin().getLogger()
                .warning("No Codec found for " + live.getClass().getName() + ", please register one using " + RuntimeConversions.class.getName() + "#registerCodec");
        //last try: just hope that SnakeYAML (which is an implementation detail technically) does the right thing
        return live;
    }

    private static Object serializeArray(Object live, ArrayParameterType arrayType, ScalaPluginClassLoader plugin) {
        if (live instanceof byte[]) {
            byte[] bytes = (byte[]) live;
            ArrayList<Integer> list = new ArrayList<>(bytes.length);
            for (int index = 0; index < bytes.length; index++) {
                list.add(Integer.valueOf(bytes[index]));
            }
            return list;
        } else if (live instanceof short[]) {
            short[] shorts = (short[]) live;
            ArrayList<Integer> list = new ArrayList<>(shorts.length);
            for (int index = 0; index < shorts.length; index++) {
                list.add(Integer.valueOf(shorts[index]));
            }
            return list;
        } else if (live instanceof int[]) {
            int[] ints = (int[]) live;
            ArrayList<Integer> list = new ArrayList<>(ints.length);
            for (int index = 0; index < ints.length; index++) {
                list.add(Integer.valueOf(ints[index]));
            }
            return list;
        } else if (live instanceof long[]) {
            long[] longs = (long[]) live;
            ArrayList<String> list = new ArrayList<>(longs.length);
            for (int index = 0; index < longs.length; index++) {
                list.add(Long.toString(longs[index]));
            }
            return list;
        } else if (live instanceof float[]) {
            float[] floats = (float[]) live;
            ArrayList<Double> list = new ArrayList<>(floats.length);
            for (int index = 0; index < floats.length; index++) {
                list.add(Double.valueOf(floats[index]));
            }
            return list;
        } else if (live instanceof double[]) {
            double[] doubles = (double[]) live;
            ArrayList<Double> list = new ArrayList<>(doubles.length);
            for (int index = 0; index < doubles.length; index++) {
                list.add(Double.valueOf(doubles[index]));
            }
            return list;
        } else if (live instanceof char[]) {
            char[] chars = (char[]) live;
            ArrayList<String> list = new ArrayList<>(chars.length);
            for (int index = 0; index < chars.length; index++) {
                list.add(Character.toString(chars[index]));
            }
            return list;
        } else if (live instanceof boolean[]) {
            boolean[] booleans = (boolean[]) live;
            ArrayList<Boolean> list = new ArrayList<>(booleans.length);
            for (int index = 0; index < booleans.length; index++) {
                list.add(Boolean.valueOf(booleans[index]));
            }
            return list;
        } else if (live instanceof Object[]) {
            Object[] objects = (Object[]) live;
            ArrayList<Object> list = new ArrayList<>(objects.length);
            for (int index = 0; index < objects.length; index++) {
                list.add(serialize(objects[index], arrayType.getComponentType(), plugin));
            }
            return list;
        } else {
            throw new IllegalArgumentException("live object must have an array type!");
        }
    }

    private static Object serializeCollection(Object live, ParameterizedParameterType type, ScalaPluginClassLoader plugin) {
        if (live instanceof Set) {
            Set<?> sourceSet = (Set<?>) live;
            Set<Object> resultSet = new LinkedHashSet<>();
            for (Object item : sourceSet) {
                resultSet.add(serialize(item, type.getTypeParameters().get(0), plugin));
            }
            return resultSet;
        } else if (live instanceof List) {
            List<?> sourceList = (List<?>) live;
            List<Object> resultList = new ArrayList<>(sourceList.size());
            for (Object item : sourceList) {
                resultList.add(serialize(item, type.getTypeParameters().get(0), plugin));
            }
            return resultList;
        } else {
            Collection<?> sourceList = (Collection<?>) live;
            List<Object> resultList = new ArrayList<>();
            for (Object item : sourceList) {
                resultList.add(serialize(item, type.getTypeParameters().get(0), plugin));
            }
            return resultList;
        }
    }

    private static Object serializeMap(Object live, ParameterizedParameterType type, ScalaPluginClassLoader plugin) {
        Map<?, ?> sourceMap = (Map<?, ?>) live;
        LinkedHashMap resultMap = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : sourceMap.entrySet()) {
            Object key = entry.getKey();
            Object value = entry.getValue();
            resultMap.put(serialize(key, type.getTypeParameters().get(0), plugin),
                    serialize(value, type.getTypeParameters().get(1), plugin));
        }
        return resultMap;
    }

    // ==================== deserialize ====================

    /**
     * This method will be called by configuration serializable types for which ScalaLoader does not know how to handle them out of the box.
     * You can register a {@link Codec} using {@link #registerCodec(ScalaPluginClassLoader, ParameterType, Codec)} or its overloads so you can specify the behaviour at runtime.
     * @param serialized the object that is going to be deserialized
     * @param type the type of the live object
     * @param pluginClassLoader the classloader of your plugin
     * @return object in its live form
     */
    @Called
    public static Object deserialize(Object serialized, ParameterType type, ScalaPluginClassLoader pluginClassLoader) {
        Class<?> rawType = type.getRawType();

        //out-of-the-box supported
        if (ConfigurationSerializable.class.isAssignableFrom(rawType)
                || rawType == String.class
                || rawType == Integer.class || rawType == int.class
                || rawType == Double.class || rawType == double.class
                || rawType == Boolean.class || rawType == boolean.class) {
            return serialized;
        }

        //other primitives
        else if (rawType == Byte.class || rawType == byte.class) {
            return ((Integer) serialized).byteValue();
        } else if (rawType == Short.class || rawType == short.class) {
            return ((Integer) serialized).shortValue();
        } else if (rawType == Long.class || rawType == long.class) {
            return Long.parseLong(((String) serialized));
        } else if (rawType == Float.class || rawType == float.class) {
            return ((Double) serialized).floatValue();
        } else if (rawType == Character.class || rawType == char.class) {
            return ((String) serialized).charAt(0);
        } else if (rawType == Void.class || rawType == void.class) {
            return null;
        }

        //built-ins
        else if (rawType == UUID.class) {
            return UUID.fromString((String) serialized);
        } else if (rawType == BigInteger.class) {
            return new BigInteger((String) serialized);
        } else if (rawType == BigDecimal.class) {
            return new BigDecimal((String) serialized);
        } else if (rawType.isEnum()) {
            return Enum.valueOf((Class<Enum>) rawType, (String) serialized);
        }

        //collections
        else if (type instanceof ArrayParameterType) {
            return deserializeArray((List<?>) serialized, (ArrayParameterType) type, pluginClassLoader);
        } else if (type instanceof ParameterizedParameterType && Collection.class.isAssignableFrom(type.getRawType())) {
            return deserializeCollection((Collection<?>) serialized, (ParameterizedParameterType) type, pluginClassLoader);
        } else if (type instanceof ParameterizedParameterType && Map.class.isAssignableFrom(type.getRawType())) {
            return deserializeMap((Map<?, ?>) serialized, (ParameterizedParameterType) type, pluginClassLoader);
        }

        //fallback
        Registrations registrations = RuntimeConversions.registrations.get(pluginClassLoader);
        if (registrations != null) {
            Maybe<Object> maybe = registrations.deserialize(type, serialized);
            if (maybe.isPresent()) {
                Object live = maybe.get();
                //no need to warn here, I guess
                return live;
            }
        }

        pluginClassLoader.getPlugin().getLogger()
                .warning("No Codec found for " + type.toString() + ", please register one using " + RuntimeConversions.class.getName() + "#registerCodec");
        //last try: just hope that SnakeYAML (which is an implementation detail technically) does the right thing
        return serialized;
    }

    private static Object deserializeArray(List<?> serialized, ArrayParameterType type, ScalaPluginClassLoader plugin) {
        ParameterType componentType = type.getComponentType();
        Class<?> componentClass = Array.newInstance(componentType.getRawType(), 0).getClass();

        Object array = Array.newInstance(componentClass, serialized.size());

        for (int index = 0; index < serialized.size(); index++) {
            //Array#set unwraps the element if the array is of a primitive type. Thanks Java! :)
            Array.set(array, index, deserialize(serialized.get(index), componentType, plugin));
        }

        return array;
    }

    private static Object deserializeCollection(Collection<?> serialized, ParameterizedParameterType type, ScalaPluginClassLoader plugin) {
        Class<?> rawType = type.getRawType();
        Collection<Object> resultCollection;

        //go through a bunch of hoops to determine which collection we should use.
        if (rawType.isInterface()) {
            //concurrent collection interfaces
            if (BlockingDeque.class.isAssignableFrom(rawType)) {
                resultCollection = new LinkedBlockingDeque<>();
            } else if (TransferQueue.class.isAssignableFrom(rawType)) {
                resultCollection = new LinkedTransferQueue<>();
            } else if (BlockingQueue.class.isAssignableFrom(rawType)) {
                resultCollection = new LinkedBlockingQueue<>();
            }
            //non-thread-safe collection interfaces
            else if (Deque.class.isAssignableFrom(rawType)) {
                resultCollection = new ArrayDeque<>(serialized.size());
            } else if (Queue.class.isAssignableFrom(rawType)) {
                resultCollection = new LinkedList<>();
            } else if (SortedSet.class.isAssignableFrom(rawType)) {
                resultCollection = new TreeSet<>();
            } else if (Set.class.isAssignableFrom(rawType)) {
                resultCollection = new LinkedHashSet<>();
            } else {
                resultCollection = new ArrayList<>(serialized.size());
            }
        } else {
            //it is a class.
            if (rawType == EnumSet.class) {
                //special-case EnumSet; it has no public no-args constructor.
                resultCollection = EnumSet.noneOf((Class<? extends Enum>) type.getTypeParameters().get(0).getRawType());
            } else {
                try {
                    //best effort, try no-args constructor
                    Constructor<?> nullaryConstructor = rawType.getConstructor(new Class[0]);
                    resultCollection = (Collection<Object>) nullaryConstructor.newInstance();
                } catch (NoSuchMethodException | InvocationTargetException | InstantiationException | IllegalAccessException e) {
                    ConfigurationSerializableError error = new ConfigurationSerializableError("Could not instantiate an instance of " + rawType.getName() + ". It has no public constructor with zero arguments.");
                    error.addSuppressed(e);
                    throw error;
                }
            }
        }

        //finally, add the items.
        for (Object item : serialized) {
            resultCollection.add(deserialize(item, type.getTypeParameters().get(0), plugin));
        }

        return resultCollection;
    }

    private static Object deserializeMap(Map<?, ?> serialized, ParameterizedParameterType type, ScalaPluginClassLoader plugin) {
        Class<?> rawType = type.getRawType();
        Map<Object, Object> resultMap;

        //go through a bunch of hoops to determine the map type
        if (rawType.isInterface()) {
            if (ConcurrentNavigableMap.class.isAssignableFrom(rawType)) {
                resultMap = new ConcurrentSkipListMap<>();
            } else if (ConcurrentMap.class.isAssignableFrom(rawType)) {
                resultMap = new ConcurrentHashMap<>();
            } else if (SortedMap.class.isAssignableFrom(rawType)) {
                resultMap = new TreeMap<>();
            } else {
                resultMap = new LinkedHashMap<>();
            }
        } else {
            //it is a class
            if (rawType == EnumMap.class) {
                //special-case EnumMap; it has no public no-args constructor.
                resultMap = new EnumMap(type.getTypeParameters().get(0).getRawType());
            } else {
                //best effort, try no-args constructor
                try {
                    Constructor<?> nullaryConstructor = rawType.getConstructor(new Class[0]);
                    resultMap = (Map<Object, Object>) nullaryConstructor.newInstance();
                } catch (NoSuchMethodException | InvocationTargetException | InstantiationException | IllegalAccessException e) {
                    ConfigurationSerializableError error = new ConfigurationSerializableError("Could not instantiate an instance of " + rawType.getName() + ". It has no public constructor with zero arguments.");
                    error.addSuppressed(e);
                    throw error;
                }
            }
        }

        //finally, add the items.
        for (Map.Entry<?, ?> entry : serialized.entrySet()) {
            Object key = entry.getKey();
            Object value = entry.getValue();
            resultMap.put(deserialize(key, type.getTypeParameters().get(0), plugin),
                    deserialize(value, type.getTypeParameters().get(1), plugin));
        }

        return resultMap;
    }


    // ======================================================

    private static class Registrations {
        private final ScalaPluginClassLoader classLoader;

        private final Map<ParameterType, Codec<?, ?>> absoluteCodecs = new HashMap<>();
        private final Map<Predicate<? super ParameterType>, Function<? super ParameterType, ? extends Codec<?, ?>>> bestEffortCodecs = new LinkedHashMap<>();

        private Registrations(ScalaPluginClassLoader classLoader) {
            this.classLoader = classLoader;
        }

        private boolean register(ParameterType type, Codec<?, ?> codec) {
            return absoluteCodecs.putIfAbsent(type, codec) == null;
        }

        private boolean register(Predicate<? super ParameterType> type, Function<? super ParameterType, ? extends Codec<?, ?>> codecFactory) {
            return bestEffortCodecs.putIfAbsent(type, codecFactory) == null;
        }

        private Maybe<Object> serialize(ParameterType parameterType, Object live) {
            Codec codec = absoluteCodecs.get(parameterType);
            if (codec != null) return Maybe.just(codec.serialize(live));

            for (Map.Entry<Predicate<? super ParameterType>, Function<? super ParameterType, ? extends Codec<?, ?>>> entry : bestEffortCodecs.entrySet()) {
                Predicate<? super ParameterType> predicate = entry.getKey();
                Function<? super ParameterType, ? extends Codec> codecFactory = entry.getValue();
                if (predicate.test(parameterType)) return Maybe.just(codecFactory.apply(parameterType).serialize(live));
            }

            return Maybe.nothing();
        }

        private Maybe<Object> deserialize(ParameterType parameterType, Object serialized) {
            Codec codec = absoluteCodecs.get(parameterType);
            if (codec != null) return Maybe.just(codec.deserialize(serialized));

            for (Map.Entry<Predicate<? super ParameterType>, Function<? super ParameterType, ? extends Codec<?, ?>>> entry : bestEffortCodecs.entrySet()) {
                Predicate<? super ParameterType> predicate = entry.getKey();
                Function<? super ParameterType, ? extends Codec> codecFactory = entry.getValue();
                if (predicate.test(parameterType)) return Maybe.just(codecFactory.apply(parameterType).deserialize(serialized));
            }

            return Maybe.nothing();
        }
    }

}
