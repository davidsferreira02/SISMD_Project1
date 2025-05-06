# Report

## Introduction

This project was developed as part of the Sistemas Multinúcleo e Distribuídos (SISMD)
course and focuses on exploring and analyzing different parallel and concurrent programming
techniques for efficiently processing large volumes of data on multicore systems.

This study implements and analyzes five distinct approaches to process a large Wikipedia data dump, measuring the frequency of words in English texts. By leveraging the parallel processing capabilities of modern multicore architectures, we aim to demonstrate significant performance improvements over traditional sequential methods. The implementations range from basic multithreading to sophisticated asynchronous programming models, each with distinct characteristics and performance implications.

The core task—counting word frequencies in large text corpora—represents a common pattern in data processing applications, while  simple, this problem becomes computationally intensive at scale, making it an ideal candidate for parallelization. Our analysis focuses not only on raw performance gains but also on the scalability, resource utilization, overhead and bottlenecks.

The project is structured into several sections, each detailing a specific implementation approach, its design considerations, and performance analysis. The report concludes with a summary of findings.

## Objectives

This project aims to implement and thoroughly analyze different approaches to efficiently process large volumes of textual data on multicore systems. By focusing on the task of word frequency analysis in a Wikipedia data dump, we seek to evaluate the performance characteristics of various concurrency models, identify their strengths and limitations, and compare them.

The project's objectives are divided into two main categories: implementation and analysis. Implementation objectives focus on developing and optimizing different concurrent solutions, while analysis objectives emphasize measuring performance, resource utilization, and scalability.

### Implementation Objectives

1. **Develop a baseline sequential solution** that processes the Wikipedia dataset without any parallelization, serving as a reference point for performance comparisons.

2. **Implement an explicit multithreaded solution** with manually managed threads, focusing on effective work distribution and proper synchronization mechanisms.

3. **Create a thread pool-based implementation** that efficiently manages thread lifecycles, reducing the overhead associated with thread creation and destruction.

4. **Design a solution using the Fork/Join framework** to utilize recursive task decomposition and work-stealing algorithms for balanced workload distribution.

5. **Develop an asynchronous implementation using CompletableFutures** to handle task dependencies and composition without explicit thread management.

6. **Apply and evaluate garbage collector tuning strategies** to optimize memory management and improve overall application performance.

### Analysis Objectives

1. **Measure and compare execution times** across all implementations to quantify performance improvements over the sequential baseline.

2. **Analyze resource utilization patterns** including CPU usage, memory consumption, and thread activity during execution.

3. **Evaluate scalability characteristics** by testing each implementation with varying dataset sizes and available processor cores.

4. **Identify performance bottlenecks and synchronization overhead** in each concurrent approach.

5. **Determine optimal concurrency strategies** for word frequency analysis based on different system configurations and dataset characteristics.

# Implementation Approach

## Sequential Implementation

The sequential implementation processes Wikipedia pages one-by-one, counting word occurrences using a single thread.

## Core Components

### 1. Variables

```java

static final int maxPages = 100000;
static final String fileName = "enwiki.xml";
private static final HashMap<String, Integer> counts = new HashMap<>();
```

This sets the maximum number of pages to process, the file name for the Wikipedia data dump, and the map for storing word counts.

### 2. Sequential Processing Loop

```java

for(Page page: pages) {
 if(page == null) break;
 Iterable<String> words = new Words(page.getText());
 for (String word: words)
   if(word.length()>1 || word.equals("a") || word.equals("I"))
     countWord(word);
 ++processedPages;    
}

```

This loop iterates through each Wikipedia page, extracts words, and counts them sequentially.

### 3. Word Counting

```java

 private static void countWord(String word) {
  counts.merge(word, 1, Integer::sum);
}
```

The merge method efficiently updates the count by either inserting a new word with count 1 or incrementing an existing word's count.

### 4. Results Processing

```java

LinkedHashMap<String, Integer> commonWords = new LinkedHashMap<>();
counts.entrySet().stream()
      .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
      .forEachOrdered(x -> commonWords.put(x.getKey(), x.getValue()));
```

This section sorts words by frequency and stores them in a LinkedHashMap to preserve order.

### 5. Performance Measurement

```java
long start = System.currentTimeMillis();
// Processing code here
long end = System.currentTimeMillis();
System.out.println("Elapsed time: " + (end - start) + "ms");
```

Simple timing mechanism to measure total execution time.

# Multithreaded(No Thread Pool) Implementation

This implementation uses **multithreading** to efficiently process a large Wikipedia XML dump file (`enwiki.xml`). It follows a **producer-consumer model** where a single thread reads pages and multiple threads process them in parallel to count word occurrences.

---

## Implementation Approach

### Multithreaded Design

Instead of sequentially processing pages, this implementation uses a **blocking queue** to decouple the reading (producer) and processing (consumers) tasks. This enables parallel execution and improves throughput on multi-core systems.



## Core Components

### 1. Configuration Variables

```java
static final int MAX_PAGES = 100000;
static final String FILE_NAME = "enwiki.xml";
static final int QUEUE_CAPACITY = 500;
static final int NUM_CONSUMERS = 30;
```

- `MAX_PAGES`: Maximum number of pages to process.
- `FILE_NAME`: Wikipedia XML file.
- `QUEUE_CAPACITY`: Size of the shared blocking queue.
- `NUM_CONSUMERS`: Number of consumer threads for parallel processing.



### 2. Shared Resources

```java
private static final BlockingQueue<Page> queue = new ArrayBlockingQueue<>(QUEUE_CAPACITY);
private static final AtomicBoolean producerDone = new AtomicBoolean(false);
private static final MapReduce mapReduce = new MapReduce();
```

- `queue`: Thread-safe queue to hold pages between producer and consumers.
- `producerDone`: Indicates when the producer has completed reading.
- `mapReduce`: Central class for counting words across all threads.



### 3. Producer Thread

```java
Thread producer = new Thread(() -> {
    Iterable<Page> pages = new Pages(MAX_PAGES, FILE_NAME);
    try {
        for (Page page : pages) {
            if (page == null) continue;
            queue.put(page);
            System.out.println("Producer added page: " + page.getTitle());
        }
    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
    } finally {
        producerDone.set(true);
    }
});
```

- Reads pages from the XML file.
- Puts each valid page into the blocking queue.
- Sets `producerDone = true` once finished.


### 4. Consumer Threads

```java
Thread consumer = new Thread(() -> {
    try {
        while (true) {
            if (producerDone.get() && queue.isEmpty()) break;

            Page page = queue.poll(100, TimeUnit.MILLISECONDS);
            if (page != null) {
                System.out.println(Thread.currentThread().getName() + " processing page: " + page.getTitle());
                mapReduce.map(page.getText());
            }
        }
    } catch (Exception e) {
        e.printStackTrace();
    }
});
```

- Continuously polls the queue.
- Processes page text with `mapReduce.map()` if available.
- Exits when producer is done and the queue is empty.



### 5. Word Counting Logic (MapReduce)

```java
public void map(String text) {
    for (String word : text.split("\\W+")) {
        if (!word.isEmpty()) {
            String lower = word.toLowerCase();
            wordCounts.merge(lower, 1, Integer::sum);
        }
    }
}
```

- Splits text using non-word characters.
- Converts to lowercase.
- Uses `ConcurrentHashMap.merge()` for thread-safe counting.



### 6. Results Aggregation

```java
public void printTopWords(int topN) {
    wordCounts.entrySet().stream()
        .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
        .limit(topN)
        .forEach(entry -> 
            System.out.println("Word: '" + entry.getKey() + "' occurred " + entry.getValue() + " times!"));
}
```

- Sorts all words by frequency.
- Displays the top N most frequent words.

# Multithreaded(Thread Pool) Implementation
This implementation processes a large number of Wikipedia pages using Java’s `ExecutorService` and a fixed thread pool to perform concurrent word counting.



## Implementation Approach

### Thread Pool Design

Instead of manually managing threads, this version uses a **thread pool** via `Executors.newFixedThreadPool()`. Each task submitted to the pool processes a Wikipedia page, extracting and counting words in parallel.



## Core Components

### 1. Configuration Variables

```java
static final int MAX_PAGES = 100000;
static final String FILE_NAME = "enwiki.xml";
static final int NUM_THREADS = 30;
```

- `MAX_PAGES`: Maximum number of pages to process.
- `FILE_NAME`: Wikipedia XML dump file.
- `NUM_THREADS`: Number of worker threads in the thread pool.





### 2. Executor Service Setup

```java
ExecutorService executor = Executors.newFixedThreadPool(NUM_THREADS);
```

- Creates a fixed-size thread pool to manage worker threads.
- Allows concurrent execution of submitted tasks without manually managing thread lifecycle.



### 3. Page Processing Loop

```java
Iterable<Page> pages = new Pages(MAX_PAGES, FILE_NAME);

for (Page page : pages) {
    if (page == null) continue;
    executor.execute(() -> mapReduce.map(page.getText()));
}
```

- Iterates through all the pages from the file.
- Each page is submitted to the executor for parallel processing.
- `mapReduce.map()` is the method that extracts and counts words in a thread-safe way.



### 4. Graceful Shutdown

```java
executor.shutdown();
try {
    if (!executor.awaitTermination(1, TimeUnit.HOURS)) {
        executor.shutdownNow();
    }
} catch (InterruptedException e) {
    executor.shutdownNow();
    Thread.currentThread().interrupt();
}
```

- Signals the executor to stop accepting new tasks.
- Waits up to 1 hour for all tasks to finish.
- Ensures clean shutdown even if interrupted.


# Fork/Join Implementation
