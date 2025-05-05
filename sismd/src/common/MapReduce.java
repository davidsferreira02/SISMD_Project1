package common;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class MapReduce {

    private final ConcurrentMap<Character, Integer> letterCounts = new ConcurrentHashMap<>();

    public void map(String text) {
        for (char c : text.toCharArray()) {
            if (Character.isLetter(c)) {
                char lower = Character.toLowerCase(c);
                letterCounts.merge(lower, 1, Integer::sum);
            }
        }
    }

    public ConcurrentMap<Character, Integer> getCounts() {
        return letterCounts;
    }

    public void printTopLetters(int topN) {
        letterCounts.entrySet().stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .limit(topN)
                .forEach(entry ->
                        System.out.println("Letter: '" + entry.getKey() + "' occurred " + entry.getValue() + " times!"));
    }
}
