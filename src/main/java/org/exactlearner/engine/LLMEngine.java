package org.exactlearner.engine;

import org.exactlearner.parser.OWLParserImpl;
import org.experiments.Environment;
import org.experiments.Result;
import org.experiments.task.ExperimentTask;
import org.experiments.task.Task;
import org.experiments.workload.OllamaWorkload;
import org.experiments.workload.OpenAIWorkload;
import org.semanticweb.elk.owlapi.ElkReasonerFactory;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.NodeSet;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import uk.ac.manchester.cs.owl.owlapi.mansyntaxrenderer.ManchesterOWLSyntaxOWLObjectRendererImpl;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Set;

public class LLMEngine implements BaseEngine {
    private final OWLOntology ontology;
    private String ontologyName = "";
    private final String model;
    private final String system;
    private final Integer maxTokens;
    private final OWLOntologyManager manager;
    private final OWLParserImpl parser;
    private final OWLReasoner reasoner;

    public LLMEngine(OWLOntology ontology, String model, String system, Integer maxTokens, OWLOntologyManager manager) {
        this.ontology = ontology;
        this.system = system;
        this.model = model;
        this.maxTokens = maxTokens;
        this.manager = manager;
        this.parser = new OWLParserImpl(ontology);
        this.reasoner = new ElkReasonerFactory().createReasoner(parser.getOwl());
    }

    public LLMEngine(String ontology, String model, String system, Integer maxTokens, OWLOntologyManager manager) {
        this.ontologyName = ontology;
        this.system = system;
        this.model = model;
        this.maxTokens = maxTokens;
        this.manager = manager;
        this.parser = new OWLParserImpl(ontologyName, manager);
        this.ontology = parser.getOwl();
        this.reasoner = new ElkReasonerFactory().createReasoner(parser.getOwl());
    }

    @Override
    public OWLSubClassOfAxiom getSubClassAxiom(OWLClassExpression classA, OWLClassExpression classB) {
        return manager.getOWLDataFactory().getOWLSubClassOfAxiom(classA, classB);
    }


    public Boolean runTaskAndGetResult(String message) {
        Runnable work;
        if (OllamaWorkload.supportedModels.contains(model)) {
            work = new OllamaWorkload(model, system, message, maxTokens);
        } else if (OpenAIWorkload.supportedModels.contains(model)) {
            work = new OpenAIWorkload(model, system, message, maxTokens);
        } else {
            throw new IllegalStateException("Invalid model.");
        }
        Task task = new ExperimentTask("statementsQuerying", model, ontologyName, message, system, work);
        Environment.run(task);

        return new Result(task.getFileName()).isTrue();

    }

    @Override
    public Set<OWLClass> getClassesInSignature() {
        return parser.getOwl().getClassesInSignature();
    }

    @Override
    public OWLEquivalentClassesAxiom getOWLEquivalentClassesAxiom(OWLClassExpression concept1, OWLClassExpression concept2) {
        return manager.getOWLDataFactory().getOWLEquivalentClassesAxiom(concept1, concept2);
    }

    @Override
    public OWLClassExpression getOWLObjectIntersectionOf(Set<OWLClassExpression> mySet) {
        return manager.getOWLDataFactory().getOWLObjectIntersectionOf(mySet);
    }

    public Set<OWLClass> getSuperClasses(OWLClassExpression superclass, boolean direct) {
        NodeSet<OWLClass> superClasses = reasoner.getSuperClasses(superclass, direct);
        return superClasses.getFlattened();
    }

    @Override
    public Boolean entailed(OWLAxiom ax) {
        ManchesterOWLSyntaxOWLObjectRendererImpl renderer = new ManchesterOWLSyntaxOWLObjectRendererImpl();
        if (ax.isOfType(AxiomType.EQUIVALENT_CLASSES)) {
            OWLEquivalentClassesAxiom eax = (OWLEquivalentClassesAxiom) ax;
            for (OWLSubClassOfAxiom sax : eax.asOWLSubClassOfAxioms()) {
                var query = renderer.render(sax).replaceAll("\r", " ").replaceAll("\n", " ");
                if (!runTaskAndGetResult(query)) {
                    return false;
                }
            }
            return true;
        }

        if (ax.isOfType(AxiomType.SUBCLASS_OF)) {
            //return entailedSubclass((OWLSubClassOfAxiom) ax);
            var query = renderer.render(ax).replaceAll("\r", " ").replaceAll("\n", " ");
            var bool = runTaskAndGetResult(query);
            return bool;
        }

        //throw new RuntimeException("Axiom type not supported " + ax.toString());
        return false;

    }

    @Override
    public Boolean entailed(Set<OWLAxiom> axioms) {
        for (OWLAxiom ax : axioms) {
            if (!entailed(ax)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public OWLOntology getOntology() {
        return parser.getOwl();
    }

    @Override
    public void disposeOfReasoner() {
        System.out.flush();
        reasoner.dispose();
    }

    @Override
    public void applyChange(OWLOntologyChange change) {
        manager.applyChange(change);
    }
}
