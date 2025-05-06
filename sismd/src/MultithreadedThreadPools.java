import common.Page;
import common.Pages;
import common.Words;

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

        mapReduce.printTopWords(3);
        System.out.println("Elapsed time: " + (end - start) + "ms");
    }
}
