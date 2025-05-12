import os
import re
import matplotlib.pyplot as plt
import numpy as np
import argparse

def extract_time_from_file(file_path):
    """Extract the elapsed time from a file."""
    try:
        with open(file_path, 'r') as file:
            content = file.read()
            # Search for elapsed time pattern
            time_match = re.search(r'Elapsed time: (\d+)ms', content)
            if time_match:
                return int(time_match.group(1))

            # Alternative format search (in case the data is in seconds)
            time_match_s = re.search(r'Elapsed time: ([\d.]+)s', content)
            if time_match_s:
                return float(time_match_s.group(1)) * 1000  # Convert to ms

        # If no match found
        print(f"No elapsed time found in {file_path}")
        return None
    except Exception as e:
        print(f"Error processing {file_path}: {e}")
        return None

def extract_word_frequencies(file_path):
    """Extract word frequencies from a file."""
    word_freq = {}
    try:
        with open(file_path, 'r') as file:
            content = file.read()
            # Search for word frequency pattern
            word_matches = re.findall(r"Word: '([^']+)' with total (\d+) occurrences!", content)
            for word, count in word_matches:
                word_freq[word] = int(count)
        return word_freq
    except Exception as e:
        print(f"Error extracting word frequencies from {file_path}: {e}")
        return {}

def main():
    parser = argparse.ArgumentParser(description='Parse and graph times from text files')
    parser.add_argument('directory', help='Directory containing .txt files to process')
    parser.add_argument('--output', '-o', default='time_comparison.png', help='Output graph filename')
    parser.add_argument('--words', '-w', action='store_true', help='Also analyze word frequencies')
    args = parser.parse_args()

    directory = args.directory

    if not os.path.isdir(directory):
        print(f"Error: {directory} is not a valid directory")
        return

    # Get all .txt files in the directory
    txt_files = [f for f in os.listdir(directory) if f.endswith('.txt')]

    if not txt_files:
        print(f"No .txt files found in {directory}")
        return

    print(f"Found {len(txt_files)} .txt files")

    # Process each file and extract times
    file_times = {}
    all_word_freqs = {}

    for file_name in txt_files:
        file_path = os.path.join(directory, file_name)
        time_ms = extract_time_from_file(file_path)
        if time_ms is not None:
            file_times[file_name] = time_ms

        if args.words:
            word_freq = extract_word_frequencies(file_path)
            if word_freq:
                all_word_freqs[file_name] = word_freq

    if not file_times:
        print("No elapsed times were found in any files")
        return

    # Create a bar chart
    plt.figure(figsize=(12, 6))

    # Sort by time for better visualization
    sorted_items = sorted(file_times.items(), key=lambda x: x[1])
    files = [item[0] for item in sorted_items]
    times = [item[1] for item in sorted_items]

    plt.bar(files, times, color='skyblue')
    plt.xlabel('Files')
    plt.ylabel('Elapsed Time (ms)')
    plt.title('Elapsed Time Comparison Across Files')
    plt.xticks(rotation=45, ha='right')
    plt.tight_layout()

    # Save and show the plot
    plt.savefig(args.output)
    print(f"Graph saved as {args.output}")

    # Display word frequency analysis if requested
    if args.words and all_word_freqs:
        # Gather all unique words across all files
        all_words = set()
        for file_words in all_word_freqs.values():
            all_words.update(file_words.keys())

        # Create a word frequency comparison graph for the top words
        top_words = sorted(all_words, key=lambda w: sum(f.get(w, 0) for f in all_word_freqs.values()), reverse=True)[:5]

        plt.figure(figsize=(12, 8))
        x = np.arange(len(all_word_freqs))
        width = 0.15

        for i, word in enumerate(top_words):
            freqs = [freq.get(word, 0) for freq in all_word_freqs.values()]
            plt.bar(x + i*width, freqs, width, label=word)

        plt.xlabel('Files')
        plt.ylabel('Occurrences')
        plt.title('Top Word Occurrences Across Files')
        plt.xticks(x + width * (len(top_words) - 1) / 2, list(all_word_freqs.keys()), rotation=45, ha='right')
        plt.legend()
        plt.tight_layout()

        # Save word frequency graph
        word_graph = f"word_frequency_comparison.png"
        plt.savefig(word_graph)
        print(f"Word frequency graph saved as {word_graph}")

    plt.show()

if __name__ == "__main__":
    main()