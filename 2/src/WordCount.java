import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;

public class WordCount {
    static final int maxPages = 100000;
    static final String fileName = "enwiki.xml";
    static final int NUMBER_THREADS = 30;

    private static final ConcurrentHashMap<String, Integer> counts = new ConcurrentHashMap<>();

    public static void main(String[] args) throws Exception {
        Thread[] threads = new Thread[NUMBER_THREADS];

        long start = System.currentTimeMillis();
        Iterable<Page> pages = new Pages(maxPages, fileName);
        int processedPages = 0;

        for (Page page : pages) {
            if (page == null) break;

            boolean assigned = false;

            while (!assigned) {
                for (int i = 0; i < NUMBER_THREADS; i++) {
                    if (threads[i] == null || !threads[i].isAlive()) {

                        threads[i] = new Thread(() -> {
                            Iterable<String> words = new Words(page.getText());
                            for (String word : words) {
                                if (word.length() > 1 || word.equals("a") || word.equals("I")) {
                                    countWord(word);
                                }
                            }
                            try {
                                Thread.sleep(5);
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                            }
                        }, "Thread-" + i);
                        System.out.println(threads[i].getName() + " a processar: " + page.getTitle() + "with text:" + page.getText());
                        threads[i].start();
                        assigned = true;
                        break;
                    }
                }
            }

            processedPages++;
        }


        for (Thread t : threads) {
            if (t != null) {
                t.join();
            }
        }

        long end = System.currentTimeMillis();

        System.out.println("Processed pages: " + processedPages);
        System.out.println("Elapsed time: " + (end - start) + "ms");

        LinkedHashMap<String, Integer> commonWords = new LinkedHashMap<>();
        counts.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .forEachOrdered(x -> commonWords.put(x.getKey(), x.getValue()));
        commonWords.entrySet().stream().limit(3).collect(Collectors.toList())
                .forEach(x -> System.out.println("Word: '" + x.getKey() + "' with total " + x.getValue() + " occurrences!"));
    }

    // merge é thread safe
    private static void countWord(String word) {
        counts.merge(word, 1, Integer::sum);
    }
}