package org.exactlearner.parser;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.exactlearner.connection.Bridge;
import org.exactlearner.connection.ChatGPTBridge;
import org.exactlearner.connection.Gpt4FreeBridge;
import org.exactlearner.connection.HuggingFaceBridge;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.Set;

class Launch {
    public static void main(String[] args) throws FileNotFoundException {
        OWLParser parser = null;
        try {
            parser = new OWLParserImpl("src/main/resources/ontologies/small/animals.owl");
        } catch (OWLOntologyCreationException e) {
            e.printStackTrace();
        }
        assert parser != null;
        var classesNames = parser.getClassesNamesAsString();

        //askGPT4Free(classesNames);
        askGPT3(classesNames);
        //askHuggingFace(classesNames);
    }

    private static void askGPT4Free(Set<String> classesNames) {
        Gpt4FreeBridge bridge = new Gpt4FreeBridge();
        sendQuestions(classesNames, "", bridge);
    }

    private static void askGPT3(Set<String> classesNames) throws FileNotFoundException {
        FileReader reader = new FileReader("src/main/resources/openai-key.json");
        JsonParser jsonParser = new JsonParser();
        JsonObject jsonObject = jsonParser.parse(reader).getAsJsonObject();
        Gson gson = new Gson();
        String key = gson.fromJson(jsonObject, String.class);
        System.out.println(key);
        ChatGPTBridge bridge = new ChatGPTBridge();
        sendQuestions(classesNames, "", bridge);
    }

    private static void askHuggingFace(Set<String> classesNames) {
        String huggingFaceKey = "key";
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
