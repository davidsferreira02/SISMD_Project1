import pandas as pd
import matplotlib.pyplot as plt
import seaborn as sns
import numpy as np
import os
import pathlib

def fix_csv_format(csv_path):
    """Fix common issues with the CSV file format"""
    fixed_csv_path = csv_path.replace('.csv', '_fixed.csv')
    
    # Read the original CSV file as text
    with open(csv_path, 'r') as f:
        lines = f.readlines()
    
    # Fix header and write to a new file
    with open(fixed_csv_path, 'w') as f:
        # Write header
        f.write("Implementation,Threads,FileSize,RunNumber,ElapsedTime,MemoryUsage,CPUTime\n")
        
        # Skip first line (header) and process data rows
        for line in lines[1:]:
            # Replace commas in decimal numbers with periods
            parts = line.strip().split(',')
            if len(parts) >= 7:  # Only process valid rows
                # Implementation, Threads, FileSize, RunNumber should be as is
                fixed_line = ','.join(parts[:4])
                
                # Handle remaining fields (convert decimal commas if any)
                for i in range(4, min(7, len(parts))):
                    # Replace any decimal commas with periods (e.g., "1,23" -> "1.23")
                    val = parts[i].replace(',', '.').strip()
                    # If empty, replace with "0"
                    if not val:
                        val = "0"
                    fixed_line += ',' + val
                
                # Make sure we have all fields
                while len(fixed_line.split(',')) < 7:
                    fixed_line += ',0'
                
                f.write(fixed_line + '\n')
    
    return fixed_csv_path

def main():
    # Create output directories for charts and tables
    output_dir = pathlib.Path('output')
    charts_dir = output_dir / 'charts'
    tables_dir = output_dir / 'tables'
    
    # Create directories if they don't exist
    for directory in [output_dir, charts_dir, tables_dir]:
        directory.mkdir(exist_ok=True)
        print(f"Created directory: {directory}")
        
    # Fix the CSV format first
    csv_path = 'benchmark_results.csv'
    if os.path.exists(csv_path):
        fixed_csv_path = fix_csv_format(csv_path)
        print(f"Created fixed CSV file: {fixed_csv_path}")
    else:
        print(f"Warning: CSV file {csv_path} not found!")
        return
    
    # Load benchmark results from the fixed CSV
    try:
        df = pd.read_csv(fixed_csv_path)
        print("CSV loaded successfully")
    except Exception as e:
        print(f"Error loading CSV: {e}")
        return
    
    # Make sure column names are stripped of spaces
    df.columns = [col.strip() for col in df.columns]
    
    # Convert to appropriate data types, handling missing values
    for col in ['ElapsedTime', 'MemoryUsage', 'CPUTime']:
        if col in df.columns:
            df[col] = pd.to_numeric(df[col], errors='coerce')
        else:
            print(f"Warning: Column {col} not found in CSV!")
    
    # Filter out rows with NaN values
    df_clean = df.dropna()
    if len(df_clean) < len(df):
        print(f"Filtered out {len(df) - len(df_clean)} rows with missing values")
    
    # Handle having at least one valid row
    if len(df_clean) == 0:
        print("No valid data rows found after filtering!")
        return
    
    # Group by implementation, threads, filesize and calculate mean and std across runs
    grouped = df_clean.groupby(['Implementation', 'Threads', 'FileSize']).agg({
        'ElapsedTime': ['mean', 'std'],
        'MemoryUsage': ['mean', 'std'],
        'CPUTime': ['mean', 'std']
    }).reset_index()
    
    # Flatten the column hierarchy
    grouped.columns = ['_'.join(col).strip('_') for col in grouped.columns.values]
    
    # Convert CPU time from seconds to milliseconds for better readability
    grouped['CPUTime_mean'] = grouped['CPUTime_mean'] * 1000
    grouped['CPUTime_std'] = grouped['CPUTime_std'] * 1000
    
    # Generate combined execution time vs. thread count charts
    plot_combined_scalability_charts(grouped, 'ElapsedTime_mean', 'Execution Time (ms)', 'ElapsedTime')
    
    # Generate combined memory usage vs. thread count charts
    plot_combined_scalability_charts(grouped, 'MemoryUsage_mean', 'Memory Usage (bytes)', 'MemoryUsage')
    
    # Generate combined CPU time vs. thread count charts
    plot_combined_scalability_charts(grouped, 'CPUTime_mean', 'CPU Time (ms)', 'CPUTime')
    
    # Generate speedup charts
    plot_speedup_charts(grouped)
    
    # Generate efficiency charts
    plot_efficiency_charts(grouped)
    
    # Generate tables
    generate_tables(grouped)
    
    print("Analysis completed successfully!")

def plot_combined_scalability_charts(df, metric, ylabel, output_filename_prefix):
    """Plots a combined chart for a given metric vs. number of threads,
    with different implementations as hues and file sizes as styles."""
    plt.figure(figsize=(14, 8))  # Adjusted for legend
    
    sns.lineplot(
        data=df,
        x='Threads',
        y=metric,
        hue='Implementation',
        style='FileSize',
        markers=True,
        dashes=True  # Ensure different styles for FileSize if not automatic
    )
    
    # Ensure x-ticks show actual thread counts used
    unique_threads = sorted(df['Threads'].unique().tolist())
    if unique_threads: # Add check for empty list to avoid error on empty df
        plt.xticks(unique_threads)
        
    plt.xlabel('Number of Threads')
    plt.ylabel(ylabel)
    plt.title(f'{ylabel} vs. Number of Threads (All Implementations)')
    
    # Adjust legend to be outside the plot
    plt.legend(title='Legend (Implementation / Pages)', bbox_to_anchor=(1.05, 1), loc='upper left')
    
    plt.grid(True)
    # Adjust layout to make space for the legend
    plt.tight_layout(rect=[0, 0, 0.85, 1]) 
    
    filename = f'{output_filename_prefix}_vs_threads_combined.png'
    filepath = pathlib.Path('output/charts') / filename
    plt.savefig(filepath)
    plt.close()  # Close the figure to free up memory
    print(f"Saved {filepath}")
    
def plot_speedup_charts(df):
    plt.figure(figsize=(12, 6))
    
    for filesize in df['FileSize'].unique():
        # Check if Sequential implementation exists for this filesize
        if not df[(df['Implementation'] == 'Sequential') & (df['FileSize'] == filesize)].empty:
            # Get sequential baseline for this filesize
            sequential = df[(df['Implementation'] == 'Sequential') & (df['FileSize'] == filesize)]['ElapsedTime_mean'].iloc[0]
            
            for impl in [i for i in df['Implementation'].unique() if i != 'Sequential']:
                data = df[(df['Implementation'] == impl) & (df['FileSize'] == filesize)]
                if not data.empty:
                    speedup = sequential / data['ElapsedTime_mean']
                    plt.plot(data['Threads'], speedup, marker='o', label=f'{impl} - {filesize} pages')
        else:
            print(f"Warning: No Sequential implementation data for filesize {filesize}")
    
    # Using integer scale instead of log scale
    plt.xticks(sorted(df['Threads'].unique().tolist()))  # Set x-ticks to exact thread values
    plt.xlabel('Number of Threads')
    plt.ylabel('Speedup vs Sequential')
    plt.title('Speedup vs Thread Count')
    plt.legend()
    plt.grid(True)
    plt.tight_layout()
    
    # Save to output/charts directory
    filepath = pathlib.Path('output/charts') / 'speedup_vs_threads.png'
    plt.savefig(filepath)
    plt.close() # Close the figure
    print(f"Saved {filepath}")
    
def plot_efficiency_charts(df):
    plt.figure(figsize=(12, 6))
    
    for filesize in df['FileSize'].unique():
        # Check if Sequential implementation exists for this filesize
        if not df[(df['Implementation'] == 'Sequential') & (df['FileSize'] == filesize)].empty:
            # Get sequential baseline for this filesize
            sequential = df[(df['Implementation'] == 'Sequential') & (df['FileSize'] == filesize)]['ElapsedTime_mean'].iloc[0]
            
            for impl in [i for i in df['Implementation'].unique() if i != 'Sequential']:
                data = df[(df['Implementation'] == impl) & (df['FileSize'] == filesize)]
                if not data.empty:
                    speedup = sequential / data['ElapsedTime_mean']
                    efficiency = speedup / data['Threads']
                    plt.plot(data['Threads'], efficiency, marker='o', label=f'{impl} - {filesize} pages')
        else:
            print(f"Warning: No Sequential implementation data for filesize {filesize}")
    
    # Using integer scale instead of log scale
    plt.xticks(sorted(df['Threads'].unique().tolist()))  # Set x-ticks to exact thread values
    plt.xlabel('Number of Threads')
    plt.ylabel('Efficiency (Speedup/Threads)')
    plt.title('Parallel Efficiency vs Thread Count')
    plt.legend()
    plt.grid(True)
    plt.tight_layout()
    
    # Save to output/charts directory
    filepath = pathlib.Path('output/charts') / 'efficiency_vs_threads.png'
    plt.savefig(filepath)
    plt.close() # Close the figure
    print(f"Saved {filepath}")
    
def generate_tables(df):
    # Generate HTML tables for each file size
    for filesize in df['FileSize'].unique():
        data = df[df['FileSize'] == filesize].pivot_table(
            index='Implementation', 
            columns='Threads',
            values=['ElapsedTime_mean', 'MemoryUsage_mean', 'CPUTime_mean']
        )
        
        # Format the table
        try:
            # Add CSS styles for borders and centered content
            table_styles = [
                {'selector': 'table', 'props': [
                    ('border-collapse', 'collapse'),
                    ('width', '100%'),
                    ('margin', '20px 0'),
                    ('font-size', '12pt'),
                ]},
                {'selector': 'th, td', 'props': [
                    ('text-align', 'center'),
                    ('padding', '8px'),
                    ('border', '1px solid #ddd'),
                ]},
                {'selector': 'thead th', 'props': [
                    ('background-color', '#f2f2f2'),
                    ('color', '#333'),
                    ('font-weight', 'bold'),
                ]},
                {'selector': 'tbody tr:nth-child(even)', 'props': [
                    ('background-color', '#f9f9f9'),
                ]},
                {'selector': 'caption', 'props': [
                    ('caption-side', 'top'),
                    ('font-weight', 'bold'),
                    ('font-size', '14pt'),
                    ('margin-bottom', '10px'),
                ]},
            ]
            
            # Format table with appropriate units
            styled_table = data.style.format({
                ('ElapsedTime_mean', i): "{:.2f} ms" for i in data.columns.levels[1]
            }).format({
                ('MemoryUsage_mean', i): "{:.2f} bytes" for i in data.columns.levels[1]
            }).format({
                ('CPUTime_mean', i): "{:.2f} ms" for i in data.columns.levels[1]
            }).set_caption(f'Performance Metrics - {filesize} pages').set_table_styles(table_styles)
            
            # Save to HTML in the tables directory
            html_filepath = pathlib.Path('output/tables') / f'table_{filesize}_pages.html'
            styled_table.to_html(html_filepath)
            print(f"Saved {html_filepath}")
            
            # Also save as CSV for easier import into other tools
            csv_filepath = pathlib.Path('output/tables') / f'table_{filesize}_pages.csv'
            data.to_csv(csv_filepath)
            print(f"Saved {csv_filepath}")
        except Exception as e:
            print(f"Error generating table for filesize {filesize}: {e}")

if __name__ == "__main__":
    main()
