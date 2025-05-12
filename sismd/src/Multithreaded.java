import common.MapReduce;
import common.Page;
import common.Pages;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.concurrent.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class Multithreaded {
    static final int MAX_PAGES = 100000;
    static final String FILE_NAME = "enwiki.xml";
    static final int QUEUE_CAPACITY = 500;
    static final int NUM_CONSUMERS = 30;

    private static final BlockingQueue<Page> queue = new ArrayBlockingQueue<>(QUEUE_CAPACITY);
    private static final AtomicBoolean producerDone = new AtomicBoolean(false);

    private static final MapReduce mapReduce = new MapReduce();

    public static void main(String[] args) {
        long start = System.currentTimeMillis();
        ThreadMXBean bean = ManagementFactory.getThreadMXBean();
        long cpuTimeBefore = bean.getCurrentThreadCpuTime();
        Runtime runtime = Runtime.getRuntime();
        runtime.gc();
        long memoryBefore = runtime.totalMemory() - runtime.freeMemory();

        // Producer thread
        Thread producer = new Thread(() -> {
            Iterable<Page> pages = new Pages(MAX_PAGES, FILE_NAME);
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
        for (int i = 0; i < NUM_CONSUMERS; i++) {
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
