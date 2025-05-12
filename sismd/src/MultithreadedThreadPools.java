import common.Page;
import common.Pages;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.Locale;

import common.MapReduce;

public class MultithreadedThreadPools {
    static final int DEFAULT_MAX_PAGES = 100000;
    static final String DEFAULT_FILE_NAME = "enwiki.xml";
    static final int DEFAULT_NUM_THREADS = 500;

    private static final MapReduce mapReduce = new MapReduce();

    public static void main(String[] args) {

        int maxPages = DEFAULT_MAX_PAGES;
        String fileName = DEFAULT_FILE_NAME;
        int numThreads = DEFAULT_NUM_THREADS;
        
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("--pages") && i + 1 < args.length) {
                maxPages = Integer.parseInt(args[i + 1]);
                i++; // Consume value
            } else if (args[i].equals("--threads") && i + 1 < args.length) {
                numThreads = Integer.parseInt(args[i + 1]);
                i++; // Consume value
            } else if (args[i].equals("--file") && i + 1 < args.length) {
                fileName = args[i + 1];
                i++; // Consume value
            }
        }

        long start = System.currentTimeMillis();
        ThreadMXBean bean = ManagementFactory.getThreadMXBean();
        long cpuTimeBefore = bean.getCurrentThreadCpuTime();
        Runtime runtime = Runtime.getRuntime();
        runtime.gc();
        long memoryBefore = runtime.totalMemory() - runtime.freeMemory();

        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        Iterable<Page> pages = new Pages(maxPages, fileName);

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
        }        long end = System.currentTimeMillis();

        long memoryAfter = runtime.totalMemory() - runtime.freeMemory();
        long cpuTimeAfter = bean.getCurrentThreadCpuTime();
        System.out.println("Elapsed time: " + (end - start) + "ms");
        System.out.println("Usage Memory: " + (memoryAfter - memoryBefore) + " bytes");
        System.out.println(String.format(Locale.US, "Usage Cpu Time %.8f seconds", (cpuTimeAfter - cpuTimeBefore) / 1_000_000_000.0));
        mapReduce.printTopWords(3);
    }
}
