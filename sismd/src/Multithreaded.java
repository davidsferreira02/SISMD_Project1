import common.MapReduce;
import common.Page;
import common.Pages;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.concurrent.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.Locale;

public class Multithreaded {
    static final int DEFAULT_MAX_PAGES = 100000;
    static final String DEFAULT_FILE_NAME = "enwiki.xml";
    static final int DEFAULT_NUM_CONSUMERS = 500;
    static final int QUEUE_CAPACITY = 500;

    private static final MapReduce mapReduce = new MapReduce();

    public static void main(String[] args) {
        int maxPages = DEFAULT_MAX_PAGES;
        String fileName = DEFAULT_FILE_NAME;
        int numConsumers = DEFAULT_NUM_CONSUMERS;
        
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("--pages") && i + 1 < args.length) {
                maxPages = Integer.parseInt(args[i + 1]);
                i++; // Consume value
            } else if (args[i].equals("--threads") && i + 1 < args.length) {
                numConsumers = Integer.parseInt(args[i + 1]);
                i++; // Consume value
            } else if (args[i].equals("--file") && i + 1 < args.length) {
                fileName = args[i + 1];
                i++; // Consume value
            }
        }
        
        System.out.println("Running with " + numConsumers + " threads, " + maxPages + " pages, file: " + fileName);

        BlockingQueue<Page> queue = new ArrayBlockingQueue<>(QUEUE_CAPACITY);
        AtomicBoolean producerDone = new AtomicBoolean(false);
        
        long start = System.currentTimeMillis();
        ThreadMXBean bean = ManagementFactory.getThreadMXBean();
        long cpuTimeBefore = bean.getCurrentThreadCpuTime();
        Runtime runtime = Runtime.getRuntime();
        runtime.gc();
        long memoryBefore = runtime.totalMemory() - runtime.freeMemory();

        // Producer thread - using final copies of variables for lambda expressions
        final int finalMaxPages = maxPages;
        final String finalFileName = fileName;
        Thread producer = new Thread(() -> {
            Iterable<Page> pages = new Pages(finalMaxPages, finalFileName);
            try {
                for (Page page : pages) {
                    if (page == null) continue;
                    queue.put(page);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                producerDone.set(true);
            }
        });

        // Consumer threads
        List<Thread> consumers = new ArrayList<>();
        for (int i = 0; i < numConsumers; i++) {
            Thread consumer = new Thread(() -> {
                try {
                    while (true) {
                        if (producerDone.get() && queue.isEmpty()) break;

                        Page page;
                        try {
                            page = queue.poll(100, TimeUnit.MILLISECONDS);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            break;
                        }

                        if (page != null) {
                            mapReduce.map(page.getText());
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            consumers.add(consumer);
        }

        producer.start();
        consumers.forEach(Thread::start);

        // Wait for all threads to finish
        try {
            producer.join();
            for (Thread consumer : consumers) {
                consumer.join();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }        long end = System.currentTimeMillis();
        long memoryAfter = runtime.totalMemory() - runtime.freeMemory();
        long cpuTimeAfter = bean.getCurrentThreadCpuTime();
        
        // Use Locale.US to ensure decimal point is '.' not ','
        System.out.println("Elapsed time: " + (end - start) + "ms");
        System.out.println("Usage Memory: " + (memoryAfter - memoryBefore) + " bytes");
        System.out.println(String.format(Locale.US, "Usage Cpu Time %.8f seconds", (cpuTimeAfter - cpuTimeBefore) / 1_000_000_000.0));

        mapReduce.printTopWords(3);
    }
}
