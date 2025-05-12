import common.Page;
import common.Pages;
import common.Words;

import java.lang.management.ThreadMXBean;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;



public class Sequential {
    static final int maxPages = 100000;
    static final String fileName = "enwiki.xml";

    private static final HashMap<String, Integer> counts =
            new HashMap<>();

    public static void main(String[] args){

        long start = System.currentTimeMillis();
        ThreadMXBean bean = ManagementFactory.getThreadMXBean();
        long cpuTimeBefore = bean.getCurrentThreadCpuTime();
        Runtime runtime = Runtime.getRuntime();
        runtime.gc();
        long memoryBefore = runtime.totalMemory() - runtime.freeMemory();
        Iterable<Page> pages = new Pages(maxPages, fileName);
        int processedPages = 0;
        for(Page page: pages) {
            if(page == null)
                break;
            Iterable<String> words = new Words(page.getText());
            for (String word: words)
                if(word.length()>1 || word.equals("a") || word.equals("I"))
                    countWord(word);
            ++processedPages;
        }
        long end = System.currentTimeMillis();
        long memoryAfter = runtime.totalMemory() - runtime.freeMemory();
        long cpuTimeAfter = bean.getCurrentThreadCpuTime();
        System.out.println("Processed pages: " + processedPages);
        System.out.println("Elapsed time: " + (end - start) + "ms");
        System.out.println("Usage Memory: " + (memoryAfter - memoryBefore) + " bytes");
        System.out.println("Usage Cpu Time  " + (cpuTimeAfter - cpuTimeBefore) / 1_000_000_000.0 + " seconds");

        LinkedHashMap<String, Integer> commonWords = new LinkedHashMap<>();
        counts.entrySet().stream().sorted(Map.Entry.comparingByValue(Comparator.reverseOrder())) .forEachOrdered(x -> commonWords.put(x.getKey(), x.getValue()));
        commonWords.entrySet().stream().limit(3).toList().forEach(x -> System.out.println("Word: '" +x.getKey()+ "' with total " +x.getValue()+" occurrences!"));
    }

    private static void countWord(String word) {
        counts.merge(word, 1, Integer::sum);
    }
}