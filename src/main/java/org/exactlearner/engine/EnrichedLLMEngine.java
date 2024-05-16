package org.exactlearner.engine;

import org.experiments.Environment;
import org.experiments.Result;
import org.experiments.task.ExperimentTask;
import org.experiments.task.Task;
import org.experiments.workload.OllamaWorkload;
import org.experiments.workload.OpenAIWorkload;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import java.util.Arrays;

public class EnrichedLLMEngine extends LLMEngine{

    public EnrichedLLMEngine(OWLOntology ontology, String model, String system, Integer maxTokens, OWLOntologyManager manager) {
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
            var pt1 = message.split(" SubClassOf ")[0];
            var pt2 = message.split(" SubClassOf ")[1];
            return "(" + addExtraSemantic(pt1) + ") SubClassOf (" + addExtraSemantic(pt2) + ")";
        } else if (message.contains("and")) {
            StringBuilder r = new StringBuilder();
            Arrays.stream(message.split(" and ")).map(this::addExtraSemantic).toList().forEach(s -> {
                r.append(s).append(" and ");
            });
            return r.substring(0, r.length() - " and ".length());
        } else if (message.contains("some")) {
            StringBuilder r = new StringBuilder();
            Arrays.stream(message.split(" some ")).map(this::addExtraSemantic).toList().forEach(s -> {
                r.append(s).append(" some ");
            });
            return r.substring(0, r.length() - " some ".length());
        } else if ((message.contains("("))) {
            return "(" + addExtraSemantic(message.replaceFirst("\\(", ""));
        } else if ((message.contains(")"))) {
            return addExtraSemantic(message.replaceFirst("\\)", "")) + ")";
        } else {
            message = message.replace(" ", "");
            if (Character.isUpperCase(message.charAt(0))) {
                return "[Class: " + message + "]";
            } else {
                return "[ObjectProperty: " + message + "]";
            }
        }
    }
}
