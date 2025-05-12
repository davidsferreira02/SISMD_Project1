package common;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class MapReduce {

    private final ConcurrentMap<String, Integer> wordCounts = new ConcurrentHashMap<>();

    public void map(String text) {
        Iterable<String> words = new Words(text);
        for (String word : words) {
            if (word.length() > 1 || word.equals("a") || word.equals("I")) {
                wordCounts.merge(word, 1, Integer::sum);
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
                .forEach(entry -> System.out
                        .println("Word: '" + entry.getKey() + "' occurred " + entry.getValue() + " times!"));
    }
}
