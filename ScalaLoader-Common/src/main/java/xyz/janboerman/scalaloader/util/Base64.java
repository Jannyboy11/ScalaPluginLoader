package xyz.janboerman.scalaloader.util;

import java.nio.charset.StandardCharsets;

public class Base64 {

    private Base64() {
    }

    public static String encode(byte[] bytes) {
        byte[] encoded = java.util.Base64.getEncoder().encode(bytes);
        String string = new String(encoded, StandardCharsets.UTF_8);
        return string;
    }

    public static byte[] decode(String string) {
        byte[] encoded = string.getBytes(StandardCharsets.UTF_8);
        byte[] raw = java.util.Base64.getDecoder().decode(encoded);
        return raw;
    }
}
