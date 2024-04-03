# Read from the configuration file and generate the table 3 in the paper
import os
import yaml
import pandas as pd

file_name = os.sep.join(["src", "main", "java", "org", "experiments", "classesQueryingConf.yml"])
configuration = yaml.load(open(file_name), Loader=yaml.FullLoader)
models = configuration["models"]
ontologies = configuration["ontologies"]
results_path = os.sep.join(["results", "classesQuerying"])
metrics = ["Accuracy", "Precision", "Recall"]
metrics_short = ["A", "P", "R"]

# Generate the table 3
# We have 5 models (rows) and 5 ontologies (columns).
# Each ontology has 3 metrics: accuracy, precision, recall.
# So in total we have 1 + 5 * 3 = 16 columns.
table = "\\begin{table*}[]\n\\centering\n"
table += "\\begin{tabular*}{\\columnwidth}{l| @{\\extracolsep{\\fill}} ccc|ccc|ccc|ccc|ccc}\n"
table += "\\hline\n"
table += "\\multicolumn{1}{c|}{\\multirow{2}{*}{\\textbf{Models}}} & "
for ontology in ontologies:
    onto = ontology.split(os.sep)[-1].replace('.owl', '').capitalize()
    # remove (...) from the ontology name
    onto = onto.split('(')[0]
    table += f"\\multicolumn{{3}}{{c{'|' if ontology != ontologies[-1] else ''}}}{{\\textbf{{{onto}}}}} & "
table = table[:-2] + "\\\\\n \multicolumn{1}{c|}{} &"
for ontology in ontologies:
    for metric in metrics_short:
        bar = '|' if metric == metrics_short[-1] and ontology != ontologies[-1] else ''
        table += f"\\multicolumn{{1}}{{c{bar}}}{{\\textbf{{{metric}}}}} &"
table = table[:-2] + "\\\\ \\hline\n"
for model in models:
    model_tmp = model
    if model.strip() == "gpt-3.5-turbo":
        model_tmp = "GPT3.5"
    table += f'{model_tmp.capitalize().split(":")[0]} & '
    for ontology in ontologies:
        # replace : with - in the model name
        # take only the name, not the whole path and remove .owl from the ontology name
        short_ontology = ontology.split(os.sep)[-1].replace(".owl", "")
        results_file = os.sep.join([results_path, f"{model.replace(':', '-')}_{short_ontology}.csv"])
        results_values = pd.read_csv(results_file, sep=";", header=0)
        results_values.columns = [x.strip() for x in results_values.columns]
        for metric in metrics:
            value = float(results_values[metric][0].replace(",", ".").strip())
            # Do not show the 0 in .## format
            # and if it is 1, show it as 1 instead of 1.00
            bar = '|' if metric == metrics[-1] and ontology != ontologies[-1] else ''
            table += f'\\multicolumn{{1}}{{r{bar}}}{{{value if value != 1 else 1}}} & '
    table = table[:-2] + "\\\\ \\hline\n"
table += "\\end{tabular*}"
table += "\\caption{Results for the experiments testing negative examples.\n%\nLabels A, P and R mean `Accuracy', `Precision' and `Recall' respectively.\n%\n}\n"
table += "\\label{table:negative}\n"
table += "\\end{table*}"
# Save the table to a file
with open("table3.tex", "w") as f:
    f.write(table)

