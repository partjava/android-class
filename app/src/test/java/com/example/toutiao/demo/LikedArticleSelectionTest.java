package com.example.toutiao.demo;
import org.junit.Test;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import static org.junit.Assert.*;
public class LikedArticleSelectionTest {
    @Test public void restoresLegacyLikesWithoutDuplicatingSavedHistory() {
        List<String> rows = Arrays.asList("new:校园新闻", "saved:校园新闻", "history:旧新闻", "history:未赞新闻");
        Set<String> liked = new HashSet<>(Arrays.asList("校园新闻", "旧新闻"));
        assertEquals(Arrays.asList("new:校园新闻", "history:旧新闻"), LikedArticleSelection.select(rows, s -> s.split(":")[1], liked::contains));
    }
    @Test public void cancelledLikeDisappearsEvenWhenSnapshotsRemain() {
        Set<String> liked = new HashSet<>(Arrays.asList("校园新闻"));
        List<String> rows = Arrays.asList("校园新闻", "校园新闻", null, "");
        assertEquals(1, LikedArticleSelection.select(rows, s -> s, liked::contains).size());
        liked.clear();
        assertTrue(LikedArticleSelection.select(rows, s -> s, liked::contains).isEmpty());
    }
}
