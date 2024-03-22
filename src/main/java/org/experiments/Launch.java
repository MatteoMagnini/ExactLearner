package org.experiments;

import org.exactlearner.parser.OWLParser;
import org.exactlearner.parser.OWLParserImpl;
import org.experiments.logger.SmartLogger;
import org.experiments.task.ExperimentTask;
import org.experiments.task.Task;
import org.experiments.workload.OllamaModels;
import org.experiments.workload.OllamaWorkload;
import org.experiments.workload.OpenAIWorkload;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import uk.ac.manchester.cs.owl.owlapi.mansyntaxrenderer.ManchesterOWLSyntaxOWLObjectRendererImpl;

import java.io.FileNotFoundException;
import java.util.Set;
import java.util.stream.Collectors;

class Launch {
    private final static String ANIMAL = "src/main/resources/ontologies/small/animals.owl";
    private final static String ANIMAL_INFERRED = "src/main/resources/ontologies/small/animals_inferred.owl";
    private final static String UNIVERSITY = "src/main/resources/ontologies/small/university.owl";
    private final static String UNIVERSITY_INFERRED = "src/main/resources/ontologies/small/university_inferred.owl";
    private final static String GENERATIONS = "src/main/resources/ontologies/small/generations(large).owl";
    private final static String GENERATIONS_INFERRED = "src/main/resources/ontologies/small/generations_inferred.owl";

    private final static String system = "Your sole task is to respond " +
            "to questions that I'm going to ask you with this syntax: 'In the real world," +
            " is 'className' a subclass of 'className2'?'" +
            " Provide a binary response indicating whether 'className' is a " +
            "subclass of 'className2' in the context of class hierarchies." +
            " Examples:" +
            "In the real world, is 'Dog' a subclass of 'Animal'? -> True" +
            "Is 'Carrot' a subclass of 'Animal'? -> False" +
            "Is 'Animal' a subclass of 'Dog'? -> False" +
            "You can only respond with either True or False.";

    public static void main(String[] args) throws FileNotFoundException {
        runOntology();
    }

    private static void runOntology() {
        var parser = loadOntology(GENERATIONS_INFERRED);
        var classesNames = parser.getClassesNamesAsString();
        var axioms = parser.getAxioms();
        var filteredManchesterSyntaxAxioms = parseAxioms(axioms);

        //sendChatGPTQuestions(classesNames, new OpenAIWorkload());
        sendOllamaQuestions(classesNames, new OllamaWorkload(),"Generations Inferred");
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

    private static void sendOllamaQuestions(Set<String> classesNames, OllamaWorkload ollama, String OntologyName) {
        SmartLogger.checkCachedFiles();
        for (String className : classesNames) {
            for (String className2 : classesNames) {
                if (!className.equals(className2)) {
                    String message = "(True or False only, i don't want explanation) In the real world, is " + className + " a subclass of " + className2 + "?";
                    ollama.setUp(OllamaModels.MISTRAL.getModelName(), message, system);
                    Task task = new ExperimentTask(message, OllamaModels.MISTRAL.getModelName(), OntologyName, message, system, ollama);
                    Environment.run(task);
                }
            }
        }
        SmartLogger.checkCachedFiles();
    }

    private static void sendChatGPTQuestions(Set<String> classesNames, OpenAIWorkload openAI) {
        for (String className : classesNames) {
            for (String className2 : classesNames) {
                if (!className.equals(className2)) {
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    String message = "(True or False only, i don't want explanation) In the real world, is " + className + " a subclass of " + className2 + "?";
                    openAI.setUp(message, system);
                    Task task = new ExperimentTask(message, "gpt3.5-turbo", "Family", message, system, openAI);
                    Environment.run(task);
                }
            }
        }
        SmartLogger.checkCachedFiles();
    }
}