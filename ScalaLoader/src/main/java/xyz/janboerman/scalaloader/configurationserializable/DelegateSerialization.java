//package xyz.janboerman.scalaloader.configurationserializable;
//
//import org.bukkit.configuration.serialization.ConfigurationSerialization;
//import xyz.janboerman.scalaloader.configurationserializable.transform.ConfigurationSerializableError;
//
//import java.lang.annotation.ElementType;
//import java.lang.annotation.Retention;
//import java.lang.annotation.RetentionPolicy;
//import java.lang.annotation.Target;
//import java.util.Collections;
//import java.util.HashMap;
//import java.util.Map;
//
////TODO I'm not sure how feasible this is yet given I do not want to traverse every class twice, but I think it can be done.
////TODO so implement the @ConfigurationSerializable annotation first.
//
//@Retention(RetentionPolicy.CLASS)
//@Target(ElementType.TYPE)
//public @interface DelegateSerialization {
//    //for sum types, list all the cases here!
//    // TODO in the future, when java adopts sealed types, we can generate this list if it's empty :)
//    // TODO we can already add the listed classes as nest members to the nest of the host.
//    // TODO the "nest host" is the class that has the DelegateDeserialization annotation
//    Class<?>[] value() default {};
//
//    //TODO classes that have this annotation should still get the ConfigurationSerializable interface
//    //TODO it should be noted that this annotation can be put on traits (which could be compiled to either interfaces or classes)
//    //TODO since interfaces can have static methods, it is no problem to generate a public static SomeTrait deserialize(Map<String, Object> map) method.
//    //TODO its implementation will need to keep which subclass was used in the map.
//
//    //@DelegateDeserialization({Just.class, Nothing.class})
//    static interface Maybe<T> {
//
//        //generated from the annotation!!
//        public static <T> Maybe<T> deserialize(Map<String, Object> map) {
//            String className = (String) map.get("$discriminator");
//            try {
//                Class clazz = Class.forName(className);
//                if (Maybe.class.isAssignableFrom(clazz)) {
//                    Map<String, Object> realValue = (Map<String, Object>) map.get("delegate");
//                    return (Maybe<T>) ConfigurationSerialization.deserializeObject(realValue, clazz);
//                } else {
//                    throw new ConfigurationSerializableError("Expecting " + className + " to be a(n) (in)direct subclass of " + Maybe.class.getName() + ".");
//                }
//            } catch (ClassNotFoundException e) {
//                throw new ConfigurationSerializableError("class " + className + " does not exist. It was expected to be a subclass of " + Maybe.class.getName() + ".", e);
//            }
//        }
//
//        //generated from the annotation!!
//        public default Map<String, Object> serialize() {
//            //TODO PROBLEM this method is never invoked when subclasses override it.
//            //TODO
//            //TODO I can think of some solutions:
//            //TODO  1: subclasses methods need to be private? and invoked using invokeSpecial which is allowed when the subclass is in the nest of the host
//            //TODO      => this comes with the problem of the subclass not being able to be serialized normally
//            //TODO      => TODO actually, is this correct? public methods can still be invoked using InvokeSpecial.
//            //TODO  2: a different method is called on the subclass instances (and the subclass doesn't override serialize())
//            //TODO      => has the same problem
//            //TODO  4: the subclasses wrap their own Map as a delegate and return the wrapper map which also includes the delegateType class name
//            //TODO      => has the same problem
//            //TODO  3: the subclasses include the delegateType into their map. (create a new HashMap, call putAll)
//            //TODO      => can cause a runtime error when the deserialization method of the subclass checks for unknown keys
//            //TODO  can bukkit's DelegateDeserialization be of use? it doesn't solve the issue of being overridden.
//            //TODO
//            //TODO so it seems that solution 3 is the best solution.
//            //TODO it seems the easiest way to do this is to rename the original serialize() method of the subclass
//            //TODO and just let the newly generated one call the renamed one.
//
//
////            if (this instanceof Just) {
////                return Map.of("$delegateType", Just.class.getName(), "delegate", ((Just) this).serialize() /*invokeSpecial?*/);
////            } else if (this instanceof Nothing) {
////                return Map.of("$delegateType", Nothing.class.getName(), "delegate", ((Nothing) this).serialize() /*invokeSpecial?*/);
////            } else {
//                throw new ConfigurationSerializableError("Non exhaustive pattern match, " + getClass().getName() + " case was not caught");
////            }
//        }
//
//    }
//
//    //@ConfigurationSerializable
//    static class Just<T> implements Maybe {
//
//        private final T value;
//
//        public Just(T value) {
//            this.value = value;
//        }
//
//        //static Just deserialize(Map<String, Object>) map) { .. }  //generated by @ConfigurationSerializable!
//
//        //generated by @ConfigurationSerializable as serialize() (or just from source code!)
//        public Map<String, Object> $serialize() {
//            return Collections.singletonMap("value", value);
//        } //but renamed by @DelegateSerialization to $serialize()
//
//        //generated by @DelegateSerialization
//        public Map<String, Object> serialize() {
//            Map<String, Object> map = new HashMap<>(2);
//            map.put("$discriminator", Just.class.getName());
//            map.put("$delegate", $serialize());
//            return map;
//        }
//    }
//
//    //@ConfigurationSerializable
//    static class Nothing implements Maybe {
//
//        //static Nothing deserialize(Map<String, Object>) map) { .. }  //generated!!
//
//        //generated by @ConfigurationSerializable (I did not apply the DelegateSerialization thingy here)
//        public Map<String, Object> serialize() {
//            Map<String, Object> map = new HashMap<>(); //TODO could specialize to Collections.emptyMap()
//            return map;
//        }
//    }
//}
