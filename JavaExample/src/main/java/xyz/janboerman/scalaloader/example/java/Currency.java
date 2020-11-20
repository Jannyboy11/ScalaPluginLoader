package xyz.janboerman.scalaloader.example.java;

import xyz.janboerman.scalaloader.configurationserializable.Scan;
import xyz.janboerman.scalaloader.configurationserializable.ConfigurationSerializable;

@ConfigurationSerializable(as = "Currency", scan = @Scan(Scan.Type.ENUM))
enum Currency {

    EUROS,
    DOLLARS,
    YEN;

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
