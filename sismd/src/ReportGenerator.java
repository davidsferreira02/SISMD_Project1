import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ReportGenerator {
    public static void main(String[] args) throws IOException {
        String csvPath = "benchmark_results.csv";
        List<String> lines = Files.readAllLines(Paths.get(csvPath));
        if (lines.size() < 2) {
            System.out.println("No benchmark data found.");
            return;
        }
        String[] headers = lines.get(0).split(",");
        List<Map<String, String>> records = new ArrayList<>();
        for (int i = 1; i < lines.size(); i++) {
            String[] cols = lines.get(i).split(",");
            Map<String, String> map = new LinkedHashMap<>();
            for (int j = 0; j < headers.length; j++) {
                map.put(headers[j], cols[j]);
            }
            records.add(map);
        }
        StringBuilder tableRows = new StringBuilder();
        for (Map<String, String> rec : records) {
            tableRows.append("<tr>");
            for (String h : headers) {
                tableRows.append("<td>").append(rec.get(h)).append("</td>");
            }
            tableRows.append("</tr>\n");
        }
        // Prepare data for chart: ExecutionTime vs ThreadCount per implementation
        StringBuilder chartData = new StringBuilder();
        chartData.append("const raw = ");
        chartData.append(toJson(records));
        chartData.append(";\n");
        String html = "<!DOCTYPE html>\n" +
                "<html><head><meta charset='utf-8'><title>Benchmark Report</title>\n" +
                "<script src='https://cdn.jsdelivr.net/npm/chart.js'></script></head><body>\n" +
                "<h1>Benchmark Results</h1>\n" +
                "<h2>Data Table</h2>\n" +
                "<table border='1'><thead><tr>";
        for (String h : headers)
            html += "<th>" + h + "</th>";
        html += "</tr></thead><tbody>" + tableRows + "</tbody></table>\n" +
                "<h2>Execution Time vs Thread Count</h2>\n" +
                "<canvas id='execChart' width='800' height='400'></canvas>\n" +
                "<script>\n" + chartData.toString() +
                "// process data for chart\n" +
                "const impls = [...new Set(raw.map(r=>r.Implementation))];\n" +
                "const datasets = impls.map(impl=>{\n" +
                "  const data = raw.filter(r=>r.Implementation===impl).map(r=>({x:+r.ThreadCount,y:+r.ExecutionTimeMs}));\n"
                +
                "  return {label:impl,data:data,fill:false,borderWidth:2};\n" +
                "});\n" +
                "new Chart(document.getElementById('execChart').getContext('2d'),{type:'line',data:{datasets:datasets},options:{scales:{x:{type:'linear',title:{display:true,text:'Threads'}},y:{title:{display:true,text:'Time (ms)'}}}}});\n"
                +
                "</script>\n" +
                "<h2>Memory Usage vs Thread Count</h2>\n" +
                "<canvas id='memChart' width='800' height='400'></canvas>\n" +
                "<script>\n" +
                "const memDatasets = impls.map(impl=>{ const data = raw.filter(r=>r.Implementation===impl).map(r=>({x:+r.ThreadCount,y:+r.MemoryUsedMB})); return {label:impl,data:data,fill:false,borderWidth:2}; });\n"
                +
                "new Chart(document.getElementById('memChart').getContext('2d'),{type:'line',data:{datasets:memDatasets},options:{scales:{x:{type:'linear',title:{display:true,text:'Threads'}},y:{title:{display:true,text:'Memory Used (MB)'}}}}});\n"
                +
                "</script>\n" +
                "<h2>CPU Load End vs Thread Count</h2>\n" +
                "<canvas id='cpuChart' width='800' height='400'></canvas>\n" +
                "<script>\n" +
                "const cpuDatasets = impls.map(impl=>{ const data = raw.filter(r=>r.Implementation===impl).map(r=>({x:+r.ThreadCount,y:+r.EndCpuLoadPct})); return {label:impl,data:data,fill:false,borderWidth:2}; });\n"
                +
                "new Chart(document.getElementById('cpuChart').getContext('2d'),{type:'line',data:{datasets:cpuDatasets},options:{scales:{x:{type:'linear',title:{display:true,text:'Threads'}},y:{title:{display:true,text:'End CPU Load (%)'}}}}});\n"
                +
                "</script>\n" +
                "</body></html>";
        Files.write(Paths.get("benchmark_report.html"), html.getBytes());
        System.out.println("Generated benchmark_report.html");
    }

    // Simple manual JSON serializer for our records
    private static String toJson(List<Map<String, String>> records) {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        boolean firstRec = true;
        for (Map<String, String> rec : records) {
            if (!firstRec)
                sb.append(",");
            firstRec = false;
            sb.append("{");
            boolean firstField = true;
            for (Map.Entry<String, String> e : rec.entrySet()) {
                if (!firstField)
                    sb.append(",");
                firstField = false;
                sb.append("\"").append(e.getKey()).append("\":\"").append(e.getValue()).append("\"");
            }
            sb.append("}");
        }
        sb.append("]");
        return sb.toString();
    }
}