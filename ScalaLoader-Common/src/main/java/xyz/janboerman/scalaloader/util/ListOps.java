package xyz.janboerman.scalaloader.util;

import java.util.List;
import java.util.ArrayList;

public class ListOps {

    private ListOps() {
    }

    public static <T> List<T> concat(List<T> one, List<T> two) {
        if (one == null) return two;
        if (two == null) return one;
        ArrayList<T> arrayList = new ArrayList<T>(one.size() + two.size());
        arrayList.addAll(one);
        arrayList.addAll(two);
        return arrayList;
    }

}
