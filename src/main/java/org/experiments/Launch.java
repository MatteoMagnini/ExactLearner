package org.experiments;

import org.analysis.ClassesResultsReader;
import org.exactlearner.parser.OWLParser;
import org.exactlearner.parser.OWLParserImpl;
import org.experiments.logger.SmartLogger;
import org.experiments.task.ExperimentTask;
import org.experiments.task.Task;
import org.experiments.workload.OllamaWorkload;
import org.semanticweb.elk.util.collections.Pair;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.yaml.snakeyaml.Yaml;
import uk.ac.manchester.cs.owl.owlapi.OWLClassImpl;
import uk.ac.manchester.cs.owl.owlapi.mansyntaxrenderer.ManchesterOWLSyntaxOWLObjectRendererImpl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import java.util.stream.Collectors;

public class Launch {

    private static HashMap<Pair<String,String>,String> queries = new HashMap<>();
    public static void main(String[] args) {
        // Read the configuration file passed by the user as an argument
        Yaml yaml = new Yaml();
        Configuration config;

        try {
            config = yaml.loadAs(new FileInputStream(args[0]), Configuration.class);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }

        // For each model in the configuration file and for each ontology in the configuration file, run the experiment
        SmartLogger.checkCachedFiles();
        for (String model : config.getModels()) {
            for (String ontology : config.getOntologies()) {
                System.out.println("Running experiment for model: " + model + " and ontology: " + ontology);
                runExperiment(model, ontology, config.getSystem(), config.getMaxTokens(), config.getType());
            }
        }

        // Recreate ontologies from results
        //get the classes first
        if (config.getType().equals("classesQuerying")) {
            queries.forEach((pair, message) -> {
                OWLOntologyManager manager= OWLManager.createOWLOntologyManager();
                try {
                    OWLOntology rootOntology=manager.loadOntologyFromOntologyDocument(new File(pair.getSecond()));
                    var reader = new ClassesResultsReader(config.getType(), pair.getFirst(), pair.getSecond(), message, config.getSystem());
                    reader.computeResults();
                    var parentClassName = reader.getParentClassName();
                    var childClassName = reader.getChildClassName();
                    OWLParserImpl parser = new OWLParserImpl(pair.getSecond());
                    rootOntology.getSubClassAxiomsForSuperClass(parser.getClasses().get()
                                    .stream()
                                    .filter(c -> c.toString().contains(parentClassName))
                                    .findFirst()
                                    .get()).stream()
                            .filter(axiom -> axiom.getSubClass().asOWLClass().toString().contains(childClassName));

                } catch (OWLOntologyCreationException e) {
                    throw new RuntimeException(e);
                }



            });
        }
    }

    private static void runExperiment(String model, String ontology, String system, int maxTokens, String type) {
        var parser = loadOntology(ontology);
        var classesNames = parser.getClassesNamesAsString();
        var axioms = parser.getAxioms();
        var filteredManchesterSyntaxAxioms = parseAxioms(axioms);

        if (type.equals("classesQuerying")){
            for (String className : classesNames) {
                for (String className2 : classesNames) {
                    if (!className.equals(className2)) {
                        String message = className + " SubClassOf " + className2;
                        queries.put(new Pair<>(model, ontology), message);
                        var work = new OllamaWorkload(model, system, message, maxTokens);
                        Task task = new ExperimentTask(type, model, ontology, message, system, work);
                        Environment.run(task);
                    }
                }
            }
        }else if (type.equals("axiomsQuerying")) {
            for (String axiom : filteredManchesterSyntaxAxioms) {
                // Remove carriage return and line feed characters
                axiom = axiom.replaceAll("\r", " ").replaceAll("\n", " ");
                var work = new OllamaWorkload(model, system, axiom, maxTokens);
                Task task = new ExperimentTask(type, model, ontology, axiom, system, work);
                Environment.run(task);
            }
        }else{
            throw new IllegalStateException("Invalid type of experiment.");
        }
    }

    private static Set<String> parseAxioms(Set<OWLAxiom> axioms) {
        return axioms.stream().filter(axiom -> !axiom.isOfType(AxiomType.DECLARATION))
                .filter(axiom -> !axiom.isOfType(AxiomType.FUNCTIONAL_OBJECT_PROPERTY))
                .filter(axiom -> !axiom.isOfType(AxiomType.SYMMETRIC_OBJECT_PROPERTY))
                .filter(axiom -> !axiom.isOfType(AxiomType.CLASS_ASSERTION)).collect(Collectors.toSet())
                .stream().map(new ManchesterOWLSyntaxOWLObjectRendererImpl()::render).collect(Collectors.toSet());
    }

    private static OWLParser loadOntology(String ontology) {
        OWLParser parser = null;
        try {
            parser = new OWLParserImpl(ontology);
        } catch (OWLOntologyCreationException e) {
            System.out.println(e.getMessage());
        }
        return parser;
    }
}