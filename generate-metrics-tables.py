import os

def read_metrics_from_file(file_path):
    print(f"Reading metrics from file: {file_path}")
    with open(file_path, 'r') as file:
        line = file.readline().strip()
        print(f"Raw metrics line: {line}")
        metrics = line.split()
        print(f"Split metrics: {metrics}")
        return metrics

def generate_latex_table(metrics, ontology_name, model):
    print(f"Generating LaTeX table for {ontology_name} - {model}")
    table = []
    table.append("\\begin{table}[h!]")
    table.append("\\centering")
    table.append(f"\\caption{{Metrics for {ontology_name} - {model}}}")
    table.append("\\begin{tabular}{|l|c|c|c|}")
    table.append("\\hline")
    table.append("Metric Type & Recall & Precision & F1 \\\\")
    table.append("\\hline")
    
    metric_types = ["M.Syntax", "NLP", "Enriched M.Syntax", "Enriched NLP"]
    
    for i in range(0, len(metrics), 3):
        row = metrics[i:i+3]
        print(f"Metrics for {metric_types[i//3]}: {row}")
        table.append(f"{metric_types[i//3]} & " + " & ".join(row) + " \\\\")
        table.append("\\hline")
    
    table.append("\\end{tabular}")
    table.append("\\end{table}")
    
    latex_table = "\n".join(table)
    print(f"Generated LaTeX table:\n{latex_table}")
    return latex_table

def main():
    results_dir = "./results/summaryFiles/"
    print(f"Reading files from directory: {results_dir}")
    for file_name in os.listdir(results_dir):
        if file_name.endswith(".txt"):
            print(f"Processing file: {file_name}")
            file_path = os.path.join(results_dir, file_name)
            parts = file_name.replace('.txt', '').split('-')
            ontology_name = '-'.join(parts[:-1])
            model = parts[-1]
            print(f"Ontology: {ontology_name}, Model: {model}")
            metrics = read_metrics_from_file(file_path)
            latex_table = generate_latex_table(metrics, ontology_name, model)

            latex_file_path = os.path.join("./results/latex-tables/", f"{ontology_name}-{model}.tex")
            print(f"Writing LaTeX table to file: {latex_file_path}")
            with open(latex_file_path, 'w') as latex_file:
                latex_file.write(latex_table)
            print(f"Finished writing LaTeX table for {file_name}\n")

if __name__ == "__main__":
    main()
