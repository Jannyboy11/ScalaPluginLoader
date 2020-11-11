package xyz.janboerman.scalaloader.example.java;

import org.bukkit.configuration.serialization.SerializableAs;
import xyz.janboerman.scalaloader.configurationserializable.Scan;
import xyz.janboerman.scalaloader.configurationserializable.ConfigurationSerializable;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@ConfigurationSerializable(as = "Currency", scan = @Scan(Scan.Type.ENUM))
//https://hub.spigotmc.org/jira/browse/SPIGOT-6234
/*enum*/ class Currency /*implements org.bukkit.configuration.serialization.ConfigurationSerializable*/ {

//    EUROS,
//    DOLLARS,
//    YEN;

    public static final Currency EUROS = new Currency("EUROS", 0);
    public static final Currency DOLLARS = new Currency("DOLLARS", 1);
    public static final Currency YEN = new Currency("YEN", 2);

    private final String name;
    private final int ordinal;

    private Currency(String name, int ordinal) {
        this.name = name;
        this.ordinal = ordinal;
    }

    //needed to simulate an enum
    public final String name() {
        return name;
    }

    //needed to simulate an enum
    public static Currency valueOf(String name) {
        if (name == null) return null;
        switch (name) {
            case "EUROS": return EUROS;
            case "DOLLARS": return DOLLARS;
            case "YEN": return YEN;
            default: return null;
        }
    }

    //needed to simulate an enum
    public String toString() {
        return name();
    }

    //needed to simulate an enum
    public boolean equals(Object o) {
        return o == this;
    }

    //needed to simulate an enum
    public int hashCode() {
        return ordinal;
    }

    //Generated!
//    public static Currency deserialize(Map<String, Object> map) {
//        return Currency.valueOf((String) map.get("name"));
//    }
//
//    @Override
//    public Map<String, Object> serialize() {
//        HashMap<String, Object> map = new HashMap<>();
//        map.put("name", name());
//        return map;
//    }

}
