import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class WordCount3 {
    static final int maxPages = 100000;
    static final String fileName = "enwiki.xml";
    static final int NUMBER_THREADS = 30;

    private static final ConcurrentHashMap<String, Integer> counts = new ConcurrentHashMap<>();

    public static void main(String[] args) throws Exception {


        ExecutorService executor = Executors.newFixedThreadPool(NUMBER_THREADS);

        long start = System.currentTimeMillis();
        Iterable<Page3> pages = new Pages3(maxPages, fileName);
        int processedPages = 0;

        for (Page3 page : pages) {
            if (page == null) break;

            executor.submit(() -> {
             Iterable<String> words = new Words3(page.getText());
             for(String word : words) {
                 if (word.length() > 1 || word.equals("a") || word.equals("I")) {
                     countWord(word);
                 }
             }
                 System.out.println(Thread.currentThread().getName() + "a processar página: " + page.getTitle());
                 try {
                     Thread.sleep(100);
                 } catch (InterruptedException e) {
                     Thread.currentThread().interrupt();
                 }
             });
            processedPages++;
            }
        executor.shutdown();
        try {
            executor.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
            executor.shutdownNow();
        }


        long end = System.currentTimeMillis();

        System.out.println("Processed pages: " + processedPages);
        System.out.println("Elapsed time: " + (end - start) + "ms");

        LinkedHashMap<String, Integer> commonWords = new LinkedHashMap<>();
        counts.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .forEachOrdered(x -> commonWords.put(x.getKey(), x.getValue()));
        commonWords.entrySet().stream().limit(3).toList()
                .forEach(x -> System.out.println("Word: '" + x.getKey() + "' with total " + x.getValue() + " occurrences!"));
    }


    private static void countWord(String word) {
        counts.merge(word, 1, Integer::sum);
    }
}