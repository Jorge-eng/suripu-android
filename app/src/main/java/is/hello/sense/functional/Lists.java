package is.hello.sense.functional;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;

public class Lists {
    @SafeVarargs
    public static <T> ArrayList<T> newArrayList(T... values) {
        ArrayList<T> list = new ArrayList<>(values.length);
        Collections.addAll(list, values);
        return list;
    }

    public static <T, R> List<R> map(@NonNull Iterable<T> source, @NonNull Function<T, R> mapper) {
        List<R> accumulator = new ArrayList<>();
        for (T value : source) {
            accumulator.add(mapper.apply(value));
        }
        return accumulator;
    }

    public static <T, K> List<List<T>> segment(@NonNull Function<T, K> keyProducer, @NonNull Iterable<T> source) {
        LinkedHashMap<K, List<T>> result = new LinkedHashMap<>();
        for (T value : source) {
            K key = keyProducer.apply(value);
            List<T> segment = result.get(key);
            if (segment == null) {
                segment = new ArrayList<>();
                result.put(key, segment);
            }
            segment.add(value);
        }
        return new ArrayList<>(result.values());
    }

    public static <T extends Comparable<T>> List<T> sorted(@NonNull Collection<T> toSort, @NonNull Comparator<T> comparator) {
        List<T> sortedCopy = new ArrayList<>(toSort.size());
        sortedCopy.addAll(toSort);
        Collections.sort(sortedCopy, comparator);
        return sortedCopy;
    }

    public static <T> List<T> filtered(@NonNull Iterable<T> toFilter, @NonNull Function<T, Boolean> predicate) {
        List<T> results = new ArrayList<>();
        for (T value : toFilter) {
            if (predicate.apply(value))
                results.add(value);
        }
        return results;
    }

    public static @Nullable <T> T findFirst(@NonNull Iterable<T> haystack, @NonNull Function<T, Boolean> needle) {
        for (T value : haystack) {
            if (needle.apply(value))
                return value;
        }

        return null;
    }
}