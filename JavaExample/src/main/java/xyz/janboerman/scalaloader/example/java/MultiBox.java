package xyz.janboerman.scalaloader.example.java;

import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.SerializableAs;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;
import static java.util.Map.entry;

public class MultiBox implements ConfigurationSerializable {

    private static final Logger logger = ExamplePlugin.getInstance().getLogger();

    private byte b;                                                     // ===> java.lang.Integer
    private short s;                                                    // ===> java.lang.Integer
    private int i;                                                      // ===> java.lang.Integer       (almost correct)
    private long l;                                                     // ===> java.lang.Integer
    private float f;                                                    // ===> java.lang.Double
    private double d;                                                   // ===> java.lang.Double        (almost correct)
    private char c;                                                     // ===> byte[]                  (DAFUQ?!)
    private boolean bool;                                               // ===> java.lang.Boolean       (almost correct)

    private Byte by = Byte.valueOf((byte) 0);                           // ===> java.lang.Integer
    private Short sh = Short.valueOf((short) 0);                        // ===> java.lang.Integer
    private Integer in = Integer.valueOf(0);                            // ===> java.lang.Integer       (correct!)
    private Long lo = Long.valueOf(0L);                                 // ===> java.lang.Integer
    private Float fl = Float.valueOf(0);                                // ===> java.lang.Double
    private Double dou = Double.valueOf(0.0);                           // ===> java.lang.Double        (correct!)
    private Character ch = Character.valueOf((char) 0);                 // ===> byte[]                  (DAFUQ?!)
    private Boolean boole = Boolean.valueOf(false);                     // ===> java.lang.Boolean       (correct!)

    private BigInteger bigInteger = new BigInteger("0");            // ===> java.lang.Integer
    private BigDecimal bigDecimal = new BigDecimal("0");            // ===> java.lang.Double

    private String string = new String("string");               // ===> java.lang.String
    private UUID uuid = new UUID(0L, 0L);
    private HelloWorld hw = HelloWorld.FOO;

    private int[] intArray = new int[] { 0 };                           // ===> java.util.ArrayList
    private int[] emptyIntArray = new int[0];                           // ===> java.util.ArrayList
    private String[] stringArray = new String[] { "" };                 // ===> java.util.ArrayList
    private String[] emptyStringArray = new String[0];                  // ===> java.util.ArrayList
    private List<String> list = List.of("");                            // ===> java.util.ArrayList     (correct!)
    private List<String> emptyList = List.of();                         // ===> java.util.ArrayList     (correct!)
    private Set<String> set = Set.of("");                               // ===> java.util.LinkedHashSet (correct!)
    private Set<String> emptySet = Set.of();                            // ===> java.util.LinkedHashSet (correct!)
    private Map<String, String> map = Map.of("", "");           // ===> java.util.LinkedHashMap (correct!)
    private Map<String, String> emptyMap = Map.of();                    // ===> java.util.LinkedHashMap (correct!)

    private String numberString = "0.5";                                // ===> java.lang.String        (correct!)

    public MultiBox() {
    }

    @Override
    public Map<String, Object> serialize() {
        return Map.ofEntries(
                entry("byte", b),
                entry("short", s),
                entry("int", i),
                entry("long", l),
                entry("float", f),
                entry("double", d),
                entry("char", c),
                entry("boolean", bool),

                entry("Byte", by),
                entry("Short", sh),
                entry("Integer", in),
                entry("Long", lo),
                entry("Float", fl),
                entry("Double", dou),
                entry("Character", ch),
                entry("Boolean", boole),

                entry("BigInteger", bigInteger),
                entry("BigDecimal", bigDecimal),

                entry("String", string),
                entry("UUID", uuid),
                entry("HelloWorld enum", hw),

                entry("empty int array", emptyIntArray),
                entry("empty string array", emptyStringArray),
                entry("empty list", emptyList),
                entry("empty set", emptySet),
                entry("empty map", emptyMap),

                entry("int array", intArray),
                entry("string array", stringArray),
                entry("list", list),
                entry("set", set),
                entry("map", map),

                entry ("number string", numberString)
        );
    }

    public static MultiBox deserialize(Map<String, Object> map) {
        for (var entry : map.entrySet()) {
            logger.info("key = " + entry.getKey() + ", value = " + entry.getValue() + ", value class = " + entry.getValue().getClass().toString());
        }

        /*
        key = ==, value = MultiBox, value class = class java.lang.String
        key = empty set, value = [], value class = class java.util.LinkedHashSet
        key = short, value = 0, value class = class java.lang.Integer
        key = empty list, value = [], value class = class java.util.ArrayList
        key = string array, value = [], value class = class java.util.ArrayList
        key = float, value = 0.0, value class = class java.lang.Double
        key = int, value = 0, value class = class java.lang.Integer
        key = number string, value = 0.5, value class = class java.lang.String
        key = Double, value = 0.0, value class = class java.lang.Double
        key = boolean, value = false, value class = class java.lang.Boolean
        key = Long, value = 0, value class = class java.lang.Integer
        key = Integer, value = 0, value class = class java.lang.Integer
        key = Byte, value = 0, value class = class java.lang.Integer
        key = String, value = string, value class = class java.lang.String
        key = Short, value = 0, value class = class java.lang.Integer
        key = double, value = 0.0, value class = class java.lang.Double
        key = map, value = {=}, value class = class java.util.LinkedHashMap
        key = Float, value = 0.0, value class = class java.lang.Double
        key = char, value = [B@7ad0d5dd, value class = class [B
        key = list, value = [], value class = class java.util.ArrayList
        key = int array, value = [0], value class = class java.util.ArrayList
        key = Character, value = [B@2f7d1aed, value class = class [B
        key = BigInteger, value = 0, value class = class java.lang.Integer
        key = Boolean, value = false, value class = class java.lang.Boolean
        key = empty string array, value = [], value class = class java.util.ArrayList
        key = set, value = [], value class = class java.util.LinkedHashSet
        key = empty int array, value = [], value class = class java.util.ArrayList
        key = empty map, value = {}, value class = class java.util.LinkedHashMap
        key = long, value = 0, value class = class java.lang.Integer
        key = BigDecimal, value = 0.0, value class = class java.lang.Double
        key = byte, value = 0, value class = class java.lang.Integer
         */

        return new MultiBox();
    }

    public static enum HelloWorld {
        FOO,
        BAR;
    }
}
