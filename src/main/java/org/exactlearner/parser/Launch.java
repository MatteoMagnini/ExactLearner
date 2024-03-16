package org.exactlearner.parser;

import org.experiments.Environment;
import org.experiments.task.ExperimentTask;
import org.experiments.task.Task;
import org.experiments.workload.OllamaModels;
import org.experiments.workload.OllamaWorkload;
import org.experiments.workload.OpenAIWorkload;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;

import java.io.FileNotFoundException;
import java.util.Set;

class Launch {
    private final static String ANIMAL_ONTOLOGY = "src/main/resources/ontologies/small/animals.owl";
    private final static String FAMILY_ONTOLOGY = "src/main/resources/ontologies/small/family.rdf.owl";

    private final static String system = "Your sole task is to respond " +
            "to questions that I'm going to ask you with this syntax: 'In the real world," +
            " is 'className' a subclass of 'className2'?'" +
            " Provide a binary response indicating whether 'className' is a " +
            "subclass of 'className2' in the context of class hierarchies." +
            " Examples:" +
            "In the real world, is 'Dog' a subclass of 'Animal'? -> True" +
            "Is 'Carrot' a subclass of 'Vegetable'? -> False" +
            "Is 'Circle' a subclass of 'Shape'? -> True" +
            "You can only respond with either True or False.";

    public static void main(String[] args) throws FileNotFoundException {
        //runAnimalOntology();
        runFamilyOntology();
    }

    private static void runFamilyOntology() {
        var parser = loadOntology(FAMILY_ONTOLOGY);
        var classesNames = parser.getClassesNamesAsString();
        var axiom = parser.getAxioms();
        //askGPT4Free(classesNames);
        //askGPT3(classesNames);
        //askHuggingFace(classesNames);
        //sendChatGPTQuestions(classesNames, new OpenAIWorkload());
        sendOllamaQuestions(classesNames, new OllamaWorkload());
    }

    private static void runAnimalOntology() {
        var parser = loadOntology(ANIMAL_ONTOLOGY);
        var classesNames = parser.getClassesNamesAsString();
        var axiom = parser.getAxioms();
        //sendOllamaQuestions(classesNames, new OllamaWorkload());
        //askGPT4Free(classesNames);
        //askGPT3(classesNames);
        //askHuggingFace(classesNames);
    }

    private static OWLParser loadOntology(String familyOntology) {
        OWLParser parser = null;
        try {
            parser = new OWLParserImpl(familyOntology);
        } catch (OWLOntologyCreationException e) {
            System.out.println(e.getMessage());
        }
        return parser;
    }

    private static void sendOllamaQuestions(Set<String> classesNames, OllamaWorkload ollama) {
        for (String className : classesNames) {
            for (String className2 : classesNames) {
                if (!className.equals(className2)) {
                    String message = "You must reply to this with just one word(True, False): In the real world, is " + className + " a subclass of " + className2 + "?";
                    ollama.setUp(OllamaModels.MIXTRAL.getModelName(), message, system);
                    Task task = new ExperimentTask(message, OllamaModels.MIXTRAL.getModelName(), "Family", message, system, ollama);
                    Environment.run(task);
                    //SmartLogger.isFileInCache(task.getFileName());
                    //SmartLogger.removeFileFromCache(task.getFileName());
                }
            }
        }
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
                    String message = "Reply to this question with ONLY True or False: In the real world, is " + className + " a subclass of " + className2 + "?";
                    openAI.setUp(message, system);
                    Task task = new ExperimentTask(message, "gpt3.5-turbo", "Family", message, system, openAI);
                    Environment.run(task);
                }
            }
        }
    }
}