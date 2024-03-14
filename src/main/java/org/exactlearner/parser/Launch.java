package org.exactlearner.parser;

import org.exactlearner.connection.Bridge;
import org.exactlearner.connection.ChatGPTBridge;
import org.exactlearner.connection.Gpt4FreeBridge;
import org.exactlearner.connection.HuggingFaceBridge;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;

import java.io.FileNotFoundException;
import java.util.Set;

class Launch {
    private final static String ANIMAL_ONTOLOGY = "src/main/resources/ontologies/small/animals.owl";
    private final static String FAMILY_ONTOLOGY = "src/main/resources/ontologies/small/family.rdf.owl";

    public static void main(String[] args) throws FileNotFoundException {
        runAnimalOntology();
        runFamilyOntology();
    }

    private static void runFamilyOntology() {
        var parser = loadOntology(FAMILY_ONTOLOGY);
        var classesNames = parser.getClassesNamesAsString();
        var axiom = parser.getAxioms();
        //askGPT4Free(classesNames);
        askGPT3(classesNames);
        //askHuggingFace(classesNames);
    }

    private static void runAnimalOntology() {
        var parser = loadOntology(ANIMAL_ONTOLOGY);
        var classesNames = parser.getClassesNamesAsString();
        var axiom = parser.getAxioms();
        //askGPT4Free(classesNames);
        askGPT3(classesNames);
        //askHuggingFace(classesNames);
    }

    private static OWLParser loadOntology(String familyOntology) {
        OWLParser parser = null;
        try {
            parser = new OWLParserImpl(ANIMAL_ONTOLOGY);
        } catch (OWLOntologyCreationException e) {
            System.out.println(e.getMessage());
        }
        return parser;
    }


    private static void askGPT4Free(Set<String> classesNames) {
        Gpt4FreeBridge bridge = new Gpt4FreeBridge();
        sendQuestions(classesNames, "", bridge);
    }

    private static void askGPT3(Set<String> classesNames) {
        String key = System.getenv("OPENAI_API_KEY");
        System.out.println(key);
        ChatGPTBridge bridge = new ChatGPTBridge();
        sendQuestions(classesNames, "", bridge);
    }

    private static void askHuggingFace(Set<String> classesNames) {
        String huggingFaceKey = System.getenv("HUGGINGFACE_API_KEY");
        HuggingFaceBridge bridge = new HuggingFaceBridge("openai-community/gpt2-xl");
        sendQuestions(classesNames, huggingFaceKey, bridge);
    }

    private static void sendQuestions(Set<String> classesNames, String key, Bridge bridge) {
        for (String className : classesNames) {
            for (String className2 : classesNames) {
                if (!className.equals(className2)) {
                    String message = "Reply to this question with ONLY True or False: In the real world, is " + className + " a subclass of " + className2 + "?";
                    System.out.println(message);
                    System.out.println(bridge.ask(message, key));
                }
            }
        }
    }
}
