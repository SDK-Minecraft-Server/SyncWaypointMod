package com.oneidler.syncwaypoint.utils;

import java.util.Collection;
import java.util.Map;

public class CollUtil {

    public static <E> boolean isEmpty(Collection<E> collection) {
        return collection == null || collection.isEmpty();
    }

    @SafeVarargs
    public static <T> boolean isEmpty(T... array) {
        return array == null || array.length == 0;
    }

    public static <K, V> boolean isEmpty(Map<K, V> map) {
        return map == null || map.isEmpty();
    }
}
