import common.Page;
import common.Pages;
import common.Words;

import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class Sequential {
    static final int maxPages = 100000;
    static final String fileName = "enwiki.xml";

    private static final HashMap<String, Integer> counts = new HashMap<>();

    public static void main(String[] args) {
        int datasetSize = args.length > 0 ? Integer.parseInt(args[0]) : maxPages;
        int threadCount = 1;
        Benchmark benchmark = new Benchmark();
        benchmark.start();

        Iterable<Page> pages = new Pages(datasetSize, fileName);
        int processedPages = 0;
        for (Page page : pages) {
            if (page == null)
                break;
            Iterable<String> words = new Words(page.getText());
            for (String word : words) {
                if (word.length() > 1 || word.equals("a") || word.equals("I")) {
                    countWord(word);
                }
            }
            processedPages++;
        }
        benchmark.end();
        benchmark.printResults("Sequential", datasetSize, threadCount);
        System.out.println("Processed pages: " + processedPages);
        LinkedHashMap<String, Integer> commonWords = new LinkedHashMap<>();
        counts.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .forEachOrdered(x -> commonWords.put(x.getKey(), x.getValue()));
        commonWords.entrySet().stream().limit(3).toList()
                .forEach(x -> System.out
                        .println("Word: '" + x.getKey() + "' with total " + x.getValue() + " occurrences!"));
        benchmark.printResults("Sequential", datasetSize, threadCount);
    }

    private static void countWord(String word) {
        counts.merge(word, 1, Integer::sum);
    }
}