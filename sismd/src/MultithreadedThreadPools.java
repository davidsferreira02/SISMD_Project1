import common.Page;
import common.Pages;
import common.Words;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import common.MapReduce;
import common.Page;
import common.Pages;

import java.util.*;
import java.util.concurrent.*;

public class MultithreadedThreadPools {
    static final int MAX_PAGES = 100000;
    static final String FILE_NAME = "enwiki.xml";
    static final int NUM_THREADS = 30;

    private static final MapReduce mapReduce = new MapReduce();

    public static void main(String[] args) {
        long start = System.currentTimeMillis();
        ThreadMXBean bean = ManagementFactory.getThreadMXBean();
        long cpuTimeBefore = bean.getCurrentThreadCpuTime();
        Runtime runtime = Runtime.getRuntime();
        runtime.gc();
        long memoryBefore = runtime.totalMemory() - runtime.freeMemory();

        ExecutorService executor = Executors.newFixedThreadPool(NUM_THREADS);
        Iterable<Page> pages = new Pages(MAX_PAGES, FILE_NAME);

        for (Page page : pages) {
            if (page == null) continue;

            executor.execute(() -> mapReduce.map(page.getText()));
        }

        executor.shutdown();
        try {
            if (!executor.awaitTermination(1, TimeUnit.HOURS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }


        long end = System.currentTimeMillis();



        long memoryAfter = runtime.totalMemory() - runtime.freeMemory();
        long cpuTimeAfter = bean.getCurrentThreadCpuTime();
        System.out.println("Elapsed time: " + (end - start) + "ms");
        System.out.println("Usage Memory: " + (memoryAfter - memoryBefore) + " bytes");
        System.out.println("Usage Cpu Time  " + (cpuTimeAfter - cpuTimeBefore) / 1_000_000_000.0 + " seconds");
        mapReduce.printTopWords(3);
    }
}
