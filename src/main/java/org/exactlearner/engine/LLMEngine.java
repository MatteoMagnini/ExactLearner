package org.exactlearner.engine;

import org.analysis.OntologyManipulator;
import org.analysis.Result;
import org.exactlearner.parser.OWLParserImpl;
import org.experiments.Environment;
import org.experiments.task.ExperimentTask;
import org.experiments.task.Task;
import org.experiments.workload.OllamaWorkload;
import org.experiments.workload.OpenAIWorkload;
import org.semanticweb.elk.owlapi.ElkReasonerFactory;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import uk.ac.manchester.cs.owl.owlapi.mansyntaxrenderer.ManchesterOWLSyntaxOWLObjectRendererImpl;

import java.util.ArrayList;
import java.util.Set;

public class LLMEngine implements BaseEngine {
    private final String ontology;
    private final String model;
    private final String system;
    private final Integer maxTokens;
    private final OWLOntologyManager manager;
    private final OWLParserImpl parser;
    private final OWLReasoner reasoner;

    public LLMEngine(String ontology, String model, String system, Integer maxTokens) {
        this.ontology = ontology;
        this.system = system;
        this.model = model;
        this.maxTokens = maxTokens;
        this.manager = OWLManager.createOWLOntologyManager();
        this.parser = OntologyManipulator.getParser(ontology);
        this.reasoner = new ElkReasonerFactory().createReasoner(parser.getOwl());
    }

    public Boolean isSubClassOf(OWLClass classA, OWLClass classB) {
        var classes = new ArrayList<OWLClass>();
        classes.add(classA);
        classes.add(classB);
        var classesAsString = classes.stream()
                .map(OWLClass::toString)
                .map(s -> s.substring(s.indexOf("#") + 1, s.length() - 1)).toList();

        return runTaskAndGetResult(classesAsString.get(0) + "SubClassOf: " + classesAsString.get(1));
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
        Task task = new ExperimentTask("statementsQuerying", model, ontology, message, system, work);
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

    private Boolean entailedSubclass(OWLSubClassOfAxiom subclassAxiom) {

        OWLClassExpression left = subclassAxiom.getSubClass();
        OWLClassExpression right = subclassAxiom.getSuperClass();
        boolean workaround = false;

        OWLClass leftName;
        if (left.isAnonymous()) {
            leftName = manager.getOWLDataFactory().getOWLClass(IRI.create("#temp001"));
        } else {
            leftName = left.asOWLClass();
        }

        OWLClass rightName;
        if (right.isAnonymous()) {
            rightName = manager.getOWLDataFactory().getOWLClass(IRI.create("#temp002"));
        } else {
            rightName = right.asOWLClass();
        }

        String sx = leftName.toString().substring(leftName.toString().indexOf("#") + 1, leftName.toString().length() - 1);
        String dx = rightName.toString().substring(rightName.toString().indexOf("#") + 1, rightName.toString().length() - 1);

        workaround = runTaskAndGetResult(sx + "SubClassOf: " + dx);
        return workaround;
    }

    public Boolean entailed(OWLAxiom ax) {
        if (ax.isOfType(AxiomType.EQUIVALENT_CLASSES)) {
            OWLEquivalentClassesAxiom eax = (OWLEquivalentClassesAxiom) ax;
            for (OWLSubClassOfAxiom sax : eax.asOWLSubClassOfAxioms()) {
                if (!entailedSubclass(sax)) {
                    return false;
                }
            }
            return true;
        }

        if (ax.isOfType(AxiomType.SUBCLASS_OF)) {
            ManchesterOWLSyntaxOWLObjectRendererImpl renderer = new ManchesterOWLSyntaxOWLObjectRendererImpl();
            var query = renderer.render(ax).replaceAll("\r", " ").replaceAll("\n", " ");
            return runTaskAndGetResult(query);
        }

        throw new RuntimeException("Axiom type not supported " + ax.toString());

    }

    public Boolean entailed(Set<OWLAxiom> axioms) {
        for (OWLAxiom ax : axioms) {
            if (!entailed(ax)) {
                return false;
            }
        }
        return true;
    }

    public OWLOntology getOntology() {
        return parser.getOwl();
    }
}
