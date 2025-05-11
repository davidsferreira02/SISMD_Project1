import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.lang.management.ManagementFactory;
import com.sun.management.OperatingSystemMXBean;

// Utility class for benchmarking execution time, CPU, and memory usage
public class Benchmark {
    private long startTime;
    private long endTime;
    private long startUsedMemory;
    private long endUsedMemory;
    private int availableProcessors;
    private double startCpuLoad;
    private double endCpuLoad;
    private OperatingSystemMXBean osBean;

    public void start() {
        System.gc();
        osBean = ManagementFactory.getPlatformMXBean(OperatingSystemMXBean.class);
        startCpuLoad = osBean.getCpuLoad();
        startTime = System.currentTimeMillis();
        startUsedMemory = getUsedMemory();
        availableProcessors = Runtime.getRuntime().availableProcessors();
    }

    public void end() {
        endTime = System.currentTimeMillis();
        endUsedMemory = getUsedMemory();
        endCpuLoad = osBean.getCpuLoad();
    }

    private long getUsedMemory() {
        Runtime runtime = Runtime.getRuntime();
        return runtime.totalMemory() - runtime.freeMemory();
    }

    public void printResults(String implementation, int datasetSize, int threadCount) {
        System.out.println("--- Benchmark Results ---");
        System.out.println("Implementation: " + implementation);
        System.out.println("Dataset size: " + datasetSize);
        System.out.println("Thread count: " + threadCount);
        System.out.println("Available processors: " + availableProcessors);
        System.out.println("Execution time (ms): " + (endTime - startTime));
        System.out.println("Memory used (MB): " + ((endUsedMemory - startUsedMemory) / (1024 * 1024)));
        System.out.println("CPU load start (%): " + String.format("%.2f", startCpuLoad * 100));
        System.out.println("CPU load end   (%): " + String.format("%.2f", endCpuLoad * 100));
        System.out.println("-------------------------");
        writeCsv(implementation, datasetSize, threadCount);
    }

    private void writeCsv(String implementation, int datasetSize, int threadCount) {
        Path path = Paths.get("benchmark_results.csv");
        boolean newFile = !Files.exists(path);
        try (BufferedWriter writer = Files.newBufferedWriter(path,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
            if (newFile) {
                writer.write(
                        "Implementation,DatasetSize,ThreadCount,ExecutionTimeMs,MemoryUsedMB,StartCpuLoadPct,EndCpuLoadPct");
                writer.newLine();
            }
            String line = String.format(
                    "%s,%d,%d,%d,%.2f,%.2f,%.2f",
                    implementation,
                    datasetSize,
                    threadCount,
                    (endTime - startTime),
                    (endUsedMemory - startUsedMemory) / (1024.0 * 1024.0),
                    startCpuLoad * 100,
                    endCpuLoad * 100);
            writer.write(line);
            writer.newLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
