package org.exactlearner.engine;

import org.exactlearner.parser.OWLParserImpl;
import org.experiments.Environment;
import org.experiments.Result;
import org.experiments.task.ExperimentTask;
import org.experiments.task.Task;
import org.experiments.workload.OllamaWorkload;
import org.experiments.workload.OpenAIWorkload;
import org.experiments.workload.WorkloadManager;
import org.experiments.workload.WorkloadManagerImpl;
import org.semanticweb.elk.owlapi.ElkReasonerFactory;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.NodeSet;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import uk.ac.manchester.cs.owl.owlapi.OWLDataFactoryImpl;
import uk.ac.manchester.cs.owl.owlapi.mansyntaxrenderer.ManchesterOWLSyntaxOWLObjectRendererImpl;

import java.util.Arrays;
import java.util.Set;

public class LLMEngine implements BaseEngine {

    private final OWLOntologyManager manager;
    private final OWLParserImpl parser;
    private final OWLReasoner reasoner;
    private final WorkloadManager workloadManager;


    public LLMEngine(OWLOntology ontology, String ontologyName, String model, String system, Integer maxTokens, OWLOntologyManager manager) {
        this.manager = manager;
        this.parser = new OWLParserImpl(ontology);
        this.reasoner = new ElkReasonerFactory().createReasoner(parser.getOwl());
        String queryFormat = "";
        this.workloadManager = new WorkloadManagerImpl(model, system, maxTokens, queryFormat, ontologyName);
    }

    public LLMEngine(OWLOntology ontology, OWLOntologyManager manager, WorkloadManager workloadManager) {
        this.manager = manager;
        this.parser = new OWLParserImpl(ontology);
        this.reasoner = new ElkReasonerFactory().createReasoner(parser.getOwl());
        this.workloadManager = workloadManager;
    }

    @Override
    public OWLSubClassOfAxiom getSubClassAxiom(OWLClassExpression classA, OWLClassExpression classB) {
        return manager.getOWLDataFactory().getOWLSubClassOfAxiom(classA, classB);
    }


    protected Boolean runTaskAndGetResult(String message) {
        message = message.replace("  ", " ");
        return workloadManager.runWorkload(message);
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
                if (!entailed(sax)) {
                    return false;
                }
            }
            return true;
        }

        if (ax.isOfType(AxiomType.SUBCLASS_OF)) {
            return entailed((OWLSubClassOfAxiom) ax);
        }

        throw new RuntimeException("Axiom type not supported " + ax);

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

    private Boolean entailed(OWLSubClassOfAxiom axiom) {
        ManchesterOWLSyntaxOWLObjectRendererImpl renderer = new ManchesterOWLSyntaxOWLObjectRendererImpl();
        if (axiom.getSuperClass() instanceof OWLObjectIntersectionOf intersection) {
            OWLClassExpression expression = axiom.getSubClass();
            for (OWLClassExpression sup : intersection.getOperands()) {
                OWLSubClassOfAxiom ax = getSubClassAxiom(expression, sup);
                String query = renderer.render(ax).replaceAll("\r", " ").replaceAll("\n", " ");
                if (!runTaskAndGetResult(query)) {
                    return false;
                }
            }
            return true;
        }
        var query = renderer.render(axiom).replaceAll("\r", " ").replaceAll("\n", " ");
        return runTaskAndGetResult(query);
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
