package xyz.janboerman.scalaloader.util;

import java.util.Arrays;

public final class ArrayOps {

    private ArrayOps() {
    }

    public static <T> T[] append(T[] array, T last) {
        int length = array.length;
        T[] result = Arrays.copyOf(array, length + 1);
        result[length] = last;
        return result;
    }

    public static <T> T[] init(T[] array) {
        int length = array.length;
        assert length > 0 : "array must have length > 0";
        return Arrays.copyOf(array, length - 1);
    }

}
