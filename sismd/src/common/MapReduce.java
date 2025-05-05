package common;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class MapReduce {

    private final ConcurrentMap<String, Integer> wordCounts = new ConcurrentHashMap<>();

    public void map(String text) {
        for (String word : text.split("\\W+")) { // Split by non-word characters
            if (!word.isEmpty()) {
                String lower = word.toLowerCase();
                wordCounts.merge(lower, 1, Integer::sum);
            }
        }
    }

    public ConcurrentMap<String, Integer> getCounts() {
        return wordCounts;
    }

    public void printTopWords(int topN) {
        wordCounts.entrySet().stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .limit(topN)
                .forEach(entry ->
                        System.out.println("Word: '" + entry.getKey() + "' occurred " + entry.getValue() + " times!"));
    }
}
