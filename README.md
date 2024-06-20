 
# Actively Learning with LLMs

This is a fork of the [ExactLearner](https://github.com/ExactLearner/ExactLearner) repository.

Here you can find the source code and results for the paper "Actively Learning Ontologies from LLMs: First Results" for DL 2024.

A reference to the paper will be put here as soon as available.

## Structure

### Run experiments

In __src/main/java/org/experiments__ there are:
- two configuration files: __axiomsQueryingConf.yml__ (for experiments 1 and 2 of the paper) and __classesQueryngConf.yml__ (for experiment 3 of the paper). In these files you can define which LLMs and Ontologies to use, as well as the system prompt (*Answer with True or False.*) and the maximum number of tokens (2).
- the main class __Launch__. You need to pass as the only argument of the program the configuration file of the experiment you want to perform.

Note that we use OpenAI API, if you want to do the same set the environment variable OPENAI_API_KEY with your own key:
> export OPENAI_API_KEY=your_key

Also, you may need to change the url for connecting with the LLMs (Ollama API) on our server in Cesena if you are not access to it (only collaborators of the University of Bologna can ask for access).
Just go to __src/main/java/org/exactlearner/OllamaBridge__ and change at line 9 *http://clusters.almaai.unibo.it:11434/api/generate* with your own server address.

Results are stored in the __cache__ folder (delete the files in the cache if you want to run new fresh experiments).

### Run analysis

Once you have gathered the responses from the LLMs in the __cache__ folder you can run in __src/main/java/org/analysis__ the two main classes __AxiomsAnalyser__ (experiments 1 and 2 of the paper) and __ClassesAnalyser__ (experiment 3 of the paper).
Both programs need to receive as argument a configuration file like in the other main.

Results are saved inside the __results__ folder.

### Visualisation

There are 3 python scripts to generate Latex tables (the ones in the paper) __generate_table1.py__, __generate_table2.py__ and __generate_table3.py__.
The scripts are tailored with the current configuration files.
You may want to modify them accordingly if you use different configuration files.
