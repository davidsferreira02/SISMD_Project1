import common.Page;
import common.Pages;
import common.Words;

import java.util.concurrent.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class Multithreaded {
    static final int MAX_PAGES = 100000;
    static final String FILE_NAME = "enwiki.xml";
    static final int QUEUE_CAPACITY = 500;
    static final int NUM_CONSUMERS = 30;

    private static final BlockingQueue<Page> queue = new ArrayBlockingQueue<>(QUEUE_CAPACITY);
    private static final ConcurrentHashMap<String, Integer> counts = new ConcurrentHashMap<>();
    private static final AtomicBoolean producerDone = new AtomicBoolean(false);

    public static void main(String[] args) {
        long start = System.currentTimeMillis();

        // Producer thread
        Thread producer = new Thread(() -> {
            Iterable<Page> pages = new Pages(MAX_PAGES, FILE_NAME);
            try {
                for (Page page : pages) {
                    if (page == null) break;
                    queue.put(page); // blocks if queue is full
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

                        Page page = null;
                        try {
                            page = queue.take(); // blocks until page is available
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            break;
                        }

                        if (page != null) {
                            Iterable<String> words = new Words(page.getText());
                            for (String word : words) {
                                if (word.length() > 1 || word.equals("a") || word.equals("I")) {
                                    counts.merge(word, 1, Integer::sum);
                                }
                            }
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

        // Wait for  threads to finish
        try {
            producer.join();
            for (Thread consumer : consumers) {
                consumer.join();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        long end = System.currentTimeMillis();
        System.out.println("Elapsed time: " + (end - start) + "ms");

        counts.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(3)
                .forEach(entry -> System.out.println(
                        "Word: '" + entry.getKey() + "' with total " + entry.getValue() + " occurrences!"
                ));
    }
}
