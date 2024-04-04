# Read from the configuration file and generate the table 3 in the paper
import os
import yaml
import pandas as pd

file_name = os.sep.join(["src", "main", "java", "org", "experiments", "axiomsQueryingConf.yml"])
configuration = yaml.load(open(file_name), Loader=yaml.FullLoader)
models = configuration["models"]
ontologies = [x for x in configuration["ontologies"] if "inferred" not in x]
results_path = os.sep.join(["results", "axiomsQuerying"])
answer_values = ["True", "False", "Unknown"]
short_answer_values = ["T", "F", "U"]

# Generate the table 3
# We have 5 models (rows) and 5 ontologies (columns).
# Each ontology has 3 possible answers: True, False, Unknown.
# So in total we have 1 + 5 * 3 = 16 columns.
table = "\\begin{table*}[]\n\\centering\n\\resizebox{\\textwidth}{!}{\n"
table += "\\begin{tabular}{l|ccc|ccc|ccc|ccc|ccc}\n"
table += "\\hline\n"
table += "\\multicolumn{1}{c|}{\\multirow{2}{*}{\\textbf{Models}}} & "
for ontology in ontologies:
    onto = ontology.split(os.sep)[-1].replace('.owl', '').capitalize()
    # remove (...) from the ontology name
    onto = onto.split('(')[0]
    table += f"\\multicolumn{{3}}{{c{'|' if ontology != ontologies[-1] else ''}}}{{\\textbf{{{onto}}}}} & "
table = table[:-2] + "\\\\\n \multicolumn{1}{c|}{} &"
for ontology in ontologies:
    for value in short_answer_values:
        bar = '|' if value == short_answer_values[-1] and ontology != ontologies[-1] else ''
        table += f"\\multicolumn{{1}}{{c{bar}}}{{\\textbf{{{value}}}}} &"
table = table[:-2] + "\\\\ \\hline\n"
for i, model in enumerate(models):
    if i % 2 == 0:
        table += '\\rowcolor[HTML]{EFEFEF}\n'
    model_tmp = model
    if model.strip() == "gpt-3.5-turbo":
        model_tmp = "GPT3.5"
    table += f'{model_tmp.capitalize().split(":")[0]} & '
    for ontology in ontologies:
        # replace : with - in the model name
        # take only the name, not the whole path and remove .owl from the ontology name
        short_ontology = ontology.split(os.sep)[-1].replace(".owl", "")
        results_file = os.sep.join([results_path, f"{model.replace(':', '-')}_{short_ontology}.csv"])
        results_values = pd.read_csv(results_file, sep=",", header=0)
        results_values.columns = [x.strip() for x in results_values.columns]
        for value_name in answer_values:
            value = int(results_values[value_name][0])
            # Do not show the 0 in .## format
            # and if it is 1, show it as 1 instead of 1.00
            bar = '|' if value_name == answer_values[-1] and ontology != ontologies[-1] else ''
            table += f'\\multicolumn{{1}}{{r{bar}}}{{{value}}} & '
    table = table[:-2] + "\\\\ \\hline\n"
table += "\\end{tabular}\n}\n"
table += "\\caption{Results for the experiments testing correctness w.r.t. axioms in the ontologies.\n%\n" \
         "Labels T, F and U mean `true', `false' and `unknown' responses count.\n%\n" \
         "We indicate the number of parameters in each model in parenthesis (e.g. Mistral has 7 billion).\n%\n" \
         "It is not known the number of parameters of GPT 3.5.\n%\n}\n"
table += "\\label{table:correctness}\n"
table += "\\end{table*}"
# Save the table to a file
with open("table1.tex", "w") as f:
    f.write(table)

