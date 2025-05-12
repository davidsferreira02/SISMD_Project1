import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import common.Page;
import common.Words;
import common.Pages;

public class CompletableFutureSolution {
    static final int DEFAULT_MAX_PAGES = 100000;
    static final String DEFAULT_FILE_NAME = "enwiki.xml";
    static final int DEFAULT_THREAD_POOL_SIZE = 500;

    public static void main(String[] args) throws Exception {
        int maxPages = DEFAULT_MAX_PAGES;
        String fileName = DEFAULT_FILE_NAME;
        int threadPoolSize = DEFAULT_THREAD_POOL_SIZE;
        
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("--pages") && i + 1 < args.length) {
                maxPages = Integer.parseInt(args[i + 1]);
                i++; // Consume value
            } else if (args[i].equals("--threads") && i + 1 < args.length) {
                threadPoolSize = Integer.parseInt(args[i + 1]);
                i++; // Consume value
            } else if (args[i].equals("--file") && i + 1 < args.length) {
                fileName = args[i + 1];
                i++; // Consume value
            }
        }
        
        System.out.println("Running with " + threadPoolSize + " threads, " + maxPages + " pages, file: " + fileName);
        
        ExecutorService executor = Executors.newFixedThreadPool(threadPoolSize);
        long start = System.currentTimeMillis();
        ThreadMXBean bean = ManagementFactory.getThreadMXBean();
        long cpuTimeBefore = bean.getCurrentThreadCpuTime();        Runtime runtime = Runtime.getRuntime();
        runtime.gc();
        long memoryBefore = runtime.totalMemory() - runtime.freeMemory();

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
        long memoryAfter = runtime.totalMemory() - runtime.freeMemory();
        long cpuTimeAfter = bean.getCurrentThreadCpuTime();        System.out.println("Processed pages: " + processedPages);
        System.out.println("Elapsed time: " + (end - start) + "ms");
        System.out.println("Usage Memory: " + (memoryAfter - memoryBefore) + " bytes");
        System.out.println(String.format(Locale.US, "Usage Cpu Time %.8f seconds", (cpuTimeAfter - cpuTimeBefore) / 1_000_000_000.0));

        LinkedHashMap<String,Integer> commonWords = new LinkedHashMap<>();
        combinedCounts.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .forEachOrdered(e -> commonWords.put(e.getKey(), e.getValue()));

        commonWords.entrySet().stream().limit(3).toList()
                .forEach(e -> System.out.println("Word: '" + e.getKey() + "' with total " + e.getValue() + " occurrences!"));
    }
}
