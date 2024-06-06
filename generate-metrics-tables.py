import os
from collections import defaultdict

PRETTY_ONTOLOGY_NAMES = {
    "animals.owl": "Animals",
    "university.owl": "University",
    "cl.owl": "Cell",
    "generations.owl": "Generations",
    "biosphere.owl": "Biosphere",
    "football.owl": "Football",
    "biological-measure-primitive.owl": "Bio-primitive",
}

PRETTY_MODEL_NAMES = {
    "llama2:13b": "Llama2 (13b)",
    "llama2": "Llama2 (7b)",
    "llama3": "Llama3 (8b)",
    "mistral": "Mistral (7b)",
    "mixtral": "Mixtral (47b)",
}


def read_metrics_from_file(file_path):
    print(f"Reading metrics from file: {file_path}")
    with open(file_path, 'r') as file:
        line = file.readline().strip()
        print(f"Raw metrics line: {line}")
        metrics = list(map(float, line.split()))
        print(f"Split metrics: {metrics}")
        return metrics


def generate_latex_table(ontology_name, model_metrics):
    print(f"Generating LaTeX table for {ontology_name}")
    table = []
    table.append("\\begin{table*}[]")
    table.append("\\centering")
    table.append("\\resizebox{\\textwidth}{!}{")
    table.append("\\begin{tabular}{|p{2cm}|p{3cm}|p{1.5cm}|p{1.5cm}|p{1.5cm}|p{1.5cm}|}")
    table.append("\\hline")
    table.append("\\multicolumn{1}{|c|}{\\textbf{Model}} & \\multicolumn{1}{|c|}{\\textbf{Prompt \& Query}} & \\multicolumn{1}{c|}{\\textbf{Accuracy}} & \\multicolumn{1}{c|}{\\textbf{Recall}} & \\multicolumn{1}{c|}{\\textbf{Precision}} & \\multicolumn{1}{c|}{\\textbf{F1-Score}} \\\\")
    table.append("\\hline")

    metric_types = ["M. OWL Syntax", "Natural Language", "E. M. OWL Syntax", "E. Natural Language"]

    for j, (model, metrics) in enumerate(model_metrics.items()):
        for i in range(0, len(metrics), 4):
            row = metrics[i:i + 4]
            metric_type = metric_types[i // 4]
            print(f"Metrics for {model} - {metric_type}: {row}")
            if i == 0:
                x = "\\rowcolor[HTML]{EFEFEF}"
                table.append(f"\\multirow{{4}}{{*}}{{{PRETTY_MODEL_NAMES[model]}}} & {x} {metric_type} & " + " & ".join(
                    map(str, row)) + " \\\\ \\cline{2-6}")
            elif metric_type == "E. Natural Language":
                x = "\\rowcolor[HTML]{FFFFFF}"
                table.append(f"& {x} {metric_type} & " + " & ".join(map(str, row)) + " \\\\ \\cline{1-6}")
            else:
                x = ""
                if i == 4:
                    x = "\\rowcolor[HTML]{FFFFFF}"
                if i == 8:
                    x = "\\rowcolor[HTML]{EFEFEF}"
                table.append(f"& {x} {metric_type} & " + " & ".join(map(str, row)) + " \\\\ \\cline{2-6}")
            if i == 12 and j != len(model_metrics) - 1:
                table.append("\\hline")

    table.append("\\end{tabular}}")
    table.append(f"\\caption{{Metrics for {ontology_name} ontology}}")
    table.append("\\end{table*}")
    latex_table = "\n".join(table)
    print(f"Generated LaTeX table:\n{latex_table}")
    return latex_table


def calculate_averages(metrics_dict, group_by):
    print(f"Calculating averages grouped by {group_by}...")
    averages = defaultdict(lambda: [0.0, 0.0, 0.0, 0.0, 0])

    for key, metrics_list in metrics_dict.items():
        group_key = key[group_by]
        for metrics in metrics_list:
            for i in range(4):  # Accuracy, Recall, Precision, F1-Score
                averages[group_key][i] += metrics[i]
            averages[group_key][4] += 1

    for group_key, sums in averages.items():
        for i in range(4):
            sums[i] = round(sums[i] / sums[4], 3)
        print(f"Averages for {group_key}: {sums[:4]}")

    return averages


def generate_average_latex_table(averages, caption, headers):
    print(f"Generating LaTeX table for {caption}")
    table = []
    table.append("\\begin{table}[h!]")
    table.append("\\centering")
    table.append("\\resizebox{\\columnwidth}{!}{")
    table.append("\\begin{tabular}{|l|c|c|c|c|}")
    table.append("\\hline")
    table.append(" & ".join(headers) + " \\\\")
    table.append("\\hline")

    for group_key, avg_metrics in averages.items():
        table.append(f"{group_key} & " + " & ".join(map(str, avg_metrics[:4])) + " \\\\")
        table.append("\\hline")

    table.append("\\end{tabular}}")
    table.append(f"\\caption{{{caption}}}")
    table.append("\\end{table}")

    latex_table = "\n".join(table)
    print(f"Generated LaTeX table for {caption}:\n{latex_table}")
    return latex_table


def main():
    results_dir = "./analysis/"
    print(f"Reading files from directory: {results_dir}")
    metrics_dict = defaultdict(list)
    all_tables = []

    ontology_metrics = defaultdict(lambda: defaultdict(list))

    for file_name in sorted(os.listdir(results_dir)):
        if file_name.endswith(".txt"):
            print(f"Processing file: {file_name}")
            file_path = os.path.join(results_dir, file_name)
            file_name = file_name.replace('-13b', '_13b')
            parts = file_name.replace('.txt', '').split('-')
            ontology_name = '-'.join(parts[:-1])
            model = parts[-1]
            model = model.replace('_', ':')
            print(f"Ontology: {ontology_name}, Model: {model}")
            metrics = read_metrics_from_file(file_path)
            metrics = [round(metric, 3) for metric in metrics]

            metric_types = ["M. OWL Syntax", "Natural Language", "E. M. OWL Syntax", "E. Natural Language"]
            for i, metric_type in enumerate(metric_types):
                key = (ontology_name, model, metric_type)
                metrics_dict[key].append(metrics[i * 4:i * 4 + 4])

            ontology_metrics[ontology_name][model].extend(metrics)
            print(f"Finished processing file: {file_name}\n")

    for ontology_name, model_metrics in sorted(ontology_metrics.items()):
        latex_table = generate_latex_table(PRETTY_ONTOLOGY_NAMES[ontology_name], model_metrics)
        all_tables.append(latex_table)

    # Calculate and generate average tables
    for group_by, caption, headers in [
        (0, "Average Metrics by Ontology", ["Ontology", "Accuracy", "Recall", "Precision", "F1-Score"]),
        (1, "Average Metrics by Model", ["Model", "Accuracy", "Recall", "Precision", "F1-Score"]),
        (2, "Average Metrics by Metric Type", ["Metric Type", "Accuracy", "Recall", "Precision", "F1-Score"])
    ]:
        averages = calculate_averages(metrics_dict, group_by)
        average_latex_table = generate_average_latex_table(averages, caption, headers)
        all_tables.append(average_latex_table)
        print(f"Finished generating LaTeX table for average metrics by {headers[0].lower()}\n")

    # Combine all tables into a single LaTeX document

    latex_file_path = os.path.join("./results/latex-tables/", "combined_metrics.tex")
    print(f"Writing combined LaTeX tables to file: {latex_file_path}")
    os.makedirs(os.path.dirname(latex_file_path), exist_ok=True)
    with open(latex_file_path, 'w') as latex_file:
        latex_file.write("\n".join(all_tables))
    print("Finished writing combined LaTeX file")


if __name__ == "__main__":
    main()
