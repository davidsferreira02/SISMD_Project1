import common.Page;
import common.Pages;
import common.WordCountRecursiveTask;

import java.util.*;
import java.util.concurrent.ForkJoinPool;

public class ForkJoinSolution {

    static final int maxPages = 100000;
    static final String fileName = "enwiki.xml";

    public static void main(String[] args){
        Benchmark benchmark = new Benchmark();
        benchmark.start();


        Iterable<Page> iterablePages = new Pages(maxPages, fileName);
        List<Page> pages = new ArrayList<>();

        for (Page page : iterablePages) {
            if (page == null) break;
            pages.add(page);
        }

        Map<String, Integer> result;
        ForkJoinPool forkJoinPool = new ForkJoinPool();
        WordCountRecursiveTask task = new WordCountRecursiveTask(pages);
        result = forkJoinPool.invoke(task);
        
        benchmark.end();
        benchmark.printResults("ForkJoin", maxPages, 1);


        System.out.println("Processed pages: " + pages.size());
        


        LinkedHashMap<String, Integer> commonWords = new LinkedHashMap<>();
        result.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .forEachOrdered(x -> commonWords.put(x.getKey(), x.getValue()));

        commonWords.entrySet().stream().limit(3).toList()
                .forEach(x -> System.out.println("Word: '" + x.getKey() + "' with total " + x.getValue() + " occurrences!"));
    }
}
