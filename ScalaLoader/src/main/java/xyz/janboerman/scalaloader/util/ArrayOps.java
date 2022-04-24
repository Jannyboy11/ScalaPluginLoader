package xyz.janboerman.scalaloader.util;

import java.util.Arrays;

public class ArrayOps {

    private ArrayOps() {
    }

    public static <T> T[] append(T[] array, T last) {
        int length = array.length;
        T[] result = Arrays.copyOf(array, length + 1);
        result[length] = last;
        return result;
    }

}
