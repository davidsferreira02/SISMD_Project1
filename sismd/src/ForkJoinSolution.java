import common.Page;
import common.Pages;
import common.WordCountRecursiveTask;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.*;
import java.util.Locale;
import java.util.concurrent.ForkJoinPool;

public class ForkJoinSolution {
    static final int DEFAULT_MAX_PAGES = 100000;
    static final String DEFAULT_FILE_NAME = "enwiki.xml";
    static final int DEFAULT_PARALLELISM = 1000;

    public static void main(String[] args){
        int maxPages = DEFAULT_MAX_PAGES;
        String fileName = DEFAULT_FILE_NAME;
        int parallelism = DEFAULT_PARALLELISM;
        
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("--pages") && i + 1 < args.length) {
                maxPages = Integer.parseInt(args[i + 1]);
                i++; // Consume value
            } else if (args[i].equals("--threads") && i + 1 < args.length) {
                parallelism = Integer.parseInt(args[i + 1]);
                i++; // Consume value
            } else if (args[i].equals("--file") && i + 1 < args.length) {
                fileName = args[i + 1];
                i++; // Consume value
            }
        }
        
        System.out.println("Running with " + parallelism + " threads, " + maxPages + " pages, file: " + fileName);
        long start = System.currentTimeMillis();
        ThreadMXBean bean = ManagementFactory.getThreadMXBean();
        long cpuTimeBefore = bean.getCurrentThreadCpuTime();
        Runtime runtime = Runtime.getRuntime();
        runtime.gc();
        long memoryBefore = runtime.totalMemory() - runtime.freeMemory();        Iterable<Page> iterablePages = new Pages(maxPages, fileName);
        List<Page> pages = new ArrayList<>();

        for (Page page : iterablePages) {
            if (page == null) continue;
            pages.add(page);
        }

        Map<String, Integer> result;
        ForkJoinPool forkJoinPool = new ForkJoinPool(parallelism);
        try {
            WordCountRecursiveTask task = new WordCountRecursiveTask(pages);
            result = forkJoinPool.invoke(task);
        } finally {
            forkJoinPool.shutdown();
        }
        long end = System.currentTimeMillis();
        long memoryAfter = runtime.totalMemory() - runtime.freeMemory();
        long cpuTimeAfter = bean.getCurrentThreadCpuTime();        System.out.println("Processed pages: " + pages.size());
        System.out.println("Elapsed time: " + (end - start) + "ms");
        System.out.println("Usage Memory: " + (memoryAfter - memoryBefore) + " bytes");
        System.out.println(String.format(Locale.US, "Usage Cpu Time %.8f seconds", (cpuTimeAfter - cpuTimeBefore) / 1_000_000_000.0));


        LinkedHashMap<String, Integer> commonWords = new LinkedHashMap<>();
        result.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .forEachOrdered(x -> commonWords.put(x.getKey(), x.getValue()));

        commonWords.entrySet().stream().limit(3).toList()
                .forEach(x -> System.out.println("Word: '" + x.getKey() + "' with total " + x.getValue() + " occurrences!"));
    }
}
