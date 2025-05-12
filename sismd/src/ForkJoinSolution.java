import common.Page;
import common.Pages;
import common.WordCountRecursiveTask;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.*;
import java.util.concurrent.ForkJoinPool;

public class ForkJoinSolution {

    static final int maxPages = 100000;
    static final String fileName = "enwiki.xml";

    public static void main(String[] args){
        long start = System.currentTimeMillis();
        ThreadMXBean bean = ManagementFactory.getThreadMXBean();
        long cpuTimeBefore = bean.getCurrentThreadCpuTime();
        Runtime runtime = Runtime.getRuntime();
        runtime.gc();
        long memoryBefore = runtime.totalMemory() - runtime.freeMemory();


        Iterable<Page> iterablePages = new Pages(maxPages, fileName);
        List<Page> pages = new ArrayList<>();

        for (Page page : iterablePages) {
            if (page == null) continue;
            pages.add(page);
        }

        Map<String, Integer> result;
        ForkJoinPool forkJoinPool = new ForkJoinPool();
        WordCountRecursiveTask task = new WordCountRecursiveTask(pages);
        result = forkJoinPool.invoke(task);
        long end = System.currentTimeMillis();
        long memoryAfter = runtime.totalMemory() - runtime.freeMemory();
        long cpuTimeAfter = bean.getCurrentThreadCpuTime();

        System.out.println("Processed pages: " + pages.size());
        System.out.println("Elapsed time: " + (end - start) + "ms");
        System.out.println("Usage Memory: " + (memoryAfter - memoryBefore) + " bytes");
        System.out.println("Usage Cpu Time  " + (cpuTimeAfter - cpuTimeBefore) / 1_000_000_000.0 + " seconds");


        LinkedHashMap<String, Integer> commonWords = new LinkedHashMap<>();
        result.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .forEachOrdered(x -> commonWords.put(x.getKey(), x.getValue()));

        commonWords.entrySet().stream().limit(3).toList()
                .forEach(x -> System.out.println("Word: '" + x.getKey() + "' with total " + x.getValue() + " occurrences!"));
    }
}
