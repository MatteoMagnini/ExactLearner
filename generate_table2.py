# Read from the configuration file and generate the table 2 in the paper
import os
import yaml
import pandas as pd

file_name = os.sep.join(["src", "main", "java", "org", "experiments", "axiomsQueryingConf.yml"])
configuration = yaml.load(open(file_name), Loader=yaml.FullLoader)
models = configuration["models"]
ontologies = [x for x in configuration["ontologies"] if "inferred" in x]
results_path = os.sep.join(["results", "axiomsQuerying"])
answer_values = ["True", "False", "Unknown", "Logic Inconsistent Axioms"]
short_answer_values = ["T", "F", "U", "L"]

# Generate the table
# We have 5 models (rows) and 5 ontologies (columns).
# We omit the models names column.
# Each ontology has 4 possible answers: True, False, Unknown, Logic Inconsistent Axioms.
# So in total we have 5 * 4 = 20 columns only
table = "\\begin{table*}[]\n\\centering\n\\resizebox{\\textwidth}{!}{\n"
table += "\\begin{tabular}{cccc|cccc|cccc|cccc|cccc}\n"
table += "\\hline\n"
for ontology in ontologies:
    onto = ontology.split(os.sep)[-1].replace('.owl', '').capitalize()
    # remove (...) from the ontology name
    onto = onto.split('(')[0]
    onto = onto.split('_')[0]
    table += f"\\multicolumn{{4}}{{c{'|' if ontology != ontologies[-1] else ''}}}{{\\textbf{{{onto}}}}} & "
table = table[:-2] + "\\\\\n"
for ontology in ontologies:
    for value in short_answer_values:
        bar = '|' if value == short_answer_values[-1] and ontology != ontologies[-1] else ''
        table += f"\\multicolumn{{1}}{{c{bar}}}{{\\textbf{{{value}}}}} &"
table = table[:-2] + "\\\\ \\hline\n"
for i, model in enumerate(models):
    if i % 2 == 0:
        table += '\\rowcolor[HTML]{EFEFEF}\n'
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
table += "\\caption{Results for the experiment testing logical consistency.\n%\n" \
         "The number of parameters of each model and the meaning of T, F, U are as in~\\Cref{table:correctness}.\n%\n" \
         "L stands for logical inconsistencies (an axiom answered as `false' or `unknown' which can be inferred from the set of the axioms answered as True, see~\\Cref{subsec:correctness-and-consistency}).\n%\n" \
         "Models' names omitted for better readability (they are the same of~\\Cref{table:correctness}).\n%\n}\n"
table += "\\label{table:logic}\n"
table += "\\end{table*}"
# Save the table to a file
with open("table2.tex", "w") as f:
    f.write(table)

