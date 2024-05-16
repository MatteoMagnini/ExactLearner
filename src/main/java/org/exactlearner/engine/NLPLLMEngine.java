package org.exactlearner.engine;

import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import java.util.Arrays;
import java.util.stream.Collectors;

public class NLPLLMEngine extends LLMEngine {
    public NLPLLMEngine(OWLOntology ontology, String model, String system, Integer maxTokens, OWLOntologyManager manager) {
        super(ontology, model, system, maxTokens, manager);
    }

    @Override
    public Boolean runTaskAndGetResult(String message) {
        message = message.replace("  ", " ");
        message = addExtraSemantic(message);
        return super.runTaskAndGetResult(message);
    }

    private String addExtraSemantic(String message) {
        if (message.contains("SubClassOf")) {
            var parts = message.split(" SubClassOf ", 2);
            String pt1 = addExtraSemantic(parts[0]);
            String pt2 = addExtraSemantic(parts[1]);
            return "Every " + pt1 + " is considered a subcategory of '" + pt2 + "'?";
        } else if (message.contains(" and ")) {
            String[] terms = message.split(" and ");
            return Arrays.stream(terms).map(this::addExtraSemantic).collect(Collectors.joining(" that is also "));
        } else if (message.contains(" some ")) {
            String[] terms = message.split(" some ");
            return Arrays.stream(terms).map(this::addExtraSemantic).collect(Collectors.joining(" "));
        } else if (message.startsWith("(") && message.endsWith(")")) {
            // Remove parentheses and process the inner expression
            return addExtraSemantic(message.substring(1, message.length() - 1));
        } else {
            if (Character.isUpperCase(message.charAt(0))) {
                return message;
            } else {
                return "something that " + message;
            }
        }
    }
}
