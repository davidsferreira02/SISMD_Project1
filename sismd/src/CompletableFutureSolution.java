import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import common.Page;
import common.Words;
import common.Pages;

public class CompletableFutureSolution {
static final int maxPages = 100000;
    static final String fileName = "enwiki.xml";
    static final int THREAD_POOL_SIZE = Runtime.getRuntime().availableProcessors();

    public static void main(String[] args) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
        long start = System.currentTimeMillis();

        Iterable<Page> pages = new Pages(maxPages, fileName);
        List<CompletableFuture<Map<String,Integer>>> futures = new ArrayList<>();
        int processedPages = 0;

        for (Page page : pages) {
            if (page == null) break;
            CompletableFuture<Map<String,Integer>> future = CompletableFuture.supplyAsync(() -> {
                Map<String,Integer> localCounts = new HashMap<>();
                Iterable<String> words = new Words(page.getText());
                for (String word : words) {
                    if (word.length() > 1 || word.equals("a") || word.equals("I")) {
                        localCounts.merge(word, 1, Integer::sum);
                    }
                }
                return localCounts;
            }, executor);
            futures.add(future);
            processedPages++;
        }

        CompletableFuture<Void> allDone = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
        allDone.join();

        Map<String,Integer> combinedCounts = new HashMap<>();
        for (CompletableFuture<Map<String,Integer>> f : futures) {
            f.join().forEach((k, v) -> combinedCounts.merge(k, v, Integer::sum));
        }

        executor.shutdown();
        long end = System.currentTimeMillis();

        System.out.println("Processed pages: " + processedPages);
        System.out.println("Elapsed time: " + (end - start) + "ms");

        LinkedHashMap<String,Integer> commonWords = new LinkedHashMap<>();
        combinedCounts.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .forEachOrdered(e -> commonWords.put(e.getKey(), e.getValue()));

        commonWords.entrySet().stream().limit(3).toList()
                .forEach(e -> System.out.println("Word: '" + e.getKey() + "' with total " + e.getValue() + " occurrences!"));
    }
}
