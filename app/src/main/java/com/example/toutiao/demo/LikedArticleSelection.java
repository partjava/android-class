package com.example.toutiao.demo;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

/** Prefer new snapshots, recover legacy records, and exclude cancelled likes. */
final class LikedArticleSelection {
    static <T> List<T> select(Iterable<T> snapshots, Function<T, String> titleOf, Predicate<String> isLiked) {
        LinkedHashMap<String, T> selected = new LinkedHashMap<>();
        for (T item : snapshots) {
            if (item == null) continue;
            String title = titleOf.apply(item);
            if (title != null && !title.isEmpty() && isLiked.test(title) && !selected.containsKey(title)) selected.put(title, item);
        }
        return new ArrayList<>(selected.values());
    }
}
