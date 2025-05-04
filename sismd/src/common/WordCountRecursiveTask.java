package common;/*  Divide a lista de Page em duas metades até o número ser inferior ao THRESHOLD.
    Conta as palavras em paralelo.
    Junta os resultados de cada sublista num único Map<String, Integer>.

*/

import java.util.*;
import java.util.concurrent.RecursiveTask;


public class WordCountRecursiveTask extends RecursiveTask<Map<String, Integer>> {
    private final List<Page> pages;
    private static final int THRESHOLD = 100;

    public WordCountRecursiveTask(List<Page> pages) {
        this.pages = pages;
    }

    @Override
    protected Map<String, Integer> compute() {
        if (pages.size() <= THRESHOLD) {
            return countWords(pages);
        } else {
            int mid = pages.size() / 2;
            WordCountRecursiveTask left = new WordCountRecursiveTask(pages.subList(0, mid));
            WordCountRecursiveTask right = new WordCountRecursiveTask(pages.subList(mid, pages.size()));
            left.fork();
            Map<String, Integer> rightResult = right.compute();
            Map<String, Integer> leftResult = left.join();
            return merge(leftResult, rightResult);
        }
    }

    private Map<String, Integer> countWords(List<Page> pages) {
        Map<String, Integer> wordCount = new HashMap<>();
        for (Page page : pages) {
            Iterable<String> words = new Words(page.getText());
            for (String word : words) {
                if (word.length() > 1 || word.equals("a") || word.equals("I")) {
                    wordCount.merge(word, 1, Integer::sum);
                }
            }
        }
        return wordCount;
    }

    private Map<String, Integer> merge(Map<String, Integer> a, Map<String, Integer> b) {
        for (Map.Entry<String, Integer> entry : b.entrySet()) {
            a.merge(entry.getKey(), entry.getValue(), Integer::sum);
        }
        return a;
    }
}