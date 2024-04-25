package org.exactlearner.engine;

import org.analysis.Result;
import org.exactlearner.parser.OWLParserImpl;
import org.experiments.Environment;
import org.experiments.task.ExperimentTask;
import org.experiments.task.Task;
import org.experiments.workload.OllamaWorkload;
import org.experiments.workload.OpenAIWorkload;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;

import java.util.ArrayList;
import java.util.Set;

public class LLMEngineImpl implements LLMEngine {
    private final String ontology;
    private final String model;
    private final String system;
    private final Integer maxTokens;
    private final OWLOntologyManager manager;
    private final OWLParserImpl parser;

    public LLMEngineImpl(String ontology, String model, String system, Integer maxTokens) {
        this.ontology = ontology;
        this.system = system;
        this.model = model;
        this.maxTokens = maxTokens;
        this.manager = OWLManager.createOWLOntologyManager();
        try {
            this.parser = new OWLParserImpl(ontology);
        } catch (OWLOntologyCreationException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean isSubClassOf(OWLClass classA, OWLClass classB) {
        var classes = new ArrayList<OWLClass>();
        classes.add(classA);
        classes.add(classB);
        var classesAsString = classes.stream()
                .map(OWLClass::toString)
                .map(s -> s.substring(s.indexOf("#") + 1, s.length() - 1)).toList();

        return runTaskAndGetResult(classesAsString.get(0) + "SubClassOf: " + classesAsString.get(1));
    }

    @Override
    public OWLSubClassOfAxiom subClassAxiom(OWLClassExpression classA, OWLClassExpression classB) {
        return manager.getOWLDataFactory().getOWLSubClassOfAxiom(classA, classB);
    }


    @Override
    public boolean runTaskAndGetResult(String message) {
        Runnable work;
        if (OllamaWorkload.supportedModels.contains(model)) {
            work = new OllamaWorkload(model, system, message, maxTokens);
        } else if (OpenAIWorkload.supportedModels.contains(model)) {
            work = new OpenAIWorkload(model, system, message, maxTokens);
        } else {
            throw new IllegalStateException("Invalid model.");
        }
        Task task = new ExperimentTask("QUERIYNG", model, ontology, message, system, work);
        Environment.run(task);
        return new Result(task.getFileName()).isTrue();
    }

    @Override
    public Set<OWLClass> getClassesInSignature() {
        return parser.getOwl().getClassesInSignature();
    }

    @Override
    public boolean entailed(OWLAxiom a) {
        return false;
    }

    @Override
    public boolean entailed(Set<OWLAxiom> s) {
        return false;
    }




}
