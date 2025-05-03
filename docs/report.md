# Report
## Introduction
This project was developed as part of the Sistemas Multinúcleo e Distribuídos (SISMD) 
course and focuses on exploring and analyzing different parallel and concurrent programming 
techniques for efficiently processing large volumes of data on multicore systems. 

This study implements and analyzes five distinct approaches to process a large Wikipedia data dump, measuring the frequency of words in English texts, by leveraging the parallel processing capabilities of modern multicore architectures, we aim to demonstrate significant performance improvements over traditional sequential methods. The implementations range from basic multithreading to sophisticated asynchronous programming models, each with distinct characteristics and performance implications.

The core task—counting word frequencies in large text corpora—represents a common pattern in data processing applications, while  simple, this problem becomes computationally intensive at scale, making it an ideal candidate for parallelization. Our analysis focuses not only on raw performance gains but also on the scalability, resource utilization, and implementation complexity of each approach.

## Objectives

This project aims to implement and thoroughly analyze different approaches to efficiently process large volumes of textual data on multicore systems, by focusing on the task of word frequency analysis in a Wikipedia data dump, we seek to evaluate the performance characteristics of various concurrency models and identify their strengths and limitations. The specific objectives of this project are:

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

5. **Assess implementation complexity and maintainability** to understand the trade-offs between performance gains and development effort.

6. **Determine optimal concurrency strategies** for word frequency analysis based on different system configurations and dataset characteristics.


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


### 2. Main Processing Loop

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

